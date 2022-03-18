package com.pku.libupgrade;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CleanCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;

import java.io.File;
import java.util.Objects;

public class GitService {
    public static Repository openRepository(String path) throws Exception {
        File folder = new File(path);
        Repository repository = null;

//        this.logger.debug(projectName + " exists. Reading properties ... (wait)");
        RepositoryBuilder builder = new RepositoryBuilder();
        repository = builder
                .setGitDir(new File(folder, ".git"))
                .readEnvironment()
                .findGitDir()
                .build();
        return repository;
    }

    public static void checkout(Repository repository, String commitId) throws Exception {
        String projectPath = repository.getDirectory().getAbsolutePath();
        String indexPath =projectPath+"/index.lock";
        File file1 = new File(indexPath);
        if (file1.exists()){
            file1.delete();
        }
        File projectDir = new File(projectPath.split("/\\.git")[0]);
        for(File f: Objects.requireNonNull(projectDir.listFiles())){
            if(f.getName().equals(".git")){
                continue;
            }
            deleteDir(f);
        }
//        this.logger.debug("Checking out {} {} ...", repository.getDirectory().getParent().toString(), commitId);
        try (Git git = new Git(repository)) {
            CleanCommand clean = git.clean().setForce(true);
            clean.call();
            ResetCommand reset = git.reset().setMode(ResetCommand.ResetType.HARD);
            reset.call();
            CheckoutCommand checkout = git.checkout().setName(commitId).setForce(true);
            checkout.call();
        }
    }

    private static boolean deleteDir(File dir) {
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
}
