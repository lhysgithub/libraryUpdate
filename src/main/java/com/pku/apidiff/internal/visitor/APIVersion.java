package com.pku.apidiff.internal.visitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pku.apidiff.Caller;
import com.pku.apidiff.FieldUsage;
import com.pku.apidiff.TypeUsage;
import com.pku.apidiff.enums.Classifier;
import com.pku.apidiff.internal.analysis.comparator.ComparatorMethod;
import com.pku.apidiff.internal.service.git.GitFile;
import com.pku.apidiff.internal.util.UtilTools;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pku.apidiff.MethodInvocationVisitor;

import java.io.*;
import java.util.*;

public class APIVersion {
	
	private ArrayList<TypeDeclaration> apiAccessibleTypes = new ArrayList<TypeDeclaration>();
	private ArrayList<TypeDeclaration> apiNonAccessibleTypes = new ArrayList<TypeDeclaration>();
	private ArrayList<EnumDeclaration> apiAccessibleEnums = new ArrayList<EnumDeclaration>();
	private ArrayList<EnumDeclaration> apiNonAccessibleEnums = new ArrayList<EnumDeclaration>();
	private Map<ChangeType, List<GitFile>> mapModifications = new HashMap<ChangeType, List<GitFile>>();
	public Map<String, List<Caller>>  apiCallersMap = new HashMap<>();
	public Map<String, List<TypeUsage>>  apiTypeUsagesMap = new HashMap<>();
	public Map<String, List<FieldUsage>>  apiFieldUsagesMap = new HashMap<>();
	private List<String> listFilesMofify = new ArrayList<String>();
	private Classifier classifierAPI;
	private String nameProject;
	
	private Logger logger = LoggerFactory.getLogger(APIVersion.class);
	
	private String path;

	public APIVersion(final String path, final File file, final Map<ChangeType, List<GitFile>> mapModifications, Classifier classifierAPI) {
		System.out.println("path:"+path);
		System.out.println(file.getAbsolutePath());
		try {
			this.classifierAPI = classifierAPI;
			this.mapModifications = mapModifications;
			this.path = path;
			this.nameProject = file.getAbsolutePath().replace(this.path + File.separator, "");
			System.out.println(this.nameProject);
	    	String prefix = file.getAbsolutePath() + File.separator;
	    	System.out.println(prefix);
			for(ChangeType changeType : this.mapModifications.keySet()){
				for(GitFile gitFile: mapModifications.get(changeType)){
					if(File.separator.equals("\\")){
						if(gitFile.getPathOld()!= null){
							this.listFilesMofify.add(prefix + gitFile.getPathOld().replace("/","\\"));
						}
						if(gitFile.getPathNew() != null && !gitFile.getPathNew().equals(gitFile.getPathOld())){
							this.listFilesMofify.add(prefix + gitFile.getPathNew().replace("/","\\"));
						}
					}
					else{
						if(gitFile.getPathOld()!= null){
							this.listFilesMofify.add(prefix + gitFile.getPathOld());
						}
						if(gitFile.getPathNew() != null && !gitFile.getPathNew().equals(gitFile.getPathOld())){
							this.listFilesMofify.add(prefix + gitFile.getPathNew());
						}
					}
				}
			}
			System.out.println("aaa:");
			System.out.println(this.listFilesMofify);
			this.parseFilesInDir(file, false);
		} catch (IOException e) {
			this.logger.error("Erro ao criar APIVersion", e);
		}
	}
	
	public APIVersion(final String nameProject, Classifier classifierAPI) {
		try {
			this.nameProject = nameProject;
			this.classifierAPI = classifierAPI;
			File path = new File(this.path + File.separator + this.nameProject);
			this.parseFilesInDir(path, true);
		} catch (IOException e) {
			this.logger.error("Erro ao criar APIVersion", e);
		}
		
	}

	public APIVersion(final String nameProject, String dirPath, Classifier classifierAPI) {
		try {
			this.nameProject = nameProject;
			this.classifierAPI = classifierAPI;
			File path = new File(dirPath);
			this.parseFilesInDir(path, true);
			saveCallers(nameProject);
			saveTypeUsages(nameProject);
			saveFieldUsages(nameProject);
//			setCallersFromJson(nameProject);
		} catch (IOException e) {
			e.printStackTrace();
			this.logger.error("Erro ao criar APIVersion", e);
		}
	}

	public void parseFilesInDir(File file, final Boolean ignoreTreeDiff) throws IOException {
		if (file.isFile()) {
			//String simpleNameFile = UtilTools.getSimpleNameFileWithouPackageWithNameLibrary(this.path, file.getAbsolutePath(), this.nameProject);
			if (UtilTools.isJavaFile(file.getName()) && this.isFileModification(file, ignoreTreeDiff) ) {
				this.parse(UtilTools.readFileToString(file.getAbsolutePath()), file, ignoreTreeDiff);		
			}
		} else {
			if(file.listFiles() != null){
				for (File f : file.listFiles()) {
					try {
						this.parseFilesInDir(f, ignoreTreeDiff);
					}
					catch (Exception e){
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void parse(String str, File source, final Boolean ignoreTreeDiff) throws IOException {
		
		if(this.mapModifications.size() > 0 && !this.isFileModification(source,ignoreTreeDiff)){
			return;
		}
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(str.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		String[] classpath = System.getProperty("java.class.path").split(";");
		String[] sources = { source.getParentFile().getAbsolutePath() };

		Hashtable<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
		parser.setUnitName(source.getAbsolutePath());

		parser.setCompilerOptions(options);
//		parser.setEnvironment(null, sources, new String[] { "UTF-8" },	true);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);

//		parser.setEnvironment(classpath, sources, new String[] { "UTF-8" },	true);
//		CompilationUnit compilationUnit = null;
		try {
			parser.setEnvironment(classpath, sources, new String[] { "UTF-8" },	true);
//			parser.setEnvironment(null, null, null,	true);
			CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
			TypeDeclarationVisitor visitorType = new TypeDeclarationVisitor();
			EnumDeclarationVisitor visitorEnum = new EnumDeclarationVisitor();
			MethodInvocationVisitor visitorMethodCall = new MethodInvocationVisitor();

			
			compilationUnit.accept(visitorType);
			compilationUnit.accept(visitorEnum);
			compilationUnit.accept(visitorMethodCall);
			
			this.configureAcessiblesAndNonAccessibleTypes(visitorType);
			this.configureAcessiblesAndNonAccessibleEnums(visitorEnum);
			this.setCallers(visitorMethodCall.callerSignatures,source.getAbsolutePath());
			this.setTypeUsages(visitorMethodCall.apiTypeUsages,source.getAbsolutePath());
			this.setFieldUsages(visitorMethodCall.apiFieldUsages,source.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
			this.logger.error("Erro ao criar AST sem source", e);
		}
	}

	public void setCallers(List<Caller> signatures, String filePath){
		this.apiCallersMap.put(filePath,signatures);}

	public void setTypeUsages(List<TypeUsage> typeUsages, String filePath){
		this.apiTypeUsagesMap.put(filePath,typeUsages);}

	public void setFieldUsages(List<FieldUsage> fieldUsages, String filePath){
		this.apiFieldUsagesMap.put(filePath,fieldUsages);}

	public void saveCallers(String libName) throws IOException {
		// libCallers/libName(g:a:v)
		if(this.apiCallersMap.size()==0){return;}
		String jsonPath = "libCallers/"+ libName+".json";
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(jsonPath)));
		ObjectMapper mapper = new ObjectMapper();
		bw.write(mapper.writeValueAsString(this.apiCallersMap));
		bw.close();
	}

	public void saveTypeUsages(String libName) throws IOException {
		// libCallers/libName(g:a:v)
		if(this.apiTypeUsagesMap.size()==0){return;}
		String jsonPath = "libTypeUsages/"+ libName+".json";
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(jsonPath)));
		ObjectMapper mapper = new ObjectMapper();
		bw.write(mapper.writeValueAsString(this.apiTypeUsagesMap));
		bw.close();
	}

	public void saveFieldUsages(String libName) throws IOException {
		// libCallers/libName(g:a:v)
		if(this.apiFieldUsagesMap.size()==0){return;}
		String jsonPath = "libFieldUsages/"+ libName+".json";
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(jsonPath)));
		ObjectMapper mapper = new ObjectMapper();
		bw.write(mapper.writeValueAsString(this.apiFieldUsagesMap));
		bw.close();
	}
	
	private void configureAcessiblesAndNonAccessibleTypes(TypeDeclarationVisitor visitorType){
		this.apiNonAccessibleTypes.addAll(visitorType.getNonAcessibleTypes());
		this.apiAccessibleTypes.addAll(visitorType.getAcessibleTypes());
	}
	
	private void configureAcessiblesAndNonAccessibleEnums(EnumDeclarationVisitor visitorType){
		this.apiNonAccessibleEnums.addAll(visitorType.getNonAcessibleEnums());
		this.apiAccessibleEnums.addAll(visitorType.getAcessibleEnums());
	}
	
	private Boolean isFileModification(final File source, final Boolean ignoreTreeDiff){
		return (ignoreTreeDiff || this.listFilesMofify.contains(source.getAbsolutePath()))? true: false;
	}

	public ArrayList<EnumDeclaration> getApiAccessibleEnums() {
		return apiAccessibleEnums;
	}

	public ArrayList<EnumDeclaration> getApiNonAccessibleEnums() {
		return apiNonAccessibleEnums;
	}

	public ArrayList<TypeDeclaration> getApiAcessibleTypes(){
		return this.apiAccessibleTypes;
	}
	
	public ArrayList<TypeDeclaration> getApiNonAcessibleTypes(){
		return this.apiNonAccessibleTypes;
	}

	public ArrayList<AbstractTypeDeclaration> getTypesPublicAndProtected() {
		ArrayList<AbstractTypeDeclaration> list = new ArrayList<AbstractTypeDeclaration>();
		list.addAll(this.getApiAcessibleTypes());
		list.addAll(this.getApiAccessibleEnums());
		return list;
	}
	

	public ArrayList<AbstractTypeDeclaration> getTypesPrivateAndDefault() {
		ArrayList<AbstractTypeDeclaration> list = new ArrayList<AbstractTypeDeclaration>();
		list.addAll(this.getApiNonAcessibleTypes());
		list.addAll(this.getApiNonAccessibleEnums());
		return list;
	}

	public EnumDeclaration getVersionNonAccessibleEnum(EnumDeclaration enumVersrionReference){
		for (EnumDeclaration enumDeclarion : this.apiNonAccessibleEnums) {
			if(enumDeclarion.resolveBinding() != null && enumVersrionReference.resolveBinding() != null){
				if(enumDeclarion.resolveBinding().getQualifiedName().equals(enumVersrionReference.resolveBinding().getQualifiedName())){
					return enumDeclarion;
				}
			}
		}

		return null;
	}

	public EnumDeclaration getVersionAccessibleEnum(EnumDeclaration enumVersrionReference){
		for (EnumDeclaration enumDeclarion : this.apiAccessibleEnums) {
			if(enumDeclarion.resolveBinding() != null && enumVersrionReference.resolveBinding() != null){
				if(enumDeclarion.resolveBinding().getQualifiedName().equals(enumVersrionReference.resolveBinding().getQualifiedName())){
					return enumDeclarion;
				}
			}
		}

		return null;
	}

	public AbstractTypeDeclaration getVersionNonAccessibleType(AbstractTypeDeclaration typeVersrionReference){
		for (AbstractTypeDeclaration typeDeclarion : this.getTypesPrivateAndDefault()) {
			if(typeDeclarion.resolveBinding() != null && typeVersrionReference.resolveBinding() != null){
				if(typeDeclarion.resolveBinding().getQualifiedName().equals(typeVersrionReference.resolveBinding().getQualifiedName())){
					return typeDeclarion;
				}
			}
		}

		return null;
	}
	

	public AbstractTypeDeclaration getVersionAccessibleType(AbstractTypeDeclaration typeVersrionReference){
		for (AbstractTypeDeclaration typeDeclarion : this.getTypesPublicAndProtected()) {
			if(typeDeclarion.resolveBinding() != null && typeVersrionReference.resolveBinding() != null){
				if(typeDeclarion.resolveBinding().getQualifiedName().equals(typeVersrionReference.resolveBinding().getQualifiedName())){
					return typeDeclarion;
				}
			}
		}
		return null;
	}
	
	public boolean containsType(TypeDeclaration type){
		return this.containsAccessibleType(type) || this.containsNonAccessibleType(type);
	}
	
	public boolean containsAccessibleType(AbstractTypeDeclaration type){
		return this.getVersionAccessibleType(type) != null;
	}
	
	public boolean containsNonAccessibleType(AbstractTypeDeclaration type){
		return this.getVersionNonAccessibleType(type) != null;
	}
	
	public boolean containsAccessibleEnum(EnumDeclaration type){
		return this.getVersionAccessibleEnum(type) != null;
	}

	public boolean containsNonAccessibleEnum(EnumDeclaration type){
		return this.getVersionNonAccessibleEnum(type) != null;
	}

	public FieldDeclaration getVersionField(FieldDeclaration field, TypeDeclaration type){
		for (TypeDeclaration versionType : this.apiAccessibleTypes) {
			if(versionType.getName().toString().equals(type.getName().toString())){
				for (FieldDeclaration versionField : versionType.getFields()) {
					String name1 = UtilTools.getFieldName(versionField);
					String name2  = UtilTools.getFieldName(field);
					if(name1 != null && name2 != null && name1.equals(name2)){
						return versionField;
					}
				}
			}
		}
		return null;
	}

	public ArrayList<MethodDeclaration> getAllEqualMethodsByName(MethodDeclaration method, TypeDeclaration type) {
		ArrayList<MethodDeclaration> result = new ArrayList<MethodDeclaration>();
		for (TypeDeclaration versionType : this.apiAccessibleTypes) {
			if(versionType.getName().toString().equals(type.getName().toString())){
				for(MethodDeclaration versionMethod : versionType.getMethods()){
					if(versionMethod.getName().toString().equals(method.getName().toString()))
						result.add(versionMethod);
				}
			}
		}
		return result;
	}
	
	public MethodDeclaration findMethodByNameAndParametersAndReturn(MethodDeclaration method, TypeDeclaration type){
		MethodDeclaration methodVersionOld = null;
		for (TypeDeclaration versionType : this.apiAccessibleTypes) {
			if(versionType.getName().toString().equals(type.getName().toString())){
				for(MethodDeclaration versionMethod : versionType.getMethods()){
					if(!ComparatorMethod.isDiffMethodByNameAndParametersAndReturn(versionMethod, method)){
						methodVersionOld =  versionMethod;
					}
				}
			}
		}
		return methodVersionOld;
	}
	
	private MethodDeclaration findMethodByNameAndReturn(MethodDeclaration method, TypeDeclaration type){
		MethodDeclaration methodVersionOld = null;
		for (TypeDeclaration versionType : this.apiAccessibleTypes) {
			if(versionType.getName().toString().equals(type.getName().toString())){
				for(MethodDeclaration versionMethod : versionType.getMethods()){
					if(!ComparatorMethod.isDiffMethodByNameAndReturn(versionMethod, method)){
						methodVersionOld =  versionMethod;
					}
				}
			}
		}
		return methodVersionOld;
	}
	
	public MethodDeclaration findMethodByNameAndParameters(MethodDeclaration method, TypeDeclaration type){
		MethodDeclaration methodVersionOld = null;
		for (TypeDeclaration versionType : this.apiAccessibleTypes) {
			if(versionType.getName().toString().equals(type.getName().toString())){
				for(MethodDeclaration versionMethod : versionType.getMethods()){
					if(!ComparatorMethod.isDiffMethodByNameAndParameters(versionMethod, method)){
						methodVersionOld =  versionMethod;
//						System.out.println("version1");// todo fix breaking change bugs
					}
				}
			}
		}
		return methodVersionOld;
	}

	public MethodDeclaration getEqualVersionMethod(MethodDeclaration method, TypeDeclaration type){
		for(MethodDeclaration methodInThisVersion : this.getAllEqualMethodsByName(method, type)){
			if(UtilTools.isEqualMethod(method, methodInThisVersion)){
				return methodInThisVersion;
			}
		}
		return null;
	}
	
	public EnumConstantDeclaration getEqualVersionConstant(EnumConstantDeclaration constant, EnumDeclaration enumReference) {
		EnumDeclaration thisVersionEnum = this.getVersionAccessibleEnum(enumReference);
		for(Object thisVersionConstant : thisVersionEnum.enumConstants()){
			if(((EnumConstantDeclaration)thisVersionConstant).getName().toString().equals(constant.getName().toString()))
				return ((EnumConstantDeclaration)thisVersionConstant);
		}

		return null;
	}
	
	public List<AbstractTypeDeclaration> getAllTypes(){
		List<AbstractTypeDeclaration> listTypesVersion = new ArrayList<AbstractTypeDeclaration>();
		listTypesVersion.addAll(this.getTypesPublicAndProtected());
		listTypesVersion.addAll(this.getTypesPrivateAndDefault());
		return listTypesVersion;
	}
	
}
