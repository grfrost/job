rm -rf bootstrap.classes bootstrap.jar
mkdir bootstrap.classes
javac -d bootstrap.classes --source-path core/src/main/java core/src/main/java/job/Job.java
jar cf bootstrap.jar -C bootstrap.classes job
