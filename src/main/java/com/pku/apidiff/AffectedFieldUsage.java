package com.pku.apidiff;

import com.pku.libupgrade.DiffCommit;

public class AffectedFieldUsage {
    public FieldUsage fieldUsage;
    public DiffCommit diffCommit;
    AffectedFieldUsage(FieldUsage fieldUsage,DiffCommit diffCommit){this.fieldUsage=fieldUsage;this.diffCommit=diffCommit;}
}
