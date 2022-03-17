package com.pku.apidiff.internal.analysis;

import com.pku.apidiff.Result;
import com.pku.apidiff.internal.refactor.RefactorProcessor;
import com.pku.apidiff.internal.refactor.RefactoringProcessorImpl;
import com.pku.apidiff.internal.visitor.APIVersion;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import refdiff.core.api.RefactoringType;
import refdiff.core.rm2.model.refactoring.SDRefactoring;

import java.util.List;
import java.util.Map;

public class DiffProcessorImpl implements DiffProcessor {

	@Override
	public Result detectChange(final APIVersion version1, final APIVersion version2, final Repository repository, final RevCommit revCommit) {
		Result result = new Result();
		Map<RefactoringType, List<SDRefactoring>> refactorings = this.detectRefactoring(repository, revCommit.getId().getName());
		result.getChangeType().addAll(new TypeDiff().detectChange(version1, version2, refactorings, revCommit));
		result.getChangeMethod().addAll(new MethodDiff().detectChange(version1, version2, refactorings, revCommit));
		result.getChangeField().addAll(new FieldDiff().detectChange(version1, version2, refactorings, revCommit));
		return result;
	}

	@Override
	public Result detectChange(final APIVersion version1, final APIVersion version2) {
		Result result = new Result();
		Map<RefactoringType, List<SDRefactoring>> refactorings = null; // todo waite for repair
		result.getChangeType().addAll(new TypeDiff().detectChange(version1, version2, refactorings));
		result.getChangeMethod().addAll(new MethodDiff().detectChange(version1, version2, refactorings));
		result.getChangeField().addAll(new FieldDiff().detectChange(version1, version2, refactorings));
		return result;
	}

	@Override
	public Map<RefactoringType, List<SDRefactoring>> detectRefactoring(Repository repository, String commit) {
		RefactorProcessor refactoringDetector = new RefactoringProcessorImpl();
		return refactoringDetector.detectRefactoringAtCommit(repository, commit);
	}

}
