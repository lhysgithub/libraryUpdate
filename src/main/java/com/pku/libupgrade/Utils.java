package com.pku.libupgrade;

import static com.pku.libupgrade.PomParser.MAVEN_CENTRAL_URI;
import static com.pku.libupgrade.PomParser.USER_LOCAL_REPO;

import java.io.IOException;
import java.util.*;

public class Utils {

    public static Map<String, String> readOutLibraries(String pom){
        String localRepo = System.getProperty("maven.repo.local", USER_LOCAL_REPO);

        String remoteReposString = System.getProperty("maven.repo.remote", MAVEN_CENTRAL_URI);
        List<String> remoteRepos = new ArrayList<>();
        for (String repo : remoteReposString.split(",")) {
            remoteRepos.add(repo.trim());
        }
        PomParser pomParser = new PomParser(localRepo, remoteRepos);
        return pomParser.readOutLibraries2(pom);
    }

    public static List<DiffCommit> getDiffList(List<Commit> versionMap, String clientName){
        List<DiffCommit> result = new ArrayList<>(); // commit pomName libName versionName

        int i = 0;
        for (Commit entry : versionMap) {
            if(i==0){
                i+=1;
                continue;
            }
            String commit = entry.commit;
            Map<String, Map<String, String>> pomMap = entry.pomMap;
            Set<String> poms = pomMap.keySet() ;

            String newCommit = versionMap.get(i-1).commit;
            Map<String, Map<String, String>> newPomMap =  versionMap.get(i-1).pomMap;
            Set<String> newPoms = newPomMap.keySet() ;
            for (String newPomName: newPoms) {
                if (!poms.contains(newPomName)) {
                    continue; // new pom file
                }
                Map<String, String> newLibMap = newPomMap.get(newPomName);
                Map<String, String> LibMap = pomMap.get(newPomName);
                Set<String> newLibs = newLibMap.keySet();
                Set<String> libs = LibMap.keySet();
                for (String newLibName : newLibs){
                    if (!libs.contains(newLibName)) {
                        result.add(new DiffCommit(commit,newCommit,clientName, newPomName,newLibName,Boolean.TRUE,  "", newLibMap.get(newLibName)));
                        // continue; // new lib
                        continue;
                    }

                    if (!newLibMap.get(newLibName).equals(LibMap.get(newLibName))){
                        String newVersion = newLibMap.get(newLibName);
                        String version = LibMap.get(newLibName);
                        result.add(new DiffCommit(commit,newCommit,clientName, newPomName,newLibName,Boolean.FALSE, version, newVersion));
                        //continue; // new version
                    }
                }
            }
            i +=1;
        }
        return result;
    }

    public static Boolean isNullOrEmpty(final String text) {
        return (text == null || "".equals(text));
    }

    public static String getPathProject(final String path, final String nameProject) throws IOException {
        String pathComplete = isNullOrEmpty(path) ? "./" : path + "/";
        pathComplete += isNullOrEmpty(nameProject) ? "" : nameProject;
        return pathComplete;
    }

}

