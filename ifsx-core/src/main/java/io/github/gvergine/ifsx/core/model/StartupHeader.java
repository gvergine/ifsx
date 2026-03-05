package io.github.gvergine.ifsx.core.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Startup header metadata from the "Startup-header" block. */
public class StartupHeader {

    private String flags1;
    private String flags2;
    private String paddrBias;
    private boolean virtual;
    private boolean littleEndian;
    private String compress;
    private String prebootSize;
    private String imagePaddr;
    private String storedSize;
    private String startupSize;
    private String imagefsSize;
    private String ramPaddr;
    private String ramSize;
    private String startupVaddr;
    private String addrOff;
    private final Map<String, String> extra = new LinkedHashMap<>();

    public String getFlags1() { return flags1; }
    public void setFlags1(String v) { this.flags1 = v; }
    public String getFlags2() { return flags2; }
    public void setFlags2(String v) { this.flags2 = v; }
    public String getPaddrBias() { return paddrBias; }
    public void setPaddrBias(String v) { this.paddrBias = v; }
    public boolean isVirtual() { return virtual; }
    public void setVirtual(boolean v) { this.virtual = v; }
    public boolean isLittleEndian() { return littleEndian; }
    public void setLittleEndian(boolean v) { this.littleEndian = v; }
    public String getCompress() { return compress; }
    public void setCompress(String v) { this.compress = v; }
    public String getPrebootSize() { return prebootSize; }
    public void setPrebootSize(String v) { this.prebootSize = v; }
    public String getImagePaddr() { return imagePaddr; }
    public void setImagePaddr(String v) { this.imagePaddr = v; }
    public String getStoredSize() { return storedSize; }
    public void setStoredSize(String v) { this.storedSize = v; }
    public String getStartupSize() { return startupSize; }
    public void setStartupSize(String v) { this.startupSize = v; }
    public String getImagefsSize() { return imagefsSize; }
    public void setImagefsSize(String v) { this.imagefsSize = v; }
    public String getRamPaddr() { return ramPaddr; }
    public void setRamPaddr(String v) { this.ramPaddr = v; }
    public String getRamSize() { return ramSize; }
    public void setRamSize(String v) { this.ramSize = v; }
    public String getStartupVaddr() { return startupVaddr; }
    public void setStartupVaddr(String v) { this.startupVaddr = v; }
    public String getAddrOff() { return addrOff; }
    public void setAddrOff(String v) { this.addrOff = v; }
    public Map<String, String> getExtra() { return extra; }
    public void putExtra(String k, String v) { extra.put(k, v); }
}
