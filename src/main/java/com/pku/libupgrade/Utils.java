package com.pku.libupgrade;

import com.csvreader.CsvReader;
import com.pku.apidiff.APIDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.pku.libupgrade.PomParser.MAVEN_CENTRAL_URI;
import static com.pku.libupgrade.PomParser.USER_LOCAL_REPO;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {
    public static Logger logger = LoggerFactory.getLogger(Utils.class);

    public static Map<String, String> readOutLibraries(String pom) {
        String localRepo = System.getProperty("maven.repo.local", USER_LOCAL_REPO);

        String remoteReposString = System.getProperty("maven.repo.remote", MAVEN_CENTRAL_URI);
        List<String> remoteRepos = new ArrayList<>();
        for (String repo : remoteReposString.split(",")) {
            remoteRepos.add(repo.trim());
        }
        PomParser pomParser = new PomParser(localRepo, remoteRepos);
        return pomParser.readOutLibraries2(pom);
    }

    public static boolean isGreater (String version1,String version2){
        String[] versionList1 = version1.split("\\.");
        String[] versionList2 = version2.split("\\.");
        String regEx="[^0-9]";
        Pattern p = Pattern.compile(regEx);
//        Matcher m = p.matcher(a);
//        System.out.println( m.replaceAll("").trim());
        for(int i=0;i< versionList1.length;i++){
//            System.out.println(versionList1[i]);
//            System.out.println(versionList2[i]);
            if(versionList1[i].equals("0-rc-202107030819")){return false;}
            if(versionList1[i].equals("RC1")){return false;}
            if(versionList2[i].equals("Final")){return false;}
            String number1s = p.matcher(versionList1[i]).replaceAll("");
            String number2s = p.matcher(versionList2[i]).replaceAll("");
            int number1 = Integer.parseInt(number1s);
            int number2 = Integer.parseInt(number2s);
            if (number1 == number2){continue;}
            return number1 > number2;
        }
        return false;
    }

    public static boolean isProjectExist(String projectName, String commitDiffPath) throws IOException {
        String inString;
        Boolean isExist = false;
        File inFile  = new File(commitDiffPath);
        BufferedReader reader = new BufferedReader(new FileReader(inFile));
        CsvReader csvReader = new CsvReader(reader,'???');
        while(csvReader.readRecord()) {
            inString = csvReader.getRawRecord();//??????????????????
            inString = inString.split("\"")[1];
            if(inString.split(",")[0].equals(projectName)){
                isExist = true;
                break;
            }
        }
        csvReader.close();
        return isExist;
    }

    public static List<String> findPopularLibFromCsv(String csv,String outputPath) throws IOException {
        List<String> result = new ArrayList<>();
        Map<String, Integer> counter = new HashMap<>();
        Map<String, Integer> groupIDCounter = new HashMap<>();
        Map<String, Integer> versionPairCounter = new HashMap<>();
        String libName = "" ;
        String groupID = "" ;
        String inString = "";
        String artifactID = "";
        String libNewVersion = "";
        String libOldVersion = "";
        String temp = "";
        Integer tempValue = 0;
        File inFile  = new File(csv);
        File popularLib = new File(outputPath);
        BufferedWriter pw = new BufferedWriter(new FileWriter(popularLib));
        try{
            BufferedReader reader = new BufferedReader(new FileReader(inFile));
            CsvReader csvReader = new CsvReader(reader,'???');

            while(csvReader.readRecord()) {
                inString = csvReader.getRawRecord();//??????????????????
                libName =  inString.split(",")[4];
                groupID = libName.split("\t")[0];
                artifactID = libName.split("\t")[1];
                libNewVersion = inString.split(",")[6];
                libOldVersion = inString.split(",")[7];
                if(libOldVersion.equals("\"")){ libOldVersion="null"; }
                else{ libOldVersion = libOldVersion.split("\"")[0];}
                temp=groupID+":"+artifactID+":"+libNewVersion;
                if(!counter.containsKey(temp)){
                    counter.put(temp,1);
                }
                else {
                    tempValue = counter.get(temp);
                    tempValue++;
                    counter.put(temp,tempValue);
                }
                temp=groupID+":"+artifactID+":"+libOldVersion;
                if(!counter.containsKey(temp)){
                    counter.put(temp,1);
                }
                else {
                    tempValue = counter.get(temp);
                    tempValue++;
                    counter.put(temp,tempValue);
                }
                temp=groupID+":"+artifactID+":"+libNewVersion+":"+libOldVersion;
                if(!versionPairCounter.containsKey(temp)){
                    versionPairCounter.put(temp,1);
                }
                else {
                    tempValue = versionPairCounter.get(temp);
                    tempValue++;
                    versionPairCounter.put(temp,tempValue);
                }
//                if(!groupIDCounter.containsKey(groupID)){
//                    groupIDCounter.put(groupID,1);
//                }
//                else {
//                    tempValue = groupIDCounter.get(groupID);
//                    tempValue++;
//                    groupIDCounter.put(groupID,tempValue);
//                }
            }
//            counter = sortByValue2(counter);
            versionPairCounter = sortByValue2(versionPairCounter);
//            versionPairCounter = sortByValue2Ascending(versionPairCounter);
//            groupIDCounter = sortByValue2(groupIDCounter);
//            System.out.println("GroupID + ArtifactID: "+counter);
//            System.out.println("versionPairCounter: "+versionPairCounter);
//            System.out.println("GroupID: "+groupIDCounter);
//            result.add("GroupID + ArtifactID: "+counter);
            result.add("versionPairCounter: "+versionPairCounter);
//            result.add("GroupID: "+groupIDCounter);
//            pw.write("GroupID + ArtifactID: "+counter+'\n');
            pw.write("versionPairCounter: "+versionPairCounter+'\n');
//            pw.write("GroupID: "+groupIDCounter+'\n');
            pw.flush();
            pw.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    public static <T> LinkedHashMap<T, Integer> sortByValue2(Map<T, Integer> hm) {
        // HashMap???entry??????List???
        List<Map.Entry<T, Integer>> list =
                new LinkedList<Map.Entry<T, Integer>>(hm.entrySet());

        //  ???List???entry???value??????
        Collections.sort(list, new Comparator<Map.Entry<T, Integer>>() {
            public int compare(Map.Entry<T, Integer> o1,
                               Map.Entry<T, Integer> o2) {
                return -((o1.getValue()).compareTo(o2.getValue()));
            }
        });

        LinkedHashMap<T, Integer> res = new LinkedHashMap<>();
        for (Map.Entry<T, Integer> aa : list) {
            res.put(aa.getKey(), aa.getValue());
        }
        return res;
    }

    public static <T> LinkedHashMap<T, Integer> sortByValue2Ascending(Map<T, Integer> hm) {
        // HashMap???entry??????List???
        List<Map.Entry<T, Integer>> list =
                new LinkedList<Map.Entry<T, Integer>>(hm.entrySet());

        //  ???List???entry???value??????
        Collections.sort(list, new Comparator<Map.Entry<T, Integer>>() {
            public int compare(Map.Entry<T, Integer> o1,
                               Map.Entry<T, Integer> o2) {
                return ((o1.getValue()).compareTo(o2.getValue()));
            }
        });

        LinkedHashMap<T, Integer> res = new LinkedHashMap<>();
        for (Map.Entry<T, Integer> aa : list) {
            res.put(aa.getKey(), aa.getValue());
        }
        return res;
    }

    public static List<String> getFileLines(File checkedBCFiles) throws IOException {
        List<String> checkedBCFilesList = new ArrayList<>();
        String line;
        BufferedReader br = new BufferedReader(new FileReader(checkedBCFiles));
        while ( (line=br.readLine())!=null ) {
            checkedBCFilesList.add(line);
        }
        br.close();
        return checkedBCFilesList;
    }

    public static void downloadPopularMavenRepository(String popularLibListPath, String LibRootPath) throws Exception {
        BufferedReader popularLibs = new BufferedReader(new FileReader(new File(popularLibListPath))); // todo fix
        String line = popularLibs.readLine();
        List<String> popularLibList = new LinkedList<>();
        File breakingChangesDir = new File("breakingChanges/");
        if (!breakingChangesDir.exists()){breakingChangesDir.mkdirs();}

        File parsedBCFiles = new File("parsedBCFiles.txt");
        if (!parsedBCFiles.exists()){parsedBCFiles.createNewFile();}
        List<String> parsedBCFilesList = getFileLines(parsedBCFiles);
        BufferedWriter bw = new BufferedWriter(new FileWriter(parsedBCFiles,false));

        if(line!=null){
            String[] split = line.split("\\{")[1].split("}")[0].split(",");
            int i=0;
            int j =0;
            int lengthLibPars = split.length;
            for(String str:split){
                // ????????????????????????
                if(str.contains("junit")){continue;}
                if(str.contains("test")){continue;}
                if(str.contains("gwt")){continue;}
                if(str.contains("guava")){continue;}
                if(str.contains("jdt")){continue;}
                if(str.contains("truth")){continue;}
//                if(i<13){
//                    i++;
//                    continue;
//                }
                i++;
                logger.error("Parsing Library "+i + "/" + lengthLibPars);
//                if(i>=100){
//                    break;
//                }
                //
                String popularTimes = str.split("=")[1];
                String[] temp = str.split("=")[0].split(":");
                String groupId = temp[0];
                if(groupId.toCharArray()[0] == ' '){ groupId = groupId.split(" ")[1];}
                String artifactId = temp[1];
                String newVersion = temp[2];
                String oldVersion = temp[3];
                String newId = groupId+":"+artifactId+":"+newVersion;
                String oldId = groupId+":"+artifactId+":"+oldVersion;
                String versionPair = popularTimes+"_"+newId+"_"+oldId+".txt";
                if(oldVersion.equals("null")){continue;}
                if(oldVersion.contains("SNAPSHOT")){continue;}
                if(newVersion.contains("SNAPSHOT")){continue;}
                if(groupId.equals("org.quickfixj")){continue;}
                if(groupId.equals("org.jooq")){continue;}
                try {
                    logger.error("Downloading "+newId+" ...");
                    String newVersionDir = PomParser.DownloadMavenLib(newId);
                    logger.error("Downloading "+oldId+" ...");
                    String oldVersionDir = PomParser.DownloadMavenLib(oldId);
                    logger.error("ApiDiffing "+newId +" and "+oldId+" ...");
                    if(versionPair.equals("com.taobao.arthas:arthas-common:3.5.2_com.taobao.arthas:arthas-common:3.5.1.txt")){
                        logger.error("target");
                    }
                    if (parsedBCFilesList.contains(versionPair)){bw.write(versionPair+"\n");bw.flush();continue;}
                    APIDiff.apiDiff(oldVersionDir,newVersionDir,oldId,newId,"breakingChanges/"+versionPair);
                    bw.write(versionPair+"\n");bw.flush();
                }
                catch (Exception e){
                    e.printStackTrace();
                    continue;
                }
                j++;
                logger.error("Parsing APIDiff "+j);
            }
            bw.close();
        }

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

    public static void unzip(String zipFile,String outputPath) throws IOException {
        if (outputPath == null)
            outputPath = "";
        else
            outputPath += File.separator;

        // 1.0 Create output directory
        File outputDirectory = new File(outputPath);

        if (outputDirectory.exists())
            outputDirectory.delete();

        outputDirectory.mkdir();

        // 2.0 Unzip (create folders & copy files)

        // 2.1 Get zip input stream
        ZipInputStream zip = new ZipInputStream(new FileInputStream(zipFile));

        ZipEntry entry = null;
        int len;
        byte[] buffer = new byte[1024];

        // 2.2 Go over each entry "file/folder" in zip file
        while ((entry = zip.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
//                System.out.println("-" + entry.getName());

                // create a new file
                File file = new File(outputPath + entry.getName());

                // create file parent directory if does not exist
                if (!new File(file.getParent()).exists())
                    new File(file.getParent()).mkdirs();

                // get new file output stream
                FileOutputStream fos = new FileOutputStream(file);

                // copy bytes
                while ((len = zip.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
            }

        }
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

