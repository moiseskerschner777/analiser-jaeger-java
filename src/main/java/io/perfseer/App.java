package io.perfseer;

import io.perfseer.cli.MainCommand;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import picocli.CommandLine;

@QuarkusMain
public class App implements io.quarkus.runtime.QuarkusApplication {

    @Inject
    MainCommand main;

    @Override
    public int run(String... args) throws Exception {
        int exit = new CommandLine(main).execute(args);
        if (args.length == 0) {
            Quarkus.waitForExit();
        }
        return exit;
    }

    public static void main(String[] args) {
        Quarkus.run(App.class, args);
    }
}
