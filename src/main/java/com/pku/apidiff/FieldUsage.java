package com.pku.apidiff;

public class FieldUsage {
    public String typeName;
    public String fieldName;
    public int position;
    public FieldUsage(String typeName, String fieldName, int position){this.typeName = typeName; this.fieldName = fieldName; this.position = position; }
}
