package com.pku.apidiff.internal.visitor;

import com.pku.apidiff.Signature;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.*;

public class MethodInvocationVisitor extends com.pku.apidiff.MethodInvocationVisitor {
    List<Signature> apiCallersMap = new LinkedList<>();
    @Override
    public boolean visit(MethodInvocation node) {
        // record filePath signature position
        List<String> signature = getCallerSignatureFromMethodInvocation(node);
        apiCallersMap.add(Signature.getSignatureFromStringList(signature,node.getStartPosition()));
        return super.visit(node);
    }
}
