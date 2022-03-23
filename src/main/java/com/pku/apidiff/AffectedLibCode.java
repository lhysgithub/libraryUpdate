package com.pku.apidiff;

public class AffectedLibCode {
    public Signature signature;
    public String oldLibId;
    public String newLibId;
    AffectedLibCode(Signature signature,String oldLibId,String newLibId){this.signature=signature;this.oldLibId = oldLibId;this.newLibId = newLibId;}
}
