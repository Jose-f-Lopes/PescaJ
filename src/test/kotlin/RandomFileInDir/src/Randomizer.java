import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class Randomizer {

    private Stack<File> history=new Stack<File>();
    private List<File> toRandomize=new ArrayList<File>();

    private File lastFile=null;


    public void addToRandomize(List<File> files){
        toRandomize.clear();
        history.clear();
        toRandomize.addAll(files);
    }

    public File getRandom(){
        if (lastFile!=null){
            history.add(lastFile);
        }
        Random random= new Random();
        int i=random.nextInt(toRandomize.size());
        File file=toRandomize.get(i);
        lastFile=file;

        return file;
    }

    public File goBackOne(){
        if (history.isEmpty()){
            throw new IllegalStateException("History is empty");
        }
        File last=history.pop();
        toRandomize.add(last);
        lastFile=last;
        return last;
    }

}
