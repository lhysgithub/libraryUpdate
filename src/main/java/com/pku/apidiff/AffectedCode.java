package com.pku.apidiff;

import com.pku.libupgrade.DiffCommit;

public class AffectedCode {
    public String name;
    public int position;
    public DiffCommit diffCommit;
    public AffectedCode(String name,int position,DiffCommit diffCommit){this.name=name;this.position=position;this.diffCommit=diffCommit;}
}
