import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


public class main implements Runnable{

    public static void main(final String[] args) {
        // get list of files
        List<Path> filelist;
        Singleton.getInstance().setFiletxt(readFile(new File(Singleton.convertedTxt)));

        createRunFile();

        try {
            filelist = FileSearch();

            for (final Path path : filelist) {
                if (path.toFile().exists()) {
                    System.out.println("Checking: " + path.toFile().getAbsolutePath());
                    // delete blocked file types
                    final String[] blockedFileTypes = { ".iso", ".metathumb", ".partial" };
                    for (final String string : blockedFileTypes) {
                        if (path.toFile().getAbsolutePath().endsWith(string)) {
                            System.out.print("Deleting File");
                            path.toFile().delete();
                            continue;
                        }
                    }
                    // skip unwanted files+
                    final String[] excludedFileTypes = { ".db", ".srt", ".xml", ".png", ".jpg", ".nfo" };
                    for (final String string : excludedFileTypes) {
                        if (path.toFile().getAbsolutePath().endsWith(string)) {
                            System.out.println("Ignoring File");
                            continue;
                        }
                    }
                    // delete small media files
                    if (path.toFile().length() < 4000000) {
                        System.out.println("Deleting File");
                        path.toFile().delete();
                        continue;
                    }
                    // check against txt file to blacklist converted videos
                    boolean fileWasConverted = false;
                    for (final String file : Singleton.getInstance().getFiletxt()) {
                        final String filePath = path.toFile().getAbsolutePath().trim();
                        if (file.trim().contains(filePath)) {
                            fileWasConverted = true;
                        }
                    }
                    if (!fileWasConverted) {
                        // add found video to conversion queue
                        Singleton.getInstance().addToFileQueue(path.toFile());
                    }
                }
            }
            final Runnable myRunnable = new main();
            final Runnable myRunnable2 = new main();

            final Thread threadOne = new Thread(myRunnable);
            final Thread threadTwo = new Thread(myRunnable2);

            threadOne.setName("ThreadOne");
            threadTwo.setName("ThreadTwo");

            threadOne.start();
            threadTwo.start();
        } catch (final IOException e) {
            e.printStackTrace();
        }

    }

    private static String[] generateExecCommand(final File originalFile, final File renamedFile) {
        // create exec command
        final String execCommand = "\"" + Singleton.HandbrakeDir + "\" --input \"" + renamedFile.getAbsolutePath()
                + "\" --output \"" + main.getOutputFile(originalFile)
                + "\" --format av_mkv --inline-parameter-sets --markers --encoder nvenc_h265 --vfr  --subtitle=1-99 --vb 3500 --arate auto --all-audio --aencoder av-aac  --maxHeight 1080 --maxWidth 1920 --keep-display-aspect";
        System.out.println(execCommand);
        return execCommand.split(" ");
    }

    static ArrayList<String> readFile(final File file) {
        final ArrayList<String> arrayList = new ArrayList<>();
        if (file.exists()) {
            try {
                final Scanner scanner = new Scanner(file);
                while (scanner.hasNext()) {
                    arrayList.add(scanner.nextLine());
                }
                scanner.close();
            } catch (final FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return arrayList;
    }

    static String getOutputFile(final File originalFile) {
        String outputFile = originalFile.getAbsolutePath().substring(0, originalFile.getAbsolutePath().length() - 3)
                + "mkv";
        outputFile = outputFile.replaceAll("2160", "1080").replaceAll("BlueRay", "").replaceAll("blueray", "");
        outputFile = outputFile.replaceAll("old_", "").replaceAll("UHD", "").replaceAll("WEBDL", "")
                .replaceAll("BR-DISK", "");
        return outputFile;
    }

    static File renameCurrentFile(final File originalFile) {
        final String newPath = originalFile.getPath().replace(originalFile.getName(), "old_" + originalFile.getName());
        final File newFile = new File(newPath);

        if (originalFile.renameTo(newFile)) {
            System.out.println("File renamed successfully to : " + newPath);
            return newFile;
        } else {
            System.out.println("Unable to rename File: " + originalFile.getAbsolutePath());
            return null;
        }
    }

    static List<Path> FileSearch() throws IOException {
        // walk the file tree finding all video files
        System.out.println("Looking for files.....");
        List<Path> filelist;
        filelist = Files.walk(Paths.get("Y:\\Movies")).filter(Files::isRegularFile).collect(Collectors.toList());
        filelist.addAll(Files.walk(Paths.get("Y:\\Shows")).filter(Files::isRegularFile).collect(Collectors.toList()));
        System.out.println("Search Completed");
        return filelist;
    }

    static void createRunFile() {
        // Create file to stop program early
        PrintWriter out;
        try {
            out = new PrintWriter(new File("RUN"));
            out.flush();
            out.close();
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("Starting Thread");
        try {
            while (!Singleton.getInstance().isFileQueueEmpty()) {
                final File originalFile = Singleton.getInstance().getFileFromQueue();

                // This is to prevent the output video from overwriting the original if the file
                // extension is the same.
                final File newFile = renameCurrentFile(originalFile);
                if (newFile == null) {
                    System.out.println("Skipping File");
                    continue;
                }

                final ProcessBuilder pb = new ProcessBuilder();
                pb.command(generateExecCommand(originalFile, newFile)); // makes the execCommand compatible with
                                                                        // processBuilder
                pb.inheritIO(); // connects handbrake io to console window
                System.out.println(pb.command());
                final Process p = pb.start();

                final File output = new File(getOutputFile(originalFile));
                final int exitcode = p.waitFor();
                System.out.println("Exited with code: " + exitcode);
                if (exitcode == 0) {
                    final long filesizeOriginal = newFile.length();
                    final long filesizeConverted = output.length();

                    // 1 bit to 1 mb = 800,000
                    if (filesizeOriginal < filesizeConverted | filesizeConverted < 4000000) {
                        System.out.println("Keeping Old FIle");
                        // original file smaller than new file, or file smaller than 1mb
                        newFile.renameTo(originalFile);
                        Singleton.getInstance().addToFileTXT(originalFile.getAbsolutePath().replace("\n", " "));
                        output.delete();
                    } else {
                        Singleton.getInstance().addToFileTXT(output.getAbsolutePath().replace("\n", " "));
                        newFile.delete();
                    }
                    Singleton.getInstance().writeFile(Singleton.getInstance().getFiletxt());
                } else {
                    System.out.println("Conversion Failed");
                    // renames the file to the original name since conversion was unsuccessful
                    newFile.renameTo(originalFile);
                    output.delete();
                }
                // Check if Run file removed, stop program if missing
                final File file1 = new File("RUN");
                if (!file1.exists()){
                    System.out.println("Stopping");
                    System.exit(0);
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Ending Thread");
    }
}
