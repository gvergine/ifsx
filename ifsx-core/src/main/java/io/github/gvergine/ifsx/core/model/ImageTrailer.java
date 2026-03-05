package io.github.gvergine.ifsx.core.model;

/** Image trailer -- SHA-512 hashes and CRC checksums. */
public class ImageTrailer {

    private String imageSha512;
    private String imageCksum;
    private String startupSha512;
    private String startupCksum;

    public String getImageSha512() { return imageSha512; }
    public void setImageSha512(String v) { this.imageSha512 = v; }
    public String getImageCksum() { return imageCksum; }
    public void setImageCksum(String v) { this.imageCksum = v; }
    public String getStartupSha512() { return startupSha512; }
    public void setStartupSha512(String v) { this.startupSha512 = v; }
    public String getStartupCksum() { return startupCksum; }
    public void setStartupCksum(String v) { this.startupCksum = v; }
}
