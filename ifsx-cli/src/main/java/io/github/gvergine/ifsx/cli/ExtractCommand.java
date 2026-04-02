package io.github.gvergine.ifsx.cli;

import io.github.gvergine.ifsx.core.extract.IfsExtractor;
import io.github.gvergine.ifsx.core.hooks.HookRunner;
import io.github.gvergine.ifsx.core.model.IfsImage;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "extract", aliases = {"x"},
    description = "Extract an IFS image to a self-contained directory",
    mixinStandardHelpOptions = true)
public class ExtractCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "IFS image file")
    private Path ifsPath;

    @Parameters(index = "1", description = "Output directory")
    private Path outputDir;

    @Option(names = "--pre-extract",
            description = "Name of a hook in ~/.ifsx/hooks/pre-extract/ to run before extraction (repeatable)",
            paramLabel = "<hook>")
    private List<String> preExtractHooks = new ArrayList<>();

    @Option(names = "--post-extract",
            description = "Name of a hook in ~/.ifsx/hooks/post-extract/ to run after extraction (repeatable)",
            paramLabel = "<hook>")
    private List<String> postExtractHooks = new ArrayList<>();

    @Override
    public Integer call() {
        try {
            HookRunner hooks = new HookRunner();
            hooks.runHooks(HookRunner.PRE_EXTRACT, preExtractHooks,
                ifsPath, outputDir, System.out::println);

            IfsExtractor ext = new IfsExtractor();
            IfsImage image = ext.extract(ifsPath, outputDir, System.out::println);
            System.out.printf("Extracted %d entries to %s%n",
                image.getEntries().size(), outputDir);
            System.out.printf("  buildfile: %s%n", outputDir.resolve("_ifsx.build"));
            System.out.printf("  metadata:  %s%n", outputDir.resolve("_ifsx.meta"));

            hooks.runHooks(HookRunner.POST_EXTRACT, postExtractHooks,
                ifsPath, outputDir, System.out::println);

            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
