package io.github.gvergine.ifsx.core.model;

/** Regular file: executables, libraries, config files. */
public class IfsFileEntry extends IfsEntry {

    private String elfInfo;

    @Override public EntryType getType() { return EntryType.FILE; }

    public String getElfInfo() { return elfInfo; }
    public void setElfInfo(String v) { this.elfInfo = v; }
    public boolean isElf() { return elfInfo != null && !elfInfo.isEmpty(); }
    public boolean isEmpty() { return getSizeBytes() == 0; }
}
