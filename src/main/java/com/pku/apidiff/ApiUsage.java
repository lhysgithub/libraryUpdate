package com.pku.apidiff;

import java.util.List;
import java.util.Map;

public class ApiUsage {
    Map<String, List<Caller>> allCallersOfProject;
    Map<String,List<TypeUsage>> allTypeUsagesOfProject;
    Map<String,List<FieldUsage>> allFieldUsagesOfProject;
    ApiUsage(Map<String, List<Caller>> allCallersOfProject,
    Map<String,List<TypeUsage>> allTypeUsagesOfProject,
    Map<String,List<FieldUsage>> allFieldUsagesOfProject){
        this.allCallersOfProject = allCallersOfProject;
        this.allTypeUsagesOfProject = allTypeUsagesOfProject;
        this.allFieldUsagesOfProject = allFieldUsagesOfProject;
    }
}
