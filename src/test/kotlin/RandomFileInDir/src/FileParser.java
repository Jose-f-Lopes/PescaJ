import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileParser {

    private boolean searchInDepth=true;
    private List<File> baseFiles=new ArrayList<File>();

    private List<File> deepFiles=new ArrayList<File>();

    public void setSearchInDepth(boolean searchInDepth) {
        this.searchInDepth = searchInDepth;
    }
    public boolean getSearchInDepth(){
        return searchInDepth;
    }

    public void buildFileList(String dir){
        buildFileAux(new File(dir),true);
    }
    private void buildFileAux(File baseDir, Boolean firstPass){
        if (!baseDir.isDirectory()){
            throw new IllegalArgumentException("Given file is not directory");
        }else{
            for(File file: baseDir.listFiles() ){
                if (firstPass && file.isFile()){
                    baseFiles.add(file);
                }
                if(!firstPass) {
                    if (file.isFile()) {
                        deepFiles.add(file);
                    }
                }
                if (file.isDirectory()){
                    buildFileAux(file,false);
                }

            }
        }
    }

    public List<File> getFiles(){
        if (searchInDepth){
            List<File> result=new ArrayList<File>();
            result.addAll(baseFiles);
            result.addAll(deepFiles);
            return result;
        }else{
            return baseFiles;
        }
    }
}
