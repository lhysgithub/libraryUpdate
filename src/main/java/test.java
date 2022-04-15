import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;

public class test {
    static String testField = "";
    public static void main(String[] args) throws IOException {
        String filePath = "src/main/java/test.java";
        File source = new File(filePath);
        test.testField="";
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(readFileToString(filePath).toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        String[] classpath = System.getProperty("java.class.path").split(";");
        String[] sources = { source.getParentFile().getAbsolutePath() };

        Hashtable<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
        parser.setUnitName(source.getAbsolutePath());

        parser.setCompilerOptions(options);
//		parser.setEnvironment(null, sources, new String[] { "UTF-8" },	true);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);

        try {
            parser.setEnvironment(classpath, sources, new String[] { "UTF-8" },	true);
//			parser.setEnvironment(null, null, null,	true);
            CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
            TestVisitor visitor = new TestVisitor();
            visitor.testField = "";
            visitor.hashCode();
            compilationUnit.accept(visitor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String readFileToString(String filePath) throws IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        char[] buf = new char[10];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }

        reader.close();

        return fileData.toString();
    }
}
