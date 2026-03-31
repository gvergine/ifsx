package io.github.gvergine.ifsx.cli;

import io.github.gvergine.ifsx.core.executor.SdpToolExecutor;
import io.github.gvergine.ifsx.core.hooks.HookRunner;
import picocli.CommandLine;

@CommandLine.Command(
    name = "ifsx",
    description = "IFS Extract/Repack Tool",
    mixinStandardHelpOptions = true,
    versionProvider = Main.VersionProvider.class,
    subcommands = { ExtractCommand.class, PackCommand.class })
public class Main implements Runnable {

    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            String v = Main.class.getPackage().getImplementationVersion();
            return new String[]{ "ifsx " + (v != null ? v : "unknown") };
        }
    }

    @Override
    public void run() { CommandLine.usage(this, System.out); }

    public static void main(String[] args) {
        HookRunner.ensureDirectories();
        SdpToolExecutor.checkSdpTools().ifPresent(System.err::println);
        int rc = new CommandLine(new Main()).execute(args);
        System.exit(rc);
    }
}
