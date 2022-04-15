import com.pku.apidiff.FieldUsage;
import com.pku.apidiff.TypeUsage;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestVisitor extends ASTVisitor {
    public String testField = "";
    public List<TypeUsage> apiTypeUsages = new ArrayList<>();
    public List<FieldUsage> apiFieldUsages = new ArrayList<>();
//    public Map<String, List<TypeUsage>> apiTypeUsageMap = new HashMap<>();
//    public Map<String, List<FieldUsage>> apiFieldUsageMap = new HashMap<>();
//    @Override // may work
//    public boolean visit(MethodInvocation node) {
//        System.out.println(node);
//        return super.visit(node);
//    }
//    @Override
//    public boolean visit(FieldAccess node) {
//        System.out.println(node);
//        return super.visit(node);
//    }
//    @Override
//    public boolean visit(MethodRef node) {
//        System.out.println(node);
//        System.out.println(node.getName());
//        System.out.println(node.getQualifier());
//        return super.visit(node);
//    }
//    @Override
//    public boolean visit(AnnotationTypeDeclaration node)  {
//        System.out.println(node);
//        return super.visit(node);
//    }
//    @Override
//    public boolean visit(AnnotationTypeMemberDeclaration node)   {
//        System.out.println(node);
//        return super.visit(node);
//    }
//    @Override
//    public boolean visit(AnonymousClassDeclaration node)   {
//        System.out.println(node);
//        return super.visit(node);
//    }
//    @Override
//    public boolean visit(MethodRefParameter node)   {
//        System.out.println(node);
//        return super.visit(node);
//    }
//    @Override
//    public boolean visit(SingleMemberAnnotation node)   {
//        System.out.println(node);
//        return super.visit(node);
//    }
//    @Override
//    public boolean visit(StringLiteral node)   {
//        System.out.println(node);
//        return super.visit(node);
//    }
//    @Override
//    public boolean visit(TypeDeclaration node)   {
//        System.out.println(node);
//        return super.visit(node);
//    }

//    worked
    @Override // VariableDeclarationStatement may work
    public boolean visit(VariableDeclarationStatement node)   {
        String typeName;
        if (node.getType().resolveBinding()!=null){typeName = node.getType().resolveBinding().getName();}
        else {typeName = "null";}
        int position = node.getStartPosition();
        apiTypeUsages.add(new TypeUsage(typeName,position));
//        System.out.println(typeName);
//        System.out.println(position);
        System.out.println(node);
        return super.visit(node);
    }

//    @Override
//    public boolean visit(VariableDeclarationExpression node)   {
//        System.out.println(node);
//        return super.visit(node);
//    }

//    // worked
    @Override // QualifiedName = FieldAccess   may work // not include member function
    public boolean visit(QualifiedName node)   {
        String typeName;
        if (node.getQualifier().resolveTypeBinding()!=null){typeName = node.getQualifier().resolveTypeBinding().getName();}
        else{typeName="null";}
        String fieldName = node.getName().toString();
        int position  = node.getStartPosition();
        apiFieldUsages.add(new FieldUsage(typeName,fieldName,position));
        System.out.println(node);
        return super.visit(node);
    }

//    @Override
//    public boolean visit(QualifiedType node)   {
//        System.out.println(node);
//        return super.visit(node);
//    }
//    @Override
//    public boolean visit(VariableDeclarationFragment node)   {
//        System.out.println(node);
//        return super.visit(node);
//    }
//    @Override
//    public boolean visit(MemberRef node)  {
//        System.out.println(node);
//        return super.visit(node);
//    }
}
