package com.pku.apidiff;

import com.pku.libupgrade.DiffCommit;

public class AffectedSignature {
    public Caller signature;
    public DiffCommit diffCommit;
    AffectedSignature(Caller signature, DiffCommit diffCommit){this.signature=signature;this.diffCommit = diffCommit;}
}
