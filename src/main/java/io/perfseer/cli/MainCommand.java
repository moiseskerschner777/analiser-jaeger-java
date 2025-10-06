package io.perfseer.cli;

import io.perfseer.config.PerfConfig;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import picocli.CommandLine;

@CommandLine.Command(name = "perfseer", mixinStandardHelpOptions = true,
        subcommands = {FetchCommand.class, FeaturesCommand.class, ScoreCommand.class, AnalyzeCommand.class})
@Dependent
public class MainCommand implements Runnable {
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Inject PerfConfig config;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }

}
