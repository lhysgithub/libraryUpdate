package com.pku;

import java.io.*;
import java.util.Objects;

public class CleanAdaptation {
    static String oldDirPath = "";
    static String newDirPath = "";
    public static void main(String[] args) throws IOException {
        // 提取适配文件对
        recurrentExtractAdaptation(oldDirPath);
    }
    public static void recurrentExtractAdaptation(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.isDirectory()){
            for(File f : Objects.requireNonNull(file.listFiles())){
                recurrentExtractAdaptation(f.getAbsolutePath()); // 遍历oldDir的java file。
            }
        }
        else if (file.getName().contains(".java")){
            // 如果在newDir中java file所属文件夹不存在，进行创建
            String javaFileInnerDirPath = file.getParent().split(oldDirPath)[1];
            File newJavaFileInnerDirFile = new File(newDirPath+javaFileInnerDirPath);
            if(!newJavaFileInnerDirFile.exists()){newJavaFileInnerDirFile.mkdirs();}

            // 读取java file。在newDir中创建java file，并保留行首为-或+的行。
            String javaFileInnerPath = filePath.split(oldDirPath)[1];
            File newJavaFile = new File(newDirPath +javaFileInnerPath);
            BufferedReader br = new BufferedReader(new FileReader(file));
            BufferedWriter bw = new BufferedWriter(new FileWriter(newJavaFile));
            String line;
            while ((line = br.readLine()) != null){
                if (line.toCharArray()[0]=='-' || line.toCharArray()[0]=='+'){
                    bw.write(line);
                    bw.flush();
                }
            }
            bw.close();
            br.close();
        }
    }

}
