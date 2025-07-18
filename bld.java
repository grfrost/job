import java.io.IOException;
import java.nio.file.Path;
import job.Job;

void main(String[] argArr) throws IOException, InterruptedException {
   var project = new Job.Project( Path.of(System.getProperty("user.dir")), Job.Reporter.progressAndErrors);
   var core = Job.Jar.of(project.id("core"));
   project.build(List.of());
}

