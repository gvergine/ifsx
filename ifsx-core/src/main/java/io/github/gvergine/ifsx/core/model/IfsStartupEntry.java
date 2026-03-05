package io.github.gvergine.ifsx.core.model;

/** The startup.* binary -- the IPL hands off to this. */
public class IfsStartupEntry extends IfsEntry {

    private String vaddr;

    @Override public EntryType getType() { return EntryType.STARTUP; }

    public String getVaddr() { return vaddr; }
    public void setVaddr(String v) { this.vaddr = v; }
}
