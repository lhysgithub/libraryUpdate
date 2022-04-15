package com.pku.apidiff;

import com.pku.libupgrade.DiffCommit;

public class AffectedTypeUsage {
    public TypeUsage typeUsage;
    public DiffCommit diffCommit;
    AffectedTypeUsage(TypeUsage typeUsage, DiffCommit diffCommit){this.typeUsage=typeUsage;this.diffCommit = diffCommit;}
}
