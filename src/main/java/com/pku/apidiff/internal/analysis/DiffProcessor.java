package com.pku.apidiff.internal.analysis;

import com.pku.apidiff.Result;
import com.pku.apidiff.internal.visitor.APIVersion;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import refdiff.core.api.RefactoringType;
import refdiff.core.rm2.model.refactoring.SDRefactoring;

import java.util.List;
import java.util.Map;

public interface DiffProcessor {
	
	public Map<RefactoringType, List<SDRefactoring>> detectRefactoring(final Repository repository, final String commit);
	
	public Result detectChange(final APIVersion version1, final APIVersion version2, final Repository repository, final RevCommit revCommit);

	public Result detectChange(final APIVersion version1, final APIVersion version2);

}
