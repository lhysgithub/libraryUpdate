package com.pku.apidiff;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MethodInvocationVisitor extends ASTVisitor{
//    private String methodSignature = "null handleBuildError(final ReactorContext buildContext, final MavenSession rootSession, final MavenProject mavenProject, Exception e, final long buildStartTime)";
//    private String methodName = "calculateExecutionPlan";
//    private List<String> signature = new LinkedList<>();
//    public List<MethodInvocation> callers = new LinkedList<>();
    public List<Caller> callerSignatures = new LinkedList<>();
    public List<TypeUsage> apiTypeUsages = new ArrayList<>();
    public List<FieldUsage> apiFieldUsages = new ArrayList<>();
//    public void setTargetMethod(String methodSignature) {
//        this.methodSignature = methodSignature;
//        this.methodName=methodSignature.split("\\(")[0].split(" ")[1];
//
//        // return type
//        List<String> signature = new LinkedList<>();
//        signature.add(methodSignature.split(" ")[0]);
//
//        // method name
//        signature.add(this.methodName);
//
//        // parameter list
//        String list = methodSignature.split("\\(")[1].split("\\)")[0];
//        for(String para:list.split(",")){
//            Integer size = para.split(" ").length;
//            signature.add(para.split(" ")[size-2]);
//        }
//    }
//    public List<MethodInvocation> getCallers(){ return this.callers; }
//    public List<Caller> getCallerSignatures(){ return this.callerSignatures; }

//    public void getSourceCodeSnippet(){
//        for(MethodInvocation i:callers){
//            System.out.println("StartPosition:" + i.getStartPosition());
//            System.out.println("Length:" + i.getLength());
//        }
//    }

    public List<String> getCallerSignatureFromMethodInvocation(MethodInvocation node){
        List<String> signature = new LinkedList<>();

//        // if iMethodBinding is not null, it could be easy to get Signature
////        IMethodBinding iMethodBinding = node.resolveMethodBinding();
////        iMethodBinding.getReturnType().getQualifiedName();
////        iMethodBinding.getName();
////        iMethodBinding.getTypeParameters();

//        List argumentsType = node.typeArguments();
//        for (Integer i=0 ; i<argumentsType.size() ; i++) {
//            Object test = argumentsType.get(i);
//            test.toString();
//        }

        // return type
        ITypeBinding returnType = node.resolveTypeBinding();
        if (returnType!=null){signature.add(returnType.getName());}
        else{signature.add("null");}

        // method name
        String methodName = node.getName().toString();
        signature.add(methodName);

        // parameter list
        List argumentsAST = node.arguments();
        for (Integer i=0 ; i<argumentsAST.size() ; i++){
            if(argumentsAST.get(i) instanceof Expression){
                Expression exp = (Expression) argumentsAST.get(i);
                ITypeBinding typeBinding = exp.resolveTypeBinding();
                if (typeBinding!=null){signature.add(typeBinding.getName());}
                else{signature.add("null");}
            }
        }

        // objectType
        Expression tempExp = node.getExpression();
        if(tempExp!=null){
            ITypeBinding tempTypeBingding = tempExp.resolveTypeBinding();
            if (tempTypeBingding!=null){
                signature.add(tempTypeBingding.getName());
            }
            else{
                signature.add("null");
            }
        }
        else{
            signature.add("null");
        }

        return signature;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        List<String> signature = getCallerSignatureFromMethodInvocation(node);


//        // add caller
//        StringBuilder str = new StringBuilder();
//        int j =0;
//        for (String i:signature) {
//            if (j==0) str.append(i);
//            else str.append(" ").append(i);
//            j++;
//        }

//        callers.add(node);
        callerSignatures.add(Caller.getSignatureFromStringList(signature,node.getStartPosition()));

        return super.visit(node);
    }
    @Override // QualifiedName = FieldAccess   may work // not include member function
    public boolean visit(QualifiedName node)   {
        String typeName;
        if (node.getQualifier().resolveTypeBinding()!=null){typeName = node.getQualifier().resolveTypeBinding().getName();}
        else{typeName="null";}
        String fieldName = node.getName().toString();
        int position  = node.getStartPosition();
        apiFieldUsages.add(new FieldUsage(typeName,fieldName,position));
//        System.out.println(node);
        return super.visit(node);
    }

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
//        System.out.println(node);
        return super.visit(node);
    }
//    public boolean visitWithTargetSignature(MethodInvocation node) {
//        List<String> signature = new LinkedList<>();
//
//        // if iMethodBinding is not null, it could be easy to get Signature
////        IMethodBinding iMethodBinding = node.resolveMethodBinding();
////        iMethodBinding.getReturnType().getQualifiedName();
////        iMethodBinding.getName();
////        iMethodBinding.getTypeParameters();
//
//        // return type
//        ITypeBinding returnType = node.resolveTypeBinding();
//        if (returnType!=null){signature.add(returnType.getName());}
//        else{signature.add("null");}
//
//        // method name
//        String methodName = node.getName().toString();
//        signature.add(methodName);
//
//        // parameter list
//        List argumentsAST = node.arguments();
//        for (Integer i=0 ; i<argumentsAST.size() ; i++){
//            if(argumentsAST.get(i) instanceof Expression){
//                Expression exp = (Expression) argumentsAST.get(i);
//                ITypeBinding typeBinding = exp.resolveTypeBinding();
//                if (typeBinding!=null){signature.add(typeBinding.getName());}
//                else{signature.add("null");}
//            }
//        }
//
//        // Signature not equal on length
//        if (signature.size() != this.signature.size()){
//            return super.visit(node);
//        }
//
//        // Signature matching
//        Boolean isEqual = true;
//        for(Integer i=0; i<signature.size();i++){
//            if(signature.get(i).equals("null") || this.signature.get(i).equals("null")){
//                continue;
//            }
//            if(!signature.get(i).equals(this.signature.get(i))){
//                isEqual = false;
//                break;
//            }
//        }
//
//        // add caller
//        if (isEqual){callers.add(node);}
//
//        return super.visit(node);
//    }

}