import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Main {
    public static List<String> readOutLibraries(String pom) throws IOException, XmlPullParserException {
        FileInputStream fis = new FileInputStream(new File(pom));
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(fis);
        List<Dependency> dependencies = model.getDependencies();
        List<String> libraryList = new LinkedList<>();
        for (Dependency dependency:dependencies) {
            String groupId = dependency.getGroupId();
            String artifactId = dependency.getArtifactId();
            String version = dependency.getVersion();
            String result = groupId+','+artifactId+','+version;
            System.out.println(result);
            libraryList.add(result);
        }
        return libraryList;
    }

    public static void main(String[] args) throws XmlPullParserException, IOException {
        List<String> libraryList = readOutLibraries("./recourse/pom.xml");
    }
}

//public class Main {
//    public static void main(String[] args) throws IOException, XmlPullParserException {
//        String pom = "pom.xml";
//        FileInputStream fis = new FileInputStream(new File(pom));
//        MavenXpp3Reader reader = new MavenXpp3Reader();
//        Model model = reader.read(fis);
//        List<Dependency> dependencies = model.getDependencies();
//        List<String> libraryList = new LinkedList<>();
//        for (Dependency dependency : dependencies) {
//            String groupId = dependency.getGroupId();
//            String artifactId = dependency.getArtifactId();
//            String version = dependency.getVersion();
//            String result = groupId + ',' + artifactId + ',' + version;
//            System.out.print(result);
//            libraryList.add(result);
//        }
//    }
//}
