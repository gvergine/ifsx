package io.github.gvergine.ifsx.core.model;

/** Image header from the "Image-header" block. */
public class ImageHeader {

    private String mountpoint;
    private String flags;
    private String script;
    private String boot;
    private String mntflg;

    public String getMountpoint() { return mountpoint; }
    public void setMountpoint(String v) { this.mountpoint = v; }
    public String getFlags() { return flags; }
    public void setFlags(String v) { this.flags = v; }
    public String getScript() { return script; }
    public void setScript(String v) { this.script = v; }
    public String getBoot() { return boot; }
    public void setBoot(String v) { this.boot = v; }
    public String getMntflg() { return mntflg; }
    public void setMntflg(String v) { this.mntflg = v; }
}
