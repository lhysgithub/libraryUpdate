import org.eclipse.jdt.core.dom.*;

public class testForStatements {
    public static void main(String[] args) {

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource("int i = 9; \n int j = i+1;".toCharArray());

        parser.setKind(ASTParser.K_STATEMENTS);

        Block block = (Block) parser.createAST(null);

        //here can access the first element of the returned statement list
        String str = block.statements().get(0).toString();

        System.out.println(str);

        block.accept(new ASTVisitor() {

            public boolean visit(SimpleName node) {

                System.out.println("Name: " + node.getFullyQualifiedName());

                return true;
            }

        });
    }
}
