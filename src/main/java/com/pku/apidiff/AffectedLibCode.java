package com.pku.apidiff;

public class AffectedLibCode {
    public Caller signature;
    public String oldLibId;
    public String newLibId;
    AffectedLibCode(Caller signature, String oldLibId, String newLibId){this.signature=signature;this.oldLibId = oldLibId;this.newLibId = newLibId;}
}
