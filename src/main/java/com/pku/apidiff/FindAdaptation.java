package com.pku.apidiff;

//import apidiff.internal.util.UtilTools;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;

public class FindAdaptation {

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

    public static void main(String[] args) throws IOException {
        String targetPath = "/Users/liuhongyi/.m2/repository/org/apache/maven/maven-core/3.0.4/maven-core-3.0.4-sources/org/apache/maven/lifecycle/internal/BuilderCommon.java";
        CompilationUnit compilationUnit = getCompilationUnit(targetPath);
        MethodInvocationVisitor visitorMethodInvocation = new MethodInvocationVisitor();
        visitorMethodInvocation.setTargetMethod("null calculateExecutionPlan(MavenSession session, MavenProject mp, String xxx)");
        compilationUnit.accept(visitorMethodInvocation);
        visitorMethodInvocation.getSourceCodeSnippet();
        System.out.println(visitorMethodInvocation.getCallers().size());
    }
}
