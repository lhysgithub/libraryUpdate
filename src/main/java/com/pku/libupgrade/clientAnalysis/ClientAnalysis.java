package com.pku.libupgrade.clientAnalysis;


import com.pku.libupgrade.Utils;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CleanCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.osgi.service.security.TrustEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class ClientAnalysis {
    private class RevFilterCommitValid extends RevFilter {

        @Override
        public final boolean include(final RevWalk walker, final RevCommit c) {

            Long diffTimestamp = 0L;
            diffTimestamp = this.calcDiffTimeCommit(c);

            if (c.getParentCount() > 1) {//merge
                logger.info("Merge of the branches deleted. [commitId=" + c.getId().getName() + "]");
                return false;
            }
            //TODO: create other filter to date.
            return true;
        }

        private Long calcDiffTimeCommit(final RevCommit c) {
            Long timestampNow = Calendar.getInstance().getTimeInMillis();
            Long timestampCommit = c.getAuthorIdent().getWhen().getTime();
            Calendar calendarCommit = Calendar.getInstance();
            calendarCommit.setTime(new Date(timestampCommit));
            return Math.abs(timestampCommit - timestampNow);
        }

        private String getDateCommitFormat(final RevCommit c) {
            Long timestampCommit = c.getAuthorIdent().getWhen().getTime();
            Calendar calendarCommit = Calendar.getInstance();
            calendarCommit.setTime(new Date(timestampCommit));
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
            return format.format(timestampCommit);
        }

        @Override
        public final RevFilter clone() {
            return this;
        }

        @Override
        public final boolean requiresCommitBody() {
            return false;
        }

        @Override
        public String toString() {
            return "RegularCommitsFilter";
        }
    }

    private Logger logger = LoggerFactory.getLogger(ClientAnalysis.class);
    public List<String> recordPomFilePath = new ArrayList<>();

    public void getPomPath(File f) {
        //System.out.println("1:"+f.getAbsolutePath());
        if (f.isDirectory()) {
            for (File ftemp : f.listFiles()) {
                getPomPath(ftemp);
            }
        } else {
            if (f.getName().endsWith("pom.xml")) {
                recordPomFilePath.add(f.getAbsolutePath());
            }
        }
    }

    public Repository openRepository(String path, String projectName) throws Exception {
        File folder = new File(Utils.getPathProject(path, projectName));
        Repository repository = null;

        this.logger.info(projectName + " exists. Reading properties ... (wait)");
        RepositoryBuilder builder = new RepositoryBuilder();
        repository = builder
                .setGitDir(new File(folder, ".git"))
                .readEnvironment()
                .findGitDir()
                .build();
        return repository;
    }

    public RevWalk createAllRevsWalk(Repository repository, String branch) throws Exception {
        List<ObjectId> currentRemoteRefs = new ArrayList<ObjectId>();
        for (Ref ref : repository.getAllRefs().values()) {
            String refName = ref.getName();
            if (refName.startsWith("refs/remotes/origin/")) {
                if (branch == null || refName.endsWith("/" + branch)) {
                    currentRemoteRefs.add(ref.getObjectId());
                }
            }
        }

        RevWalk walk = new RevWalk(repository);
        for (ObjectId newRef : currentRemoteRefs) {
            walk.markStart(walk.parseCommit(newRef));
        }
        walk.setRevFilter(new RevFilterCommitValid());
        return walk;
    }

    public List<String> getAllCommitId(Repository repository, String branch) throws Exception {

        List<String> returnCommitList = new ArrayList<>();
        RevWalk revWalk = createAllRevsWalk(repository, branch);
        Iterator<RevCommit> i = revWalk.iterator();
        while (i.hasNext()) {
            RevCommit currentCommit = i.next();
            if (currentCommit.getParentCount() != 0) {
                String commitId = currentCommit.getParent(0).getName();
                returnCommitList.add(commitId);
            }

        }
        return returnCommitList;
    }

    public void checkout(Repository repository, String commitId) throws Exception {
        this.logger.info("Checking out {} {} ...", repository.getDirectory().getParent().toString(), commitId);
        try (Git git = new Git(repository)) {
            CleanCommand clean = git.clean().setForce(true);
            clean.call();
            ResetCommand reset = git.reset().setMode(ResetCommand.ResetType.HARD);
            reset.call();
            CheckoutCommand checkout = git.checkout().setName(commitId);
            checkout.call();
        }
    }


    public static void main(String[] args) throws Exception {
        String projectPath = "../dataset/";
        String projectName = "plantuml";
        ClientAnalysis clientAnalysis = new ClientAnalysis();
        Repository repository = clientAnalysis.openRepository(projectPath, projectName);
        List<String> commitIds = clientAnalysis.getAllCommitId(repository, "master");
        System.out.println(commitIds.size());
        System.out.println(commitIds);
        int i = 0;
        for (String commitId : commitIds) {
            System.out.println(commitId);
            clientAnalysis.checkout(repository, commitId);
            clientAnalysis.recordPomFilePath = new ArrayList<>();
            clientAnalysis.getPomPath(new File(projectPath + "\\" + projectName));
            Map<String, Map<String, String>> totPomInfoMap = new HashMap<>();
            for (String pomPath : clientAnalysis.recordPomFilePath) {
                Map<String, String> pomInfoMap = Utils.readOutLibraries(pomPath);
                totPomInfoMap.put(pomPath.replace(projectPath, ""), pomInfoMap);
            }
            System.out.println(totPomInfoMap);

            i += 1;
            if (i > 100) {
                break;
            }

        }


    }

}
