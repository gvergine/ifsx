package io.github.gvergine.ifsx.core.model;

/**
 * POSIX file attributes as reported by dumpifs.
 */
public class FileAttributes {

    private int uid;
    private int gid;
    private String mode;  // octal, e.g. "0755"
    private long ino;
    private String mtime; // hex timestamp

    public FileAttributes() {}

    public FileAttributes(int uid, int gid, String mode, long ino, String mtime) {
        this.uid = uid; this.gid = gid; this.mode = mode;
        this.ino = ino; this.mtime = mtime;
    }

    public int getUid() { return uid; }
    public void setUid(int uid) { this.uid = uid; }
    public int getGid() { return gid; }
    public void setGid(int gid) { this.gid = gid; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public long getIno() { return ino; }
    public void setIno(long ino) { this.ino = ino; }
    public String getMtime() { return mtime; }
    public void setMtime(String mtime) { this.mtime = mtime; }

    @Override
    public String toString() {
        return String.format("uid=%d gid=%d mode=%s ino=%d mtime=%s",
            uid, gid, mode, ino, mtime);
    }
}
