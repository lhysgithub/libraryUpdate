package com.pku.libupgrade;

import java.io.IOException;

public class CleanDiffCommit {
    public static void main(String[] args) throws IOException {
        DiffCommit.cleanCSV("commitDiff.csv","commitDiff1.csv");
    }
}
