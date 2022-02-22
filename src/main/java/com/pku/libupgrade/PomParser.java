package com.pku.libupgrade;


import com.google.common.collect.ImmutableSet;
import com.pku.libupgrade.internal.ConsoleRepositoryListener;
import com.pku.libupgrade.internal.ConsoleTransferListener;
import com.pku.libupgrade.internal.Slf4jLoggerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.*;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.repository.internal.MavenServiceLocator;
import org.codehaus.plexus.*;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.connector.async.AsyncRepositoryConnectorFactory;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManager;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.filter.DependencyFilterUtils;

import java.io.File;
import java.util.*;

public class PomParser {
    public static final String USER_LOCAL_REPO = System.getProperty("user.home") + "/.m2/repository";
    public static final String MAVEN_CENTRAL_URI = "https://repo1.maven.org/maven2/";
    public static final Set<String> DEPRECATED_MAVEN_CENTRAL_URIS = ImmutableSet.<String>builder()
            .add("http://repo1.maven.org/maven2")
            .add("http://repo1.maven.org/maven2/")
            .add("http://repo.maven.apache.org/maven2")
            .add("http://repo.maven.apache.org/maven2/")
            .build();

    private final RepositorySystem repositorySystem;
    private final MavenRepositorySystemSession repositorySystemSession;
    private final List<RemoteRepository> repositories;

    public PomParser(String localRepositoryDir, String... remoteRepositoryUris)
    {
        this(localRepositoryDir, Arrays.asList(remoteRepositoryUris));
    }

    public PomParser(String localRepositoryDir, List<String> remoteRepositoryUris)
    {
        MavenServiceLocator locator = new MavenServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.addService(RepositoryConnectorFactory.class, AsyncRepositoryConnectorFactory.class);
        repositorySystem = locator.getService(RepositorySystem.class);

        repositorySystemSession = new MavenRepositorySystemSession();

        LocalRepositoryManager localRepositoryManager = new SimpleLocalRepositoryManager(localRepositoryDir);
        repositorySystemSession.setLocalRepositoryManager(localRepositoryManager);

        repositorySystemSession.setTransferListener(new ConsoleTransferListener());
        repositorySystemSession.setRepositoryListener(new ConsoleRepositoryListener());

        List<RemoteRepository> repositories = new ArrayList<>(remoteRepositoryUris.size());
        int index = 0;
        for (String repositoryUri : remoteRepositoryUris) {
            repositories.add(new RemoteRepository("repo-" + index++, "default", repositoryUri));
        }
        this.repositories = Collections.unmodifiableList(repositories);
    }

    public List<Artifact> resolveArtifacts(Artifact... sourceArtifacts)
    {
        return resolveArtifacts(Arrays.asList(sourceArtifacts));
    }

    public List<Artifact> resolveArtifacts(Iterable<? extends Artifact> sourceArtifacts)
    {
        CollectRequest collectRequest = new CollectRequest();
        for (Artifact sourceArtifact : sourceArtifacts) {
            collectRequest.addDependency(new Dependency(sourceArtifact, JavaScopes.RUNTIME));
        }
        for (RemoteRepository repository : repositories) {
            // Hack: avoid using deprecated Maven Central URLs
            if (DEPRECATED_MAVEN_CENTRAL_URIS.contains(repository.getUrl())) {
                repository = new RemoteRepository(repository.getId(), repository.getContentType(), MAVEN_CENTRAL_URI);
            }
            collectRequest.addRepository(repository);
        }

        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME));

        return resolveArtifacts(dependencyRequest);
    }

    private List<Artifact> resolveArtifacts(DependencyRequest dependencyRequest)
    {
        DependencyResult dependencyResult;
        try {
            dependencyResult = repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest);
        }
        catch (DependencyResolutionException e) {
            dependencyResult = e.getResult();
        }
        List<ArtifactResult> artifactResults = dependencyResult.getArtifactResults();
        List<Artifact> artifacts = new ArrayList<>(artifactResults.size());
        for (ArtifactResult artifactResult : artifactResults) {
            if (artifactResult.isMissing()) {
                artifacts.add(artifactResult.getRequest().getArtifact());
            }
            else {
                artifacts.add(artifactResult.getArtifact());
            }
        }

        return Collections.unmodifiableList(artifacts);
    }

    public Map<String, String> readOutLibraries2(String pom){
        File pomFile = new File(pom);
        MavenProject pom_ = getMavenProject(pomFile);
        Map<String, String> returnMap = new HashMap<>();
        for (org.apache.maven.model.Dependency dependency : pom_.getDependencies()) {
            String groupId = dependency.getGroupId();
            String artifactId = dependency.getArtifactId();
            String version = dependency.getVersion();
            returnMap.put(groupId + "\t" + artifactId, version);
        }
        return returnMap;
    }

    private MavenProject getMavenProject(File pomFile)
    {
        try {
            PlexusContainer container = container();
            org.apache.maven.repository.RepositorySystem lrs = container.lookup(org.apache.maven.repository.RepositorySystem.class);
            ProjectBuilder projectBuilder = container.lookup(ProjectBuilder.class);
            ProjectBuildingRequest request = new DefaultProjectBuildingRequest();
            request.setSystemProperties(requiredSystemProperties());
            request.setRepositorySession(repositorySystemSession);
            request.setProcessPlugins(false);
            request.setLocalRepository(lrs.createDefaultLocalRepository());
            request.setRemoteRepositories(Arrays.asList(new ArtifactRepository[] {lrs.createDefaultRemoteRepository()}.clone()));
            ProjectBuildingResult result = projectBuilder.build(pomFile, request);
            return result.getProject();
        }
        catch (Exception e) {
            throw new RuntimeException("Error loading pom: " + pomFile.getAbsolutePath(), e);
        }
    }

    private static PlexusContainer container()
    {
        try {
            ClassWorld classWorld = new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());

            ContainerConfiguration cc = new DefaultContainerConfiguration()
                    .setClassWorld(classWorld)
                    .setRealm(null)
                    .setName("maven");

            DefaultPlexusContainer container = new DefaultPlexusContainer(cc);

            // NOTE: To avoid inconsistencies, we'll use the Thread context class loader exclusively for lookups
            container.setLookupRealm(null);

            container.setLoggerManager(new Slf4jLoggerManager());
            container.getLoggerManager().setThresholds(Logger.LEVEL_INFO);

            return container;
        }
        catch (PlexusContainerException e) {
            throw new RuntimeException("Error loading Maven system", e);
        }
    }

    private Properties requiredSystemProperties()
    {
        Properties properties = new Properties();
        properties.setProperty("java.version", System.getProperty("java.version"));
        return properties;
    }
}
