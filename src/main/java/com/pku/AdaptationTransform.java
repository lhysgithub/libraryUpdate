package com.pku;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.TextEdit;
import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
//import com.github.mpkorstanje.SetMetric;

public class AdaptationTransform {
    static String oldDirPath = "";
    static String newDirPath = "";
    public static void main(String[] args) throws IOException {
        // find adaptations of same broken api
//        recurrentExtractAdaptationsOfSameBrokenAPI(oldDirPath,oldDirPath,oldDirPath);

        // todo: circulate select adaptationA and adaptationB in a adaptations set
        String adaptationAPath = "D:\\dataset3\\adaptation\\library\\org.yaml_snakeyaml_1.4_1.16\\Method_ Change in Return Type Method _null getTag Node\\178_org.yaml.snakeyaml.serializer.Serializer.java"; // absolutely  path
        String adaptationBPath = "";
        float similarity = generateAndEvaluateAdaptation(adaptationAPath,adaptationBPath);
        System.out.println("similarity: "+similarity);
        // read java file and remove prefix (-,+) and creat ast ? yes for the find apis and parameters

        // find broken api

        // determine transform type

            // 1. for delete transform

            // 2. for alternative api transform

                // find alternative api

                // determine every parameter of alternative api

                // get parameters of alternative api

            // 3. for alternative code block transform

        // evaluate the generated adaptation between the ground truth with token similarity
    }
    public static float generateAndEvaluateAdaptation(String adaptationAPath, String adaptationBPath) throws IOException {
        String affectedAPath = adaptationAPath.replace("adaptation","affected");
        String affectedBPath = adaptationBPath.replace("adaptation","affected");
        File affectedAFile = new File(affectedAPath);
        File affectedBFile = new File(affectedBPath);
        File adaptationAFile = new File(adaptationAPath);
        File adaptationBFile = new File(adaptationBPath);
        String affectedASource = getFileContentWithoutPrefix(affectedAPath);
        String affectedBSource = getFileContentWithoutPrefix(affectedBPath);
        String adaptationASource = getFileContentWithoutPrefix(adaptationAPath);
        String adaptationBSource = getFileContentWithoutPrefix(adaptationBPath);
        int affectedALength = affectedASource.split("\n").length;
        int adaptationALength = adaptationASource.split("\n").length;
        int affectedBLength = affectedBSource.split("\n").length;
        int adaptationBLength = adaptationBSource.split("\n").length;
        String brokenType = affectedAFile.getParent().split("_")[0]; // for windows
        String brokenSubType = affectedAFile.getParent().split("_")[1]; // for windows
        String brokenApi = affectedAFile.getParent().replace(brokenType+"_","").replace(brokenSubType+"_","");
        String adaptationType = "";
        String generatedAffectedSource = "";
        String generatedAdaptationSource = "";
        int generatedAffectedLength = 0;
        int generatedAdaptationLength = 0;

        // determine transform type
        if(adaptationALength==0){
            adaptationType = "Delete";
        }
        else if (adaptationALength==affectedALength && adaptationALength==1){
            adaptationType = "AlterAPI";
        }
        else if (adaptationALength!=affectedALength && adaptationALength>=1){
            adaptationType = "AlterCode";
        }

        // generated adaptation
        if(brokenType.contains("Method")){
            String brokenMethodName = brokenApi.split(" ")[1];
            if(adaptationType.equals("Delete")){
                generatedAffectedLength = affectedALength;
                generatedAdaptationLength = 0;
                generatedAffectedSource = getGeneratedAffectedSource(generatedAffectedLength,affectedASource,affectedBSource);
                generatedAdaptationSource = "";
            }
            else if (adaptationType.equals("AlterAPI")){
                generatedAffectedLength = affectedALength;
                generatedAdaptationLength = adaptationALength;
                generatedAffectedSource = getGeneratedAffectedSource(generatedAffectedLength,affectedASource,affectedBSource);
                // todo: find broken api and map parameters and return variable
                generatedAdaptationSource = getAlterAPIGeneratedAdaptationSource(generatedAdaptationLength, brokenApi,
                        affectedASource, affectedBSource, generatedAffectedSource,adaptationASource,adaptationBSource);
            }
            else{
                generatedAffectedLength = affectedALength;
                generatedAdaptationLength = adaptationALength;
                generatedAffectedSource = getGeneratedAffectedSource(generatedAffectedLength,affectedASource,affectedBSource);
                generatedAdaptationSource = adaptationASource;
            }
        }
        else if(brokenType.contains("Type")){
            String brokenTypeName = brokenApi;
        }
        else if(brokenType.contains("Filed")){
            String brokenTypeName = brokenApi.split(" ")[1];
            String brokenFiledName = brokenApi.split(" ")[0];
        }
        // remove blank space chart than evaluate similarity. String.replaceAll("\\s*","")

        // use 调和 avg to evaluate similarity ? 没必要，直接两个字符串加一起，然后求相似度

        // generate delete code
        // generate adding code - need source code?
        String str1 = (affectedASource + adaptationASource).replaceAll("\\s*","");
        String str2 = (generatedAffectedSource + generatedAdaptationSource).replaceAll("\\s*","");
//        StringMetric metric = StringMetrics.cosineSimilarity();
        StringMetric metric1 = StringMetrics.levenshtein();
        return metric1.compare(str1, str2);
    }

    public static class AlterMethodVisitor extends ASTVisitor {
        public String targetMethodName = "";
        public List<Parameter> targetParameters=null;
        public APIUsage apiUsage=null;
        @Override
        public boolean visit(MethodInvocation node) { // for Method broken api
            String methodName = node.getName().toString();
            double ratio = 0.0;
            int hitCount = 0;
            int targetParameterLength = targetParameters.size();
            List argumentsAST = node.arguments();
            List<Parameter> parameters = new LinkedList<>();
            int parameterLength = argumentsAST.size();
            int lagerLength = Math.max(targetParameterLength, parameterLength);
            // parameter list
            for (int i=0 ; i<parameterLength ; i++){
                if(argumentsAST.get(i) instanceof Expression){
                    Expression exp = (Expression) argumentsAST.get(i);
                    String parameterName = exp.toString();
                    int index = -1;
                    String type = "";
                    int oldHitCount= hitCount;
                    for(Parameter p:targetParameters){
                        if(p.content.equals(parameterName)){
                            hitCount++;
                            index = targetParameters.indexOf(p);
                            break;
                        }
                    }
                    if (oldHitCount==hitCount){parameters.add(new Parameter("new",parameterName,index));}
                    else {parameters.add(new Parameter("reserve",parameterName,index));}
                }
            }
            ratio= hitCount*1.0/lagerLength;
            if(ratio>0.5){apiUsage = new APIUsage(methodName,parameters);}// 超参
            return super.visit(node);
        }
    }
    public static class BrokenMethodVisitor extends ASTVisitor {
        public String targetMethodName = "";
        public APIUsage apiUsage=null;
        @Override
        public boolean visit(MethodInvocation node) { // for Method broken api
            String methodName = node.getName().toString();
            if (methodName.equals(targetMethodName)){
                // parameter list
                List argumentsAST = node.arguments();
                List<Parameter> parameters = new LinkedList<>();
                int parameterLength = argumentsAST.size();
                for (int i=0 ; i<parameterLength ; i++){
                    if(argumentsAST.get(i) instanceof Expression){
                        Expression exp = (Expression) argumentsAST.get(i);
                        parameters.add(new Parameter("unknown",exp.toString(),i));
                    }
                }
                apiUsage = new APIUsage(methodName,parameters);
            }
            return super.visit(node);
        }
    }
    public static class BrokenFieldVisitor extends ASTVisitor {
        @Override // QualifiedName = FieldAccess // not include member function
        public boolean visit(QualifiedName node)   { // for Field broken api
            String typeName;
            if (node.getQualifier().resolveTypeBinding()!=null){typeName = node.getQualifier().resolveTypeBinding().getName();}
            else{typeName="null";}
            String fieldName = node.getName().toString();
            int position  = node.getStartPosition();
            System.out.println(node);
            return super.visit(node);
        }
    }
    public static class BrokenTypeVisitor extends ASTVisitor {
        @Override // VariableDeclarationStatement
        public boolean visit(VariableDeclarationStatement node)   { // for Type broken api
            String typeName;
            if (node.getType().resolveBinding()!=null){typeName = node.getType().resolveBinding().getName();}
            else {typeName = "null";}
            int position = node.getStartPosition();
            System.out.println(typeName);
            System.out.println(position);
            System.out.println(node);
            return super.visit(node);
        }
    }
    public static class Parameter {
        public String type; // new, reserve for adaptation code or unknown for affected code
        public String content; // name of parameter
        public int index; // for AlterApi adaptation code, index is the position of correspond parameter in affected code
        Parameter(String _type, String _content, int _index){
            type = _type;
            content = _content;
            index = _index;
        }
    }
    public static class APIUsage{
        public String name; // method nmae
        public List<Parameter> parameters; // name of parameter
        APIUsage(String _name,List<Parameter> _parameters){
            name = _name;
            parameters = _parameters;
        }
    }

    public static String getAlterAPIGeneratedAdaptationSource(int generatedAdaptationLength, String brokenApi, String affectedASource, String affectedBSource, String generatedAffectedSource,String adaptationASource,String adaptationBSource){
        String brokenMethodName = brokenApi.split(" ")[1];
        String generatedAdaptationSource = "";
        // find broken api and map parameters
        APIUsage affectedAUsage =  getAPIUsageFromMethodName(brokenMethodName,affectedASource);
        APIUsage generatedAffectedUsage =  getAPIUsageFromMethodName(brokenMethodName,generatedAffectedSource);
        APIUsage adaptationAUsage =  getAPIUsageFromMethodName(brokenMethodName,adaptationASource);
        if (adaptationAUsage==null){adaptationAUsage =  getAPIUsageFromParametersName(affectedAUsage.name,affectedAUsage.parameters,adaptationASource);}
        // generate
        APIUsage generatedAdaptationUsage = generatedMethodAPIUsage(generatedAffectedUsage,adaptationAUsage);

        return generatedSourcecodeWithAPIUsage(generatedAffectedSource,generatedAffectedUsage,generatedAdaptationUsage);
    }

    public static String generatedSourcecodeWithAPIUsage(String sourcecode, APIUsage oldAPIUsage, APIUsage newAPIUsage){
        org.eclipse.jface.text.Document document = new org.eclipse.jface.text.Document(sourcecode);
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(document.get().toCharArray()); // read java file and remove prefix (-,+)
        parser.setKind(ASTParser.K_STATEMENTS);
        Block block = (Block) parser.createAST(null); // creat ast
        ASTRewrite astRewrite = ASTRewrite.create(block.getAST());
        AST ast = block.getAST();
        EidtMethodVisitor emv = new EidtMethodVisitor();
        emv.rewriter = astRewrite;
        emv.ast = ast;
        emv.document =document;
        emv.block = block;
        block.accept(emv);
        return document.get();
    }
    public static class EidtMethodVisitor extends ASTVisitor {
        public ASTRewrite rewriter = null;
        public AST ast = null;
        public Block block = null;
        public org.eclipse.jface.text.Document document;
        public APIUsage oldAPIUsage;
        public APIUsage newAPIUsage;
        @Override
        public boolean visit(MethodInvocation node) { // for Method broken api
            String brokenMethodName = oldAPIUsage.name;
            String methodName = node.getName().toString();
            if (methodName.equals(brokenMethodName)){
                // create new statements for insertion
                MethodInvocation newInvocation = ast.newMethodInvocation();
                newInvocation.setName(ast.newSimpleName(newAPIUsage.name));
                ASTParser parserT = ASTParser.newParser(AST.JLS8);
                parserT.setKind(ASTParser.K_EXPRESSION);
                for (Parameter p:newAPIUsage.parameters){
                    int i = newAPIUsage.parameters.indexOf(p);
                    if(p.index==-1){
                        parserT.setSource(p.content.toCharArray()); // read java file and remove prefix (-,+)
                        Expression expression = (Expression) parserT.createAST(null); // creat ast
                        newInvocation.arguments().add(i,expression);
                    }
                    else{
                        parserT.setSource(oldAPIUsage.parameters.get(p.index).content.toCharArray()); // read java file and remove prefix (-,+)
                        Expression expression = (Expression) parserT.createAST(null); // creat ast
                        newInvocation.arguments().add(i,expression);
                    }
                }
                ListRewrite listRewrite = rewriter.getListRewrite(block, Block.STATEMENTS_PROPERTY);
                listRewrite.replace(node,newInvocation,null);
                TextEdit edits = rewriter.rewriteAST(document,null);
                try {
                    edits.apply(document);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
            return super.visit(node);
        }
    }
    public static APIUsage generatedMethodAPIUsage(APIUsage generatedAffectedUsage,APIUsage adaptationAUsage){
        String methodName = adaptationAUsage.name;
        List<Parameter> parameters = new LinkedList<>();
        for (Parameter p:adaptationAUsage.parameters){
            if (p.index==-1){parameters.add(p);}
            else {parameters.add(generatedAffectedUsage.parameters.get(p.index));}
        }
        return new APIUsage(methodName,parameters);
    }

    // read java file and remove prefix (-,+) and creat ast ? yes for the find apis and parameters
    public static APIUsage getAPIUsageFromMethodName(String brokenMethodName,String sourcecode){
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(sourcecode.toCharArray()); // read java file and remove prefix (-,+)
        parser.setKind(ASTParser.K_STATEMENTS);
        Block block = (Block) parser.createAST(null); // creat ast
        BrokenMethodVisitor bmv = new BrokenMethodVisitor();
        bmv.targetMethodName = brokenMethodName;
        block.accept(bmv);
        return bmv.apiUsage;
    }
    public static APIUsage getAPIUsageFromParametersName(String brokenMethodName, List<Parameter> parameters,String sourcecode){
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(sourcecode.toCharArray()); // read java file and remove prefix (-,+)
        parser.setKind(ASTParser.K_STATEMENTS);
        Block block = (Block) parser.createAST(null); // creat ast
        AlterMethodVisitor amv = new AlterMethodVisitor();
        amv.targetMethodName = brokenMethodName;
        amv.targetParameters = parameters;
        block.accept(amv);
        return amv.apiUsage;
    }

    public static String getGeneratedAffectedSource(int generatedAffectedLength,String affectedASource,String affectedBSource){
        StringBuilder generatedAffectedSource = new StringBuilder();
        int affectedALength = affectedASource.split("\n").length;
        int affectedBLength = affectedBSource.split("\n").length;
        if(generatedAffectedLength >= affectedBLength){return affectedBSource;} // todo: generate affected code from original sourcecode
        else{
            for(int i=0;i<generatedAffectedLength;i++){
                String line = affectedBSource.split("\n")[i];
                generatedAffectedSource.append(line);
            }
            return generatedAffectedSource.toString();
        }
    }

    public static String getFileContentWithoutPrefix(String filePath) throws IOException {
        StringBuilder content= new StringBuilder();
        String line;
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        while ((line= br.readLine())!=null){
            line = line.substring(1);
            content.append(line);
        }
        br.close();
        return content.toString();
    }

    // clean adaptations to get adaptations set of same broken api
//    public static void recurrentExtractAdaptationsOfSameBrokenAPI(String filePath,String oldDirPath,String newDirPath) throws IOException { // extract adaptations of same broken api // output: save adaptations of same broken api
//        File file = new File(filePath);
//        if (file.isDirectory()){
//            String currentBrokenAPISignature = "";
//            String oldBrokenAPISignature = "";
////            String currentBrokenAPIName = "";
////            String oldBrokenAPIName = "";
//            File oldFile = null;
//            String brokenType = "";
//            File newVersionFile = null;
//            Boolean isFoundAdaptationsOfSameBrokenAPI = false;
//            for(File f : Objects.requireNonNull(file.listFiles())){
//                if (f.isDirectory()){ recurrentExtractAdaptationsOfSameBrokenAPI(f.getAbsolutePath(),oldDirPath,newDirPath); } // 遍历Dir的java file。
//                else if (f.getName().contains(".java")){
////                    brokenType = f.getName().split("_")[0];
////                    currentBrokenAPISignature = f.getName().split(":")[2].split("_")[0]; // for linux
//                    currentBrokenAPISignature = f.getName().split("_")[2]; // for windows
////                    String[] brokenSignature = currentBrokenAPISignature.split(" ");
////                    if (brokenType.equals("Method")){currentBrokenAPIName = brokenSignature[1];}
////                    else{currentBrokenAPIName = brokenSignature[0];}
////                    currentBrokenAPIName = currentBrokenAPISignature.split(" ")[1]; // for method broken
//                    newVersionFile = copyNewFile(f.getAbsolutePath(),oldDirPath,newDirPath);
////                    if (currentBrokenAPIName.equals(oldBrokenAPIName)){// find adaptations of same broken api
//                    if (currentBrokenAPISignature.equals(oldBrokenAPISignature)){// find adaptations of same broken api
//                        isFoundAdaptationsOfSameBrokenAPI = true;
//                        // extract adaptations of same broken api
//                    }else{
//                        oldBrokenAPISignature = currentBrokenAPISignature;
////                        oldBrokenAPIName = currentBrokenAPIName;
//                        if (oldFile==null){continue;} // fist epoch, continue
//                        if(!isFoundAdaptationsOfSameBrokenAPI){oldFile.delete();} // find another broken API and the latest adaptation is not in useful adaptations set
//                        isFoundAdaptationsOfSameBrokenAPI = false;
//                    }
//                }
//                oldFile = newVersionFile;
//            }
//        }
//    }
//    public static void createNewDir(String filePath,String dirPath, String newDirPath){
//        File file = new File(filePath);
//        String javaFileInnerDirPath = file.getParent().replace(dirPath,newDirPath);
//        File newJavaFileInnerDirFile = new File(javaFileInnerDirPath);
//        if(!newJavaFileInnerDirFile.exists()){newJavaFileInnerDirFile.mkdirs();}
//    }
//    public static File copyNewFile(String filePath, String oldDirPath, String newDirPath) throws IOException {
//        createNewDir(filePath,oldDirPath,newDirPath);
//        File file = new File(filePath);
//        String javaFileInnerPath = filePath.replace(oldDirPath,newDirPath);
//        File newJavaFile = new File(javaFileInnerPath);
//        BufferedReader br = new BufferedReader(new FileReader(file));
//        BufferedWriter bw = new BufferedWriter(new FileWriter(newJavaFile));
//        String line;
//        while ((line = br.readLine()) != null){
//            bw.write(line);
//            bw.flush();
//        }
//        bw.close();
//        br.close();
//        return newJavaFile;
//    }
}
