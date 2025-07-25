import java.io.IOException;
import java.nio.file.Path;

import job.Jar;
import job.Project;
import job.Reporter;

void main(String[] argArr) throws IOException, InterruptedException {
   var project = new Project( Path.of(System.getProperty("user.dir")), Reporter.progressAndErrors);
   Jar.of(project.id("core"));
   project.build();
}

