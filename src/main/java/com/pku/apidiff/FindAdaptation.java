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

import static com.pku.apidiff.CodeDiff.getGitCodeDiff;
import static com.pku.apidiff.CodeDiff.getFileCodeDiff;
import static com.pku.libupgrade.PomParser.USER_LOCAL_REPO;
import static com.pku.libupgrade.Utils.sortByValue2;


// 输入 breakingChanges, commitDiffs
// 输出 affectedCodes, adaptationCodes
public class FindAdaptation {
    private static Logger logger = LoggerFactory.getLogger(FindAdaptation.class);
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

//    public static void main(String[] args) throws IOException {
//        String targetPath = "/Users/liuhongyi/.m2/repository/org/apache/maven/maven-core/3.0.4/maven-core-3.0.4-sources/org/apache/maven/lifecycle/internal/BuilderCommon.java";
//        CompilationUnit compilationUnit = getCompilationUnit(targetPath);
//        MethodInvocationVisitor visitorMethodInvocation = new MethodInvocationVisitor();
//        visitorMethodInvocation.setTargetMethod("null calculateExecutionPlan(MavenSession session, MavenProject mp, String xxx)");
//        compilationUnit.accept(visitorMethodInvocation);
//        visitorMethodInvocation.getSourceCodeSnippet();
//        System.out.println(visitorMethodInvocation.getCallers().size());
//    }

    public static void getAllSignatureProjectLeve(String  projectPath, File file, Map<String,List<Signature>> allCallerSignaturesOfProject) throws IOException {
//        File targetDir = new File(targetPath);
        if (file.isDirectory()){
            for(File f: Objects.requireNonNull(file.listFiles())){
               if (f.isDirectory()) getAllSignatureProjectLeve(projectPath,f,allCallerSignaturesOfProject);
               if (f.isFile() && f.getName().contains(".java")) getAllSignatureFileLevel(f.getAbsolutePath(),allCallerSignaturesOfProject);
            }
        }
        else {
            logger.error("Error for getAllSignatureProjectLeve");
        }
    }

    public static void getAllSignatureFileLevel(String targetPath,Map<String,List<Signature>> allCallerSignaturesOfProject) throws IOException {
        CompilationUnit compilationUnit = getCompilationUnit(targetPath);
        MethodInvocationVisitor visitorMethodInvocation = new MethodInvocationVisitor(); // todo 提取Signature，然后再做匹配
        compilationUnit.accept(visitorMethodInvocation);
        allCallerSignaturesOfProject.put(targetPath,visitorMethodInvocation.getCallerSignatures());
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

    public static List<String> getBreakingChangeSignatures(String breakingChangeFilePath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(new File(breakingChangeFilePath)));
        String line;
        List<String> signatures = new LinkedList<>();
        while ( (line=br.readLine())!=null ){
            if (line.split(":")[0].equals("Method")){
                if (line.split(" ")[1].equals("Remove")){
                    signatures.add(line.split("<code>")[1].split("</code>")[0] + "-"+ line.split("<code>")[2].split("</code>")[0]);
                }
            }
        }
        br.close();
        return signatures;
    }

//    public static List<String> getSignatureFromStringOld(String s) {
//        List<String> signature = new LinkedList<>();
//
//        // return type
//        signature.add(s.split(" ")[0]);
//
//        // method name
//        signature.add(s.split("\\(")[0].split(" ")[1]);
//
//        // parameter list
//        if(s.split("\\(")[1].equals(")")) return signature;
//        String list = s.split("\\(")[1].split("\\)")[0];
//        for (String para : list.split(",")) {
//            Integer size = para.split(" ").length;
//            signature.add(para.split(" ")[size - 2]);
//        }
//        return signature;
//    }

    public static List<String> getSignatureSimpleNameFromBreakingChangeQualifiedNameString(String s) {
        List<String> signature = new LinkedList<>();
        // targetSignature
        String targetSignature = s.split("-")[0];
        String targetObjectType = s.split("-")[1];
        String[] targetObjectTypeStack = targetObjectType.split("\\.");

        // return type
        signature.add(targetSignature.split(" ")[0]);

        // method name
        signature.add(targetSignature.split("\\(")[0].split(" ")[1]);

        // parameter list
        if(targetSignature.split("\\(")[1].equals(")")) return signature;
        String list = targetSignature.split("\\(")[1].split("\\)")[0];
        for (String para : list.split(",")) {
            Integer size = para.split(" ").length;
            if(size==1){
                signature.add(para.split(" ")[size - 1]);
            }
            else{
                signature.add(para.split(" ")[size - 2]);
            }
        }

        // target object
        signature.add(targetObjectTypeStack[targetObjectTypeStack.length-1]);
        return signature;
    }



    public static Map<String,List<AffectedCode>> getAffectedDiffCommit(String signature, DiffCommit diffCommit) throws Exception {
        String clientPath =  "../dataset/"+diffCommit.clientName;
        Repository r = GitService.openRepository(clientPath);
        GitService.checkout(r,diffCommit.commit);
        logger.error("getAllSignatureProjectLeve..." + clientPath);
        File client = new File(clientPath);
        Map<String,List<Signature>> allCallerSignaturesOfProject = new HashMap<>();
        getAllSignatureProjectLeve(clientPath,client,allCallerSignaturesOfProject);
        List<String> targetSignature =  getSignatureSimpleNameFromBreakingChangeQualifiedNameString(signature);
        Map<String,List<AffectedCode>> affected = new HashMap<>();
        for(String filePath: allCallerSignaturesOfProject.keySet()){
            List<AffectedCode> temp = new ArrayList<>();
            for(Signature s: allCallerSignaturesOfProject.get(filePath)){
                List<String> nowSignature = Arrays.asList(s.signature.split(" "));
                if(isEqualSignatureStringList(nowSignature,targetSignature)){
                    temp.add(new AffectedCode(s,diffCommit));
                }
            }
            if (temp.size()!=0)
                affected.put(filePath,temp);
        }
        return affected;
    }


    public static void getCodePathFromDiffCommit(Map<String,List<AffectedCode>> affected) throws Exception {
        for(String filePath:affected.keySet()){
            for(AffectedCode a:affected.get(filePath)){
                getGitCodeDiff(filePath,a.diffCommit,5,a.signature.position,a.signature.signature);
            }
        }
    }

//

    public static Map<String, List<Signature>> setCallersFromJson(String libName) throws IOException {
        // libCallers/libName(g:a:v)
        Map<String, List<Signature>>  apiCallersMap = new HashMap<>();
        String jsonPath = "libCallers/"+ libName+".json";
        BufferedReader br = new BufferedReader(new FileReader(new File(jsonPath)));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode parser = mapper.readTree(br);
        for (Iterator<String> it = parser.fieldNames(); it.hasNext(); ) {
            String filePath = it.next();
            JsonNode item = parser.get(filePath);
            List<Signature> tempSig = new LinkedList<>();
            for(JsonNode j:item){
                Signature temp =new Signature(j.get("signature").asText(),j.get("position").asInt());
                tempSig.add(temp);
            }
            apiCallersMap.put(filePath,tempSig);
        }
        return apiCallersMap;
    }

    public static void findAffectedCodeAndAdaptationFrom3rdLib(Map<String, List<Signature>> oldVersion3rdLibCallers,List<String> breakingChangeSignaturesQualifiedName,String oldLibId,String newLibId) throws Exception {
        for (String key:oldVersion3rdLibCallers.keySet()){
            for(Signature callSig:oldVersion3rdLibCallers.get(key)){
                for(String breakQualifiedSig:breakingChangeSignaturesQualifiedName){
                    if (isEqualSignatureStringAndList(callSig.signature,getSignatureSimpleNameFromBreakingChangeQualifiedNameString(breakQualifiedSig))){// find affected code
                        String newProjectPath = getProjectPath(newLibId);
                        String filePathInProject = key.split("sources")[1];
                        getCodePathFromLibSelf(key,newProjectPath+filePathInProject,callSig,oldLibId,newLibId);

                    }
                }
            }
        }
    }

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

    public static void getCodePathFromLibSelf(String oldFilePath,String newFilePath,Signature callSig,String oldId,String newId) throws Exception {
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

    public static void getAffectedCode(String breakingChangesDir) throws Exception {
        File bcd = new File(breakingChangesDir);
//        List<String>  targetSignatures = new LinkedList<>();
//        List<DiffCommit> targetDiffCommits = new LinkedList<>();
        Map<String,List<AffectedCode>> affectedCode;
        Map<BreakChangeSet,Integer> bks = new HashMap<>();
        int w =0;
        for(File f: Objects.requireNonNull(bcd.listFiles())){ // 对于每一组破坏性变更版本，提取所有的破坏性变更Signature
//            if(w>0){
//                w++;
//                continue;
//            }
//            w++;
            if(f.getName().equals(".DS_Store")){continue;}
            if(f.getName().contains("junit")){continue;}
            if(f.getName().contains("test")){continue;}
            if(f.getName().contains("gwt")){continue;}
            if(f.getName().contains("guava")){continue;}
            if(f.getName().contains("jdt")){continue;}
            if(f.getName().contains("truth")){continue;}
            int temp = Integer.parseInt((f.getName().split("_")[0]));
            bks.put(new BreakChangeSet(f.getName().split("_")[1],f.getName().split("_")[2].split("\\.txt")[0]),temp);
        }
        bks = sortByValue2(bks);
        // UML Diff
        for (BreakChangeSet key:bks.keySet()) {
            logger.error("fetchBreakingChangeLibAndVersions...");
            String newLibId = key.newLibId;
            String oldLibId = key.oldLibId;
            logger.error("getBreakingChangeSignatures...");
            List<String> breakingChangeSignaturesQualifiedName = getBreakingChangeSignatures(breakingChangesDir + "/" + bks.get(key) + "_" + newLibId + "_" + oldLibId + ".txt");
            // UMLDiff
            Map<String, List<Signature>> oldVersion3rdLibCallers = setCallersFromJson(oldLibId);
            findAffectedCodeAndAdaptationFrom3rdLib(oldVersion3rdLibCallers,breakingChangeSignaturesQualifiedName,oldLibId,newLibId);
            // UMLDiff end
        }
        // UML Diff end

        int k =0;
        for (BreakChangeSet key:bks.keySet()){
//        for(File f: Objects.requireNonNull(bcd.listFiles())){ // 对于每一组破坏性变更版本，提取所有的破坏性变更Signature
//            if(k<5){
//                k++;
//                continue;
//            }
//            k++;
//            if(f.getName().equals(".DS_Store")){continue;}
            logger.error("fetchBreakingChangeLibAndVersions...");
//            String newVersion = f.getName().split("_")[0];
//            String oldVersion = f.getName().split("_")[1].split("\\.txt")[0];
            String newLibId = key.newLibId;
            String oldLibId = key.oldLibId;
            logger.error("getBreakingChangeSignatures...");
            List<String> breakingChangeSignaturesQualifiedName = getBreakingChangeSignatures(breakingChangesDir+"/"+bks.get(key)+"_"+newLibId+"_"+oldLibId+".txt");
            // UMLDiff
//            Map<String, List<Signature>> oldVersion3rdLibCallers = setCallersFromJson(oldLibId);
//            findAffectedCodeAndAdaptationFrom3rdLib(oldVersion3rdLibCallers,breakingChangeSignaturesQualifiedName,oldLibId,newLibId);
            // UMLDiff end

            // pkuDiff
            List<String> checkedSignature = new LinkedList<>();
//            targetSignatures.addAll(breakingChangeSignaturesQualifiedName);
            logger.error("findAffectedClientDiffCommit...");
            List<DiffCommit> possibleAffectedDiffCommit = findAffectedClientDiffCommit("commitDiff1.csv",newLibId,oldLibId);
//            targetDiffCommits.addAll(possibleAffectedDiffCommit);  // 筛选客户端diffCommit: 客户端存在将第三方库从A版本升级到B版本的行为
            logger.error("breakingChangeSignaturesQualifiedName.size: "+ breakingChangeSignaturesQualifiedName.size());
            logger.error("possibleAffectedDiffCommit.size: "+ possibleAffectedDiffCommit.size());
            logger.error("getAffectedDiffCommit...");
            int i =0;
            // getAllCallerAt3rdLib
            for(String s: breakingChangeSignaturesQualifiedName){ // 对于所有的破坏性变更Signature Qualitative qualified
//                if(i<8) {
//                    i++;
//                    continue;
//                }
//                i++;
                if (checkedSignature.contains(s)) {continue;}
                checkedSignature.add(s);
                logger.error("for breakingChangeSignaturesQualifiedName..."+s+ " "+ newLibId + " "+ oldLibId);
                Set<String> checkedCommit = new HashSet<>();
                int j =0;
                // TODO:detect adaptation on 3rd lib
                for(DiffCommit d:possibleAffectedDiffCommit){
//                    if(j<1){
//                        j++;
//                        continue;
//                    }
//                    j++;
                    logger.error("for DiffCommit..."+d);
                    if(checkedCommit.contains(d.commit)){
                        logger.error("This DiffCommit has checked...");
                        continue;
                    }
//                    try{
//                        affectedCode = getAffectedDiffCommit(s,d); // 筛选客户端diffCommit: oldCommit 调用了 第三方库 breaking change 之前的 API
//                        checkedCommit.add(d.commit);
//                    }
//                    catch (Exception e){
//                        affectedCode = new HashMap<>();
//                        logger.error(e.toString());
//                    }
                    affectedCode = retryGetAffectedDiffCommit(s,d,0,1);
                    if (affectedCode.size()!=0){
                        checkedCommit.add(d.commit);
                        logger.error("affectedCode.size: "+affectedCode.size());
                        logger.error("getCodePathFromDiffCommit...");
                        getCodePathFromDiffCommit(affectedCode);
                    }
                    else {
                        logger.error("noAffectedCode...");
                    }
                }
            }
            // pkuDiff end
        }

//        getBreakingChangeSignatures()
    }

    public static Map<String,List<AffectedCode>> retryGetAffectedDiffCommit(String s, DiffCommit d ,int retryTimes, int upperTimes){
        if (retryTimes>=upperTimes){
            return new HashMap<>();
        }
        Map<String,List<AffectedCode>> affectedCode;
        try{
            affectedCode = getAffectedDiffCommit(s,d); // 筛选客户端diffCommit: oldCommit 调用了 第三方库 breaking change 之前的 API
            return affectedCode;

        }
        catch (Exception e){
            logger.error(e.toString());
            e.printStackTrace();
            return retryGetAffectedDiffCommit(s,d,retryTimes+1,upperTimes);
        }
    }

    public static Map<String,List<AffectedCode>> getAffectedDiffCommitWithoutRetry(String s, DiffCommit d ,int retryTimes, int upperTimes) throws Exception {
        Map<String,List<AffectedCode>> affectedCode;
        affectedCode = getAffectedDiffCommit(s,d); // 筛选客户端diffCommit: oldCommit 调用了 第三方库 breaking change 之前的 API
        return affectedCode;
    }

    public static void main(String[] args) throws Exception {
        getAffectedCode("breakingChanges1");
    }
}
