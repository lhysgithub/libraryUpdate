package com.pku.libupgrade;

import com.pku.libupgrade.PomParser;
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

    public static Boolean isNullOrEmpty(final String text) {
        return (text == null || "".equals(text));
    }

    public static String getPathProject(final String path, final String nameProject) throws IOException {
        String pathComplete = isNullOrEmpty(path) ? "./" : path + "/";
        pathComplete += isNullOrEmpty(nameProject) ? "" : nameProject;
        return pathComplete;
    }

}

