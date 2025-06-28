import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import job.Job;
public class bld{
    public static void main(String[] argArr) throws IOException, InterruptedException {
        Path userDir = Path.of(System.getProperty("user.dir"));
        var project = new Job.Project(userDir.getFileName().toString().equals("intellij") ? userDir.getParent() : userDir);
        var core = Job.Jar.of(project.id("core-1.0"));
        project.start(argArr);
    }
}
