import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.TextEdit;

import java.io.File;
import java.io.IOException;

public class testRewrite {
    public static void main(String[] args) throws IOException, BadLocationException {
        File javaSRC = new File("test123.java");
        final String source = FileUtils.readFileToString(javaSRC);
        org.eclipse.jface.text.Document document = new org.eclipse.jface.text.Document(source);
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(document.get().toCharArray());
        parser.setKind(ASTParser.K_STATEMENTS);
        final Block block = (Block) parser.createAST(null);
        ASTRewrite rewriter = ASTRewrite.create(block.getAST());

        ASTParser parser1 = ASTParser.newParser(AST.JLS8);
        parser1.setSource(document.get().toCharArray());
        parser1.setKind(ASTParser.K_STATEMENTS);
        final Block block1 = (Block) parser1.createAST(null);
        AST ast1 =block1.getAST();
        Statement oldStatement1 = (Statement) block1.statements().get(0);
        MethodInvocation newInvocation1 = ast1.newMethodInvocation();
        newInvocation1.setName(ast1.newSimpleName("add"));

        // for getting insertion position
        AST ast = block.getAST();
        Statement oldStatement = (Statement) block.statements().get(0);

        // create new statements for insertion
        MethodInvocation newInvocation = ast.newMethodInvocation();
        newInvocation.setName(ast.newSimpleName("add"));
        newInvocation.setExpression((Expression) ASTNode.copySubtree(ast,newInvocation1));
        Statement newStatement = ast.newExpressionStatement(newInvocation);

        //create ListRewrite
        ListRewrite listRewrite = rewriter.getListRewrite(block, Block.STATEMENTS_PROPERTY);
//        listRewrite.insertFirst(newStatement, null);
        listRewrite.replace(oldStatement,newStatement,null);

        TextEdit edits = rewriter.rewriteAST(document,null);
        edits.apply(document);
//        FileUtils.write(javaSRC, document.get());

        System.out.println(block.statements());
        System.out.println(document.get());
    }
//    public static void main(String[] args) throws IOException, BadLocationException {
//        File javaSRC = new File("test123.java");
//        final String source = FileUtils.readFileToString(javaSRC);
//        org.eclipse.jface.text.Document document = new org.eclipse.jface.text.Document(source);
//        ASTParser parser = ASTParser.newParser(AST.JLS8);
//        parser.setSource(document.get().toCharArray());
//        parser.setKind(ASTParser.K_STATEMENTS);
//
//        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
//        ASTRewrite rewriter = ASTRewrite.create(cu.getAST());
//
//        cu.recordModifications();
//
//        // for getting insertion position
//        AST ast = cu.getAST();
//        TypeDeclaration typeDecl = (TypeDeclaration) cu.types().get(0);
//        MethodDeclaration methodDecl = typeDecl.getMethods()[0];
//        Block block = methodDecl.getBody();
//
//        // create new statements for insertion
//        MethodInvocation newInvocation = ast.newMethodInvocation();
//        newInvocation.setName(ast.newSimpleName("add"));
//        Statement newStatement = ast.newExpressionStatement(newInvocation);
//
//        //create ListRewrite
//        ListRewrite listRewrite = rewriter.getListRewrite(block, Block.STATEMENTS_PROPERTY);
//        listRewrite.insertFirst(newStatement, null);
//
//        TextEdit edits = rewriter.rewriteAST(document,null);
//        edits.apply(document);
////        FileUtils.write(javaSRC, document.get());
//
//        System.out.println(block.statements());
//        System.out.println(document.get());
//    }
}
