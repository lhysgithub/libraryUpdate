package com.pku.apidiff;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import com.pku.libupgrade.DiffCommit;
import com.pku.libupgrade.GitService;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.Repository;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CodeDiff {
//    public static void main(String[] args) throws IOException, DiffException {
//        getCodeDiff("BuilderCommon1.txt","BuilderCommon2.txt",5,3882);
//    }

    public static List<String> getCodeDiff(String originFilePath, String targetFilePath, int contextWidth, int sourceCharNumber) throws IOException, DiffException {
//        int sourceLineNumber = 92;
        int sourceLineNumber = getLineNumber(originFilePath,sourceCharNumber);
        AtomicInteger targetLineNumber = new AtomicInteger();
        File src = new File(originFilePath);
        File target = new File(targetFilePath);
        List<String> original = IOUtils.readLines(new FileInputStream(src), "UTF-8");
        List<String> revised = IOUtils.readLines(new FileInputStream(target), "UTF-8");
        List<String> codeDiff = new LinkedList<>();
        List<String> diffOriginal = new LinkedList<>();
        List<String> diffPatched = new LinkedList<>();
        Patch<String> diff = DiffUtils.diff(original, revised);
        List<AbstractDelta<String>> deltas = diff.getDeltas();
        deltas.forEach(delta -> {
            switch (delta.getType()) {
                case INSERT: // todo fix new file start with a new blank line
                    //新增
                    Chunk<String> insert = delta.getTarget();
                    System.out.println("+ " + (insert.getPosition() + 1) + " " + insert.getLines());
                    break;
                case CHANGE:
                    //修改
                    Chunk<String> source = delta.getSource();
                    Chunk<String> target1 = delta.getTarget();
                    System.out.println("\n- " + (source.getPosition() + 1) + " " + source.getLines() + "\n+ " + "" + (target1.getPosition() + 1) + " " + target1.getLines());
                    if (source.getPosition() == sourceLineNumber){
                        diffOriginal.addAll(source.getLines());
                        diffPatched.addAll(target1.getLines());
                        targetLineNumber.set(target1.getPosition());
                    }
                    break;
                case DELETE:
                    //删除
                    Chunk<String> delete = delta.getSource();
                    System.out.println("- " + (delete.getPosition() + 1) + " " + delete.getLines());
                    if (delete.getPosition() == sourceLineNumber){
                        diffOriginal.addAll(delete.getLines());
                        targetLineNumber.set(delete.getPosition());
                    }
                    break;
                case EQUAL:
//                    System.out.println("无变化");
                    break;
            }
        });
        List<String> resultDiffOriginal = getDiffWithContext(originFilePath,diffOriginal,sourceLineNumber,contextWidth);
        List<String> resultDiffPatched = getDiffWithContext(targetFilePath,diffPatched, targetLineNumber.get(),contextWidth);

        for(String i:resultDiffOriginal){
            System.out.println(i);
        }
        System.out.println("***************************************");
        for(String i:resultDiffPatched){
            System.out.println(i);
        }
        return codeDiff;
    }


    public static List<String> getGitCodeDiff(String FilePath, DiffCommit diffCommit, int contextWidth, int sourceCharNumber) throws Exception {
//        int sourceLineNumber = 92;
        String oldCommit = diffCommit.commit;
        String newCommit = diffCommit.newCommit;
        String projectPath = "../dataset/"+diffCommit.clientName;
        int sourceLineNumber = getLineNumber(FilePath,sourceCharNumber);
        AtomicInteger targetLineNumber = new AtomicInteger();
        targetLineNumber.set(sourceLineNumber);
        Repository r = GitService.openRepository(projectPath);
        GitService.checkout(r,oldCommit);
        File src = new File(FilePath);
        List<String> original = IOUtils.readLines(new FileInputStream(src), "UTF-8");
        GitService.checkout(r,newCommit);
        File target = new File(FilePath);
        List<String> revised = IOUtils.readLines(new FileInputStream(target), "UTF-8");
        List<String> codeDiff = new LinkedList<>();
        List<String> diffOriginal = new LinkedList<>();
        List<String> diffPatched = new LinkedList<>();
        Patch<String> diff = DiffUtils.diff(original, revised);
        List<AbstractDelta<String>> deltas = diff.getDeltas();
        AtomicReference<Boolean> isPoint = new AtomicReference<>(false);
        deltas.forEach(delta -> {
            switch (delta.getType()) {
                case INSERT: // todo fix new file start with a new blank line
                    //新增
                    Chunk<String> insert = delta.getTarget();
//                    System.out.println("+ " + (insert.getPosition() + 1) + " " + insert.getLines());
                    break;
                case CHANGE:
                    //修改
                    Chunk<String> source = delta.getSource();
                    Chunk<String> target1 = delta.getTarget();
                    if (source.getPosition() == sourceLineNumber){
                        System.out.println("\n- " + (source.getPosition() + 1) + " " + source.getLines() + "\n+ " + "" + (target1.getPosition() + 1) + " " + target1.getLines());
                        diffOriginal.addAll(source.getLines());
                        diffPatched.addAll(target1.getLines());
                        targetLineNumber.set(target1.getPosition());
                        isPoint.set(true);
                    }
                    break;
                case DELETE:
                    //删除
                    Chunk<String> delete = delta.getSource();

                    if (delete.getPosition() == sourceLineNumber){
                        System.out.println("- " + (delete.getPosition() + 1) + " " + delete.getLines());
                        diffOriginal.addAll(delete.getLines());
                        targetLineNumber.set(delete.getPosition());
                        isPoint.set(true);
                    }
                    break;
                case EQUAL:
                    System.out.println("无变化");
                    break;
            }
        });
        if (!isPoint.get()){
            System.out.println("Find affected client code, but did not get adaptation");
            return codeDiff;
        }
        System.out.println("get an adaptation");

        List<String> resultDiffOriginal = getDiffWithContextFromLines(original,diffOriginal,sourceLineNumber,contextWidth);
        List<String> resultDiffPatched = getDiffWithContextFromLines(revised,diffPatched, targetLineNumber.get(),contextWidth);
//        List<String> resultDiffOriginal = getDiffWithContext(FilePath,diffOriginal,sourceLineNumber,contextWidth);
//        List<String> resultDiffPatched = getDiffWithContext(targetFilePath,diffPatched, targetLineNumber.get(),contextWidth);
        String affected = "affected/"+ FilePath.replace('/','.');
        String adaptation = "adaptation/"+ FilePath.replace('/','.');
        writeFile(affected,resultDiffOriginal);
        writeFile(adaptation,resultDiffPatched);
//        for(String i:resultDiffOriginal){
//            System.out.println(i);
//        }
//        System.out.println("***************************************");
//        for(String i:resultDiffPatched){
//            System.out.println(i);
//        }
        return codeDiff;
    }

    public static void writeFile(String path,List<String> content) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(path)));
        for(String i:content){
           bw.write(i+"\n");
           bw.flush();
        }
        bw.close();
    }

    public static List<String> getDiffWithContextFromLines(List<String> file,List<String> diff, int targetLineNumber, int contextWidth){
        List<String> result = new LinkedList<>();
        int diffLength = diff.size();
        int lineNumber = 0;
        for(String line:file){
            // before
            if (targetLineNumber-contextWidth <= lineNumber && lineNumber < targetLineNumber){
                result.add(line);
            }
            // now
            if (targetLineNumber <= lineNumber && lineNumber < targetLineNumber + diffLength){
                result.add(line);
            }
            // after
            if (targetLineNumber + diffLength <= lineNumber && lineNumber < targetLineNumber + diffLength + contextWidth){
                result.add(line);
            }
            lineNumber++;
        }
        return result;
    }

    public static List<String> getDiffWithContext(String filePath,List<String> diff, int targetLineNumber, int contextWidth) throws IOException {
        List<String> result = new LinkedList<>();
        int diffLength = diff.size();
        BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
        String line;
        int lineNumber = 0;
        while ((line=br.readLine())!=null) {
            // before
            if (targetLineNumber-contextWidth <= lineNumber && lineNumber < targetLineNumber){
                result.add(line);
            }
            // now
            if (targetLineNumber <= lineNumber && lineNumber < targetLineNumber + diffLength){
                result.add(line);
            }
            // after
            if (targetLineNumber + diffLength <= lineNumber && lineNumber < targetLineNumber + diffLength + contextWidth){
                result.add(line);
            }
            lineNumber++;
        }
        br.close();
        return result;
    }

    public static int getLineNumber(String filePath,int charPosition) throws IOException {
        int lineNumber = 0;
        int charNumber = 0;
        FileInputStream fis = new FileInputStream(new File(filePath));
        byte[] bys = new byte[10000];
        while (fis.read(bys, 0, bys.length)!=-1) {
            for(char c: new String(bys).toCharArray()){
                if(charNumber==charPosition) {return lineNumber;}
                if(c=='\n') {lineNumber++;}
                charNumber++;
            }
        }
        fis.close();
        return lineNumber;
    }
}
