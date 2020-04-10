import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/*
TODO: Split list dynamically
TODO:  2 Threads of handbrake
TODO: Make thread safe. Synchronised?
TODO: Move process execution to own class
 */

public class main implements Runnable{


    public static void main(String[] args){
        //get list of files
        List<Path> filelist;
        Singleton.getInstance().setFiletxt(readFile(new File(Singleton.convertedTxt)));

        createRunFile();

        try {
            filelist = FileSearch();

            for (Path path : filelist){
                if(path.toFile().exists()){
                    //skip unwanted files
                    if (path.toFile().getAbsolutePath().endsWith(".db")){
                        continue;
                    }
                    System.out.println("Checking: " + path.toFile().getAbsolutePath());
                    //check against txt file to blacklist converted videos
                    boolean fileWasConverted = false;
                    for (String file : Singleton.getInstance().getFiletxt()){
                        String filePath = path.toFile().getAbsolutePath().trim();
                        if (file.trim().contains(filePath)){
                            fileWasConverted = true;
                        }
                    }
                    if (!fileWasConverted){
                        //add found video to conversion queue
                        Singleton.getInstance().addToFileQueue(path.toFile());
                    }
                }
            }
            Runnable myRunnable = new main();
            Runnable myRunnable2 = new main();

            Thread threadOne = new Thread(myRunnable);
            Thread threadTwo = new Thread(myRunnable2);

            threadOne.setName("ThreadOne");
            threadTwo.setName("ThreadTwo");

            threadOne.start();
            threadTwo.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private static String generateExecCommand(File originalFile, File renamedFile){
        //create exec command
        String execCommand ="\"" +  Singleton.HandbrakeDir + "\" --input \"" + renamedFile.getAbsolutePath() + "\" --output \"" + main.getOutputFile(originalFile) + "\" --format av_mp4 --inline-parameter-sets --markers --optimize --encoder nvenc_h265 --encoder-preset slow --vfr --all-subtitles --encoder-level auto --vb 3500 --ab 320 --arate auto --all-audio --aencoder copy:ac3,copy:dts --audio-fallback ac3 --mixdown stereo --maxHeight 1080 --maxWidth 1920 --keep-display-aspect";
        return execCommand;
    }
    static ArrayList<String> readFile(File file){
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

    static String getOutputFile(File originalFile){
        String outputFile = originalFile.getAbsolutePath().substring(0,originalFile.getAbsolutePath().length()-3) + "mp4";
        outputFile = outputFile.replaceAll("2160","1080").replaceAll("BlueRay","").replaceAll("blueray","");
        return outputFile;
    }
    static File renameCurrentFile(File originalFile) throws IOException {
        String newPath = originalFile.getPath().replace(originalFile.getName(), "old_" + originalFile.getName());
        File newFile =  new File(newPath);

        if (originalFile.renameTo(newFile)) {
            System.out.println("File renamed successfully to : " +  newPath);
            return newFile;
        }
        else {
            System.out.println("Unable to rename File: " + originalFile.getAbsolutePath());
            return null;
        }
    }
    static List<Path> FileSearch() throws IOException {
        //walk the file tree finding all video files
        System.out.println("Looking for files.....");
        List<Path> filelist;
        filelist = Files.walk(Paths.get("Y:\\Movies"))
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());
        filelist.addAll(Files.walk(Paths.get("Y:\\Shows"))
                .filter(Files::isRegularFile)
                .collect(Collectors.toList()));
        System.out.println("Search Completed");
        return filelist;
    }
    static void createRunFile(){
        //Create file to stop program early
        PrintWriter out;
        try {
            out = new PrintWriter(new File("RUN"));
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        System.out.println("Starting Thread");
        try {
            while(!Singleton.getInstance().isFileQueueEmpty()){
                File originalFile = Singleton.getInstance().getFileFromQueue();

                //This is to prevent the output video from overwriting the original if the file extension is the same.
                File newFile = renameCurrentFile(originalFile);
                if (newFile == null){
                    System.out.println("Skipping File");
                    continue;
                }
                ProcessBuilder pb = new ProcessBuilder();
                pb.command(generateExecCommand(originalFile,newFile).split(" ")); // makes the execCommand compatible with processBuilder
                pb.inheritIO(); //connects handbrake io to console window
                System.out.println(pb.command());
                Process p = pb.start();
                if (p.waitFor() == 0){
                    Singleton.getInstance().addToFileTXT(originalFile.getAbsolutePath().replace("\n"," "));
                    System.out.println("Deleting File: " + newFile.getAbsolutePath());
                    newFile.delete();
                    Singleton.getInstance().writeFile(Singleton.getInstance().getFiletxt());
                }
                else{
                    //renames the file to the original name since conversion was unsuccessful
                    newFile.renameTo(originalFile);
                }
                //Check if Run file removed, stop program if missing
                File file1 = new File("RUN");
                if (!file1.exists()){
                    System.out.println("Stopping");
                    System.exit(1);
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Ending Thread");
    }
}
