rm -rf build
javac -d build/bootstrap.classes --source-path core/src/main/java core/src/main/java/job/Job.java
jar cf bootstrap.jar -C build/bootstrap.classes job
java -cp bootstrap.jar scripts/bld.java 
rm -rf build/bootstrap.classes bootstrap.jar
