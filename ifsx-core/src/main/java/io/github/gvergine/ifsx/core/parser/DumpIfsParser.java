package io.github.gvergine.ifsx.core.parser;

import io.github.gvergine.ifsx.core.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Line-oriented state machine that turns dumpifs -vvvvv output into an
 * IfsImage model. Designed for 5v verbosity, which includes the startup-script
 * body. Extra ELF segment lines simply do not match any pattern and fall
 * through silently.
 *
 * States:
 *   INITIAL        -- scanning for the "Offset Size Entry Name" header
 *   ENTRIES        -- main loop: entries, attributes, ELF tags, script body
 *   STARTUP_HEADER -- collecting startup-header key=value detail lines
 *   IMAGE_HEADER   -- collecting the image-header flags line
 *   TRAILER        -- collecting SHA/cksum lines at the end
 */
public class DumpIfsParser {

    private enum State {
        INITIAL,
        ENTRIES,
        STARTUP_HEADER,
        IMAGE_HEADER,
        TRAILER
    }

    //  400000      200        0 *.boot
    //    ----     ----     ---- bin/sh -> /proc/boot/ksh
    private static final Pattern ENTRY_LINE = Pattern.compile(
        "^\\s*([0-9a-fA-F]+|-{4})\\s+([0-9a-fA-F]+|-{4})\\s+([0-9a-fA-F]+|-{4})\\s+(.+)$");

    // gid=0 uid=0 mode=0755 ino=1 mtime=69963e27
    private static final Pattern ATTR_LINE = Pattern.compile(
        "^\\s+gid=(\\d+)\\s+uid=(\\d+)\\s+mode=([0-7]+)\\s+ino=(\\d+)\\s+mtime=([0-9a-fA-F]+)$");

    // ----- procnto-smp-instr - ELF64LE ET_EXEC EM_X86_64 -----
    // Indented in 2v, column 0 in 5v -- handle both.
    private static final Pattern ELF_TAG = Pattern.compile(
        "^\\s*-{5}\\s+(.+?)\\s+-{5}$");

    // Startup-header flags1=0x21 flags2=0 paddr_bias=0
    private static final Pattern STARTUP_HDR = Pattern.compile(
        ".*Startup-header\\s+flags1=(\\S+)\\s+flags2=(\\S+)\\s+paddr_bias=(\\S+).*");

    // Image-header mountpoint=/
    private static final Pattern IMAGE_HDR = Pattern.compile(
        ".*Image-header\\s+mountpoint=(\\S+).*");

    // flags=0x1c script=3 boot=2684354562 mntflg=0
    private static final Pattern IMG_FLAGS = Pattern.compile(
        "^\\s+flags=(\\S+)\\s+script=(\\S+)\\s+boot=(\\S+)\\s+mntflg=(\\S+)$");

    // Offset  Size  Entry  Name
    private static final Pattern HEADER_LINE = Pattern.compile(
        "^\\s*Offset\\s+Size\\s+Entry\\s+Name.*$");

    // Image SHA512=... / Startup SHA512=...
    private static final Pattern TRAILER_SHA = Pattern.compile(
        "^(Image|Startup)\\s+SHA512=(.+)$");

    // Image cksum=... / Startup cksum=...
    private static final Pattern TRAILER_CKSUM = Pattern.compile(
        "^(Image|Startup)\\s+cksum=(\\S+)$");


    // ---- public entry points ----

    public IfsImage parse(Path file) throws IOException {
        try (BufferedReader r = Files.newBufferedReader(file)) {
            return doParse(r);
        }
    }

    public IfsImage parse(String text) throws IOException {
        try (BufferedReader r = new BufferedReader(new StringReader(text))) {
            return doParse(r);
        }
    }


    // ---- state machine ----

    private IfsImage doParse(BufferedReader reader) throws IOException {
        IfsImage image = new IfsImage();
        State state = State.INITIAL;
        IfsEntry current = null;
        String line;

        while ((line = reader.readLine()) != null) {
            switch (state) {

            case INITIAL:
                if (HEADER_LINE.matcher(line).matches()) {
                    state = State.ENTRIES;
                }
                break;

            case STARTUP_HEADER:
                Matcher shEntry = ENTRY_LINE.matcher(line);
                if (shEntry.matches()) {
                    state = State.ENTRIES;
                    current = createEntry(image, shEntry);
                } else if (image.getStartupHeader() != null) {
                    parseStartupKv(image.getStartupHeader(), line.trim());
                }
                break;

            case IMAGE_HEADER:
                Matcher mf = IMG_FLAGS.matcher(line);
                if (mf.matches() && image.getImageHeader() != null) {
                    ImageHeader ih = image.getImageHeader();
                    ih.setFlags(mf.group(1));
                    ih.setScript(mf.group(2));
                    ih.setBoot(mf.group(3));
                    ih.setMntflg(mf.group(4));
                }
                state = State.ENTRIES;
                break;

            case TRAILER:
                applyTrailer(image, line);
                break;

            case ENTRIES:
                // ELF one-liner? Attach to current file entry.
                Matcher elfMatch = ELF_TAG.matcher(line);
                if (elfMatch.matches()) {
                    if (current instanceof IfsFileEntry f) {
                        f.setElfInfo(elfMatch.group(1).trim());
                    }
                    break;
                }

                // Trailer section?
                if (line.contains("Image-trailer")) {
                    image.setTrailer(new ImageTrailer());
                    state = State.TRAILER;
                    break;
                }

                // Loose checksum lines
                if (TRAILER_SHA.matcher(line).matches()
                        || TRAILER_CKSUM.matcher(line).matches()) {
                    applyTrailer(image, line);
                    break;
                }

                // Entry line?
                Matcher em = ENTRY_LINE.matcher(line);
                if (em.matches()) {
                    current = createEntry(image, em);
                    if (current == null && line.contains("Startup-header")) {
                        state = State.STARTUP_HEADER;
                    } else if (current == null && line.contains("Image-header")) {
                        state = State.IMAGE_HEADER;
                    }
                    break;
                }

                // Attribute line?
                Matcher am = ATTR_LINE.matcher(line);
                if (am.matches()) {
                    FileAttributes attrs = new FileAttributes(
                        Integer.parseInt(am.group(2)),  // uid
                        Integer.parseInt(am.group(1)),  // gid
                        am.group(3),                    // mode
                        Long.parseLong(am.group(4)),    // ino
                        am.group(5));                   // mtime
                    if (current != null) {
                        current.setAttributes(attrs);
                    } else {
                        image.setRootAttributes(attrs);
                    }
                    break;
                }

                // Inline script body
                if (current instanceof IfsScriptEntry script) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        // dumpifs adds spaces inside script attributes: [ +session ] -> [+session]
                        trimmed = trimmed.replaceAll("\\[\\s+\\+", "[+")
                                         .replaceAll("\\s+\\]", "]");
                        script.addScriptLine(trimmed);
                    }
                }
                // Anything else (5v ELF segments, separators) falls through.
                break;
            }
        }

        return image;
    }


    // ---- entry creation ----

    private IfsEntry createEntry(IfsImage image, Matcher m) {
        String offset = m.group(1);
        String size   = m.group(2);
        String entry  = m.group(3);
        String name   = m.group(4).trim();

        if (name.equals("*.boot")) {
            IfsBootRecord e = new IfsBootRecord();
            e.setPath("*.boot"); e.setOffset(offset); e.setSize(size);
            image.addEntry(e); return e;
        }
        if (name.equals("startup.*")) {
            IfsStartupEntry e = new IfsStartupEntry();
            e.setPath("startup.*"); e.setOffset(offset); e.setSize(size);
            e.setVaddr(dash(entry));
            image.addEntry(e); return e;
        }
        if (name.startsWith("Startup-header")) {
            Matcher sh = STARTUP_HDR.matcher(name);
            StartupHeader hdr = new StartupHeader();
            if (sh.matches()) {
                hdr.setFlags1(sh.group(1));
                hdr.setFlags2(sh.group(2));
                hdr.setPaddrBias(sh.group(3));
            }
            image.setStartupHeader(hdr); return null;
        }
        if (name.startsWith("Image-header")) {
            Matcher ih = IMAGE_HDR.matcher(name);
            ImageHeader hdr = new ImageHeader();
            if (ih.matches()) hdr.setMountpoint(ih.group(1));
            image.setImageHeader(hdr); return null;
        }
        if (name.equals("Image-directory") || name.equals("Root-dirent")) {
            return null;
        }
        if (name.equals("Image-trailer")) {
            image.setTrailer(new ImageTrailer()); return null;
        }
        if (name.contains(" -> ")) {
            String[] parts = name.split(" -> ", 2);
            IfsSymlinkEntry e = new IfsSymlinkEntry();
            e.setPath(parts[0].trim()); e.setTarget(parts[1].trim());
            e.setOffset(dash(offset)); e.setSize(dash(size));
            image.addEntry(e); return e;
        }
        if (name.contains("startup-script")) {
            IfsScriptEntry e = new IfsScriptEntry();
            e.setPath(name); e.setOffset(dash(offset)); e.setSize(dash(size));
            image.addEntry(e); return e;
        }
        if (offset.equals("----") && size.equals("----")) {
            IfsDirectoryEntry e = new IfsDirectoryEntry();
            e.setPath(name); e.setOffset(null); e.setSize(null);
            image.addEntry(e); return e;
        }
        IfsFileEntry e = new IfsFileEntry();
        e.setPath(name); e.setOffset(dash(offset)); e.setSize(dash(size));
        image.addEntry(e); return e;
    }


    // ---- startup header parsing ----

    private void parseStartupKv(StartupHeader hdr, String line) {
        if (hdr == null || line.isEmpty()) return;
        if (line.equals("virtual"))       { hdr.setVirtual(true); return; }
        if (line.equals("little-endian")) { hdr.setLittleEndian(true); return; }
        if (line.equals("big-endian"))    { hdr.setLittleEndian(false); return; }

        for (String tok : line.split("\\s+")) {
            int eq = tok.indexOf('=');
            if (eq > 0) {
                applyField(hdr, tok.substring(0, eq), tok.substring(eq + 1));
            }
        }
    }

    private void applyField(StartupHeader hdr, String key, String val) {
        switch (key) {
            case "compress"      -> hdr.setCompress(val);
            case "preboot_size"  -> hdr.setPrebootSize(val);
            case "image_paddr"   -> hdr.setImagePaddr(val);
            case "stored_size"   -> hdr.setStoredSize(val);
            case "startup_size"  -> hdr.setStartupSize(val);
            case "imagefs_size"  -> hdr.setImagefsSize(val);
            case "ram_paddr"     -> hdr.setRamPaddr(val);
            case "ram_size"      -> hdr.setRamSize(val);
            case "startup_vaddr" -> hdr.setStartupVaddr(val);
            case "addr_off"      -> hdr.setAddrOff(val);
            default              -> hdr.putExtra(key, val);
        }
    }


    // ---- trailer parsing ----

    private void applyTrailer(IfsImage image, String line) {
        if (image.getTrailer() == null) image.setTrailer(new ImageTrailer());
        ImageTrailer t = image.getTrailer();

        Matcher sha = TRAILER_SHA.matcher(line);
        if (sha.matches()) {
            if ("Image".equals(sha.group(1))) t.setImageSha512(sha.group(2));
            else                              t.setStartupSha512(sha.group(2));
            return;
        }
        Matcher ck = TRAILER_CKSUM.matcher(line);
        if (ck.matches()) {
            if ("Image".equals(ck.group(1))) t.setImageCksum(ck.group(2));
            else                             t.setStartupCksum(ck.group(2));
        }
    }

    private static String dash(String v) {
        return "----".equals(v) ? null : v;
    }
}
