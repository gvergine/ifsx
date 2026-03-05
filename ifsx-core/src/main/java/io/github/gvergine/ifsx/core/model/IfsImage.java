package io.github.gvergine.ifsx.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete in-memory representation of a parsed IFS image. */
public class IfsImage {

    private StartupHeader startupHeader;
    private ImageHeader imageHeader;
    private ImageTrailer trailer;
    private FileAttributes rootAttributes;
    private final List<IfsEntry> entries = new ArrayList<>();

    public StartupHeader getStartupHeader() { return startupHeader; }
    public void setStartupHeader(StartupHeader v) { this.startupHeader = v; }
    public ImageHeader getImageHeader() { return imageHeader; }
    public void setImageHeader(ImageHeader v) { this.imageHeader = v; }
    public ImageTrailer getTrailer() { return trailer; }
    public void setTrailer(ImageTrailer v) { this.trailer = v; }
    public FileAttributes getRootAttributes() { return rootAttributes; }
    public void setRootAttributes(FileAttributes v) { this.rootAttributes = v; }

    public List<IfsEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void addEntry(IfsEntry entry) {
        entry.setOrderIndex(entries.size());
        entries.add(entry);
    }

    public List<IfsEntry> findByPath(String path) {
        return entries.stream().filter(e -> path.equals(e.getPath())).toList();
    }

    public <T extends IfsEntry> List<T> getEntriesByType(Class<T> type) {
        return entries.stream().filter(type::isInstance).map(type::cast).toList();
    }

    public IfsScriptEntry getStartupScript() {
        return getEntriesByType(IfsScriptEntry.class).stream()
            .findFirst().orElse(null);
    }
}
