package com.pku;

import java.io.*;
import java.util.Objects;

//import static com.pku.AdaptationTransform.recurrentExtractAdaptationsOfSameBrokenAPI;

public class CleanAdaptation {
    static String oldDirPath = "d:\\dataset";
    static String newDirPath = "d:\\dataset1";
    static String thirdDirPath = "d:\\dataset2";
    static String fourthDirPath = "d:\\dataset3";
    public static void main(String[] args) throws IOException {
        // filter the adaptations of non-breaking change
//        cleanNonBreakingChange(oldDirPath);
        // extract affected and adaptation file pair
//        recurrentExtractAdaptation(oldDirPath,oldDirPath,newDirPath);
        // extract adaptations of same BrokenAPI
//        recurrentExtractAdaptationsOfSameBrokenAPI(newDirPath,newDirPath,thirdDirPath);
        // change view
        recurrentExtractAdaptationsOfSameBrokenAPIWithOtherView(newDirPath,newDirPath,thirdDirPath);
        // change view
        recurrentExtractAdaptationsWithFiles(thirdDirPath,thirdDirPath,fourthDirPath);
    }
    public static void recurrentExtractAdaptationsWithFiles(String filePath,String oldDirPath,String newDirPath) throws IOException {
        File file = new File(filePath);
        if (file.isDirectory()){
            for(File f : Objects.requireNonNull(file.listFiles())){
                recurrentExtractAdaptationsWithFiles(f.getAbsolutePath(),oldDirPath,newDirPath); // 遍历oldDir的java file。
            }
        }
        else if (file.getName().contains(".java")){
            copyNewFile(file.getAbsolutePath(),oldDirPath,newDirPath);
        }
    }

    public static void recurrentExtractAdaptation(String filePath,String oldDirPath,String newDirPath) throws IOException {
        File file = new File(filePath);
        if (file.isDirectory()){
            for(File f : Objects.requireNonNull(file.listFiles())){
                recurrentExtractAdaptation(f.getAbsolutePath(),oldDirPath,newDirPath); // 遍历oldDir的java file。
            }
        }
        else if (file.getName().contains(".java")){
            // 如果在newDir中java file所属文件夹不存在，进行创建
            String javaFileInnerDirPath = file.getParent().replace(oldDirPath,newDirPath);
            File newJavaFileInnerDirFile = new File(javaFileInnerDirPath);
            if(!newJavaFileInnerDirFile.exists()){newJavaFileInnerDirFile.mkdirs();}

            // 读取java file。在newDir中创建java file，并保留行首为-或+的行。
            String javaFileInnerPath = filePath.replace(oldDirPath,newDirPath);
            File newJavaFile = new File(javaFileInnerPath);
            BufferedReader br = new BufferedReader(new FileReader(file));
            BufferedWriter bw = new BufferedWriter(new FileWriter(newJavaFile));
            String line;
            while ((line = br.readLine()) != null){
                char[] lineChars = line.toCharArray();
                if(lineChars.length==0){continue;}
                if (lineChars[0]=='-' || lineChars[0]=='+'){
                    bw.write(line+"\n");
                    bw.flush();
                }
            }
            bw.close();
            br.close();
        }
    }
    public static void cleanNonBreakingChange(String filePath){
        File file = new File(filePath);
        if(file.isDirectory()) {
            for (File f : Objects.requireNonNull(file.listFiles())) {
                cleanNonBreakingChange(f.getPath());
            }
        }
        else{
            if (isNoneBreakingChange(file.getName())){
                file.deleteOnExit();
            }
        }
    }
    public static boolean isNoneBreakingChange(String filename){
        if(filename.contains("Add Type")){return true;}
        if(filename.contains("Add Static Modifier")){return true;}
        if(filename.contains("Add Final Modifier")){return true;} // ?
        if(filename.contains("Remove Final Modifier")){return true;}
        if(filename.contains("Add Method")){return true;}
        if(filename.contains("Add Field")){return true;}
        if(filename.contains("Gain")){return true;}
        if(filename.contains("Deprecated")){return true;}
        return false;
    }
    // clean adaptations to get adaptations set of same broken api
    public static void recurrentExtractAdaptationsOfSameBrokenAPIWithOtherView(String filePath,String oldDirPath,String newDirPath) throws IOException { // extract adaptations of same broken api // output: save adaptations of same broken api
        File file = new File(filePath);
        if (file.isDirectory()){
            String currentBrokenAPISignature = "";
            String oldBrokenAPISignature = "";
//            String currentBrokenAPIName = "";
//            String oldBrokenAPIName = "";
            File oldFile = null;
            String brokenType = "";
            String brokenSubType = "";
            File newVersionFile = null;
            Boolean isFoundAdaptationsOfSameBrokenAPI = false;
            int filesLength = Objects.requireNonNull(file.listFiles()).length;
            int index = 0;
            if (filesLength ==0 || filesLength ==1 && Objects.requireNonNull(file.listFiles())[0].isFile()) return;
            for(File f : Objects.requireNonNull(file.listFiles())){
                index++;
                if (f.isDirectory()){ recurrentExtractAdaptationsOfSameBrokenAPIWithOtherView(f.getAbsolutePath(),oldDirPath,newDirPath); } // 遍历Dir的java file。
                else if (f.getName().contains(".java")){
                    brokenType = f.getName().split("_")[0];
                    brokenSubType = f.getName().split("_")[1];
//                    currentBrokenAPISignature = f.getName().split(":")[2].split("_")[0]; // for linux
//                    currentBrokenAPISignature = f.getName().split("_")[2]; // for windows
//                    char[] signatureChars = currentBrokenAPISignature.toCharArray();
////                    System.out.println(f.getName());
//                    for(int i =3;signatureChars.length==0 || signatureChars[signatureChars.length-1]==' ';i++){// for windows
//                        currentBrokenAPISignature = currentBrokenAPISignature +"_"+f.getName().split("_")[i];
//                        signatureChars = currentBrokenAPISignature.toCharArray();
//                    }
                    currentBrokenAPISignature = f.getName().split("_[0-9]+")[0].replace(brokenType+"_","").replace(brokenSubType+"_",""); // for windows
//                    String[] brokenSignature = currentBrokenAPISignature.split(" ");
//                    if (brokenType.equals("Method")){currentBrokenAPIName = brokenSignature[1];}
//                    else{currentBrokenAPIName = brokenSignature[0];}
//                    currentBrokenAPIName = currentBrokenAPISignature.split(" ")[1]; // for method broken
                    newVersionFile = copyNewFileWithOtherView(f.getAbsolutePath(),oldDirPath,newDirPath,currentBrokenAPISignature+"_",currentBrokenAPISignature+"\\"); // for windows
//                    if (currentBrokenAPIName.equals(oldBrokenAPIName)){// find adaptations of same broken api
                    if (currentBrokenAPISignature.equals(oldBrokenAPISignature)){// find adaptations of same broken api
                        isFoundAdaptationsOfSameBrokenAPI = true;
                        // extract adaptations of same broken api
                    }else{
//                        oldBrokenAPIName = currentBrokenAPIName;
                        if (oldFile==null){oldBrokenAPISignature = currentBrokenAPISignature;oldFile = newVersionFile;continue;} // fist epoch, continue
                        if(!isFoundAdaptationsOfSameBrokenAPI){oldFile.delete();} // find another broken API and the latest adaptation is not in useful adaptations set
                        isFoundAdaptationsOfSameBrokenAPI = false;
                        if(index==filesLength){newVersionFile.delete();}
                    }
                    oldBrokenAPISignature = currentBrokenAPISignature;
                }
                oldFile = newVersionFile;
            }
        }
    }
    public static File copyNewFileWithOtherView(String filePath, String oldDirPath, String newDirPath,String oldView,String newView) throws IOException {
        File file = new File(filePath);
        String javaFileInnerPath = filePath.replace(oldDirPath,newDirPath).replace(oldView,newView);
        File newJavaFile = new File(javaFileInnerPath);
        String javaFileInnerDirPath = newJavaFile.getParent();
        File newJavaFileInnerDirFile = new File(javaFileInnerDirPath);
        if(!newJavaFileInnerDirFile.exists()){newJavaFileInnerDirFile.mkdirs();}
        BufferedReader br = new BufferedReader(new FileReader(file));
        BufferedWriter bw = new BufferedWriter(new FileWriter(newJavaFile));
        String line;
        while ((line = br.readLine()) != null){
            bw.write(line+"\n");
            bw.flush();
        }
        bw.close();
        br.close();
        return newJavaFile;
    }
    public static File copyNewFile(String filePath, String oldDirPath, String newDirPath) throws IOException {
        File file = new File(filePath);
        String javaFileInnerPath = filePath.replace(oldDirPath,newDirPath);
        File newJavaFile = new File(javaFileInnerPath);
        String javaFileInnerDirPath = newJavaFile.getParent();
        File newJavaFileInnerDirFile = new File(javaFileInnerDirPath);
        if(!newJavaFileInnerDirFile.exists()){newJavaFileInnerDirFile.mkdirs();}
        BufferedReader br = new BufferedReader(new FileReader(file));
        BufferedWriter bw = new BufferedWriter(new FileWriter(newJavaFile));
        String line;
        while ((line = br.readLine()) != null){
            bw.write(line+"\n");
            bw.flush();
        }
        bw.close();
        br.close();
        return newJavaFile;
    }
}
