package com.pku;

import java.io.*;
import java.util.Objects;
//import org.simmetrics.SetMetric;

public class AdaptationTransform {
    public static class parameter{
        public String type; // new or reserve
        public String content; // name of parameter
        parameter(String _type,String _content){
            type = _type;
            content = _content;
        }
    }
    static String oldDirPath = "";
    static String newDirPath = "";
    public static void main(String[] args) throws IOException {
        // find adaptations of same broken api
        recurrentExtractAdaptationsOfSameBrokenAPI(oldDirPath);

        // todo: circulate select adaptationA and adaptationB in a adaptations set
        String adaptationAPath = ""; // absolutely  path
        String adaptationBPath = "";
        // read java file and remove prefix (-,+) and creat ast ? yes for the find apis and parameters

        // find broken api

        // determine transform type

            // 1. for delete transform

            // 2. for alternative api transform

                // find alternative api

                // determine every parameter of alternative api

                // get parameters of alternative api

            // 3. for alternative code block transform

        // evaluate the generated adaptation between the ground truth with token similarity
    }
    public static void generateAdaptation(String adaptationAPath,String adaptationBPath){
        String affectedAPath = adaptationAPath.replace("adaptation","affected");
        String affectedBPath = adaptationBPath.replace("adaptation","affected");

    }

    // clean adaptations to get adaptations set of same broken api
    public static void recurrentExtractAdaptationsOfSameBrokenAPI(String filePath) throws IOException { // extract adaptations of same broken api // output: save adaptations of same broken api
        File file = new File(filePath);
        if (file.isDirectory()){
            String currentBrokenAPISignature = "";
            String currentBrokenAPIName = "";
            String oldBrokenAPIName = "";
            File oldFile = null;
            Boolean isFoundAdaptationsOfSameBrokenAPI = false;
            for(File f : Objects.requireNonNull(file.listFiles())){
                if (f.isDirectory()){ recurrentExtractAdaptationsOfSameBrokenAPI(f.getAbsolutePath()); } // 遍历Dir的java file。
                if (f.getName().contains(".java")){
                    currentBrokenAPISignature = f.getName().split(":")[2].split("_")[0];
                    currentBrokenAPIName = currentBrokenAPISignature.split(" ")[1];
                    copyNewFile(f.getAbsolutePath(),oldDirPath,newDirPath);
                    if (currentBrokenAPIName.equals(oldBrokenAPIName)){// find adaptations of same broken api
                        isFoundAdaptationsOfSameBrokenAPI = true;
                        // extract adaptations of same broken api
                    }else{
                        oldBrokenAPIName = currentBrokenAPIName;
                        if (oldFile==null){continue;} // fist epoch, continue
                        if(!isFoundAdaptationsOfSameBrokenAPI){oldFile.delete();} // find another broken API and the latest adaptation is not in useful adaptations set
                    }
                }
                oldFile = f;
            }
        }
    }
    public static void createNewDir(String filePath,String dirPath, String newDirPath){
        File file = new File(filePath);
        String javaFileInnerDirPath = file.getParent().split(dirPath)[1];
        File newJavaFileInnerDirFile = new File(newDirPath+javaFileInnerDirPath);
        if(!newJavaFileInnerDirFile.exists()){newJavaFileInnerDirFile.mkdirs();}
    }
    public static void copyNewFile(String filePath, String oldDirPath, String newDirPath) throws IOException {
        createNewDir(filePath,oldDirPath,newDirPath);
        File file = new File(filePath);
        String javaFileInnerPath = filePath.split(oldDirPath)[1];
        File newJavaFile = new File(newDirPath +javaFileInnerPath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        BufferedWriter bw = new BufferedWriter(new FileWriter(newJavaFile));
        String line;
        while ((line = br.readLine()) != null){
            bw.write(line);
            bw.flush();
        }
        bw.close();
        br.close();
    }
}
