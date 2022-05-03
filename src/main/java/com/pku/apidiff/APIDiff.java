package com.pku.apidiff;

import com.pku.apidiff.enums.Classifier;
import com.pku.apidiff.internal.analysis.DiffProcessor;
import com.pku.apidiff.internal.analysis.DiffProcessorImpl;
import com.pku.apidiff.internal.service.git.GitFile;
import com.pku.apidiff.internal.service.git.GitService;
import com.pku.apidiff.internal.service.git.GitServiceImpl;
import com.pku.apidiff.internal.util.UtilTools;
import com.pku.apidiff.internal.visitor.APIVersion;
import com.pku.libupgrade.Utils;
import com.pku.libupgrade.clientAnalysis.ClientAnalysis;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class APIDiff implements DiffDetector{
	
	private String nameProject;
	
	private String path;
	
	private String url;

	public static Logger logger = LoggerFactory.getLogger(APIDiff.class);

	public APIDiff() {
	}

	public APIDiff(final String nameProject, final String url) {
		this.url = url;
		this.nameProject = nameProject;
	}
	
	public String getPath() {return path;}

	public void setPath(String path) {this.path = path;}

//	public static void main(String []args) throws Exception {
//		APIDiff diff = new APIDiff("fastjson", "https://github.com/alibaba/fastjson.git");
//		diff.setPath("../dataset/");
////		Result result = diff.detectChangeAllHistory("master", Classifier.API);
//		Result result = diff.diffLib("/Users/liuhongyi/.m2/repository/org/apache/maven/maven-core/3.0.4/maven-core-3.0.4-sources",
//				"/Users/liuhongyi/.m2/repository/org/apache/maven/maven-core/3.1.0/maven-core-3.1.0-sources",diff.nameProject, Classifier.API);
//		for(Change changeMethod : result.getChangeMethod()){
//			System.out.println("\n" + changeMethod.getCategory().getDisplayName() + " - " + changeMethod.getDescription());
//		}
//	}
	public static void main(String[] args) throws Exception {
		Utils.findPopularLibFromCsv("commitDiff1.csv","popularLib.txt");
		Utils.downloadPopularMavenRepository("popularLib.txt", "../dataset/");
	}

	public static void apiDiff() throws Exception {
		Utils.findPopularLibFromCsv("commitDiff1.csv","popularLib.txt");
		Utils.downloadPopularMavenRepository("popularLib.txt", "../dataset/");
	}

	public static void apiDiff(String oldPath, String newPath,String oldId, String newId, String versionPairPath) throws Exception {
		APIDiff diff = new APIDiff();
		Result result = diff.diffLib(oldPath, newPath,oldId ,newId, Classifier.API);
		if (result.getChangeMethod().size()==0 && result.getChangeField().size()==0 && result.getChangeType().size()==0) {return;}
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(versionPairPath)));
		for(Change changeType : result.getChangeType()){
			logger.error("Type: "+ changeType.getCategory().getDisplayName() + " - " + changeType.getDescription());
			bw.write("Type: "+changeType.getCategory().getDisplayName() + " - " + changeType.getDescription() + '\n');
			bw.flush();
		}
		for(Change changeMethod : result.getChangeMethod()){
			logger.error("Method: "+ changeMethod.getCategory().getDisplayName() + " - " + changeMethod.getDescription());
			bw.write("Method: "+changeMethod.getCategory().getDisplayName() + " - " + changeMethod.getDescription() + '\n');
			bw.flush();
		}
		for(Change changeField : result.getChangeField()){
			logger.error("Field: "+ changeField.getCategory().getDisplayName() + " - " + changeField.getDescription());
			bw.write("Field: "+changeField.getCategory().getDisplayName() + " - " + changeField.getDescription() + '\n');
			bw.flush();
		}
		bw.close();
	}

	@Override
	public Result detectChangeAtCommit(String commitIdOld,String commitIdNew, Classifier classifierAPI) {
		Result result = new Result();
		try {
			GitService service = new GitServiceImpl();
			Repository repository = service.openRepositoryAndCloneIfNotExists(this.path, this.nameProject, this.url);
			RevCommit commitOld = service.createRevCommitByCommitId(repository, commitIdOld);
			RevCommit commitNew = service.createRevCommitByCommitId(repository, commitIdNew);
			Result resultByClassifier = this.diffCommit(commitOld,commitNew, repository, this.nameProject, classifierAPI);
			result.getChangeType().addAll(resultByClassifier.getChangeType());
			result.getChangeMethod().addAll(resultByClassifier.getChangeMethod());
			result.getChangeField().addAll(resultByClassifier.getChangeField());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in calculating commitn diff ", e);
		}
		logger.info("Finished processing.");
		return result;
	}
	
	@Override
	public Result detectChangeAllHistory(String branch, List<Classifier> classifiers) throws Exception {

		Result result = new Result();
		GitService service = new GitServiceImpl();
		Repository repository = service.openRepositoryAndCloneIfNotExists(this.path, this.nameProject, this.url);

		Repository localRepo;
		localRepo = new FileRepository(this.path+File.separator+this.nameProject + File.separator+".git");
		Git git = new Git(localRepo);
		System.out.println("获取本地");
		ListTagCommand listTagCommand=git.tagList();
		List<Ref> refs=listTagCommand.call();
		List<String> commitList=new ArrayList<>();
		List<String> tagList=new ArrayList<>();
		for(Ref ref:refs){
			System.out.println(ref.getName().replace("refs/tags/",""));
			System.out.println(ref.getObjectId().getName());
			tagList.add(ref.getName().replace("refs/tags/",""));
			commitList.add(ref.getObjectId().getName());
		}
		System.out.println(commitList.size());
		System.out.println(tagList.size());

		for(Classifier classifierAPI: classifiers){
//			RevCommit commitOld = service.createRevCommitByCommitId(repository, "914996cac11108ec1a02c21a10af53ebc4980d7f");//e2aafc15d47c7313d47644ba1b9212ee1aed0dd0
//			RevCommit commitNew = service.createRevCommitByCommitId(repository, "3035749168c8a4187cf3a51d19a6aee3bc5958d1");//6460f65759694488446a51e79f74c742290fc13e
			RevCommit commitOld = service.createRevCommitByCommitId(repository, "e2aafc15d47c7313d47644ba1b9212ee1aed0dd0");//e2aafc15d47c7313d47644ba1b9212ee1aed0dd0
			RevCommit commitNew = service.createRevCommitByCommitId(repository, "6460f65759694488446a51e79f74c742290fc13e");//6460f65759694488446a51e79f74c742290fc13e
//			System.out.println(commitOld.getName()+"\t"+commitNew.getName());
//			System.out.println(tagList.get(i)+"\t"+tagList.get(i+1));
			Result resultByClassifier = this.diffCommit(commitOld, commitNew, repository, this.nameProject, classifierAPI);
			result.getChangeType().addAll(resultByClassifier.getChangeType());
			result.getChangeMethod().addAll(resultByClassifier.getChangeMethod());
			result.getChangeField().addAll(resultByClassifier.getChangeField());
		}
		for(Change changeMethod : result.getChangeMethod()){
			System.out.println("output:");
			System.out.println("\n" + changeMethod.getCategory().getDisplayName() + " - " + changeMethod.getDescription());
		}
		this.logger.info("Finished processing.");
		return result;
	}
	
	@Override
	public Result detectChangeAllHistory(List<Classifier> classifiers) throws Exception {
		return this.detectChangeAllHistory(null, classifiers);
	}
	
	@Override
	public Result fetchAndDetectChange(List<Classifier> classifiers) {
		Result result = new Result();
		this.logger.info("Finished processing.");
		return result;
	}
	
	@Override
	public Result detectChangeAllHistory(String branch, Classifier classifier) throws Exception {
		return this.detectChangeAllHistory(branch, Arrays.asList(classifier));
	}

	@Override
	public Result detectChangeAllHistory(Classifier classifier) throws Exception {
		return this.detectChangeAllHistory(Arrays.asList(classifier));
	}

	@Override
	public Result fetchAndDetectChange(Classifier classifier) throws Exception {
		return this.fetchAndDetectChange(Arrays.asList(classifier));
	}


	private Result diffLib(final String oldPath, final String newPath, String oldId, String newId, Classifier classifierAPI) throws Exception{
		APIVersion version1 = new APIVersion(oldId,oldPath,classifierAPI);//old version
		APIVersion version2 = new APIVersion(newId,newPath,classifierAPI);//new version
		DiffProcessor diff = new DiffProcessorImpl();
		return diff.detectChange(version1, version2); // todo waite for repair
	}

	private Result diffCommit(final RevCommit commitOld, final RevCommit commitNew, final Repository repository, String nameProject, Classifier classifierAPI) throws Exception{
		File projectFolder = new File(UtilTools.getPathProject(this.path, nameProject));
		if(commitNew.getParentCount() != 0){//there is at least one parent
			try {
				GitService service = new GitServiceImpl();
				Map<ChangeType, List<GitFile>> mapModifications = service.fileTreeDiff(repository, commitOld,commitNew);
				//System.out.println(mapModifications);
				APIVersion version1 = this.getAPIVersionByCommit(commitOld.getName(), projectFolder, repository, commitNew, classifierAPI,mapModifications);//old version
				APIVersion version2 = this.getAPIVersionByCommit(commitNew.getName(), projectFolder, repository, commitNew, classifierAPI,mapModifications); //new version
				DiffProcessor diff = new DiffProcessorImpl();
				return diff.detectChange(version1, version2, repository, commitNew);
			} catch (Exception e) {
				e.printStackTrace();
				this.logger.error("Error during checkout [commit=" + commitNew + "]");
			}
		}
		return new Result();
	}
	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}
	private APIVersion getAPIVersionByCommit(String commit, File projectFolder, Repository repository, RevCommit currentCommit, Classifier classifierAPI,Map<ChangeType, List<GitFile>> mapModifications) throws Exception{
		GitService service = new GitServiceImpl();
		for(File f:projectFolder.listFiles()){
			if(f.getName().equals(".git")){
				continue;
			}
			deleteDir(f);
		}
		boolean isT=false;
		if(projectFolder.listFiles().length==1){
			if(projectFolder.listFiles()[0].getName().equals(".git")){
				isT=true;
			}
		}
		System.out.println("isT:"+isT);
		if(isT){
			try{
				service.checkout(repository, commit);
			}
			catch (Exception e){
				e.printStackTrace();
			}
		}
		else{
			System.out.println("notTrue");
		}
		return new APIVersion(this.path, projectFolder, mapModifications, classifierAPI);
	}

}
