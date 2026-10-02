package com.gitintelligence.git;

import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
public class RemoteRepositoryService {

    private static final int MAX_CACHED_REPOSITORIES = 5;

    private final Map<String, Path> repositoryCache =
            new ConcurrentHashMap<>();

    /**
     * Gets an existing cached clone or clones the repository.
     */
    public synchronized Path getOrCloneRepository(
            String repositoryUrl) throws Exception {

        validateRepositoryUrl(repositoryUrl);

        String normalizedUrl =
                normalizeRepositoryUrl(repositoryUrl);

        Path cachedPath =
                repositoryCache.get(normalizedUrl);

        if (cachedPath != null && Files.exists(cachedPath)) {
            return cachedPath;
        }

        if (cachedPath != null) {
            repositoryCache.remove(normalizedUrl);
        }

        if (repositoryCache.size() >= MAX_CACHED_REPOSITORIES) {

            String oldestKey =
                    repositoryCache.keySet()
                            .iterator()
                            .next();

            Path oldestPath =
                    repositoryCache.remove(oldestKey);

            cleanupRepository(oldestPath);
        }

        Path temporaryDirectory =
                Files.createTempDirectory(
                        "git-intelligence-"
                );

        try {

            Git.cloneRepository()
                    .setURI(normalizedUrl)
                    .setDirectory(
                            temporaryDirectory.toFile()
                    )
                    .setCloneAllBranches(false)
                    .call();

            repositoryCache.put(
                    normalizedUrl,
                    temporaryDirectory
            );

            return temporaryDirectory;

        } catch (Exception e) {

            cleanupRepository(
                    temporaryDirectory
            );

            throw e;
        }
    }

    /**
     * Returns a cached repository for the URL,
     * or clones it if necessary.
     */
    public Path resolveRepositoryUrl(
            String repositoryUrl) throws Exception {

        return getOrCloneRepository(repositoryUrl);
    }

    /**
     * Finds which cached GitHub repository owns a path.
     */
    public String findRepositoryUrlForPath(
            String repositoryPath) {

        if (repositoryPath == null ||
                repositoryPath.isBlank()) {

            return null;
        }

        Path requestedPath =
                Path.of(repositoryPath)
                        .toAbsolutePath()
                        .normalize();

        for (Map.Entry<String, Path> entry :
                repositoryCache.entrySet()) {

            Path cachedPath =
                    entry.getValue()
                            .toAbsolutePath()
                            .normalize();

            if (requestedPath.startsWith(cachedPath)) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * Checks whether a path belongs to one of our cached
     * remote repositories.
     */
    public boolean isCachedRepositoryPath(
            String repositoryPath) {

        return findRepositoryUrlForPath(
                repositoryPath
        ) != null;
    }

    /**
     * Returns the cached path for a repository URL,
     * without cloning a second copy.
     */
    public Path getCachedRepository(
            String repositoryUrl) {

        if (repositoryUrl == null ||
                repositoryUrl.isBlank()) {

            return null;
        }

        Path path =
                repositoryCache.get(
                        normalizeRepositoryUrl(
                                repositoryUrl
                        )
                );

        if (path != null &&
                Files.exists(path)) {

            return path;
        }

        return null;
    }

    /**
     * Normalizes common GitHub URL forms.
     */
    public String normalizeRepositoryUrl(
            String repositoryUrl) {

        String url =
                repositoryUrl.trim();

        if (url.endsWith("/")) {
            url = url.substring(
                    0,
                    url.length() - 1
            );
        }

        if (url.endsWith(".git")) {
            return url;
        }

        return url + ".git";
    }

    /**
     * Validates that the URL points to GitHub over HTTPS.
     */
    private void validateRepositoryUrl(
            String repositoryUrl) {

        if (repositoryUrl == null ||
                repositoryUrl.isBlank()) {

            throw new IllegalArgumentException(
                    "Repository URL is required."
            );
        }

        try {

            URI uri =
                    URI.create(
                            repositoryUrl.trim()
                    );

            String scheme =
                    uri.getScheme();

            String host =
                    uri.getHost();

            if (!"https".equalsIgnoreCase(scheme)) {

                throw new IllegalArgumentException(
                        "Only HTTPS GitHub repository URLs are supported."
                );
            }

            if (host == null ||
                    !"github.com".equalsIgnoreCase(host)) {

                throw new IllegalArgumentException(
                        "Only GitHub repositories are currently supported."
                );
            }

            String path =
                    uri.getPath();

            if (path == null ||
                    path.isBlank() ||
                    path.equals("/")) {

                throw new IllegalArgumentException(
                        "A valid GitHub repository path is required."
                );
            }

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "Invalid GitHub repository URL: "
                            + repositoryUrl,
                    e
            );
        }
    }

    /**
     * Cleanup one repository.
     */
    public void cleanupRepository(
            Path repositoryPath) {

        if (repositoryPath == null) {
            return;
        }

        if (!Files.exists(repositoryPath)) {
            return;
        }

        try (Stream<Path> paths =
                     Files.walk(repositoryPath)) {

            paths.sorted(
                            Comparator.reverseOrder()
                    )
                    .forEach(path -> {

                        try {

                            Files.deleteIfExists(
                                    path
                            );

                        } catch (IOException e) {

                            throw new UncheckedIOException(
                                    e
                            );
                        }
                    });

        } catch (
                IOException |
                UncheckedIOException e) {

            System.err.println(
                    "Warning: unable to completely clean "
                            + "temporary repository: "
                            + e.getMessage()
            );
        }
    }

    /**
     * Cleanup everything when the Spring application stops.
     */
    public synchronized void cleanupAllRepositories() {

        for (Path path :
                repositoryCache.values()) {

            cleanupRepository(path);
        }

        repositoryCache.clear();
    }
}