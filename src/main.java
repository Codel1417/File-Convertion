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
final static String HandbrakeDir = "HandBrakeCLI.exe";
final static String convertedTxt = "converted.txt";
    public static void main(String[] args){
        //get list of files
        List<Path> filelist;
        Queue<File> fileQueue = new LinkedList<>();
        ArrayList<String> filetxt = readFile(new File(convertedTxt));

        //Create file to stop program early
        PrintWriter out;
        try {
            out = new PrintWriter(new File("RUN"));
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }



        System.out.println("Starting Search");
        try {
            //walk the file tree finding all video files
            filelist = Files.walk(Paths.get("Y:\\Movies"))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
            filelist.addAll(Files.walk(Paths.get("Y:\\Shows"))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList()));
            for (Path path : filelist){
                if(path.toFile().exists()){
                    System.out.println("Checking: " + path.toFile().getAbsolutePath());
                    //check against txt
                    boolean fileWasConverted = false;

                    for (String file : filetxt){
                        String filePath = path.toFile().getAbsolutePath().trim();
                        //System.out.println("Comparing " + filePath + " to " + file);
                        if (file.trim().contains(filePath)){
                            fileWasConverted = true;
                        }
                    }
                    if (!fileWasConverted){
                        //add found video to conversion queue
                        fileQueue.add(path.toFile());                    }
                }
            }
            for(File file : fileQueue){
                String newpath = file.getPath().replace(file.getName(), "old_" + file.getName());
                File newfile =  new File(newpath);

                if (file.renameTo(newfile)) {
                    System.out.println("File renamed successfully to : " +  newpath);
                }
                else {
                    System.out.println("Unable to rename file");
                    break;
                }

                //create exec command
                String outputFile = file.getAbsolutePath().substring(0,file.getAbsolutePath().length()-3) + "mp4";
                outputFile = outputFile.replaceAll("2160","1080").replaceAll("BlueRay","").replaceAll("blueray","");
                String execCommand ="\"" +  HandbrakeDir + "\" --input \"" + newfile.getAbsolutePath() + "\" --output \"" + outputFile + "\" --format av_mp4 --inline-parameter-sets --markers --optimize --encoder nvenc_h265 --vfr --all-subtitles --encoder-level auto --vb 3500 --ab 320 --arate auto --all-audio --mixdown stereo --maxHeight 1080 --maxWidth 1920 --keep-display-aspect  --encoder-preset slow ";
                System.out.println("Exec: "+ execCommand);
                ProcessBuilder pb = new ProcessBuilder();
                pb.command(execCommand.split(" "));
                System.out.println(pb.command());
                pb.inheritIO();
                Process p = pb.start();

                if (p.waitFor() == 0){
                    filetxt.add(outputFile.replace("\n"," "));
                    System.out.println("Deleting File: " + file.getAbsolutePath());
                    newfile.delete();
                    writeFile(filetxt);
                }
                else{
                    newfile.renameTo(file);
                }
                //Check if Run file removed, stop program if missing
                File file1 = new File("RUN");
                if (!file1.exists()){
                    System.out.println("Stopping");
                    System.exit(0);
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }
    private static ArrayList<String> readFile(File file){
        ArrayList<String> arrayList = new ArrayList<>();
        if (file.exists()){
            try {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNext()){
                    arrayList.add(scanner.nextLine());
                }
                scanner.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return arrayList;
    }
    private static void writeFile(ArrayList<String> arrayList){
        System.out.println("Updating TXT File");
        PrintWriter out = null;
        try {
            out = new PrintWriter(new File(convertedTxt));
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
