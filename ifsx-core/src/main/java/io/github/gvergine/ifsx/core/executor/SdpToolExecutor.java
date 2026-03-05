package io.github.gvergine.ifsx.core.executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** ProcessBuilder wrapper for calling dumpifs and mkifs. */
public class SdpToolExecutor {

    private static final String DUMPIFS = "dumpifs";
    private static final String MKIFS   = "mkifs";

    /**
     * Checks whether {@code dumpifs} and {@code mkifs} are available on PATH.
     *
     * @return an {@link Optional} containing a warning message if one or both
     *         tools are missing, or empty if both are found.
     */
    public static Optional<String> checkSdpTools() {
        boolean hasDumpifs = isOnPath(DUMPIFS);
        boolean hasMkifs   = isOnPath(MKIFS);
        if (!hasDumpifs || !hasMkifs) {
            return Optional.of(
                "Warning: dumpifs and/or mkifs not found. Did you source the SDP?");
        }
        return Optional.empty();
    }

    private static boolean isOnPath(String executable) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return false;
        for (String dir : pathEnv.split(File.pathSeparator)) {
            if (new File(dir, executable).canExecute()) return true;
        }
        return false;
    }

    /** Run dumpifs -vvvvv for metadata, including startup-script body. */
    public String runDumpIfsVerbose(Path ifsPath) throws IOException {
        return runDumpIfsVerbose(ifsPath, null);
    }

    public String runDumpIfsVerbose(Path ifsPath, Consumer<String> lineConsumer) throws IOException {
        return runDumpIfs(lineConsumer, "-vvvvv", ifsPath.toString());
    }

    public String runDumpIfsExtract(Path ifsPath, Path outputDir) throws IOException {
        return runDumpIfsExtract(ifsPath, outputDir, null);
    }

    public String runDumpIfsExtract(Path ifsPath, Path outputDir, Consumer<String> lineConsumer) throws IOException {
        return runDumpIfs(lineConsumer, "-x", "-d", outputDir.toString(), ifsPath.toString());
    }

    /** Extract a single file from an IFS image to outputDir, preserving its directory structure. */
    public void runDumpIfsExtractFile(Path ifsPath, Path outputDir, String entryPath) throws IOException {
        runDumpIfs((Consumer<String>) null, "-x", "-d", outputDir.toString(), ifsPath.toString(), "-f", entryPath);
    }

    public String runDumpIfs(String... args) throws IOException {
        return runDumpIfs(null, args);
    }

    public String runDumpIfs(Consumer<String> lineConsumer, String... args) throws IOException {
        return execute(null, lineConsumer, DUMPIFS, args);
    }

    public String runMkIfs(String... args) throws IOException {
        return execute(null, null, MKIFS, prepend("-v", args));
    }

    public String runMkIfsInDir(Path workDir, String... args) throws IOException {
        return runMkIfsInDir(workDir, null, prepend("-v", args));
    }

    public String runMkIfsInDir(Path workDir, Consumer<String> lineConsumer, String... args) throws IOException {
        return execute(workDir, lineConsumer, MKIFS, prepend("-v", args));
    }

    private static String[] prepend(String first, String[] rest) {
        String[] result = new String[rest.length + 1];
        result[0] = first;
        System.arraycopy(rest, 0, result, 1, rest.length);
        return result;
    }

    private String execute(Path workDir, Consumer<String> lineConsumer,
                           String tool, String... args) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(tool);
        for (String a : args) cmd.add(a);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (workDir != null) {
            pb.directory(workDir.toFile());
            pb.environment().put("MKIFS_PATH", workDir.toAbsolutePath().toString());
        }
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line).append('\n');
                if (lineConsumer != null) lineConsumer.accept(line);
            }
        }
        try {
            int rc = proc.waitFor();
            if (rc != 0)
                throw new IOException(
                    tool + " exited with code " + rc + ". Output:\n" + output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted waiting for " + tool, e);
        }
        return output.toString();
    }
}
