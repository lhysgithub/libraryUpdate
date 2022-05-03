package com.pku.libupgrade.clientAnalysis;


import com.pku.libupgrade.Commit;
import com.pku.libupgrade.DiffCommit;
import com.pku.libupgrade.PomParser;
import com.pku.libupgrade.Utils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.pku.apidiff.APIDiff.apiDiff;
import static com.pku.libupgrade.PomParser.pomParse;
import static com.pku.libupgrade.Utils.getFileLines;

public class ClientAnalysis {
//    public static void main(String[] args) throws Exception {
//        pomParse();
//        apiDiff();
//        getAffectedCode("breakingChanges");
//    }
//    codenvy,
    public static String bigProjects = "nexus-public,nexus-public,openapi-generator,ORCID-Source,BroadleafCommerce,gulimall-learning,nifty-gui,tencentcloud-sdk-java,jena,k,ksql,goclipse,imagej2,kylin,jcodec,TrackRay,shopping-mmall,hbase,microservices-platform,jpmml-sklearn,ansj_seg,openpnp,dependency-track,jgraphx,AuthMeReloaded,nodebox,freedomotic,bdp-platform,druid,keycloak-quickstarts,WebCollector,helicalinsight,Cloud9,datagear,jdonframework,midpoint,liquibase,graphify,vert.x,skywalking,infinitest,opennms,resteasy,product-ei,dbeaver,dropwizard,ojAlgo,zxing,roboguice,zingg,libjitsi,Cynthia,quarkus,sia-task,thingsboard,zstack,UniversalMediaServer,Spring-Cloud-Platform,pentaho-platform,multibit,tlaplus,tutorials,kogito-runtimes,iri,ObjectLogger,hop,karaf,james-project,sqlite-jdbc,dss,captcha,cdhproject,SecurityShepherd,idecore,MMall,BenchmarkJava,WebLogic-Shiro-shell,mqcloud,basex,jenkins,checkstyle,CorfuDB,pwm,simplenlg,pulsar,TexasHoldemSolverJava,sql4es,opensearchserver,thucydides,SikuliX-2014,AudioBookConverter,searchcode-server,javacpp-presets,jigasi,wildfly,steady,wicket,soapui,rome,h2o-2,TwelveMonkeys,aircompressor,engine,archi,isis,cat,ios-driver,hops,Haafiz,simpleimage,itextpdf,presto,floodlight,javamelody,IPED,dataverse,dataease,primefaces,zeppelin,pentaho-kettle,cbioportal,tess4j,openaudible,hazelcast-jet,maven-framework-project,wro4j,hadoop-mini-clusters,data-algorithms-book,legacy-jclouds,easyexcel,pmd,datashare,evosuite,aerogear-unifiedpush-server,jave2,scouter,openmeetings,google-api-java-client-services,jblas,hyscale,infinispan,gravitee-api-management,Springboot-Notebook,soot,cogcomp-nlp,metron,EFAK,netty,droolsjbpm-integration,oshi,jstorm,mondrian,datawave,jMetal,t-vault,robovm,ldbz-shop,kylo,MediathekView,inception,rascal,atmosphere,picocli,OptimizedImageEnhance,burlap,ofdrw,gridgain-old,finmath-lib,mcg-helper,unidbg,kite,product-is,Java_NFe,zookeeper,book,framework,kkFileView,iotdb,innodb-java-reader,hetu-core,pdfbox,maven-plugins,studio3,jOOQ,fixflow,UML-Designer,UICrawler,Spark,WebGoat-Legacy,BiglyBT,plc4x,springcloud-2020,consulo,tetrad,jersey,sensei,javaCrawling,questdb,languagetool,hive,minicat,mogu_blog_v2,jmc,SproutLife,google-cloud-java,jackson-databind,neuralnetworks,gwt-bootstrap,librec,Elasticsearch-Hbase,OpenRefine,ShiroAttack2,nbscala,RedisClient,angel,OpenTripPlanner,OpenMetadata,cdk,hazelcast,libreplan,htm.java,OpenLegislation,kkFileViewOfficeEdit,webprotege,drill,ditto,robospice,jackson-core,hibernate-search,Pydev,repairnator,server,SquidLib,majiang_algorithm,XR3Player,ambari,jpress,Activiti,movsim,groovy-eclipse,incubator-linkis,esper,Ant-Media-Server,egeria,openhab1-addons,flink,helidon,html2file,aws-toolkit-eclipse,database,CERMINE,nuxeo,appfuse,smarthome,Payara,metersphere,Gaffer,xmall,hopsworks,azure-sdk-for-java,waffle,ranger,nifi,novel-plus,birt,hadoop,mesh,cello,neo4j,sonar-cxx,liugh-parent,JCloisterZone,ruoyi-vue-pro,mybatis-3,weblaf,opencast,mage,dataloader,Achilles,Chronicle-Queue,heideltime,nuls-v1,sonic-server-simple,voxelshop,galaxysql,Vault,jackrabbit,byte-buddy,jstarcraft-rns,matsim-libs,graylog2-server,wetech-admin,Minim,LIRE,SchemaCrawler,keycloak,phoenix,Bytecoder,spring-cloud-contract,Makelangelo-software,accumulo,helix,cxf,structr,itext7,superword,VnCoreNLP,hapi-fhir,morphia,activemq-artemis,drools,hera,hawtio,DependencyCheck,cloudsimplus,oltpbench,lwjglbook,backend,davmail,hazelcast-code-samples,product-apim,exist,lwjgl3,pallas,pinot,zeebe,Essentials,hudi,word,dew,MicroCommunity,athenz,ES-Fastloader,camel,AppiumTestDistribution,spring-cloud-shop,logging-log4j2,owlapi,kafka-streams-machine-learning-examples,bolo-solo,framework-learning,youlai-mall,manifold,Chronicle-Core,flexmark-java,CoreNLP,kubernetes-client,killbill,ovirt-engine,DataSphereStudio,dolphinscheduler,core-geonetwork,openwebflow,Discovery,jetty.project,iveely.search,tika,dcm4che,SuperMarket,solo,trino,SikuliX1,guava,ontop,anserini,cyberduck,Android-Cookbook-Examples,arx,docx4j,Web-Karma,AsciidocFX,cldr,DataCleaner,tern.java,itranswarp,jclouds,Universal-G-Code-Sender,alluxio,openimaj,s3s3mirror,jeecg,vespa,HiBench,miaosha,github-api,titan,xJavaFxTool,jackrabbit-oak,teiid,mp4parser,ongdb,sakai,spring-security-registration,binnavi,msgraph-sdk-java,knime-core,shardingsphere,orientdb,choco-solver,error-prone,spring-ide,erupt,gephi,mahout,archiva,OpenClinica,ignite,rexster,DWSurvey,cf-java-client,cloudstack,biojava,yago3,FXGL,WS-Attacker,org.openwms,htmlunit,geowave,amazon-kinesis-scaling-utils,gatk,blinkid-android,alipay-sdk-java-all,storm,shopizer,light-task-scheduler,struts,atlas,shop,openmrs-core,google-drive-ftp-adapter,deeplearning4j,lumify,dal,DBus,cdap,mall-learning,jflex,pinpoint,wecube-platform,opsu,tinkerpop,Kylin,xwiki-platform,Openfire,opengrok,sonar-java,zanata-platform,Shop-for-JavaWeb,openhab-addons,XLearning,openiot,camunda-bpm-platform,optaplanner,findbugs,airsonic,onedev,CQL,bytecode-viewer,teavm,tomee,systemds,TornadoVM,gisgraphy,flowable-engine,graphhopper";
//    public static String[] bigProjectsList;
    public static void main(String[] argss) throws Exception {
//        bigProjectsList = bigProjects.split(",");
        String projectPath = "../dataset/";
        String projectName = "plantuml";
        File commitDiffFile = new File("commitDiff.csv");

        File checkedClientFiles = new File("checkedClientFiles.txt");
        if (!checkedClientFiles.exists()){checkedClientFiles.createNewFile();}
        List<String> parsedBCFilesList = getFileLines(checkedClientFiles);
        BufferedWriter bw = new BufferedWriter(new FileWriter(checkedClientFiles,false));

        if (!commitDiffFile.exists()) {commitDiffFile.createNewFile();}
        //        String localSourceDir =  PomParser.DownloadMavenLib("org.apache.maven:maven-core:3.0.4");
        //        String localSourceDir =  PomParser.DownloadMavenLib("org.apache.maven:maven-core:3.1.0");
        //        System.out.println(localSourceDir);
//                DiffCommit.cleanCSV("commitDiff.csv","commitDiff1.csv");
        //        detectVersionChange(projectPath,projectName);
        //        String url = getGitUrl(projectName);
        //        MongoDBJDBC.findPopularLib();

        // 遍历所有repository
        int i =0 ;
        File file = new File(projectPath);
        File[] fs = file.listFiles();
        assert fs != null;
        int lengthProject = fs.length;
        for(File f:fs){					        //遍历File[]数组
            if(f.isDirectory())
//                if (i<3){
//                    i++;
//                    continue;
//                }
                i++;
                projectName = f.getName();
            logger.error("pomParse " + i + "/" + lengthProject+f.getName());
            if (bigProjects.contains(projectName)) {continue;} // 跳过大项目
            if(Utils.isProjectExist(projectName,"commitDiff.csv")) {continue;}
            if (parsedBCFilesList.contains(projectName)){bw.write(projectName+"\n");bw.flush();continue;}
            detectVersionChange(projectPath,projectName);
        }
        bw.close();
        //        Utils.findPopularLibFromCsv("commitDiff.csv","popularLib.txt");
        //        MongoDBJDBC.findPopularLib();
    }

    private static class RevFilterCommitValid extends RevFilter {

        @Override
        public final boolean include(final RevWalk walker, final RevCommit c) {

            Long diffTimestamp = 0L;
            diffTimestamp = this.calcDiffTimeCommit(c);

            if (c.getParentCount() > 1) {//merge
//                logger.debug("Merge of the branches deleted. [commitId=" + c.getId().getName() + "]");
                return false;
            }
            //TODO: create other filter to date.
            return true;
        }

        private Long calcDiffTimeCommit(final RevCommit c) {
            Long timestampNow = Calendar.getInstance().getTimeInMillis();
            Long timestampCommit = c.getAuthorIdent().getWhen().getTime();
            Calendar calendarCommit = Calendar.getInstance();
            calendarCommit.setTime(new Date(timestampCommit));
            return Math.abs(timestampCommit - timestampNow);
        }

        private String getDateCommitFormat(final RevCommit c) {
            Long timestampCommit = c.getAuthorIdent().getWhen().getTime();
            Calendar calendarCommit = Calendar.getInstance();
            calendarCommit.setTime(new Date(timestampCommit));
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
            return format.format(timestampCommit);
        }

        @Override
        public final RevFilter clone() {
            return this;
        }

        @Override
        public final boolean requiresCommitBody() {
            return false;
        }

        @Override
        public String toString() {
            return "RegularCommitsFilter";
        }
    }

    public static Logger logger = LoggerFactory.getLogger(ClientAnalysis.class);
    public List<String> recordPomFilePath = new ArrayList<>();

    public void getPomPath(File f) {
        //System.out.println("1:"+f.getAbsolutePath());
        if (f.isDirectory()) {
            for (File ftemp : f.listFiles()) {
                getPomPath(ftemp);
            }
        } else {
            if (f.getName().endsWith("pom.xml")) {
                recordPomFilePath.add(f.getAbsolutePath());
            }
        }
    }

    public Repository openRepository(String path, String projectName) throws Exception {
        File folder = new File(Utils.getPathProject(path, projectName));
        Repository repository = null;

//        this.logger.debug(projectName + " exists. Reading properties ... (wait)");
        RepositoryBuilder builder = new RepositoryBuilder();
        repository = builder
                .setGitDir(new File(folder, ".git"))
                .readEnvironment()
                .findGitDir()
                .build();
        return repository;
    }

    public RevWalk createAllRevsWalk(Repository repository, String branch) throws Exception {
        List<ObjectId> currentRemoteRefs = new ArrayList<ObjectId>();
        for (Ref ref : repository.getAllRefs().values()) {
            String refName = ref.getName();
            if (refName.startsWith("refs/remotes/origin/")) {
                if (branch == null || refName.endsWith("/" + branch)) {
                    currentRemoteRefs.add(ref.getObjectId());
                }
            }
        }

        RevWalk walk = new RevWalk(repository);
        for (ObjectId newRef : currentRemoteRefs) {
            walk.markStart(walk.parseCommit(newRef));
        }
        walk.setRevFilter(new RevFilterCommitValid());
        return walk;
    }

    public List<String> getAllCommitId(Repository repository, String branch) throws Exception {

        List<String> returnCommitList = new ArrayList<>();
        RevWalk revWalk = createAllRevsWalk(repository, branch);
        Iterator<RevCommit> i = revWalk.iterator();
        while (i.hasNext()) {
            RevCommit currentCommit = i.next();
            if (currentCommit.getParentCount() != 0) {
                String commitId = currentCommit.getParent(0).getName();
                returnCommitList.add(commitId);
            }

        }
        return returnCommitList;
    }

    public void checkout(Repository repository, String commitId) throws Exception {
//        this.logger.debug("Checking out {} {} ...", repository.getDirectory().getParent().toString(), commitId);
        try (Git git = new Git(repository)) {
            CleanCommand clean = git.clean().setForce(true);
            clean.call();
            ResetCommand reset = git.reset().setMode(ResetCommand.ResetType.HARD);
            reset.call();
            CheckoutCommand checkout = git.checkout().setName(commitId).setForce(true);
            checkout.call();
        }
    }

    public static void detectVersionChange(String projectPath, String projectName) throws Exception {
        String indexPath =projectPath+"/"+projectName+"/.git/index.lock";
        File file1 = new File(indexPath);
        if (file1.exists()){
            file1.delete();
        }

        ClientAnalysis clientAnalysis = new ClientAnalysis();
        Repository repository = clientAnalysis.openRepository(projectPath, projectName);
//        List<String> commitIds = clientAnalysis.getAllCommitId(repository, "master"); // todo: if not have master branch
        Git git = new Git(repository);
        ListTagCommand listTagCommand=git.tagList();
        List<Ref> refs=listTagCommand.call();
        List<String> commitIds=new ArrayList<>();
        List<String> tagList=new ArrayList<>();
        for(Ref ref:refs){
//            System.out.println(ref.getName().replace("refs/tags/",""));
//            System.out.println(ref.getObjectId().getName());
            tagList.add(ref.getName().replace("refs/tags/",""));
            commitIds.add(ref.getObjectId().getName());
        }
//        System.out.println(commitIds.size());
//        System.out.println(tagList.size());
//        System.out.println("repository.getBranch:"+repository.getBranch());
        logger.error("projectName: "+ projectName + " commits' size: "+commitIds.size()+" tag's size: "+tagList.size());
        List<Commit> versionMap = new ArrayList<>();
        // commit pomName libName versionName
        for (String commitId : commitIds) {
            logger.error(commitId);
            try {
                clientAnalysis.checkout(repository, commitId);
            }
            catch (Exception e){
                ClientAnalysis.logger.error(e.toString());
                break;
            }
            clientAnalysis.recordPomFilePath = new ArrayList<>();
            clientAnalysis.getPomPath(new File(projectPath + "/" + projectName));
            //+"/" +"pom.xml"
            Map<String, Map<String, String>> totPomInfoMap = new HashMap<>();
            for (String pomPath : clientAnalysis.recordPomFilePath) {
                Map<String, String> pomInfoMap = new HashMap<>();
                try {
                    pomInfoMap = Utils.readOutLibraries(pomPath);
                }
                catch (Exception e){
//                    ClientAnalysis.logger.error("pom parse error occurred but program continues");
//                    e.printStackTrace();
                    ;
                }
                totPomInfoMap.put(pomPath.replace(projectPath, ""), pomInfoMap); // do not anonymize username
//                totPomInfoMap.put(pomPath.split(projectPath)[1], pomInfoMap); // this is the true path, but pomParse
//                has running
            }
            System.out.println(totPomInfoMap);
            versionMap.add(new Commit(commitId,totPomInfoMap));
        }
        List<DiffCommit> diffList = Utils.getDiffList(versionMap, projectName);
        logger.error("diffList size: "+diffList.size());
        for (DiffCommit it : diffList){
            it.print();
            it.saveCSV("commitDiff.csv");
//            MongoDBJDBC.insertCommitDiff(it);
        }
    }



}
