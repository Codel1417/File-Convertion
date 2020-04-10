import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;

public class Singleton {
    final static String HandbrakeDir = "HandBrakeCLI.exe";
    final static String convertedTxt = "converted.txt";
    private ArrayList<String> filetxt = new ArrayList<>();
    private LinkedList<File> fileQueue = new LinkedList<>();


        private static volatile Singleton _instance;

    private Singleton() {}

    public synchronized static Singleton getInstance(){
            if(_instance == null){
                synchronized(Singleton.class){
                    if(_instance == null)
                        _instance = new Singleton();
                }
            }
            return _instance;
        }

    public synchronized File getFileFromQueue(){
        File file = fileQueue.pop();
        System.out.println("Getting item from Queue: " + file.getName());
        return file;
    }

    public synchronized boolean isFileQueueEmpty(){
        return fileQueue.isEmpty();
    }
    public synchronized void writeFile(ArrayList<String> arrayList){
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

    public synchronized void addToFileQueue(File file){
            System.out.println("Adding to queue");
            fileQueue.add(file);
    }
    public synchronized void addToFileTXT(String string){
            filetxt.add(string);
    }
    public ArrayList<String> getFiletxt() {
        return filetxt;
    }

    public void setFiletxt(ArrayList<String> filetxt) {
        this.filetxt = filetxt;
    }
}
