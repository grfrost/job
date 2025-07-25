package job;

import java.util.function.Consumer;

public class Reporter {
    public final Consumer<String> command = System.out::println;
    public final Consumer<String> progress = System.out::println;
    public final Consumer<String> error = System.out::println;
    public final Consumer<String> info = System.out::println;
    public final Consumer<String> warning = System.out::println;
    public final Consumer<String> note = System.out::println;

    public void command(Dependency dependency, String command) {
        if (dependency != null) {
            this.command.accept("# " + dependency.id().projectRelativeHyphenatedName() + " command line ");
        }
        this.command.accept(command);
    }

    public void progress(Dependency dependency, String command) {
        if (dependency != null) {
            progress.accept("# " + dependency.id().projectRelativeHyphenatedName() + " " + command);
        }
    }

    public void error(Dependency dependency, String command) {
        if (dependency != null) {
            error.accept("# " + dependency.id().projectRelativeHyphenatedName() + " error ");
        }
        error.accept(command);
    }

    public void info(Dependency dependency, String command) {
        // if (dependency != null) {
        //     info.accept("# "+dependency.id().projectRelativeHyphenatedName+" info ");
        //  }
        info.accept(command);
    }

    public void note(Dependency dependency, String command) {
        //  if (dependency != null) {
        //    note.accept("# "+dependency.id().projectRelativeHyphenatedName+" note ");
        //  }
        note.accept(command);
    }

    public void warning(Dependency dependency, String command) {
        //   if (dependency != null) {
        //      warning.accept("# "+dependency.id().projectRelativeHyphenatedName+" warning ");
        //  }
        warning.accept(command);
    }

    static Reporter verbose = new Reporter();
    public static Reporter commandsAndErrors = new Reporter() {
        @Override
        public void warning(Dependency dependency, String command) {

        }

        @Override
        public void info(Dependency dependency, String command) {

        }

        @Override
        public void note(Dependency dependency, String command) {

        }

        @Override
        public void progress(Dependency dependency, String command) {

        }

    };

    public static Reporter progressAndErrors = new Reporter() {
        @Override
        public void warning(Dependency dependency, String command) {

        }

        @Override
        public void info(Dependency dependency, String command) {

        }

        @Override
        public void note(Dependency dependency, String command) {

        }

        @Override
        public void command(Dependency dependency, String command) {

        }

        public void progress(Dependency dependency, String command) {
            if (dependency != null) {
                progress.accept(dependency.id().projectRelativeHyphenatedName() + ":" + command);
            }
        }
    };
}
