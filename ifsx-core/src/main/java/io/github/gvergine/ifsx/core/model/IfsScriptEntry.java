package io.github.gvergine.ifsx.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Startup script with inline content dumped by dumpifs -v. */
public class IfsScriptEntry extends IfsEntry {

    private final List<String> scriptLines = new ArrayList<>();

    @Override public EntryType getType() { return EntryType.SCRIPT; }

    public List<String> getScriptLines() {
        return Collections.unmodifiableList(scriptLines);
    }
    public void addScriptLine(String line) { scriptLines.add(line); }

    public String getScriptContent() {
        return String.join(System.lineSeparator(), scriptLines);
    }
}
