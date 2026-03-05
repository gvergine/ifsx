package io.github.gvergine.ifsx.core.pack;

import io.github.gvergine.ifsx.core.executor.SdpToolExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Repacks a previously extracted IFS directory back into an IFS image.
 *
 * Expects the directory to contain a _ifsx.build buildfile produced by
 * IfsExtractor. Invokes mkifs with that buildfile from within the extracted
 * directory so that relative file paths in the buildfile resolve correctly.
 */
public class IfsPacker {

    private final SdpToolExecutor executor;

    public IfsPacker() {
        this.executor = new SdpToolExecutor();
    }

    /**
     * @param extractedDir directory produced by ifsx extract (must contain _ifsx.build)
     * @param outputIfs    path for the resulting IFS image
     */
    public void pack(Path extractedDir, Path outputIfs) throws IOException {
        pack(extractedDir, outputIfs, null);
    }

    public void pack(Path extractedDir, Path outputIfs, Consumer<String> lineConsumer) throws IOException {
        Path bldFile = extractedDir.resolve("_ifsx.build");
        if (!Files.exists(bldFile)) {
            throw new IOException(
                "No _ifsx.build found in " + extractedDir
                + ". Run 'ifsx extract' first.");
        }
        if (outputIfs.getParent() != null) {
            Files.createDirectories(outputIfs.getParent());
        }
        executor.runMkIfsInDir(extractedDir, lineConsumer,
            bldFile.getFileName().toString(),
            outputIfs.toAbsolutePath().toString());
    }
}
