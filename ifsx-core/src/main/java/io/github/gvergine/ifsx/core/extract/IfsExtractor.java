package io.github.gvergine.ifsx.core.extract;

import io.github.gvergine.ifsx.core.builder.BuildfileGenerator;
import io.github.gvergine.ifsx.core.builder.MetaGenerator;
import io.github.gvergine.ifsx.core.executor.SdpToolExecutor;
import io.github.gvergine.ifsx.core.model.IfsDirectoryEntry;
import io.github.gvergine.ifsx.core.model.IfsEntry;
import io.github.gvergine.ifsx.core.model.IfsFileEntry;
import io.github.gvergine.ifsx.core.model.IfsImage;
import io.github.gvergine.ifsx.core.model.IfsSymlinkEntry;
import io.github.gvergine.ifsx.core.parser.DumpIfsParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Orchestrates IFS extraction end-to-end:
 *   1. dumpifs -vvvvv  -> parse IfsImage model
 *   2. per-entry loop  -> dumpifs -x for each IfsFileEntry (+ boot/startup),
 *                         overwriting duplicates with a warning
 *   3. generate        -> _ifsx.build + _ifsx.meta
 *
 * Entries are processed in offset order so duplicate numbering stays in sync
 * with BuildfileGenerator, which uses the same ordering.
 */
public class IfsExtractor {

    private final SdpToolExecutor executor;
    private final DumpIfsParser parser;
    private final BuildfileGenerator buildfileGen;
    private final MetaGenerator metaGen;

    public IfsExtractor() {
        this.executor     = new SdpToolExecutor();
        this.parser       = new DumpIfsParser();
        this.buildfileGen = new BuildfileGenerator();
        this.metaGen      = new MetaGenerator();
    }

    public IfsImage extract(Path ifsPath, Path outputDir) throws IOException {
        return extract(ifsPath, outputDir, null);
    }

    public IfsImage extract(Path ifsPath, Path outputDir, Consumer<String> lineConsumer) throws IOException {
        Files.createDirectories(outputDir);

        // Phase 1: parse metadata
        emit(lineConsumer, "[1/3] Parsing metadata...");
        String verbose = executor.runDumpIfsVerbose(ifsPath);
        IfsImage image = parser.parse(verbose);
        emit(lineConsumer, "      Parsed " + image.getEntries().size() + " entries.");

        // Phase 2: extract entries one-by-one.
        // Sort by offset so duplicate numbering matches BuildfileGenerator.
        // Entries without an offset (dirs, symlinks) sort first, in original order.
        List<IfsEntry> sorted = image.getEntries().stream()
            .sorted(Comparator
                .comparingLong((IfsEntry e) ->
                    e.getOffset() != null ? Long.parseLong(e.getOffset(), 16) : -1L)
                .thenComparingInt(IfsEntry::getOrderIndex))
            .toList();

        emit(lineConsumer, "[2/3] Extracting files...");
        Map<String, Integer> seen = new HashMap<>();
        for (IfsEntry entry : sorted) {
            extractEntry(ifsPath, outputDir, entry, seen, lineConsumer);
        }

        // Phase 3: write buildfile and meta
        emit(lineConsumer, "[3/3] Writing metadata files...");
        buildfileGen.generate(image, outputDir, outputDir.resolve("_ifsx.build"));
        metaGen.generate(image, outputDir.resolve("_ifsx.meta"));
        emit(lineConsumer, "      Done.");

        return image;
    }

    // ── per-entry dispatch ──────────────────────────────────────────────────

    private void extractEntry(Path ifsPath, Path outputDir, IfsEntry entry,
                               Map<String, Integer> seen, Consumer<String> lineConsumer)
            throws IOException {
        String path = entry.getPath();
        if (path == null) return;

        emit(lineConsumer, "Extracting " + path + " ...");

        if (entry instanceof IfsDirectoryEntry) {
            Files.createDirectories(destPath(outputDir, path));
            emit(lineConsumer, "  d " + path);
            return;
        }

        if (entry instanceof IfsSymlinkEntry sym) {
            createSymlink(outputDir, path, sym.getTarget(), lineConsumer);
            return;
        }

        if (entry instanceof IfsFileEntry) {
            int count = seen.merge(path, 1, Integer::sum);
            extractFileEntry(ifsPath, outputDir, path, count, lineConsumer);
        }

        // Boot record, startup binary, and inline script are all reconstructed
        // by mkifs from the buildfile directives — nothing to extract.
    }

    private void extractFileEntry(Path ifsPath, Path outputDir, String path,
                                   int occurrence, Consumer<String> lineConsumer) throws IOException {
        Path dest = destPath(outputDir, path);

        if (occurrence == 1) {
            // First occurrence — always extract to the canonical destination.
            Files.createDirectories(dest.getParent());
            executor.runDumpIfsExtractFile(ifsPath, outputDir, path);
            fixDotfilePath(dest, lineConsumer);
            emit(lineConsumer, "  + " + path);
            return;
        }

        // Duplicate (occurrence >= 2) — always overwrite, emit a warning.
        emit(lineConsumer, "  WARNING: duplicate entry dup" + occurrence + ": " + path + " — overwriting");
        executor.runDumpIfsExtractFile(ifsPath, outputDir, path);
        fixDotfilePath(dest, lineConsumer);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    /**
     * dumpifs drops the directory separator before dot-prefixed filenames:
     *   proc/boot/.init_script  →  proc/boot.init_script
     * If the expected destination is missing and its name starts with '.',
     * look for the mangled path and move the file to the correct location.
     */
    private static void fixDotfilePath(Path dest, Consumer<String> lineConsumer) throws IOException {
        if (Files.exists(dest)) return;
        String filename = dest.getFileName().toString();
        if (!filename.startsWith(".")) return;
        Path mangled = dest.getParent().getParent()
                           .resolve(dest.getParent().getFileName().toString() + filename);
        if (Files.exists(mangled)) {
            Files.createDirectories(dest.getParent());
            Files.move(mangled, dest);
            emit(lineConsumer, "  (fixed dotfile path: " + mangled.getFileName() + " → " + dest + ")");
        }
    }

    private void createSymlink(Path outputDir, String path, String target,
                                Consumer<String> lineConsumer) {
        try {
            Path dest = destPath(outputDir, path);
            Files.createDirectories(dest.getParent());
            if (!Files.exists(dest, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                Files.createSymbolicLink(dest, Path.of(target));
            }
            emit(lineConsumer, "  l " + path + " -> " + target);
        } catch (IOException | UnsupportedOperationException e) {
            emit(lineConsumer, "  ~ symlink skipped (" + e.getMessage() + "): " + path);
        }
    }

    private static Path destPath(Path outputDir, String entryPath) {
        String stripped = entryPath.startsWith("/") ? entryPath.substring(1) : entryPath;
        return outputDir.resolve(stripped);
    }

    private static void emit(Consumer<String> consumer, String message) {
        if (consumer != null) consumer.accept(message);
    }
}
