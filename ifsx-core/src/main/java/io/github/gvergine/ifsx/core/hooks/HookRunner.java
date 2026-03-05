package io.github.gvergine.ifsx.core.hooks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Discovers and executes user-defined hook executables from ~/.ifsx/<phase>/.
 *
 * Hooks are called with two arguments: <ifsPath> <directory>.
 * stdout and stderr are merged and streamed to the provided lineConsumer.
 * A non-zero exit code causes an IOException, stopping the chain.
 */
public class HookRunner {

    public static final String PRE_EXTRACT  = "pre-extract";
    public static final String POST_EXTRACT = "post-extract";
    public static final String PRE_PACK     = "pre-pack";
    public static final String POST_PACK    = "post-pack";

    /**
     * Creates ~/.ifsx/ and all four phase subdirectories if they do not already exist.
     * Silently ignores failures (e.g. read-only home directory).
     */
    public static void ensureDirectories() {
        for (String phase : new String[]{PRE_EXTRACT, POST_EXTRACT, PRE_PACK, POST_PACK}) {
            try {
                Files.createDirectories(hooksDir(phase));
            } catch (IOException ignored) {
            }
        }
    }

    /** Returns executable file names in ~/.ifsx/<phase>/, sorted alphabetically. */
    public static List<String> availableHooks(String phase) {
        Path dir = hooksDir(phase);
        if (!Files.isDirectory(dir)) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(p -> !Files.isDirectory(p) && Files.isExecutable(p))
                  .map(p -> p.getFileName().toString())
                  .sorted()
                  .forEach(names::add);
        } catch (IOException e) {
            // Best-effort scan; return what was found so far
        }
        return names;
    }

    /**
     * Runs the named hooks for the given phase in order.
     * Each hook receives ifsPath and directory as positional arguments.
     *
     * @throws IOException if any hook exits with a non-zero code
     */
    public void runHooks(String phase, List<String> hookNames,
                         Path ifsPath, Path directory,
                         Consumer<String> lineConsumer) throws IOException {
        if (hookNames.isEmpty()) return;
        Path dir = hooksDir(phase);
        for (String name : hookNames) {
            runHook(dir.resolve(name), ifsPath, directory, lineConsumer);
        }
    }

    private void runHook(Path hook, Path ifsPath, Path directory,
                         Consumer<String> lineConsumer) throws IOException {
        emit(lineConsumer, "[hook] " + hook.getFileName()
            + "  " + ifsPath.toAbsolutePath()
            + "  " + directory.toAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(
            hook.toAbsolutePath().toString(),
            ifsPath.toAbsolutePath().toString(),
            directory.toAbsolutePath().toString());
        pb.redirectErrorStream(true);

        Process proc = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line).append('\n');
                emit(lineConsumer, line);
            }
        }
        try {
            int rc = proc.waitFor();
            if (rc != 0) {
                throw new IOException(
                    "Hook '" + hook.getFileName() + "' exited with code " + rc
                    + (output.isEmpty() ? "" : ".\nOutput:\n" + output));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted waiting for hook " + hook.getFileName(), e);
        }
        emit(lineConsumer, "[hook] " + hook.getFileName() + " ok.");
    }

    private static Path hooksDir(String phase) {
        return Path.of(System.getProperty("user.home"), ".ifsx", phase);
    }

    private static void emit(Consumer<String> consumer, String message) {
        if (consumer != null) consumer.accept(message);
    }
}
