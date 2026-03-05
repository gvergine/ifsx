package io.github.gvergine.ifsx.core.model;

/** Directory entry. Offset and size are always null. */
public class IfsDirectoryEntry extends IfsEntry {
    @Override public EntryType getType() { return EntryType.DIRECTORY; }
}
