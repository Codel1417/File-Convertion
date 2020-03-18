import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class main {
final static String HandbrakeDir = "C:" + File.separator + "Program Files" + File.separator + "HandBrake"+File.separator+"HandBrakeCLI.exe";

    public static void main(String[] args){
        //get list of files
        List<Path> filelist;
        Queue<File> fileQueue = new LinkedList<>();
        ArrayList<String> filetxt = readFile(new File("converted.txt"));
        try {
            filelist = Files.walk(Paths.get("Y:\\Shows\\Arrow"))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());

            for (Path path : filelist){
                if(path.toFile().exists()){
                    System.out.println("Checking: " + path.toFile().getAbsolutePath());
                    //check against txt
                    if (filetxt.contains(path.toFile().getAbsolutePath())){
                        break;
                    }
                    else{
                        System.out.println("Unconverted File Found");
                        fileQueue.add(path.toFile());
                    }
                }
            }
            for(File file : fileQueue){
                //create exec command
                String outputFile = file.getAbsolutePath().substring(0,file.getAbsolutePath().length()-3) + "mp4";
                String execCommand ="\"" +  HandbrakeDir + "\" -i \"" + file.getAbsolutePath() + "\" -o \"" + outputFile + "\" -f av_mp4 --optimize -e nvenc_h264 -q 22 -B 160 --all-audio --maxHeight 1080 --maxWidth 1920";
                System.out.println("Exec: "+ execCommand);
                //Process p = Runtime.getRuntime().exec(execCommand);
                ProcessBuilder pb = new ProcessBuilder();
                pb.command(execCommand.split(" "));
                System.out.println(pb.command());
                pb.inheritIO();
                Process p = pb.start();

                if (p.waitFor() == 0){
                    filetxt.add(outputFile);
                    System.out.println("Deleting File: " + file.getAbsolutePath());
                    file.delete();
                }
            }
            writeFile(filetxt);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    private static ArrayList<String> readFile(File file){
        ArrayList<String> arrayList = new ArrayList<>();
        if (file.exists()){
            try {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNext()){
                    arrayList.add(scanner.next());
                }
                scanner.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return arrayList;
    }
    private static void writeFile(ArrayList<String> arrayList){
        PrintWriter out = null;
        try {
            out = new PrintWriter(new File("converted.txt"));
            for (String string : arrayList){
                out.println(string);
            }
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
