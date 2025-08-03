package job;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ForkExec {
    public record Result(
            Dependency dependency,
            Path path,
            Opts opts,
            int status,
            List<String> stdErrAndOut){
    }
    static Result forkExec(Dependency dependency, Path path, Opts opts) {
        try {
            List<String> stdErrAndOut = new ArrayList<>();
            Process process = new ProcessBuilder()
                    .directory(path.toFile())
                    .command(opts.opts)
                    .redirectErrorStream(true)
                    .start();
            new BufferedReader(new InputStreamReader(process.getInputStream())).lines().forEach(line->{
                System.out.println(line);
                stdErrAndOut.add(line);
            });
            process.waitFor();
            return new Result(dependency, path, opts, process.exitValue(), stdErrAndOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Opts {
        List<String> opts = new ArrayList<>();
        private Opts(){

        }
        Opts add(String ...opts){
            this.opts.addAll(List.of(opts));
            return this;
        }

        public static Opts of(String executable) {
            Opts opts = new Opts();
            opts.add(executable);
            return opts;
        }

        @Override
        public String toString() {
            return String.join(" ", opts);
        }

    }
}
