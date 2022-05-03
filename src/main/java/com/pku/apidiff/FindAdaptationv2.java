package com.pku.apidiff;

//import apidiff.internal.util.UtilTools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pku.libupgrade.DiffCommit;
import com.pku.libupgrade.GitService;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static com.pku.apidiff.CleanAdaptation.isNoneBreakingChange;
import static com.pku.apidiff.CodeDiff.getFileCodeDiff;
import static com.pku.apidiff.CodeDiff.getGitCodeDiff;
import static com.pku.libupgrade.PomParser.USER_LOCAL_REPO;
import static com.pku.libupgrade.Utils.getFileLines;
import static com.pku.libupgrade.Utils.sortByValue2Ascending;


// 输入 breakingChanges, commitDiffs
// 输出 affectedCodes, adaptationCodes
public class FindAdaptationv2 {
    public static void main(String[] args) throws Exception {
        String breakingChangesDir = "breakingChanges1";
        File bcd = new File(breakingChangesDir);

        Map<BreakChangeSet,Integer> bks = new HashMap<>();
        int w =0;
        for(File f: Objects.requireNonNull(bcd.listFiles())){ // 对于每一组破坏性变更版本，提取所有的破坏性变更Signature
            if(f.getName().equals(".DS_Store")){continue;}
            if(f.getName().contains("junit")){continue;}
            if(f.getName().contains("test")){continue;}
            if(f.getName().contains("gwt")){continue;}
            if(f.getName().contains("guava")){continue;}
            if(f.getName().contains("jdt")){continue;}
            if(f.getName().contains("truth")){continue;}
            if(f.getName().split("_").length!=3){continue;}
            int temp = Integer.parseInt((f.getName().split("_")[0]));
            bks.put(new BreakChangeSet(f.getName().split("_")[1],f.getName().split("_")[2].split("\\.txt")[0]),temp);
        }
        bks = sortByValue2Ascending(bks);
        // UML Diff
        File checkedLibBCFiles = new File("checkedLibBCFiles.txt");
        if (!checkedLibBCFiles.exists()){checkedLibBCFiles.createNewFile();}
        List<String> checkedLibBCFilesList = getFileLines(checkedLibBCFiles);
        BufferedWriter bwLib = new BufferedWriter(new FileWriter(checkedLibBCFiles,false));
        int k =0;
        int lengthBCS = bks.size();
        for (BreakChangeSet key:bks.keySet()) {
            k++;
//            if(k<4989){continue;}
            logger.error("fetch lib "+k+"/"+lengthBCS+" BreakingChangeLibAndVersions...");
            String newLibId = key.newLibId;
            String oldLibId = key.oldLibId;
            String breakChangeFileName = breakingChangesDir+"/"+bks.get(key)+"_"+newLibId+"_"+oldLibId+".txt";
            if (checkedLibBCFilesList.contains(breakChangeFileName)){bwLib.write(breakChangeFileName+"\n");bwLib.flush();continue;}
            List<String> breakingChanges = getBreakingChanges(breakChangeFileName);
            for (String breakingChange:breakingChanges){
                if(isNoneBreakingChange(breakingChange)){continue;}
                logger.error("find affected and adaptation for "+breakingChange);
                getLibAdaptation(breakingChange,newLibId,oldLibId);
            }
            bwLib.write(breakChangeFileName+"\n");
            bwLib.flush();
        }
        bwLib.close();
        // UML Diff end

        // PKU Diff
        File checkedBCFiles = new File("checkedBCFiles.txt");
        if (!checkedBCFiles.exists()){checkedBCFiles.createNewFile();}
        List<String> checkedBCFilesList = getFileLines(checkedBCFiles);
        BufferedWriter bw = new BufferedWriter(new FileWriter(checkedBCFiles,false));
        k =0;
        for (BreakChangeSet key:bks.keySet()){
            k++;
            logger.error("fetch "+k+"/"+lengthBCS+" BreakingChangeLibAndVersions...");
            String newLibId = key.newLibId;
            String oldLibId = key.oldLibId;
            String breakChangeFileName = breakingChangesDir+"/"+bks.get(key)+"_"+newLibId+"_"+oldLibId+".txt";
            if (checkedBCFilesList.contains(breakChangeFileName)){bw.write(breakChangeFileName+"\n");bw.flush();continue;}
//            logger.error("getBreakingChangeSignatures..."+ breakChangeFileName);
            List<String> breakingChanges = getBreakingChanges(breakChangeFileName);
            for (String breakingChange:breakingChanges){
                if(isNoneBreakingChange(breakingChange)){continue;}
                logger.error("find affected and adaptation for "+breakingChange);
                getAdaptation(breakingChange,newLibId,oldLibId);
            }
            bw.write(breakChangeFileName+"\n");
            bw.flush();
        }
        bw.close();
        // PKU Diff end
    }

    public static void getLibAdaptation(String breakingChange,String newLibId,String oldLibId) throws Exception {
        String breakingChangeSuperType = breakingChange.split(":")[0]; // Type/Method/Field
        String breakingChangeSubType = breakingChange.split(":")[1].split("-")[0];
        String target = breakingChange.split("<code>")[1].split("</code>")[0];
        String parent;
        if(breakingChangeSuperType.equals("Type")){
            String[] targetList = target.split("\\.");
            String targetSimpleName = targetList[targetList.length-1];
            String ApiUsage = targetSimpleName;
            Map<String, List<TypeUsage>> oldVersion3rdLibTypeUsage = setTypeUsagesFromJson(oldLibId);
            getTypeAdaptationFromLib(oldVersion3rdLibTypeUsage, ApiUsage, oldLibId, newLibId,breakingChangeSuperType,breakingChangeSubType);
        }
        else if(breakingChangeSuperType.equals("Method")){
            parent = breakingChange.split("<code>")[2].split("</code>")[0];
            String[] parentList = parent.split("\\.");
            String parentSimpleName = parentList[parentList.length-1];
            String ApiUsage = getSignatureStringFromQualifiedName(target)+" "+parentSimpleName;
            Map<String, List<Caller>> oldVersion3rdLibCallers = setCallersFromJson(oldLibId);
            getMethodAdaptationFromLib(oldVersion3rdLibCallers, ApiUsage, oldLibId, newLibId,breakingChangeSuperType,breakingChangeSubType);
        }
        else if(breakingChangeSuperType.equals("Field")){
            parent = breakingChange.split("<code>")[2].split("</code>")[0];
            String[] parentList = parent.split("\\.");
            String parentSimpleName = parentList[parentList.length-1];
            String ApiUsage = parentSimpleName + " " + target;
            Map<String, List<FieldUsage>> oldVersion3rdLibFieldUsage = setFieldUsagesFromJson(oldLibId);
            getFieldAdaptationFromLib(oldVersion3rdLibFieldUsage, ApiUsage, oldLibId, newLibId,breakingChangeSuperType,breakingChangeSubType);
        }
    }

    public static void getMethodAdaptationFromLib(Map<String, List<Caller>> oldVersion3rdLibCallers, String ApiUsage, String oldLibId, String newLibId,String breakingChangeSuperType,String breakingChangeSubType) throws Exception {
        for (String key:oldVersion3rdLibCallers.keySet()){
            for(Caller callSig:oldVersion3rdLibCallers.get(key)){
                if (isEqualSignatureString(callSig.signature,ApiUsage)){// find affected code
                    String newProjectPath = getProjectPath(newLibId);
                    String filePathInProject = key.split("-sources")[1];
                    getFileCodeDiff(key,newProjectPath+filePathInProject,20,callSig.position,breakingChangeSuperType+":"+breakingChangeSubType+":"+callSig.signature,oldLibId,newLibId);
                }
            }
        }
    }

    public static void getTypeAdaptationFromLib(Map<String, List<TypeUsage>> oldVersion3rdLibTypeUsages, String ApiUsage, String oldLibId, String newLibId,String breakingChangeSuperType,String breakingChangeSubType) throws Exception {
        for (String key:oldVersion3rdLibTypeUsages.keySet()){
            for(TypeUsage typeUsage:oldVersion3rdLibTypeUsages.get(key)){
                if (ApiUsage.equals(typeUsage.typeName)){// find affected code
                    String newProjectPath = getProjectPath(newLibId);
                    String filePathInProject = key.split("-sources")[1];
                    getFileCodeDiff(key,newProjectPath+filePathInProject,20,typeUsage.position,breakingChangeSuperType+":"+breakingChangeSubType+":"+typeUsage.typeName,oldLibId,newLibId);
                }
            }
        }
    }

    public static void getFieldAdaptationFromLib(Map<String, List<FieldUsage>> oldVersion3rdLibFieldUsages, String ApiUsage, String oldLibId, String newLibId,String breakingChangeSuperType,String breakingChangeSubType) throws Exception {
        for (String key:oldVersion3rdLibFieldUsages.keySet()){
            for(FieldUsage fieldUsage:oldVersion3rdLibFieldUsages.get(key)){
                if (ApiUsage.equals(fieldUsage.typeName+" "+fieldUsage.fieldName)){// find affected code
                    String newProjectPath = getProjectPath(newLibId);
                    String filePathInProject = key.split("-sources")[1];
                    getFileCodeDiff(key,newProjectPath+filePathInProject,20,fieldUsage.position,breakingChangeSuperType+":"+breakingChangeSubType+":"+fieldUsage.typeName+" "+fieldUsage.fieldName,oldLibId,newLibId);
                }
            }
        }
    }

    public static Logger logger = LoggerFactory.getLogger(FindAdaptationv2.class);
    public static CompilationUnit getCompilationUnit(String javaFilePath) {
        byte[] input = null;
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(javaFilePath));
            input = new byte[bufferedInputStream.available()];
            bufferedInputStream.read(input);
            bufferedInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ASTParser astParser = ASTParser.newParser(AST.JLS8);
        astParser.setSource(new String(input).toCharArray());
        astParser.setKind(ASTParser.K_COMPILATION_UNIT);
        Hashtable<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
        astParser.setCompilerOptions(options);
        astParser.setEnvironment(null, null, null, true);
        astParser.setResolveBindings(true);
        astParser.setBindingsRecovery(true);
        astParser.setUnitName("any_name");
        CompilationUnit result = (CompilationUnit) (astParser.createAST(null));
        return result;
    }

    public static Map<String, List<Caller>> setClientCallersFromJson(String jsonPath) throws IOException {
        // libCallers/libName(g:a:v)
        Map<String, List<Caller>>  apiCallersMap = new HashMap<>();
        File json = new File(jsonPath);
        if (!json.exists()) {return apiCallersMap;}
        BufferedReader br = new BufferedReader(new FileReader(new File(jsonPath)));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode parser = mapper.readTree(br);
        for (Iterator<String> it = parser.fieldNames(); it.hasNext(); ) {
            String filePath = it.next();
            JsonNode item = parser.get(filePath);
            List<Caller> tempSig = new LinkedList<>();
            for(JsonNode j:item){
                Caller temp =new Caller(j.get("signature").asText(),j.get("position").asInt());
                tempSig.add(temp);
            }
            apiCallersMap.put(filePath,tempSig);
        }
        return apiCallersMap;
    }

    public static Map<String, List<TypeUsage>> setClientTypeUsageFromJson(String jsonPath) throws IOException {
        // libCallers/libName(g:a:v)
        Map<String, List<TypeUsage>>  apiTypeUsageMap = new HashMap<>();
        File json = new File(jsonPath);
        if (!json.exists()) {return apiTypeUsageMap;}
        BufferedReader br = new BufferedReader(new FileReader(new File(jsonPath)));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode parser = mapper.readTree(br);
        for (Iterator<String> it = parser.fieldNames(); it.hasNext(); ) {
            String filePath = it.next();
            JsonNode item = parser.get(filePath);
            List<TypeUsage> tempSig = new LinkedList<>();
            for(JsonNode j:item){
                TypeUsage temp =new TypeUsage(j.get("typeName").asText(),j.get("position").asInt());
                tempSig.add(temp);
            }
            apiTypeUsageMap.put(filePath,tempSig);
        }
        return apiTypeUsageMap;
    }

    public static Map<String, List<FieldUsage>> setClientFieldUsageFromJson(String jsonPath) throws IOException {
        // libCallers/libName(g:a:v)
        Map<String, List<FieldUsage>>  apiFieldUsageMap = new HashMap<>();
        File json = new File(jsonPath);
        if (!json.exists()) {return apiFieldUsageMap;}
        BufferedReader br = new BufferedReader(new FileReader(new File(jsonPath)));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode parser = mapper.readTree(br);
        for (Iterator<String> it = parser.fieldNames(); it.hasNext(); ) {
            String filePath = it.next();
            JsonNode item = parser.get(filePath);
            List<FieldUsage> tempSig = new LinkedList<>();
            for(JsonNode j:item){
                FieldUsage temp =new FieldUsage(j.get("typeName").asText(),j.get("fieldName").asText(),j.get("position").asInt());
                tempSig.add(temp);
            }
            apiFieldUsageMap.put(filePath,tempSig);
        }
        return apiFieldUsageMap;
    }

    public static void saveCallers(String clientName, Map<String,List<Caller>> allCallerSignaturesOfProject, String commit) throws IOException {
        // libCallers/libName(g:a:v)
        if(allCallerSignaturesOfProject.size()==0){return;}
        String jsonDir = "clientCallers/"+clientName+"/";
        File jsonPathDir = new File(jsonDir);
        if (!jsonPathDir.exists()){jsonPathDir.mkdirs();}
        String jsonPath = jsonDir +commit+ ".json";
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(jsonPath)));
        ObjectMapper mapper = new ObjectMapper();
        bw.write(mapper.writeValueAsString(allCallerSignaturesOfProject));
        bw.close();
    }

    public static void saveTypeUsages(String clientName, Map<String,List<TypeUsage>> allTypeUsagesOfProject, String commit) throws IOException {
        // libCallers/libName(g:a:v)
        if(allTypeUsagesOfProject.size()==0){return;}
        String jsonDir = "clientTypeUsages/"+clientName+"/";
        File jsonPathDir = new File(jsonDir);
        if (!jsonPathDir.exists()){jsonPathDir.mkdirs();}
        String jsonPath = jsonDir +commit+ ".json";
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(jsonPath)));
        ObjectMapper mapper = new ObjectMapper();
        bw.write(mapper.writeValueAsString(allTypeUsagesOfProject));
        bw.close();
    }

    public static void saveFieldUsages(String clientName, Map<String,List<FieldUsage>> allFieldUsagesOfProject, String commit) throws IOException {
        // libCallers/libName(g:a:v)
        if(allFieldUsagesOfProject.size()==0){return;}
        String jsonDir = "clientFieldUsages/"+clientName+"/";
        File jsonPathDir = new File(jsonDir);
        if (!jsonPathDir.exists()){jsonPathDir.mkdirs();}
        String jsonPath = jsonDir +commit+ ".json";
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(jsonPath)));
        ObjectMapper mapper = new ObjectMapper();
        bw.write(mapper.writeValueAsString(allFieldUsagesOfProject));
        bw.close();
    }

    public static ApiUsage getAllApiUsageProjectLeveFromUsageJsonOrAST(String  projectPath, File file, String commit) throws IOException {
//        File targetDir = new File(targetPath);
        Map<String,List<Caller>> allCallerSignaturesOfProject;
        Map<String,List<TypeUsage>> allTypeUsagesOfProject;
        Map<String,List<FieldUsage>> allFieldUsagesOfProject;
        ApiUsage apiUsage;
        String callerJsonName = "clientCallers/"+file.getName()+"/"+commit+ ".json";
        String typeUsageJsonName = "clientTypeUsages/"+file.getName()+"/"+commit+ ".json";
        String fieldUsageJsonName = "clientFieldUsages/"+file.getName()+"/"+commit+ ".json";
        File callerJsonFile = new File(callerJsonName);
        File typeUsageJsonFile = new File(typeUsageJsonName);
        File fieldUsageJsonFile = new File(fieldUsageJsonName);
        if (callerJsonFile.exists() && typeUsageJsonFile.exists() && fieldUsageJsonFile.exists()){ // 已经保存了该commit的all caller
            logger.error("getAllApiUsageProjectLeveFromJson");
            allCallerSignaturesOfProject = setClientCallersFromJson(callerJsonName);
            allTypeUsagesOfProject = setClientTypeUsageFromJson(typeUsageJsonName);
            allFieldUsagesOfProject = setClientFieldUsageFromJson(fieldUsageJsonName);
            apiUsage = new ApiUsage(allCallerSignaturesOfProject,allTypeUsagesOfProject,allFieldUsagesOfProject);
            return apiUsage;
        }
        allCallerSignaturesOfProject = new HashMap<>();
        allTypeUsagesOfProject = new HashMap<>();
        allFieldUsagesOfProject = new HashMap<>();
        logger.error("getAllApiUsageProjectLeveFromAST");
        try {
            Repository r = GitService.openRepository(projectPath);
            GitService.checkout(r,commit);
            getAllApiUsageProjectLeve(projectPath,file,allCallerSignaturesOfProject,allTypeUsagesOfProject,allFieldUsagesOfProject);
        }
        catch (Exception e){
            logger.error("getAllApiUsageProjectLeveFromAST failed");
            e.printStackTrace();
        }
        saveCallers(file.getName(),allCallerSignaturesOfProject,commit);
        saveTypeUsages(file.getName(),allTypeUsagesOfProject,commit);
        saveFieldUsages(file.getName(),allFieldUsagesOfProject,commit);
        apiUsage = new ApiUsage(allCallerSignaturesOfProject,allTypeUsagesOfProject,allFieldUsagesOfProject);
        return apiUsage;

    }

    public static void getAllApiUsageProjectLeve(String  projectPath, File file, Map<String,List<Caller>> allCallerSignaturesOfProject, Map<String,List<TypeUsage>> allTypeUsagesOfProject, Map<String,List<FieldUsage>> allFieldUsagesOfProject) throws IOException {
//        File targetDir = new File(targetPath);
        if (file.isDirectory()){ // 未保存了该commit的all caller, 静态分析并提取
            for(File f: Objects.requireNonNull(file.listFiles())){
               if (f.isDirectory()) getAllApiUsageProjectLeve(projectPath,f,allCallerSignaturesOfProject,allTypeUsagesOfProject,allFieldUsagesOfProject);
               if (f.isFile() && f.getName().contains(".java")) getAllApiUsageFileLevel(f.getAbsolutePath(),allCallerSignaturesOfProject,allTypeUsagesOfProject,allFieldUsagesOfProject);
            }
        }
        else {
            logger.error("Error for getAllSignatureProjectLeve");
        }
    }

    public static void getAllApiUsageFileLevel(String targetPath, Map<String,List<Caller>> allCallerSignaturesOfProject, Map<String,List<TypeUsage>> allTypeUsagesOfProject, Map<String,List<FieldUsage>> allFieldUsagesOfProject) throws IOException {
        CompilationUnit compilationUnit = getCompilationUnit(targetPath);
        MethodInvocationVisitor visitorMethodInvocation = new MethodInvocationVisitor(); // todo 提取Signature，然后再做匹配
        compilationUnit.accept(visitorMethodInvocation);
        allCallerSignaturesOfProject.put(targetPath,visitorMethodInvocation.callerSignatures);
        allTypeUsagesOfProject.put(targetPath,visitorMethodInvocation.apiTypeUsages);
        allFieldUsagesOfProject.put(targetPath,visitorMethodInvocation.apiFieldUsages);
    }

    public static List<DiffCommit> findAffectedClientDiffCommit (String commitDiffCSVPath, String newVersion,String oldVersion) throws IOException {
        List<DiffCommit> result = new LinkedList<>();
        BufferedReader br = new BufferedReader(new FileReader(new File(commitDiffCSVPath)));
        String line;
        DiffCommit diffCommit;
        String targetGroupId = oldVersion.split(":")[0];
        String targetArtifactId = oldVersion.split(":")[1];
        String targetVersionId = oldVersion.split(":")[2];
        String targetNewVersion = newVersion.split(":")[2];
        while ( (line=br.readLine())!=null ){
            diffCommit = new DiffCommit(line);
            if (targetGroupId.equals(diffCommit.libName.split("\t")[0])
                    && targetArtifactId.equals(diffCommit.libName.split("\t")[1])
                    && targetVersionId.equals(diffCommit.oldVersion)
                    && targetNewVersion.equals(diffCommit.newVersion)){
                    result.add(diffCommit);
            }
        }
        br.close();
        return result;
    }
    public static List<String> getBreakingChanges(String breakingChangeFilePath) throws IOException {
        List<String> bkcs = new LinkedList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(breakingChangeFilePath)));
            String line;
            while ( (line=br.readLine())!=null ){
                bkcs.add(line);
            }
            br.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return bkcs;
    }

    // need to add other breaking change except method remove
//    public static List<String> getMethodRemoveBreakingChanges(String breakingChangeFilePath) throws IOException {
//        List<String> bkcs = new LinkedList<>();
//        try {
//            BufferedReader br = new BufferedReader(new FileReader(new File(breakingChangeFilePath)));
//            String line;
//            while ( (line=br.readLine())!=null ){
//                if (line.split(":")[0].equals("Method")){
//                    if (line.split(" ")[1].equals("Remove")){
//                        bkcs.add(line.split("<code>")[1].split("</code>")[0] + "-"+ line.split("<code>")[2].split("</code>")[0]);
//                    }
//                }
//            }
//            br.close();
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//        return bkcs;
//    }
//    public static List<String> getTypeRemoveBreakingChanges(String breakingChangeFilePath) throws IOException {
//        List<String> bkcs = new LinkedList<>();
//        try {
//            BufferedReader br = new BufferedReader(new FileReader(new File(breakingChangeFilePath)));
//            String line;
//            while ( (line=br.readLine())!=null ){
//                if (line.split(":")[0].equals("Type")){
//                    if (line.split(" ")[1].equals("Remove")){
//                        bkcs.add(line.split("<code>")[1].split("</code>")[0]);
//                    }
//                }
//            }
//            br.close();
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//        return bkcs;
//    }

//    public static List<String> getFieldRemoveBreakingChanges(String breakingChangeFilePath) throws IOException {
//        List<String> bkcs = new LinkedList<>();
//        try {
//            BufferedReader br = new BufferedReader(new FileReader(new File(breakingChangeFilePath)));
//            String line;
//            while ( (line=br.readLine())!=null ){
//                if (line.split(":")[0].equals("Field")){
//                    if (line.split(" ")[1].equals("Remove")){
//                        bkcs.add(line.split("<code>")[1].split("</code>")[0] + "-"+ line.split("<code>")[2].split("</code>")[0]);
//                    }
//                }
//            }
//            br.close();
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//        return bkcs;
//    }

//    public static List<String> getSignatureList(String s) {
//        List<String> signature = new LinkedList<>();
//        // targetSignature
//        String targetSignature = s.split("-")[0];
//        String targetObjectType = s.split("-")[1];
//        String[] targetObjectTypeStack = targetObjectType.split("\\.");
//
//        // return type
//        signature.add(targetSignature.split(" ")[0]);
//
//        // method name
//        signature.add(targetSignature.split("\\(")[0].split(" ")[1]);
//
//        // parameter list
//        if(targetSignature.split("\\(")[1].equals(")")) return signature;
//        String list = targetSignature.split("\\(")[1].split("\\)")[0];
//        for (String para : list.split(",")) {
//            Integer size = para.split(" ").length;
//            if(size==1){
//                signature.add(para.split(" ")[size - 1]);
//            }
//            else{
//                signature.add(para.split(" ")[size - 2]);
//            }
//        }
//
//        // target object
//        signature.add(targetObjectTypeStack[targetObjectTypeStack.length-1]);
//        return signature;
//    }

    public static String getSignatureStringFromQualifiedName(String targetSignature) {
//        List<String> signature = new LinkedList<>();
        StringBuilder signature = new StringBuilder();
        // targetSignature

        // return type
        signature.append(targetSignature.split(" ")[0]);

        // method name
        signature.append(" ").append(targetSignature.split("\\(")[0].split(" ")[1]);

        // parameter list
        if(targetSignature.split("\\(")[1].equals(")")) return signature.toString();
        String list = targetSignature.split("\\(")[1].split("\\)")[0];
        for (String para : list.split(",")) {
            Integer size = para.split(" ").length;
            if(size==1){
                signature.append(" ").append(para.split(" ")[size - 1]);
            }
            else{
                signature.append(" ").append(para.split(" ")[size - 2]);
            }
        }

        return signature.toString();
    }

    public static boolean isBadSignature(List<String> signature){
        int count = 0;
        if (signature.get(1).equals("equals")){return true;}
        for (String s:signature){
            if(!s.equals("null")){count+=1;}
        }
        return count==1;
    }

//    public static Map<String,List<AffectedSignature>> getAffectedCallerDiffCommit(String signature, DiffCommit diffCommit) throws Exception {
//        List<String> targetSignature =  getSignatureList(signature);
//        Map<String,List<AffectedSignature>> affectedCaller = new HashMap<>();
//        String clientPath =  "../dataset/"+diffCommit.clientName;
//        logger.error("getAllSignatureProjectLeve..." + clientPath);
//        File client = new File(clientPath);
//        ApiUsage apiUsage = getAllApiUsageProjectLeveFromUsageJsonOrAST(clientPath,client,diffCommit.commit);
//        Map<String,List<Caller>> allCallerSignaturesOfProject = apiUsage.allCallersOfProject;
//        for(String filePath: allCallerSignaturesOfProject.keySet()){
//            List<AffectedSignature> temp = new ArrayList<>();
//            for(Caller s: allCallerSignaturesOfProject.get(filePath)){
//                List<String> nowSignature = Arrays.asList(s.signature.split(" "));
//                if(isEqualSignatureStringList(nowSignature,targetSignature)){
//                    temp.add(new AffectedSignature(s,diffCommit));
//                }
//            }
//            if (temp.size()!=0)
//                affectedCaller.put(filePath,temp);
//        }
//        return affectedCaller;
//    }

//    public static Map<String,List<AffectedTypeUsage>> getAffectedTypeUsageDiffCommit(String targetTypeUsage, DiffCommit diffCommit) throws Exception {
//        Map<String,List<AffectedTypeUsage>> affectedTypeUsage = new HashMap<>();
//        String clientPath =  "../dataset/"+diffCommit.clientName;
//        logger.error("getAllSignatureProjectLeve..." + clientPath);
//        File client = new File(clientPath);
//        ApiUsage apiUsage = getAllApiUsageProjectLeveFromUsageJsonOrAST(clientPath,client,diffCommit.commit);
//        Map<String,List<TypeUsage>> allTypeUsagesOfProject = apiUsage.allTypeUsagesOfProject;
//        for(String filePath: allTypeUsagesOfProject.keySet()){
//            List<AffectedTypeUsage> temp = new ArrayList<>();
//            for(TypeUsage s: allTypeUsagesOfProject.get(filePath)){
//                String nowTypeUsage = s.typeName;
//                if(nowTypeUsage.equals(targetTypeUsage)){
//                    temp.add(new AffectedTypeUsage(s,diffCommit));
//                }
//            }
//            if (temp.size()!=0)
//                affectedTypeUsage.put(filePath,temp);
//        }
//        return affectedTypeUsage;
//    }

//    public static Map<String,List<AffectedFieldUsage>> getAffectedFieldUsageDiffCommit(String targetTypeUsage, DiffCommit diffCommit) throws Exception {
//        Map<String,List<AffectedFieldUsage>> affectedFieldUsage = new HashMap<>();
//        String clientPath =  "../dataset/"+diffCommit.clientName;
//        logger.error("getAllSignatureProjectLeve..." + clientPath);
//        File client = new File(clientPath);
//        ApiUsage apiUsage = getAllApiUsageProjectLeveFromUsageJsonOrAST(clientPath,client,diffCommit.commit);
//        Map<String,List<FieldUsage>> allFieldUsagesOfProject = apiUsage.allFieldUsagesOfProject;
//        for(String filePath: allFieldUsagesOfProject.keySet()){
//            List<AffectedFieldUsage> temp = new ArrayList<>();
//            for(FieldUsage s: allFieldUsagesOfProject.get(filePath)){
//                String nowTypeUsage = s.typeName+"-"+s.fieldName;
//                if(nowTypeUsage.equals(targetTypeUsage)){
//                    temp.add(new AffectedFieldUsage(s,diffCommit));
//                }
//            }
//            if (temp.size()!=0)
//                affectedFieldUsage.put(filePath,temp);
//        }
//        return affectedFieldUsage;
//    }

    public static Map<String,List<AffectedCode>> getAffectedFromTypeUsage(String targetApiUsage,DiffCommit diffCommit, Map<String,List<TypeUsage>> ApiUsages, String breakingChangeSuperType, String breakingChangeSubType) throws Exception {
        Map<String,List<AffectedCode>> affectedCodes = new HashMap<>();
        for(String filePath: ApiUsages.keySet()){
            List<AffectedCode> temp = new ArrayList<>();
            for(TypeUsage s: ApiUsages.get(filePath)){
                String nowApiUsage = s.typeName;
                if(nowApiUsage.equals(targetApiUsage)){
                    temp.add(new AffectedCode(breakingChangeSuperType+":"+breakingChangeSubType+":"+nowApiUsage, s.position, diffCommit));
                }
            }
            if (temp.size()!=0)
                affectedCodes.put(filePath,temp);
        }
        return affectedCodes;
    }

    public static Map<String,List<AffectedCode>> getAffectedFromFieldUsage(String targetApiUsage,DiffCommit diffCommit, Map<String,List<FieldUsage>> ApiUsages, String breakingChangeSuperType, String breakingChangeSubType) throws Exception {
        Map<String,List<AffectedCode>> affectedCodes = new HashMap<>();
        for(String filePath: ApiUsages.keySet()){
            List<AffectedCode> temp = new ArrayList<>();
            for(FieldUsage s: ApiUsages.get(filePath)){
                String nowApiUsage = s.typeName+" "+s.fieldName;
                if(nowApiUsage.equals(targetApiUsage)){
                    temp.add(new AffectedCode(breakingChangeSuperType+":"+breakingChangeSubType+":"+nowApiUsage, s.position, diffCommit));
                }
            }
            if (temp.size()!=0)
                affectedCodes.put(filePath,temp);
        }
        return affectedCodes;
    }

    public static Map<String,List<AffectedCode>> getAffectedFromMethodUsage(String targetApiUsage,DiffCommit diffCommit, Map<String,List<Caller>> ApiUsages, String breakingChangeSuperType, String breakingChangeSubType) throws Exception {
        Map<String,List<AffectedCode>> affectedCodes = new HashMap<>();
        for(String filePath: ApiUsages.keySet()){
            List<AffectedCode> temp = new ArrayList<>();
            for(Caller s: ApiUsages.get(filePath)){
                String nowApiUsage = s.signature;
                if(isEqualSignatureString(nowApiUsage,targetApiUsage)){
                    temp.add(new AffectedCode(breakingChangeSuperType+":"+breakingChangeSubType+":"+nowApiUsage, s.position, diffCommit));
                }
            }
            if (temp.size()!=0)
                affectedCodes.put(filePath,temp);
        }
        return affectedCodes;
    }

    public static Map<String,List<AffectedCode>> getAffectedApiUsageDiffCommit(String targetApiUsage, DiffCommit diffCommit, String breakingChangeSuperType, String breakingChangeSubType) throws Exception {
        String clientPath =  "../dataset/"+diffCommit.clientName;
        logger.error("getAllSignatureProjectLeve..." + clientPath);
        File client = new File(clientPath);
        ApiUsage apiUsage = getAllApiUsageProjectLeveFromUsageJsonOrAST(clientPath,client,diffCommit.commit);
        Map<String,List<AffectedCode>> affectedCodes;
        if(breakingChangeSuperType.equals("Type")){
            affectedCodes = getAffectedFromTypeUsage(targetApiUsage,diffCommit,apiUsage.allTypeUsagesOfProject,breakingChangeSuperType,breakingChangeSubType);
//            if (breakingChangeSubType.contains("Remove Type")){
//                affectedCodes = getAffectedFromTypeUsage(targetApiUsage,diffCommit,apiUsage.allTypeUsagesOfProject,breakingChangeSuperType,breakingChangeSubType);
//            }
        }else if (breakingChangeSuperType.equals("Method")){
            affectedCodes = getAffectedFromMethodUsage(targetApiUsage,diffCommit,apiUsage.allCallersOfProject,breakingChangeSuperType,breakingChangeSubType);
//            if(breakingChangeSubType.contains("Remove Method")){
//                affectedCodes = getAffectedFromMethodUsage(targetApiUsage,diffCommit,apiUsage.allCallersOfProject,breakingChangeSuperType,breakingChangeSubType);
//            }
        }else {//if (breakingChangeSuperType.equals("Field")){
            affectedCodes = getAffectedFromFieldUsage(targetApiUsage,diffCommit,apiUsage.allFieldUsagesOfProject,breakingChangeSuperType,breakingChangeSubType);
//            if (breakingChangeSubType.contains("Remove Field")){
//                affectedCodes = getAffectedFromFieldUsage(targetApiUsage,diffCommit,apiUsage.allFieldUsagesOfProject,breakingChangeSuperType,breakingChangeSubType);
//            }
        }
        return affectedCodes;
    }

//    public static void getMethodRemoveCodePathFromDiffCommit(Map<String,List<AffectedSignature>> affected) throws Exception {
//        for(String filePath:affected.keySet()){
//            for(AffectedSignature a:affected.get(filePath)){
//                try {
//                    getGitCodeDiff(filePath,a.diffCommit,5,a.signature.position,a.signature.signature);
//                }
//                catch (Exception e){
//                    logger.error("get code diff error");
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

    public static void getCodePathFromDiffCommit(Map<String,List<AffectedCode>> affected) throws Exception {
        for(String filePath:affected.keySet()){
            for(AffectedCode a:affected.get(filePath)){
                try {
                    getGitCodeDiff(filePath,a.diffCommit,5,a.position,a.name);
                }
                catch (Exception e){
                    logger.error("get code diff error");
                    e.printStackTrace();
                }
            }
        }
    }

//    public static void getTypeRemoveCodePathFromDiffCommit(Map<String,List<AffectedTypeUsage>> affected) throws Exception {
//        for(String filePath:affected.keySet()){
//            for(AffectedTypeUsage a:affected.get(filePath)){
//                try {
//                    getGitCodeDiff(filePath,a.diffCommit,5,a.typeUsage.position,a.typeUsage.typeName);
//                }
//                catch (Exception e){
//                    logger.error("get code diff error");
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//    public static void getFieldRemoveCodePathFromDiffCommit(Map<String,List<AffectedFieldUsage>> affected) throws Exception {
//        for(String filePath:affected.keySet()){
//            for(AffectedFieldUsage a:affected.get(filePath)){
//                try {
//                    getGitCodeDiff(filePath,a.diffCommit,5,a.fieldUsage.position,a.fieldUsage.typeName+"-"+a.fieldUsage.fieldName);
//                }
//                catch (Exception e){
//                    logger.error("get code diff error");
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

    public static Map<String, List<Caller>> setCallersFromJson(String libName) throws IOException {
        // libCallers/libName(g:a:v)
        Map<String, List<Caller>>  apiCallersMap = new HashMap<>();
        String jsonPath = "libCallers/"+ libName+".json";
        File json = new File(jsonPath);
        if (!json.exists()) {return apiCallersMap;}
        BufferedReader br = new BufferedReader(new FileReader(new File(jsonPath)));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode parser = mapper.readTree(br);
        for (Iterator<String> it = parser.fieldNames(); it.hasNext(); ) {
            String filePath = it.next();
            JsonNode item = parser.get(filePath);
            List<Caller> tempSig = new LinkedList<>();
            for(JsonNode j:item){
                Caller temp =new Caller(j.get("signature").asText(),j.get("position").asInt());
                tempSig.add(temp);
            }
            apiCallersMap.put(filePath,tempSig);
        }
        return apiCallersMap;
    }

    public static Map<String, List<TypeUsage>> setTypeUsagesFromJson(String libName) throws IOException {
        // libCallers/libName(g:a:v)
        Map<String, List<TypeUsage>>  apiCallersMap = new HashMap<>();
        String jsonPath = "libTypeUsages/"+ libName+".json";
        File json = new File(jsonPath);
        if (!json.exists()) {return apiCallersMap;}
        BufferedReader br = new BufferedReader(new FileReader(new File(jsonPath)));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode parser = mapper.readTree(br);
        for (Iterator<String> it = parser.fieldNames(); it.hasNext(); ) {
            String filePath = it.next();
            JsonNode item = parser.get(filePath);
            List<TypeUsage> tempSig = new LinkedList<>();
            for(JsonNode j:item){
                TypeUsage temp =new TypeUsage(j.get("typeName").asText(),j.get("position").asInt());
                tempSig.add(temp);
            }
            apiCallersMap.put(filePath,tempSig);
        }
        return apiCallersMap;
    }

    public static Map<String, List<FieldUsage>> setFieldUsagesFromJson(String libName) throws IOException {
        // libCallers/libName(g:a:v)
        Map<String, List<FieldUsage>>  apiCallersMap = new HashMap<>();
        String jsonPath = "libFieldUsages/"+ libName+".json";
        File json = new File(jsonPath);
        if (!json.exists()) {return apiCallersMap;}
        BufferedReader br = new BufferedReader(new FileReader(new File(jsonPath)));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode parser = mapper.readTree(br);
        for (Iterator<String> it = parser.fieldNames(); it.hasNext(); ) {
            String filePath = it.next();
            JsonNode item = parser.get(filePath);
            List<FieldUsage> tempSig = new LinkedList<>();
            for(JsonNode j:item){
                FieldUsage temp =new FieldUsage(j.get("typeName").asText(),j.get("fieldName").asText(),j.get("position").asInt());
                tempSig.add(temp);
            }
            apiCallersMap.put(filePath,tempSig);
        }
        return apiCallersMap;
    }

//    public static void findAffectedCodeAndAdaptationFrom3rdLib(Map<String, List<Caller>> oldVersion3rdLibCallers, List<String> breakingChangeSignaturesQualifiedName, String oldLibId, String newLibId) throws Exception {
//        for (String key:oldVersion3rdLibCallers.keySet()){
//            for(Caller callSig:oldVersion3rdLibCallers.get(key)){
//                for(String breakQualifiedSig:breakingChangeSignaturesQualifiedName){
//                    if (isEqualSignatureStringAndList(callSig.signature, getSignatureList(breakQualifiedSig))){// find affected code
//                        String newProjectPath = getProjectPath(newLibId);
//                        String filePathInProject = key.split("sources")[1];
//                        getCodePathFromLibSelf(key,newProjectPath+filePathInProject,callSig,oldLibId,newLibId);
//                    }
//                }
//            }
//        }
//    }

    public static String getProjectPath(String id){
        String groupId=id.split(":")[0],artifactId=id.split(":")[1],versionId =id.split(":")[2];
        String localCentral = USER_LOCAL_REPO+"/";
        String filePath = "";
        for(String temp:groupId.split("\\.")){
            filePath = filePath + temp + "/";
        }
        filePath = filePath + artifactId + "/";
        filePath = filePath + versionId + "/";
        filePath = filePath + artifactId + "-" + versionId + "-sources";
        return localCentral+filePath;
    }

    public static void getCodePathFromLibSelf(String oldFilePath, String newFilePath, Caller callSig, String oldId, String newId) throws Exception {
        // filePath1 filePath2
        getFileCodeDiff(oldFilePath,newFilePath,20,callSig.position,callSig.signature,oldId,newId);
    }

    public static boolean isEqualSignatureString(String sig1, String sig2){
        List<String> sig1List = Arrays.asList(sig1.split(" "));
        List<String> sig2List = Arrays.asList(sig2.split(" "));
        return isEqualSignatureStringList(sig1List,sig2List);
    }

    public static boolean isEqualSignatureStringAndList(String sig1, List<String> sig2List){
        List<String> sig1List = Arrays.asList(sig1.split(" "));
        return isEqualSignatureStringList(sig1List,sig2List);
    }

    public static boolean isEqualSignatureStringList(List<String>  sig1List, List<String> sig2List){
        // Signature not equal on length
        if (sig1List.size()!=sig2List.size()){
            return false;
        }

        if (isBadSignature(sig1List) || isBadSignature(sig2List)) {
//            logger.error("find a bad signature");
            return false;
        }

        Boolean isEqual = true;
        for(Integer i=0; i<sig1List.size();i++){
            if(sig1List.get(i).equals("null") || sig2List.get(i).equals("null")){
                continue;
            }
            if(!sig1List.get(i).equals(sig2List.get(i))){ // todo fix type analysis
                isEqual = false;
                break;
            }
        }
        return isEqual;
    }

    public static void getAdaptation(String breakingChange,String newLibId,String oldLibId) throws Exception {
        Map<String,List<AffectedCode>> affectedCodes=null;
        Set<String> checkedCommit = new HashSet<>();
        List<DiffCommit> possibleAffectedDiffCommit = findAffectedClientDiffCommit("commitDiff1.csv",newLibId,oldLibId);
        logger.error("possibleAffectedDiffCommit.size: "+ possibleAffectedDiffCommit.size());
        String breakingChangeSuperType = breakingChange.split(":")[0]; // Type/Method/Field
        String breakingChangeSubType = breakingChange.split(":")[1].split("-")[0];
        String target = breakingChange.split("<code>")[1].split("</code>")[0];
        String parent;
        int j =0;
        for(DiffCommit d:possibleAffectedDiffCommit) {
            if(checkedCommit.contains(d.commit)){logger.error("This DiffCommit has checked...");continue;}checkedCommit.add(d.commit);
            // Find Affected position
            if (breakingChangeSuperType.equals("Type")){
                if(breakingChangeSubType.contains("Remove Type")){
                    String[] targetList = target.split("\\.");
                    String targetSimpleName = targetList[targetList.length-1];
                    affectedCodes = retryGetAffectedApiUsageDiffCommit(targetSimpleName,d,0,1,breakingChangeSuperType,breakingChangeSubType);
                }
            }
            else if (breakingChangeSuperType.equals("Method")){
                parent = breakingChange.split("<code>")[2].split("</code>")[0];
                String[] parentList = parent.split("\\.");
                String parentSimpleName = parentList[parentList.length-1];
                if(breakingChangeSubType.contains("Remove Method")){
                    affectedCodes = retryGetAffectedApiUsageDiffCommit(getSignatureStringFromQualifiedName(target)+" "+parentSimpleName,d,0,1,breakingChangeSuperType,breakingChangeSubType);
                }
            }
            else {
                parent = breakingChange.split("<code>")[2].split("</code>")[0];
                String[] parentList = parent.split("\\.");
                String parentSimpleName = parentList[parentList.length-1];
                if(breakingChangeSubType.contains("Remove Field")){
                    affectedCodes = retryGetAffectedApiUsageDiffCommit(parentSimpleName+" "+target,d,0,1,breakingChangeSuperType,breakingChangeSubType);
                }
            }
            // Extract Adaptation
            if (affectedCodes!=null && affectedCodes.size()!=0){
                logger.error("affectedCode.size: "+affectedCodes.size());
                getCodePathFromDiffCommit(affectedCodes);
            }
            else {
                logger.error("no affectedCode or other breaking change types...");
            }
        }
    }

//    public static void getMethodRemoveAdaptation(List<String> methodRemoveSignaturesQualifiedName,String newLibId,String oldLibId) throws Exception {
//        // pkuDiff
//        Map<String,List<AffectedSignature>> affectedCode;
//        List<String> checkedSignature = new LinkedList<>();
////        logger.error("findAffectedClientDiffCommit...");
//        List<DiffCommit> possibleAffectedDiffCommit = findAffectedClientDiffCommit("commitDiff1.csv",newLibId,oldLibId);
//        logger.error("methodRemoveSignaturesQualifiedName.size: "+ methodRemoveSignaturesQualifiedName.size());
//        logger.error("possibleAffectedDiffCommit.size: "+ possibleAffectedDiffCommit.size());
////        logger.error("getAffectedDiffCommit...");
//        int i =0;
//        for(String s: methodRemoveSignaturesQualifiedName){ // 对于所有的破坏性变更Signature Qualitative qualified
//            List<String> targetSignature =  getSignatureList(s);
//            if (isBadSignature(targetSignature)) {
//                logger.error("skip bad methodRemoveSignaturesQualifiedName..."+s+ " "+ newLibId + " "+ oldLibId);
//                continue;
//            }
//            if (checkedSignature.contains(s)) {
//                logger.error("skip checked methodRemoveSignaturesQualifiedName..."+s+ " "+ newLibId + " "+ oldLibId);
//                continue;
//            }
//            checkedSignature.add(s);
//            logger.error("for methodRemoveSignaturesQualifiedName..."+s+ " "+ newLibId + " "+ oldLibId);
//            Set<String> checkedCommit = new HashSet<>();
//            int j =0;
//            for(DiffCommit d:possibleAffectedDiffCommit){
//                logger.error("for DiffCommit..."+d);
//                if(checkedCommit.contains(d.commit)){
//                    logger.error("This DiffCommit has checked...");
//                    continue;
//                }
//                affectedCode = retryGetAffectedCallerDiffCommit(s,d,0,1);
//                if (affectedCode.size()!=0){
//                    checkedCommit.add(d.commit);
//                    logger.error("affectedCode.size: "+affectedCode.size());
//                    logger.error("getCodePathFromDiffCommit...");
//                    getMethodRemoveCodePathFromDiffCommit(affectedCode);
//                }
//                else {
//                    logger.error("noAffectedCode...");
//                }
//            }
//        }
//        // pkuDiff end
//    }

//    public static void getTypeRemoveAdaptation(List<String> typeRemoveQualifiedName,String newLibId,String oldLibId) throws Exception {
//        // pkuDiff
//        Map<String,List<AffectedTypeUsage>> affectedCode;
//        List<String> checkedTypeRemove = new LinkedList<>();
//        logger.error("findAffectedClientDiffCommit...");
//        List<DiffCommit> possibleAffectedDiffCommit = findAffectedClientDiffCommit("commitDiff1.csv",newLibId,oldLibId);
//        logger.error("typeRemoveQualifiedName.size: "+ typeRemoveQualifiedName.size());
//        logger.error("possibleAffectedDiffCommit.size: "+ possibleAffectedDiffCommit.size());
//        logger.error("getAffectedDiffCommit...");
//        int i =0;
//        for(String s: typeRemoveQualifiedName){ // 对于所有的破坏性变更Signature Qualitative qualified
////            if (s.equals("null")) {logger.error("skip bad methodRemoveSignaturesQualifiedName..."+s+ " "+ newLibId + " "+ oldLibId);continue;}
//            if (checkedTypeRemove.contains(s)) {logger.error("skip checked typeRemoveSignaturesQualifiedName..."+s+ " "+ newLibId + " "+ oldLibId);continue;}
//            checkedTypeRemove.add(s);
//            logger.error("for typeRemoveSignaturesQualifiedName..."+s+ " "+ newLibId + " "+ oldLibId);
//            Set<String> checkedCommit = new HashSet<>();
//            int j =0;
//            for(DiffCommit d:possibleAffectedDiffCommit){
//                logger.error("for DiffCommit..."+d);
//                if(checkedCommit.contains(d.commit)){logger.error("This DiffCommit has checked...");continue;}
//                affectedCode = retryGetAffectedTypeUsageDiffCommit(s,d,0,1);
//                if (affectedCode.size()!=0){
//                    checkedCommit.add(d.commit);
//                    logger.error("affectedCode.size: "+affectedCode.size());
//                    logger.error("getCodePathFromDiffCommit...");
//                    getTypeRemoveCodePathFromDiffCommit(affectedCode);
//                }
//                else {logger.error("noAffectedCode...");}
//            }
//        }
//        // pkuDiff end
//    }

//    public static void getFieldRemoveAdaptation(List<String> fieldRemoveQualifiedName,String newLibId,String oldLibId) throws Exception {
//        // pkuDiff
//        Map<String,List<AffectedFieldUsage>> affectedCode;
//        List<String> checkedFieldRemove = new LinkedList<>();
//        logger.error("findAffectedClientDiffCommit...");
//        List<DiffCommit> possibleAffectedDiffCommit = findAffectedClientDiffCommit("commitDiff1.csv",newLibId,oldLibId);
//        logger.error("typeRemoveQualifiedName.size: "+ fieldRemoveQualifiedName.size());
//        logger.error("possibleAffectedDiffCommit.size: "+ possibleAffectedDiffCommit.size());
//        logger.error("getAffectedDiffCommit...");
//        int i =0;
//        for(String s: fieldRemoveQualifiedName){ // 对于所有的破坏性变更Signature Qualitative qualified
////            if (s.equals("null")) {logger.error("skip bad methodRemoveSignaturesQualifiedName..."+s+ " "+ newLibId + " "+ oldLibId);continue;}
//            if (checkedFieldRemove.contains(s)) {logger.error("skip checked fieldRemoveSignaturesQualifiedName..."+s+ " "+ newLibId + " "+ oldLibId);continue;}
//            checkedFieldRemove.add(s);
//            logger.error("for fieldRemoveSignaturesQualifiedName..."+s+ " "+ newLibId + " "+ oldLibId);
//            Set<String> checkedCommit = new HashSet<>();
//            int j =0;
//            for(DiffCommit d:possibleAffectedDiffCommit){
//                logger.error("for DiffCommit..."+d);
//                if(checkedCommit.contains(d.commit)){logger.error("This DiffCommit has checked...");continue;}
//                affectedCode = retryGetAffectedFieldUsageDiffCommit(s,d,0,1);
//                if (affectedCode.size()!=0){
//                    checkedCommit.add(d.commit);
//                    logger.error("affectedCode.size: "+affectedCode.size());
//                    logger.error("getCodePathFromDiffCommit...");
//                    getFieldRemoveCodePathFromDiffCommit(affectedCode);
//                }
//                else {logger.error("noAffectedCode...");}
//            }
//        }
//        // pkuDiff end
//    }



//    public static Map<String,List<AffectedSignature>> retryGetAffectedCallerDiffCommit(String s, DiffCommit d , int retryTimes, int upperTimes){
//        if (retryTimes>=upperTimes){
//            return new HashMap<>();
//        }
//        Map<String,List<AffectedSignature>> affectedCode;
//        try{
//            affectedCode = getAffectedCallerDiffCommit(s,d); // 筛选客户端diffCommit: oldCommit 调用了 第三方库 breaking change 之前的 API
//            return affectedCode;
//
//        }
//        catch (Exception e){
//            logger.error(e.toString());
//            e.printStackTrace();
//            return retryGetAffectedCallerDiffCommit(s,d,retryTimes+1,upperTimes);
//        }
//    }
//
//    public static Map<String,List<AffectedTypeUsage>> retryGetAffectedTypeUsageDiffCommit(String s, DiffCommit d , int retryTimes, int upperTimes){
//        if (retryTimes>=upperTimes){
//            return new HashMap<>();
//        }
//        Map<String,List<AffectedTypeUsage>> affectedCode;
//        try{
//            affectedCode = getAffectedTypeUsageDiffCommit(s,d); // 筛选客户端diffCommit: oldCommit 调用了 第三方库 breaking change 之前的 API
//            return affectedCode;
//
//        }
//        catch (Exception e){
//            logger.error(e.toString());
//            e.printStackTrace();
//            return retryGetAffectedTypeUsageDiffCommit(s,d,retryTimes+1,upperTimes);
//        }
//    }
//
//    public static Map<String,List<AffectedFieldUsage>> retryGetAffectedFieldUsageDiffCommit(String s, DiffCommit d , int retryTimes, int upperTimes){
//        if (retryTimes>=upperTimes){
//            return new HashMap<>();
//        }
//        Map<String,List<AffectedFieldUsage>> affectedCode;
//        try{
//            affectedCode = getAffectedFieldUsageDiffCommit(s,d); // 筛选客户端diffCommit: oldCommit 调用了 第三方库 breaking change 之前的 API
//            return affectedCode;
//
//        }
//        catch (Exception e){
//            logger.error(e.toString());
//            e.printStackTrace();
//            return retryGetAffectedFieldUsageDiffCommit(s,d,retryTimes+1,upperTimes);
//        }
//    }

    public static Map<String,List<AffectedCode>> retryGetAffectedApiUsageDiffCommit(String s, DiffCommit d , int retryTimes, int upperTimes, String breakingChangeSuperType, String breakingChangeSubType){
        if (retryTimes>=upperTimes){
            return new HashMap<>();
        }
        Map<String,List<AffectedCode>> affectedCode;
        try{
            affectedCode = getAffectedApiUsageDiffCommit(s,d,breakingChangeSuperType,breakingChangeSubType); // 筛选客户端diffCommit: oldCommit 调用了 第三方库 breaking change 之前的 API
            return affectedCode;

        }
        catch (Exception e){
            logger.error(e.toString());
            e.printStackTrace();
            return retryGetAffectedApiUsageDiffCommit(s,d,retryTimes+1,upperTimes,breakingChangeSuperType,breakingChangeSubType);
        }
    }

//    public static Map<String,List<AffectedCode>> getAffectedDiffCommitWithoutRetry(String s, DiffCommit d ,int retryTimes, int upperTimes) throws Exception {
//        Map<String,List<AffectedCode>> affectedCode;
//        affectedCode = getAffectedDiffCommit(s,d); // 筛选客户端diffCommit: oldCommit 调用了 第三方库 breaking change 之前的 API
//        return affectedCode;
//    }


}
