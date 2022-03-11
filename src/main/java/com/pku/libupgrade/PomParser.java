package com.pku.libupgrade;


import com.google.common.collect.ImmutableSet;
import com.pku.libupgrade.internal.ConsoleRepositoryListener;
import com.pku.libupgrade.internal.ConsoleTransferListener;
import com.pku.libupgrade.internal.Slf4jLoggerManager;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.*;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.repository.internal.MavenServiceLocator;
import org.codehaus.plexus.*;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
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
    public static final String MAVEN_CENTRAL_URI = "https://repo1.maven.org/maven2/,https://repo.maven.apache.org/maven2/";
    public static final Set<String> DEPRECATED_MAVEN_CENTRAL_URIS = ImmutableSet.<String>builder()
            .add("https://repo1.maven.org/maven2")
            .add("https://repo1.maven.org/maven2/")
            .add("https://repo.maven.apache.org/maven2")
            .add("https://repo.maven.apache.org/maven2/")
            .build();

    private final MavenRepositorySystemSession repositorySystemSession;

    public PomParser(String localRepositoryDir, List<String> remoteRepositoryUris)
    {
        MavenServiceLocator locator = new MavenServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.addService(RepositoryConnectorFactory.class, AsyncRepositoryConnectorFactory.class);

        repositorySystemSession = new MavenRepositorySystemSession();

        LocalRepositoryManager localRepositoryManager = new SimpleLocalRepositoryManager(localRepositoryDir);
        repositorySystemSession.setLocalRepositoryManager(localRepositoryManager);

        repositorySystemSession.setTransferListener(new ConsoleTransferListener());
        repositorySystemSession.setRepositoryListener(new ConsoleRepositoryListener());
    }

    public Map<String, String> readOutLibraries2(String pom) {
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

    private MavenProject getMavenProject(File pomFile) {
        try {
            PlexusContainer container = container();
            org.apache.maven.repository.RepositorySystem lrs = container.lookup(org.apache.maven.repository.RepositorySystem.class);
            ProjectBuilder projectBuilder = container.lookup(ProjectBuilder.class);
            ProjectBuildingRequest request = new DefaultProjectBuildingRequest();
            request.setSystemProperties(requiredSystemProperties());
            request.setRepositorySession(repositorySystemSession);
            request.setProcessPlugins(false);
            request.setLocalRepository(lrs.createDefaultLocalRepository());
            ArtifactRepository remoteRepository = lrs.createDefaultRemoteRepository();
            remoteRepository.setUrl("https://repo.maven.apache.org/maven2/");
            request.setRemoteRepositories(Arrays.asList(new ArtifactRepository[] {remoteRepository}.clone()));
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
