package com.pku.apidiff;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CodeDiff {
    public static void main(String[] args) throws IOException, DiffException {
        getCodeDiff("BuilderCommon1.txt","BuilderCommon2.txt",5,3882);
    }

    public static List<String> getCodeDiff(String originFilePath, String targetFilePath, int contextWidth, int sourceCharNumber) throws IOException, DiffException {
        int sourceLineNumber = 92;
//        sourceLineNumber = getLineNumber(originFilePath,sourceCharNumber);
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
        FileInputStream fis = new FileInputStream(new File(filePath));
        byte[] bys = new byte[100];
        while (fis.read(bys, 0, bys.length)!=-1) {
            for(char c: new String(bys).toCharArray()){
                if(c=='\n') {lineNumber++;}
            }
        }
        fis.close();
        return lineNumber;
    }
}
