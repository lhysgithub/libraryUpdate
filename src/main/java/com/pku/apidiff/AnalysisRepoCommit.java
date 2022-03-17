package com.pku.apidiff;

public class AnalysisRepoCommit {


    public Result detectChangeAllHistory(String branch,String path,String nameProject) throws Exception {
        Result result = new Result();
//        GitService service = new GitServiceImpl();
//        Repository repository = service.openRepositoryAndCloneIfNotExists(path, nameProject);
//        RevWalk revWalk = service.createAllRevsWalk(repository, branch);
//        //Commits.
//        Iterator<RevCommit> i = revWalk.iterator();
//        while(i.hasNext()){
//            RevCommit currentCommit = i.next();
//
//            for(Classifier classifierAPI: classifiers){
//                Result resultByClassifier = this.diffCommit(currentCommit, repository, this.nameProject, classifierAPI);
//                result.getChangeType().addAll(resultByClassifier.getChangeType());
//                result.getChangeMethod().addAll(resultByClassifier.getChangeMethod());
//                result.getChangeField().addAll(resultByClassifier.getChangeField());
//            }
//        }
//        this.logger.info("Finished processing.");
        return result;
    }
}
