package io.github.gvergine.ifsx.core.model;

/**
 * Base class for every entry in an IFS image.
 * Entries are kept in original IFS order -- critical for faithful repack.
 */
public abstract class IfsEntry {

    public enum EntryType { FILE, DIRECTORY, SYMLINK, BOOT_RECORD, STARTUP, SCRIPT }

    private String path;
    private String offset;
    private String size;
    private FileAttributes attributes;
    private int orderIndex;

    public abstract EntryType getType();

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getOffset() { return offset; }
    public void setOffset(String offset) { this.offset = offset; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public FileAttributes getAttributes() { return attributes; }
    public void setAttributes(FileAttributes a) { this.attributes = a; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int i) { this.orderIndex = i; }

    public long getSizeBytes() {
        if (size == null || size.equals("----")) return -1;
        return Long.parseLong(size, 16);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (offset=%s, size=%s)",
            getType(), path, offset, size);
    }
}
