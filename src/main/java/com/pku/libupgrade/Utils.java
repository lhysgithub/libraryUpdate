package com.pku.libupgrade;

import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Utils {
    public static Map<String, String> readOutLibraries(String pom) throws IOException, XmlPullParserException {
        FileInputStream fis = new FileInputStream(new File(pom));
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(fis);
        List<Dependency> dependencies = model.getDependencies();
        Properties properties = model.getProperties();
        Map<String, String> map = new HashMap<>();
        map.put("project.version", model.getVersion());
        StrSubstitutor strSubstitutor_forp = new StrSubstitutor(map);
        for(String key:properties.stringPropertyNames()){
            String value = strSubstitutor_forp.replace(properties.getProperty(key));
            map.put(key,value);
        }
        StrSubstitutor strSubstitutor_forv = new StrSubstitutor(map);
        Map<String, String> returnMap = new HashMap<>();
        for (Dependency dependency : dependencies) {
            String groupId = dependency.getGroupId();
            String artifactId = dependency.getArtifactId();
            String version = strSubstitutor_forv.replace(dependency.getVersion());
            returnMap.put(groupId + "\t" + artifactId, version);
        }
        return returnMap;
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

