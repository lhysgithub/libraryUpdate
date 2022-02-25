package com.pku.libupgrade;

import java.util.Map;

public class Commit {
    String commit;
    Map<String, Map<String, String>> pomMap;
    public Commit(String commit_, Map<String, Map<String, String>> pomMap_){
        commit = commit_;
        pomMap = pomMap_;
    }
}
