import java.io.IOException;
import java.nio.file.Path;
import job.Job;

    public static void main(String[] argArr) throws IOException, InterruptedException {
        Path userDir = Path.of(System.getProperty("user.dir"));
        var project = new Job.Project(userDir.getFileName().toString().equals("intellij") ? userDir.getParent() : userDir, new Job.Reporter());
        var core = Job.Jar.of(project.id("core"));
        project.start(argArr);
    }

