package com.pku.apidiff;

import java.io.File;
import java.util.Objects;

import static com.pku.apidiff.APIDiff.deleteDir;
import static com.pku.libupgrade.Utils.isGreater;

public class CleanAdaptation {
    public static void main(String[] args) {
        String dirName = "/Users/liuhongyi/PycharmProjects/LUAnalysis/LU20220427/affected/client";
        File adaptationDir = new File(dirName);
        // library
//        for(File f: Objects.requireNonNull(adaptationDir.listFiles())){
//            String fileName = f.getName();
//            if(fileName.equals(".DS_Store")){continue;}
////            System.out.println(fileName);
//            String newVersion = fileName.split("_")[0].split(":")[2];
//            String oldVersion = fileName.split("_")[1];
////            System.out.println(newVersion);
////            System.out.println(oldVersion);
//            if(!isGreater(newVersion,oldVersion)){
//                File adaptationSubDir = new File(dirName+"/"+fileName);
////                adaptationSubDir.deleteOnExit();
//                System.out.println(adaptationSubDir.getName());
//                deleteDir(adaptationSubDir);
//            }
//        }
        // client
//        for(File f_lib: Objects.requireNonNull(adaptationDir.listFiles())){
//            String libFileName = f_lib.getName();
//            if(libFileName.equals(".DS_Store")){continue;}
//            File lib_file = new File(dirName + "/"+libFileName);
//            for(File f: Objects.requireNonNull(lib_file.listFiles())){
//                String fileName = f.getName();
//                if(fileName.equals(".DS_Store")){continue;}
//                String newVersion = fileName.split("_")[0];
//                String oldVersion = fileName.split("_")[1];
//                if(!isGreater(newVersion,oldVersion)){
//                    File adaptationSubDir = new File(dirName+"/"+libFileName+"/"+fileName);
//                    System.out.println(adaptationSubDir.getName());
//                    deleteDir(adaptationSubDir);
//                }
//            }
//        }

        // library
        cleanNonBreakingChange(dirName);
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
}
