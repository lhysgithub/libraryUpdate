package com.pku.apidiff;

import com.pku.libupgrade.DiffCommit;

public class AffectedCode {
    public Signature signature;
    public DiffCommit diffCommit;
    AffectedCode(Signature signature,DiffCommit diffCommit){this.signature=signature;this.diffCommit = diffCommit;}
}
