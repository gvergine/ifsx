package io.github.gvergine.ifsx.core.model;

/** The *.boot record at the very start of the image. */
public class IfsBootRecord extends IfsEntry {
    @Override public EntryType getType() { return EntryType.BOOT_RECORD; }
}
