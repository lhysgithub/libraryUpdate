package com.pku.apidiff;

//import apidiff.internal.util.UtilTools;
import com.pku.libupgrade.DiffCommit;
import com.pku.libupgrade.GitService;
import com.pku.libupgrade.clientAnalysis.ClientAnalysis;
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

    public static List<String> getSignatureFromString(String s) {
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
            signature.add(para.split(" ")[size - 2]);
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
        List<String> targetSignature =  getSignatureFromString(signature);
        Map<String,List<AffectedCode>> affected = new HashMap<>();
        for(String filePath: allCallerSignaturesOfProject.keySet()){
            List<AffectedCode> temp = new ArrayList<>();
            for(Signature s: allCallerSignaturesOfProject.get(filePath)){
                List<String> nowSignature = Arrays.asList(s.signature.split(" "));
                 // Signature not equal on length
                 if (nowSignature.size() != targetSignature.size()){
                    continue;
                }
                // Signature matching
                Boolean isEqual = true;
                for(Integer i=0; i<nowSignature.size();i++){
                    if(nowSignature.get(i).equals("null") || targetSignature.get(i).equals("null")){
                        continue;
                    }
                    if(!nowSignature.get(i).equals(targetSignature.get(i))){ // todo fix type analysis
                        isEqual = false;
                        break;
                    }
                }
                if(isEqual){
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

    public static void getAffectedCode(String breakingChangesDir) throws Exception {
        File bcd = new File(breakingChangesDir);
        List<String>  targetSignatures = new LinkedList<>();
        List<DiffCommit> targetDiffCommits = new LinkedList<>();
        Map<String,List<AffectedCode>> affectedCode;
        int k =0;
        for(File f: Objects.requireNonNull(bcd.listFiles())){ // 对于每一组破坏性变更版本，提取所有的破坏性变更Signature
//            if(k<1){
//                k++;
//                continue;
//            }
//            k++;
            if(f.getName().equals(".DS_Store")){continue;}
            logger.error("fetchBreakingChangeLibAndVersions...");
            String newVersion = f.getName().split("_")[0];
            String oldVersion = f.getName().split("_")[1].split("\\.txt")[0];
            logger.error("getBreakingChangeSignatures...");
            List<String> breakingChangeSignatures = getBreakingChangeSignatures(f.getAbsolutePath());
            List<String> checkedSignature = new LinkedList<>();
            targetSignatures.addAll(breakingChangeSignatures);
            logger.error("findAffectedClientDiffCommit...");
            List<DiffCommit> possibleAffectedDiffCommit = findAffectedClientDiffCommit("commitDiff1.csv",newVersion,oldVersion);
            targetDiffCommits.addAll(possibleAffectedDiffCommit);  // 筛选客户端diffCommit: 客户端存在将第三方库从A版本升级到B版本的行为
            logger.error("breakingChangeSignatures.size: "+ breakingChangeSignatures.size());
            logger.error("possibleAffectedDiffCommit.size: "+ possibleAffectedDiffCommit.size());
            logger.error("getAffectedDiffCommit...");
            int i =0;
            for(String s: breakingChangeSignatures){ // 对于所有的破坏性变更Signature
//                if(i<48) {
//                    i++;
//                    continue;
//                }
//                i++;
                if (checkedSignature.contains(s)) {continue;}
                checkedSignature.add(s);
                logger.error("for breakingChangeSignatures..."+s+ " "+ newVersion + " "+ oldVersion);
                Set<String> checkedCommit = new HashSet<>();
                int j=0;
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
                    affectedCode = retryGetAffectedDiffCommit(s,d,0);
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
        }

//        getBreakingChangeSignatures()
    }

    public static Map<String,List<AffectedCode>> retryGetAffectedDiffCommit(String s, DiffCommit d ,int retryTimes){
        if (retryTimes>=3){
            return new HashMap<>();
        }
        Map<String,List<AffectedCode>> affectedCode;
        try{
            affectedCode = getAffectedDiffCommit(s,d); // 筛选客户端diffCommit: oldCommit 调用了 第三方库 breaking change 之前的 API
            return affectedCode;

        }
        catch (Exception e){
            logger.error(e.toString());
            return retryGetAffectedDiffCommit(s,d,retryTimes+1);
        }
    }

    public static void main(String[] args) throws Exception {
        getAffectedCode("breakingChanges");
    }
}
