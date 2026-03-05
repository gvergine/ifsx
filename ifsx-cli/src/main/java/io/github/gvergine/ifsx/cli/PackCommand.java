package io.github.gvergine.ifsx.cli;

import io.github.gvergine.ifsx.core.hooks.HookRunner;
import io.github.gvergine.ifsx.core.pack.IfsPacker;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "pack", aliases = {"p"},
    description = "Repack an extracted IFS directory back into an IFS image",
    mixinStandardHelpOptions = true)
public class PackCommand implements Callable<Integer> {

    @Parameters(index = "0",
        description = "Extracted directory (must contain _ifsx.build)")
    private Path extractedDir;

    @Parameters(index = "1", defaultValue = "",
        description = "Output IFS image path (default: <dir>.ifs)")
    private String outputIfs;

    @Option(names = "--pre-pack",
            description = "Name of a hook in ~/.ifsx/pre-pack/ to run before packing (repeatable)",
            paramLabel = "<hook>")
    private List<String> prePackHooks = new ArrayList<>();

    @Option(names = "--post-pack",
            description = "Name of a hook in ~/.ifsx/post-pack/ to run after packing (repeatable)",
            paramLabel = "<hook>")
    private List<String> postPackHooks = new ArrayList<>();

    @Override
    public Integer call() {
        try {
            Path output = outputIfs.isBlank()
                ? Path.of(extractedDir.toAbsolutePath() + ".ifs")
                : Path.of(outputIfs);

            HookRunner hooks = new HookRunner();
            hooks.runHooks(HookRunner.PRE_PACK, prePackHooks,
                output, extractedDir, System.out::println);

            new IfsPacker().pack(extractedDir, output, System.out::println);
            System.out.println("Packed: " + output);

            hooks.runHooks(HookRunner.POST_PACK, postPackHooks,
                output, extractedDir, System.out::println);

            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
