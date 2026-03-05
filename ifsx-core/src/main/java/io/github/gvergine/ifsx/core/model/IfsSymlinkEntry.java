package io.github.gvergine.ifsx.core.model;

/** Symbolic link. Parsed from "name -> target" in dumpifs output. */
public class IfsSymlinkEntry extends IfsEntry {

    private String target;

    @Override public EntryType getType() { return EntryType.SYMLINK; }

    public String getTarget() { return target; }
    public void setTarget(String t) { this.target = t; }

    @Override public String toString() {
        return String.format("[SYMLINK] %s -> %s", getPath(), target);
    }
}
