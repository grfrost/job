package job;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Util {


    public static boolean grep(Pattern pattern, String str){
        return pattern.matcher(str).matches();
    }

    public static boolean grepLines(Pattern pattern, List<String> lines){
        var result=new boolean[]{false};
        lines.forEach(line->{
            result[0] |= grep(pattern, line);
        });
        return result[0];
    }


    public static boolean grepLines(List<Pattern> patterns, List<String> lines){
        for (var pattern:patterns){
            if (grepLines(pattern, lines)){
                return true;
            }
        }
        return false;
    }

    public static boolean grepLines(Pattern pattern, Path path){
        try{
            return grepLines(pattern, Files.readAllLines(path));
        }catch(IOException i){
            return false;
        }
    }

    public static boolean grepLines(List<Pattern> patterns, Path path){
        try{
            return grepLines(patterns, Files.readAllLines(path));
        }catch(IOException i){
            return false;
        }
    }

    public static void recurse(Path dir, Predicate<Path> dirPredicate, Predicate<Path> filePredicate, Consumer<Path> consumer){
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    if (dirPredicate.test(entry)) {
                        recurse(entry, dirPredicate,filePredicate, consumer);
                   // }else{
                     //   System.out.println(entry + "failed dir predicate" );
                    }
                }else if (filePredicate.test(entry)){
                    consumer.accept(entry);
                }
            }
        }catch(IOException ioe){
            throw new IllegalStateException(ioe);
        }
    }
}
