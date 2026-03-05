package io.github.gvergine.ifsx.gui;

import io.github.gvergine.ifsx.core.model.IfsEntry;

/**
 * Wrapper for TreeView items. Holds a display name and the
 * underlying IfsEntry (null for the root node).
 */
public record IfsEntryItem(String displayName, IfsEntry entry) {

    @Override
    public String toString() {
        return displayName;
    }
}
