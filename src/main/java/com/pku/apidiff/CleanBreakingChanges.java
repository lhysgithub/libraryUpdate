package com.pku.apidiff;

import java.io.File;
import java.util.Objects;

import static com.pku.libupgrade.Utils.isGreater;

public class CleanBreakingChanges {
    public static void main(String[] args) {
        String breakingChangesDir = "breakingChanges1";
        String breakingChangesDir2 = "breakingChanges2";
        File bcd = new File(breakingChangesDir);
        for(File f: Objects.requireNonNull(bcd.listFiles())){
            String fileName = f.getName();
            if(f.getName().equals(".DS_Store")){continue;}
            if(f.getName().contains("junit")){continue;}
            if(f.getName().contains("test")){continue;}
            if(f.getName().contains("gwt")){continue;}
            if(f.getName().contains("guava")){continue;}
            if(f.getName().contains("jdt")){continue;}
            if(f.getName().contains("truth")){continue;}
            if(f.getName().split("_").length!=3){continue;}
            String newVersion = fileName.split("_")[1].split(":")[2];
            String oldVersion = fileName.split("_")[2].split(":")[2];
            if(isGreater(newVersion,oldVersion)){
                // cp to breakingChangesDir2
            }
        }
    }

}
