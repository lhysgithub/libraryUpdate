package com.pku.apidiff;

import java.util.List;

public class Signature {
    public String signature;
    public int position;
    public Signature(String signature, int position){this.signature = signature; this.position = position;}
    public static Signature getSignatureFromStringList(List<String> signature, int position){
        // add caller
        StringBuilder str = new StringBuilder();
        int j =0;
        for (String i:signature) {
            if (j==0) str.append(i);
            else str.append(" ").append(i);
            j++;
        }
        return new Signature(str.toString(),position);
    }
}
