package io.github.gvergine.ifsx.cli;

import io.github.gvergine.ifsx.core.executor.SdpToolExecutor;
import io.github.gvergine.ifsx.core.hooks.HookRunner;
import picocli.CommandLine;

@CommandLine.Command(
    name = "ifsx",
    description = "IFS Extract/Repack Tool",
    mixinStandardHelpOptions = true,
    version = "ifsx 0.1.0",
    subcommands = { ExtractCommand.class, PackCommand.class })
public class Main implements Runnable {

    @Override
    public void run() { CommandLine.usage(this, System.out); }

    public static void main(String[] args) {
        HookRunner.ensureDirectories();
        SdpToolExecutor.checkSdpTools().ifPresent(System.err::println);
        int rc = new CommandLine(new Main()).execute(args);
        System.exit(rc);
    }
}
