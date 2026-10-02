package com.gitintelligence.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.io.DisabledOutputStream;

public class GitRepositoryAnalyzer {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    .withZone(ZoneId.systemDefault());

    private static final DecimalFormat DECIMAL_FORMAT =
            new DecimalFormat("0.00");

    /*
     * ============================================================
     * PERFORMANCE CACHE
     * ============================================================
     *
     * A single Repository instance is used during one complete
     * repository analysis.
     *
     * The old implementation repeatedly recalculated the same
     * Git diffs for:
     *
     * - commit timeline
     * - contributor statistics
     * - file frequency
     * - file churn
     * - file contributors
     * - latest commit per file
     * - repository summary
     *
     * These caches allow each commit diff to be calculated once
     * and reused by all of those operations.
     */

    private final Map<Repository, RepositoryCache> repositoryCaches =
            new IdentityHashMap<>();

    private static class RepositoryCache {

        private List<Map<String, Object>> commitHistory;

        private Integer commitCount;

        private List<String> contributors;

        private final Map<String, List<Map<String, Object>>>
                changedFilesByCommit =
                new HashMap<>();

        private final Map<String, int[]>
                lineStatsByCommit =
                new HashMap<>();

        private List<String> allChangedFiles;

        private List<Map<String, Object>>
                fileChangeFrequency;

        private List<Map<String, Object>>
                fileChurnStatistics;

        private final Map<String, List<String>>
                fileContributors =
                new HashMap<>();

        private final Map<String, Map<String, Object>>
                latestCommitByFile =
                new HashMap<>();

        private List<Map<String, Object>>
                commitTimeline;

        private List<Map<String, Object>>
                commitActivityByAuthor;

        private List<Map<String, Object>>
                contributorActivityDetails;

        private List<Map<String, Object>>
                commitActivityByDate;

        private List<Map<String, Object>>
                linesChangedByDate;

        private List<Map<String, Object>>
                fileExtensionStatistics;

        private List<Map<String, Object>>
                directoryStatistics;

        private Map<String, Object>
                repositorySummary;
    }

    private RepositoryCache getCache(
            Repository repository) {

        return repositoryCaches.computeIfAbsent(
                repository,
                key -> new RepositoryCache()
        );
    }

    // ============================================================
    // Repository
    // ============================================================

    public Repository openRepository(
            String path) throws IOException {

        Path repositoryPath =
                Paths.get(path)
                        .toAbsolutePath()
                        .normalize();

        FileRepositoryBuilder builder =
                new FileRepositoryBuilder();

        return builder
                .setGitDir(
                        Files.isDirectory(
                                repositoryPath.resolve(".git")
                        )
                                ? repositoryPath
                                        .resolve(".git")
                                        .toFile()
                                : repositoryPath.toFile()
                )
                .readEnvironment()
                .findGitDir()
                .build();
    }

    public String getRepositoryName(
            Repository repository) {

        if (repository == null) {
            return "";
        }

        String workTreeName =
                repository.getWorkTree() == null
                        ? ""
                        : repository.getWorkTree().getName();

        if (!workTreeName.isBlank()) {
            return workTreeName;
        }

        return repository.getDirectory()
                .getParentFile()
                .getName();
    }

    public String getCurrentBranch(
            Repository repository)
            throws IOException {

        return repository.getBranch();
    }

    // ============================================================
    // Commits
    // ============================================================

    public int getCommitCount(
            Repository repository)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        if (cache.commitCount != null) {
            return cache.commitCount;
        }

        cache.commitCount =
                getCommitHistory(repository).size();

        return cache.commitCount;
    }

    public List<Map<String, Object>> getCommitHistory(
            Repository repository)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        if (cache.commitHistory != null) {
            return cache.commitHistory;
        }

        List<Map<String, Object>> commits =
                new ArrayList<>();

        try (RevWalk walk =
                     new RevWalk(repository)) {

            ObjectId head =
                    repository.resolve(Constants.HEAD);

            if (head == null) {
                cache.commitHistory = commits;
                cache.commitCount = 0;
                return commits;
            }

            walk.markStart(
                    walk.parseCommit(head)
            );

            for (RevCommit commit : walk) {

                Map<String, Object> data =
                        new LinkedHashMap<>();

                data.put(
                        "id",
                        commit.getName()
                );

                data.put(
                        "shortId",
                        commit.getName().substring(
                                0,
                                Math.min(
                                        8,
                                        commit.getName().length()
                                )
                        )
                );

                data.put(
                        "message",
                        commit.getShortMessage()
                );

                data.put(
                        "author",
                        commit.getAuthorIdent() == null
                                ? ""
                                : commit.getAuthorIdent().getName()
                );

                data.put(
                        "authorEmail",
                        commit.getAuthorIdent() == null
                                ? ""
                                : commit.getAuthorIdent().getEmailAddress()
                );

                data.put(
                        "date",
                        formatDateTime(
                                commit.getCommitTime()
                        )
                );

                data.put(
                        "dateOnly",
                        formatDateOnly(
                                commit.getCommitTime()
                        )
                );

                data.put(
                        "parentCount",
                        commit.getParentCount()
                );

                commits.add(data);
            }
        }

        cache.commitHistory = commits;
        cache.commitCount = commits.size();

        return commits;
    }

    public Map<String, Object> getLatestCommit(
            Repository repository)
            throws IOException {

        try (RevWalk walk =
                     new RevWalk(repository)) {

            ObjectId head =
                    repository.resolve(Constants.HEAD);

            if (head == null) {
                return new LinkedHashMap<>();
            }

            RevCommit commit =
                    walk.parseCommit(head);

            return commitToMap(commit);
        }
    }

    public String getLatestCommitId(
            Repository repository)
            throws IOException {

        Map<String, Object> commit =
                getLatestCommit(repository);

        return toStringValue(
                commit.get("id")
        );
    }

    public String getLatestCommitMessage(
            Repository repository)
            throws IOException {

        Map<String, Object> commit =
                getLatestCommit(repository);

        return toStringValue(
                commit.get("message")
        );
    }

    public String getLatestCommitAuthor(
            Repository repository)
            throws IOException {

        Map<String, Object> commit =
                getLatestCommit(repository);

        return toStringValue(
                commit.get("author")
        );
    }

    public String getLatestCommitAuthorEmail(
            Repository repository)
            throws IOException {

        Map<String, Object> commit =
                getLatestCommit(repository);

        return toStringValue(
                commit.get("authorEmail")
        );
    }

    public String getLatestCommitDate(
            Repository repository)
            throws IOException {

        Map<String, Object> commit =
                getLatestCommit(repository);

        return toStringValue(
                commit.get("date")
        );
    }

    // ============================================================
    // Changed files
    // ============================================================

    public List<Map<String, Object>> getChangedFiles(
            Repository repository,
            RevCommit commit)
            throws IOException {

        if (commit == null) {
            return new ArrayList<>();
        }

        RepositoryCache cache =
                getCache(repository);

        String commitId =
                commit.getName();

        List<Map<String, Object>> cached =
                cache.changedFilesByCommit.get(
                        commitId
                );

        if (cached != null) {
            return cached;
        }

        List<Map<String, Object>> files =
                new ArrayList<>();

        RevTree currentTree =
                commit.getTree();

        RevTree parentTree = null;

        if (commit.getParentCount() > 0) {

            try (RevWalk walk =
                         new RevWalk(repository)) {

                RevCommit parent =
                        walk.parseCommit(
                                commit.getParent(0)
                        );

                parentTree =
                        parent.getTree();
            }
        }

        try (
                DiffFormatter formatter =
                        new DiffFormatter(
                                DisabledOutputStream.INSTANCE
                        )
        ) {

            formatter.setRepository(repository);

            formatter.setDiffComparator(
                    RawTextComparator.DEFAULT
            );

            formatter.setDetectRenames(true);

            List<DiffEntry> entries =
                    formatter.scan(
                            parentTree,
                            currentTree
                    );

            for (DiffEntry entry : entries) {

                Map<String, Object> file =
                        new LinkedHashMap<>();

                String oldPath =
                        entry.getOldPath();

                String newPath =
                        entry.getNewPath();

                String path =
                        DiffEntry.DEV_NULL.equals(newPath)
                                ? oldPath
                                : newPath;

                file.put(
                        "path",
                        path
                );

                file.put(
                        "oldPath",
                        DiffEntry.DEV_NULL.equals(oldPath)
                                ? ""
                                : oldPath
                );

                file.put(
                        "newPath",
                        DiffEntry.DEV_NULL.equals(newPath)
                                ? ""
                                : newPath
                );

                file.put(
                        "changeType",
                        entry.getChangeType().name()
                );

                int[] lineStats =
                        getDiffLineStats(
                                formatter,
                                entry
                        );

                file.put(
                        "linesAdded",
                        lineStats[0]
                );

                file.put(
                        "linesRemoved",
                        lineStats[1]
                );

                file.put(
                        "linesChanged",
                        lineStats[0] + lineStats[1]
                );

                files.add(file);
            }
        }

        files.sort(
                Comparator.comparing(
                        item ->
                                toStringValue(
                                        item.get("path")
                                )
                )
        );

        cache.changedFilesByCommit.put(
                commitId,
                files
        );

        return files;
    }

    public int getFilesAdded(
            Repository repository)
            throws IOException {

        return countChangeType(
                repository,
                DiffEntry.ChangeType.ADD
        );
    }

    public int getFilesModified(
            Repository repository)
            throws IOException {

        return countChangeType(
                repository,
                DiffEntry.ChangeType.MODIFY
        );
    }

    public int getFilesDeleted(
            Repository repository)
            throws IOException {

        return countChangeType(
                repository,
                DiffEntry.ChangeType.DELETE
        );
    }

    public int getFilesRenamed(
            Repository repository)
            throws IOException {

        return countChangeType(
                repository,
                DiffEntry.ChangeType.RENAME
        );
    }

    // ============================================================
    // Line statistics
    // ============================================================

    public long getLinesAdded(
            Repository repository,
            RevCommit commit)
            throws IOException {

        int[] stats =
                getCommitLineStats(
                        repository,
                        commit
                );

        return stats[0];
    }

    public long getLinesRemoved(
            Repository repository,
            RevCommit commit)
            throws IOException {

        int[] stats =
                getCommitLineStats(
                        repository,
                        commit
                );

        return stats[1];
    }

    public long getLinesChanged(
            Repository repository,
            RevCommit commit)
            throws IOException {

        int[] stats =
                getCommitLineStats(
                        repository,
                        commit
                );

        return (long) stats[0]
                + stats[1];
    }

    // ============================================================
    // Contributors
    // ============================================================

    public int getContributorCommitCount(
            Repository repository,
            String contributor)
            throws IOException {

        int count = 0;

        for (Map<String, Object> commit :
                getCommitHistory(repository)) {

            if (contributor.equals(
                    toStringValue(
                            commit.get("author")
                    )
            )) {
                count++;
            }
        }

        return count;
    }

    public long getContributorLinesChanged(
            Repository repository,
            String contributor)
            throws IOException {

        long total = 0;

        for (Map<String, Object> commit :
                getCommitHistory(repository)) {

            String author =
                    toStringValue(
                            commit.get("author")
                    );

            if (!contributor.equals(author)) {
                continue;
            }

            RevCommit revCommit =
                    parseCommit(
                            repository,
                            toStringValue(
                                    commit.get("id")
                            )
                    );

            total += getLinesChanged(
                    repository,
                    revCommit
            );
        }

        return total;
    }

    public int getContributorFilesChanged(
            Repository repository,
            String contributor)
            throws IOException {

        Set<String> files =
                new TreeSet<>();

        for (Map<String, Object> commit :
                getCommitHistory(repository)) {

            String author =
                    toStringValue(
                            commit.get("author")
                    );

            if (!contributor.equals(author)) {
                continue;
            }

            RevCommit revCommit =
                    parseCommit(
                            repository,
                            toStringValue(
                                    commit.get("id")
                            )
                    );

            for (Map<String, Object> file :
                    getChangedFiles(
                            repository,
                            revCommit
                    )) {

                files.add(
                        toStringValue(
                                file.get("path")
                        )
                );
            }
        }

        return files.size();
    }

    public List<String> getContributors(
            Repository repository)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        if (cache.contributors != null) {
            return cache.contributors;
        }

        Set<String> contributors =
                new TreeSet<>();

        for (Map<String, Object> commit :
                getCommitHistory(repository)) {

            String author =
                    toStringValue(
                            commit.get("author")
                    );

            if (!author.isBlank()) {
                contributors.add(author);
            }
        }

        cache.contributors =
                new ArrayList<>(contributors);

        return cache.contributors;
    }

    public List<Map<String, Object>>
    getCommitActivityByAuthor(
            Repository repository)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        if (cache.commitActivityByAuthor != null) {
            return cache.commitActivityByAuthor;
        }

        Map<String, Map<String, Object>> grouped =
                new TreeMap<>();

        for (Map<String, Object> commit :
                getCommitHistory(repository)) {

            String author =
                    toStringValue(
                            commit.get("author")
                    );

            Map<String, Object> data =
                    grouped.computeIfAbsent(
                            author,
                            key -> {

                                Map<String, Object> value =
                                        new LinkedHashMap<>();

                                value.put(
                                        "author",
                                        key
                                );

                                value.put(
                                        "commits",
                                        0
                                );

                                value.put(
                                        "linesAdded",
                                        0L
                                );

                                value.put(
                                        "linesRemoved",
                                        0L
                                );

                                value.put(
                                        "linesChanged",
                                        0L
                                );

                                return value;
                            }
                    );

            data.put(
                    "commits",
                    toInt(
                            data.get("commits")
                    ) + 1
            );

            RevCommit revCommit =
                    parseCommit(
                            repository,
                            toStringValue(
                                    commit.get("id")
                            )
                    );

            int[] stats =
                    getCommitLineStats(
                            repository,
                            revCommit
                    );

            data.put(
                    "linesAdded",
                    toLong(
                            data.get("linesAdded")
                    ) + stats[0]
            );

            data.put(
                    "linesRemoved",
                    toLong(
                            data.get("linesRemoved")
                    ) + stats[1]
            );

            data.put(
                    "linesChanged",
                    toLong(
                            data.get("linesChanged")
                    ) + stats[0] + stats[1]
            );
        }

        List<Map<String, Object>> result =
                new ArrayList<>(
                        grouped.values()
                );

        result.sort(
                Comparator
                        .comparingInt(
                                (Map<String, Object> item) ->
                                        toInt(
                                                item.get("commits")
                                        )
                        )
                        .reversed()
                        .thenComparing(
                                item ->
                                        toStringValue(
                                                item.get("author")
                                        )
                        )
        );

        cache.commitActivityByAuthor =
                result;

        return result;
    }

    public List<Map<String, Object>>
    getContributorActivityDetails(
            Repository repository)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        if (cache.contributorActivityDetails != null) {
            return cache.contributorActivityDetails;
        }

        List<Map<String, Object>> result =
                new ArrayList<>();

        for (String contributor :
                getContributors(repository)) {

            Map<String, Object> data =
                    new LinkedHashMap<>();

            data.put(
                    "author",
                    contributor
            );

            data.put(
                    "commits",
                    getContributorCommitCount(
                            repository,
                            contributor
                    )
            );

            data.put(
                    "linesChanged",
                    getContributorLinesChanged(
                            repository,
                            contributor
                    )
            );

            data.put(
                    "filesChanged",
                    getContributorFilesChanged(
                            repository,
                            contributor
                    )
            );

            result.add(data);
        }

        result.sort(
                Comparator
                        .comparingInt(
                                (Map<String, Object> item) ->
                                        toInt(
                                                item.get("commits")
                                        )
                        )
                        .reversed()
                        .thenComparing(
                                item ->
                                        toStringValue(
                                                item.get("author")
                                        )
                        )
        );

        cache.contributorActivityDetails =
                result;

        return result;
    }

    // ============================================================
    // Date analytics
    // ============================================================

    public List<Map<String, Object>>
    getCommitActivityByDate(
            Repository repository)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        if (cache.commitActivityByDate != null) {
            return cache.commitActivityByDate;
        }

        Map<String, Map<String, Object>> grouped =
                new TreeMap<>();

        for (Map<String, Object> commit :
                getCommitHistory(repository)) {

            String date =
                    toStringValue(
                            commit.get("dateOnly")
                    );

            Map<String, Object> data =
                    grouped.computeIfAbsent(
                            date,
                            key -> {

                                Map<String, Object> value =
                                        new LinkedHashMap<>();

                                value.put(
                                        "date",
                                        key
                                );

                                value.put(
                                        "commits",
                                        0
                                );

                                return value;
                            }
                    );

            data.put(
                    "commits",
                    toInt(
                            data.get("commits")
                    ) + 1
            );
        }

        cache.commitActivityByDate =
                new ArrayList<>(
                        grouped.values()
                );

        return cache.commitActivityByDate;
    }

    public List<Map<String, Object>>
    getLinesChangedByDate(
            Repository repository)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        if (cache.linesChangedByDate != null) {
            return cache.linesChangedByDate;
        }

        Map<String, Map<String, Object>> grouped =
                new TreeMap<>();

        for (Map<String, Object> commit :
                getCommitHistory(repository)) {

            String date =
                    toStringValue(
                            commit.get("dateOnly")
                    );

            RevCommit revCommit =
                    parseCommit(
                            repository,
                            toStringValue(
                                    commit.get("id")
                            )
                    );

            int[] stats =
                    getCommitLineStats(
                            repository,
                            revCommit
                    );

            Map<String, Object> data =
                    grouped.computeIfAbsent(
                            date,
                            key -> {

                                Map<String, Object> value =
                                        new LinkedHashMap<>();

                                value.put(
                                        "date",
                                        key
                                );

                                value.put(
                                        "linesAdded",
                                        0L
                                );

                                value.put(
                                        "linesRemoved",
                                        0L
                                );

                                value.put(
                                        "linesChanged",
                                        0L
                                );

                                return value;
                            }
                    );

            data.put(
                    "linesAdded",
                    toLong(
                            data.get("linesAdded")
                    ) + stats[0]
            );

            data.put(
                    "linesRemoved",
                    toLong(
                            data.get("linesRemoved")
                    ) + stats[1]
            );

            data.put(
                    "linesChanged",
                    toLong(
                            data.get("linesChanged")
                    ) + stats[0] + stats[1]
            );
        }

        cache.linesChangedByDate =
                new ArrayList<>(
                        grouped.values()
                );

        return cache.linesChangedByDate;
    }

    // ============================================================
    // Branches
    // ============================================================

    public List<String> getBranches(
            Repository repository)
            throws IOException {

        List<String> branches =
                new ArrayList<>();

        for (String branch :
                repository.getRefDatabase()
                        .getRefsByPrefix(
                                Constants.R_HEADS
                        )
                        .stream()
                        .map(ref ->
                                Repository.shortenRefName(
                                        ref.getName()
                                )
                        )
                        .toList()) {

            branches.add(branch);
        }

        Collections.sort(branches);

        return branches;
    }

    // ============================================================
    // Files
    // ============================================================

    public List<String> getAllChangedFiles(
            Repository repository)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        if (cache.allChangedFiles != null) {
            return cache.allChangedFiles;
        }

        Set<String> files =
                new TreeSet<>();

        for (Map<String, Object> commit :
                getCommitHistory(repository)) {

            RevCommit revCommit =
                    parseCommit(
                            repository,
                            toStringValue(
                                    commit.get("id")
                            )
                    );

            for (Map<String, Object> file :
                    getChangedFiles(
                            repository,
                            revCommit
                    )) {

                String path =
                        toStringValue(
                                file.get("path")
                        );

                if (!path.isBlank()) {
                    files.add(path);
                }
            }
        }

        cache.allChangedFiles =
                new ArrayList<>(files);

        return cache.allChangedFiles;
    }

    public int getFileChangeCount(
            Repository repository,
            String filePath)
            throws IOException {

        for (Map<String, Object> file :
                getFileChangeFrequency(repository)) {

            if (filePath.equals(
                    toStringValue(
                            file.get("file")
                    )
            )) {

                return toInt(
                        file.get("changeCount")
                );
            }
        }

        return 0;
    }

    public long getFileLinesAdded(
            Repository repository,
            String filePath)
            throws IOException {

        for (Map<String, Object> file :
                getFileChurnStatistics(repository)) {

            if (filePath.equals(
                    toStringValue(
                            file.get("file")
                    )
            )) {

                return toLong(
                        file.get("linesAdded")
                );
            }
        }

        return 0;
    }

    public long getFileLinesRemoved(
            Repository repository,
            String filePath)
            throws IOException {

        for (Map<String, Object> file :
                getFileChurnStatistics(repository)) {

            if (filePath.equals(
                    toStringValue(
                            file.get("file")
                    )
            )) {

                return toLong(
                        file.get("linesRemoved")
                );
            }
        }

        return 0;
    }

    public long getFileChurn(
            Repository repository,
            String filePath)
            throws IOException {

        return getFileLinesAdded(
                repository,
                filePath
        ) + getFileLinesRemoved(
                repository,
                filePath
        );
    }

    public List<String> getFileContributors(
            Repository repository,
            String filePath)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        List<String> cached =
                cache.fileContributors.get(
                        filePath
                );

        if (cached != null) {
            return cached;
        }

        Set<String> contributors =
                new TreeSet<>();

        for (Map<String, Object> commit :
                getCommitHistory(repository)) {

            RevCommit revCommit =
                    parseCommit(
                            repository,
                            toStringValue(
                                    commit.get("id")
                            )
                    );

            boolean changed = false;

            for (Map<String, Object> file :
                    getChangedFiles(
                            repository,
                            revCommit
                    )) {

                if (filePath.equals(
                        toStringValue(
                                file.get("path")
                        )
                )) {

                    changed = true;
                    break;
                }
            }

            if (changed &&
                    revCommit.getAuthorIdent() != null) {

                contributors.add(
                        revCommit.getAuthorIdent().getName()
                );
            }
        }

        List<String> result =
                new ArrayList<>(contributors);

        cache.fileContributors.put(
                filePath,
                result
        );

        return result;
    }

    public Map<String, Object>
    getLatestCommitChangingFile(
            Repository repository,
            String filePath)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        Map<String, Object> cached =
                cache.latestCommitByFile.get(
                        filePath
                );

        if (cached != null) {
            return cached;
        }

        for (Map<String, Object> commit :
                getCommitHistory(repository)) {

            RevCommit revCommit =
                    parseCommit(
                            repository,
                            toStringValue(
                                    commit.get("id")
                            )
                    );

            for (Map<String, Object> file :
                    getChangedFiles(
                            repository,
                            revCommit
                    )) {

                if (filePath.equals(
                        toStringValue(
                                file.get("path")
                        )
                )) {

                    Map<String, Object> result =
                            new LinkedHashMap<>();

                    result.put(
                            "id",
                            revCommit.getName()
                    );

                    result.put(
                            "shortId",
                            revCommit.getName().substring(
                                    0,
                                    Math.min(
                                            8,
                                            revCommit.getName().length()
                                    )
                            )
                    );

                    result.put(
                            "message",
                            revCommit.getShortMessage()
                    );

                    result.put(
                            "author",
                            revCommit.getAuthorIdent() == null
                                    ? ""
                                    : revCommit
                                            .getAuthorIdent()
                                            .getName()
                    );

                    result.put(
                            "date",
                            formatDateTime(
                                    revCommit.getCommitTime()
                            )
                    );

                    cache.latestCommitByFile.put(
                            filePath,
                            result
                    );

                    return result;
                }
            }
        }

        Map<String, Object> empty =
                new LinkedHashMap<>();

        cache.latestCommitByFile.put(
                filePath,
                empty
        );

        return empty;
    }

    public String getFileExtension(
            String filePath) {

        if (filePath == null ||
                filePath.isBlank()) {

            return "";
        }

        String name =
                Paths.get(filePath)
                        .getFileName()
                        .toString();

        int index =
                name.lastIndexOf('.');

        if (index <= 0 ||
                index == name.length() - 1) {

            return "";
        }

        return name.substring(
                        index + 1
                )
                .toLowerCase();
    }

    public List<Map<String, Object>>
    getFileChangeFrequency(
            Repository repository)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        if (cache.fileChangeFrequency != null) {
            return cache.fileChangeFrequency;
        }

        Map<String, Integer> counts =
                new TreeMap<>();

        for (Map<String, Object> commit :
                getCommitHistory(repository)) {

            RevCommit revCommit =
                    parseCommit(
                            repository,
                            toStringValue(
                                    commit.get("id")
                            )
                    );

            for (Map<String, Object> file :
                    getChangedFiles(
                            repository,
                            revCommit
                    )) {

                String path =
                        toStringValue(
                                file.get("path")
                        );

                counts.put(
                        path,
                        counts.getOrDefault(
                                path,
                                0
                        ) + 1
                );
            }
        }

        List<Map<String, Object>> result =
                new ArrayList<>();

        for (Map.Entry<String, Integer> entry :
                counts.entrySet()) {

            Map<String, Object> data =
                    new LinkedHashMap<>();

            data.put(
                    "file",
                    entry.getKey()
            );

            data.put(
                    "changeCount",
                    entry.getValue()
            );

            result.add(data);
        }

        result.sort(
                Comparator
                        .comparingInt(
                                (Map<String, Object> item) ->
                                        toInt(
                                                item.get(
                                                        "changeCount"
                                                )
                                        )
                        )
                        .reversed()
                        .thenComparing(
                                item ->
                                        toStringValue(
                                                item.get("file")
                                        )
                        )
        );

        cache.fileChangeFrequency =
                result;

        return result;
    }

    public List<Map<String, Object>>
    getFileChurnStatistics(
            Repository repository)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        if (cache.fileChurnStatistics != null) {
            return cache.fileChurnStatistics;
        }

        Map<String, Map<String, Object>> grouped =
                new TreeMap<>();

        for (Map<String, Object> commit :
                getCommitHistory(repository)) {

            RevCommit revCommit =
                    parseCommit(
                            repository,
                            toStringValue(
                                    commit.get("id")
                            )
                    );

            for (Map<String, Object> file :
                    getChangedFiles(
                            repository,
                            revCommit
                    )) {

                String path =
                        toStringValue(
                                file.get("path")
                        );

                Map<String, Object> data =
                        grouped.computeIfAbsent(
                                path,
                                key -> {

                                    Map<String, Object> value =
                                            new LinkedHashMap<>();

                                    value.put(
                                            "file",
                                            key
                                    );

                                    value.put(
                                            "linesAdded",
                                            0L
                                    );

                                    value.put(
                                            "linesRemoved",
                                            0L
                                    );

                                    value.put(
                                            "churn",
                                            0L
                                    );

                                    value.put(
                                            "changeCount",
                                            0
                                    );

                                    return value;
                                }
                        );

                long added =
                        toLong(
                                file.get("linesAdded")
                        );

                long removed =
                        toLong(
                                file.get("linesRemoved")
                        );

                data.put(
                        "linesAdded",
                        toLong(
                                data.get("linesAdded")
                        ) + added
                );

                data.put(
                        "linesRemoved",
                        toLong(
                                data.get("linesRemoved")
                        ) + removed
                );

                data.put(
                        "churn",
                        toLong(
                                data.get("churn")
                        ) + added + removed
                );

                data.put(
                        "changeCount",
                        toInt(
                                data.get("changeCount")
                        ) + 1
                );
            }
        }

        List<Map<String, Object>> result =
                new ArrayList<>(
                        grouped.values()
                );

        result.sort(
                Comparator
                        .comparingLong(
                                (Map<String, Object> item) ->
                                        toLong(
                                                item.get("churn")
                                        )
                        )
                        .reversed()
                        .thenComparing(
                                item ->
                                        toStringValue(
                                                item.get("file")
                                        )
                        )
        );

        cache.fileChurnStatistics =
                result;

        return result;
    }

    public List<Map<String, Object>>
    getFileExtensionStatistics(
            Repository repository)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        if (cache.fileExtensionStatistics != null) {
            return cache.fileExtensionStatistics;
        }

        Map<String, Map<String, Object>> grouped =
                new TreeMap<>();

        for (String file :
                getAllChangedFiles(repository)) {

            String extension =
                    getFileExtension(file);

            if (extension.isBlank()) {
                extension = "[none]";
            }

            Map<String, Object> data =
                    grouped.computeIfAbsent(
                            extension,
                            key -> {

                                Map<String, Object> value =
                                        new LinkedHashMap<>();

                                value.put(
                                        "extension",
                                        key
                                );

                                value.put(
                                        "files",
                                        0
                                );

                                return value;
                            }
                    );

            data.put(
                    "files",
                    toInt(
                            data.get("files")
                    ) + 1
            );
        }

        List<Map<String, Object>> result =
                new ArrayList<>(
                        grouped.values()
                );

        result.sort(
                Comparator
                        .comparingInt(
                                (Map<String, Object> item) ->
                                        toInt(
                                                item.get("files")
                                        )
                        )
                        .reversed()
                        .thenComparing(
                                item ->
                                        toStringValue(
                                                item.get("extension")
                                        )
                        )
        );

        cache.fileExtensionStatistics =
                result;

        return result;
    }

    public List<Map<String, Object>>
    getDirectoryStatistics(
            Repository repository)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        if (cache.directoryStatistics != null) {
            return cache.directoryStatistics;
        }

        Map<String, Map<String, Object>> grouped =
                new TreeMap<>();

        for (String file :
                getAllChangedFiles(repository)) {

            String normalized =
                    file.replace('\\', '/');

            int slash =
                    normalized.lastIndexOf('/');

            String directory =
                    slash < 0
                            ? "[root]"
                            : normalized.substring(
                                    0,
                                    slash
                            );

            Map<String, Object> data =
                    grouped.computeIfAbsent(
                            directory,
                            key -> {

                                Map<String, Object> value =
                                        new LinkedHashMap<>();

                                value.put(
                                        "directory",
                                        key
                                );

                                value.put(
                                        "files",
                                        0
                                );

                                return value;
                            }
                    );

            data.put(
                    "files",
                    toInt(
                            data.get("files")
                    ) + 1
            );
        }

        List<Map<String, Object>> result =
                new ArrayList<>(
                        grouped.values()
                );

        result.sort(
                Comparator
                        .comparingInt(
                                (Map<String, Object> item) ->
                                        toInt(
                                                item.get("files")
                                        )
                        )
                        .reversed()
                        .thenComparing(
                                item ->
                                        toStringValue(
                                                item.get("directory")
                                        )
                        )
        );

        cache.directoryStatistics =
                result;

        return result;
    }

    // ============================================================
    // Commit timeline
    // ============================================================

    public List<Map<String, Object>>
    getCommitTimeline(
            Repository repository)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        if (cache.commitTimeline != null) {
            return cache.commitTimeline;
        }

        List<Map<String, Object>> timeline =
                new ArrayList<>();

        for (Map<String, Object> commit :
                getCommitHistory(repository)) {

            String commitId =
                    toStringValue(
                            commit.get("id")
                    );

            RevCommit revCommit =
                    parseCommit(
                            repository,
                            commitId
                    );

            int[] stats =
                    getCommitLineStats(
                            repository,
                            revCommit
                    );

            Map<String, Object> data =
                    new LinkedHashMap<>();

            data.put(
                    "id",
                    revCommit.getName()
            );

            data.put(
                    "shortId",
                    revCommit.getName().substring(
                            0,
                            Math.min(
                                    8,
                                    revCommit.getName().length()
                            )
                    )
            );

            data.put(
                    "message",
                    revCommit.getShortMessage()
            );

            data.put(
                    "author",
                    revCommit.getAuthorIdent() == null
                            ? ""
                            : revCommit
                                    .getAuthorIdent()
                                    .getName()
            );

            data.put(
                    "date",
                    formatDateTime(
                            revCommit.getCommitTime()
                    )
            );

            data.put(
                    "dateOnly",
                    formatDateOnly(
                            revCommit.getCommitTime()
                    )
            );

            data.put(
                    "filesChanged",
                    getChangedFiles(
                            repository,
                            revCommit
                    ).size()
            );

            data.put(
                    "linesAdded",
                    stats[0]
            );

            data.put(
                    "linesRemoved",
                    stats[1]
            );

            data.put(
                    "linesChanged",
                    stats[0] + stats[1]
            );

            timeline.add(data);
        }

        cache.commitTimeline =
                timeline;

        return timeline;
    }

    // ============================================================
    // Largest commits
    // ============================================================

    public List<Map<String, Object>>
    getLargestCommits(
            Repository repository,
            int limit)
            throws IOException {

        List<Map<String, Object>> commits =
                new ArrayList<>(
                        getCommitTimeline(repository)
                );

        commits.sort(
                Comparator
                        .comparingLong(
                                (Map<String, Object> item) ->
                                        toLong(
                                                item.get(
                                                        "linesChanged"
                                                )
                                        )
                        )
                        .reversed()
                        .thenComparing(
                                item ->
                                        toStringValue(
                                                item.get("id")
                                        )
                        )
        );

        if (limit <= 0 ||
                commits.size() <= limit) {

            return commits;
        }

        return new ArrayList<>(
                commits.subList(
                        0,
                        limit
                )
        );
    }

    // ============================================================
    // Repository summary
    // ============================================================

    public Map<String, Object>
    getRepositorySummary(
            Repository repository)
            throws IOException {

        RepositoryCache cache =
                getCache(repository);

        if (cache.repositorySummary != null) {
            return cache.repositorySummary;
        }

        Map<String, Object> summary =
                new LinkedHashMap<>();

        summary.put(
                "repositoryName",
                getRepositoryName(repository)
        );

        summary.put(
                "currentBranch",
                getCurrentBranch(repository)
        );

        summary.put(
                "commitCount",
                getCommitCount(repository)
        );

        summary.put(
                "contributors",
                getContributors(repository).size()
        );

        summary.put(
                "branches",
                getBranches(repository).size()
        );

        summary.put(
                "filesChanged",
                getAllChangedFiles(repository).size()
        );

        summary.put(
                "filesAdded",
                getFilesAdded(repository)
        );

        summary.put(
                "filesModified",
                getFilesModified(repository)
        );

        summary.put(
                "filesDeleted",
                getFilesDeleted(repository)
        );

        summary.put(
                "filesRenamed",
                getFilesRenamed(repository)
        );

        long totalAdded = 0;
        long totalRemoved = 0;

        for (Map<String, Object> commit :
                getCommitHistory(repository)) {

            RevCommit revCommit =
                    parseCommit(
                            repository,
                            toStringValue(
                                    commit.get("id")
                            )
                    );

            int[] stats =
                    getCommitLineStats(
                            repository,
                            revCommit
                    );

            totalAdded += stats[0];
            totalRemoved += stats[1];
        }

        summary.put(
                "linesAdded",
                totalAdded
        );

        summary.put(
                "linesRemoved",
                totalRemoved
        );

        summary.put(
                "linesChanged",
                totalAdded + totalRemoved
        );

        summary.put(
                "latestCommit",
                getLatestCommit(repository)
        );

        cache.repositorySummary =
                summary;

        return summary;
    }

    // ============================================================
    // Internal diff helpers
    // ============================================================

    private int countChangeType(
            Repository repository,
            DiffEntry.ChangeType changeType)
            throws IOException {

        int count = 0;

        for (Map<String, Object> commit :
                getCommitHistory(repository)) {

            RevCommit revCommit =
                    parseCommit(
                            repository,
                            toStringValue(
                                    commit.get("id")
                            )
                    );

            for (Map<String, Object> file :
                    getChangedFiles(
                            repository,
                            revCommit
                    )) {

                if (changeType.name().equals(
                        toStringValue(
                                file.get("changeType")
                        )
                )) {

                    count++;
                }
            }
        }

        return count;
    }

    private int[] getCommitLineStats(
            Repository repository,
            RevCommit commit)
            throws IOException {

        if (commit == null) {
            return new int[]{0, 0};
        }

        RepositoryCache cache =
                getCache(repository);

        String commitId =
                commit.getName();

        int[] cached =
                cache.lineStatsByCommit.get(
                        commitId
                );

        if (cached != null) {
            return cached;
        }

        int added = 0;
        int removed = 0;

        /*
         * Important:
         *
         * Do NOT create another DiffFormatter here.
         *
         * getChangedFiles() already calculates the diff
         * and line statistics for this commit.
         */

        for (Map<String, Object> file :
                getChangedFiles(
                        repository,
                        commit
                )) {

            added += toInt(
                    file.get("linesAdded")
            );

            removed += toInt(
                    file.get("linesRemoved")
            );
        }

        int[] result =
                new int[]{
                        added,
                        removed
                };

        cache.lineStatsByCommit.put(
                commitId,
                result
        );

        return result;
    }

    private int[] getDiffLineStats(
            DiffFormatter formatter,
            DiffEntry entry) {

        try {

            EditList edits =
                    formatter
                            .toFileHeader(entry)
                            .toEditList();

            int added = 0;
            int removed = 0;

            for (var edit : edits) {

                added +=
                        edit.getEndB()
                                - edit.getBeginB();

                removed +=
                        edit.getEndA()
                                - edit.getBeginA();
            }

            return new int[]{
                    added,
                    removed
            };

        } catch (Exception ignored) {

            return new int[]{
                    0,
                    0
            };
        }
    }

    // ============================================================
    // Commit helpers
    // ============================================================

    private RevCommit parseCommit(
            Repository repository,
            String commitId)
            throws IOException {

        try (RevWalk walk =
                     new RevWalk(repository)) {

            return walk.parseCommit(
                    ObjectId.fromString(commitId)
            );
        }
    }

    private Map<String, Object> commitToMap(
            RevCommit commit) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "id",
                commit.getName()
        );

        data.put(
                "shortId",
                commit.getName().substring(
                        0,
                        Math.min(
                                8,
                                commit.getName().length()
                        )
                )
        );

        data.put(
                "message",
                commit.getShortMessage()
        );

        data.put(
                "author",
                commit.getAuthorIdent() == null
                        ? ""
                        : commit
                                .getAuthorIdent()
                                .getName()
        );

        data.put(
                "authorEmail",
                commit.getAuthorIdent() == null
                        ? ""
                        : commit
                                .getAuthorIdent()
                                .getEmailAddress()
        );

        data.put(
                "date",
                formatDateTime(
                        commit.getCommitTime()
                )
        );

        data.put(
                "dateOnly",
                formatDateOnly(
                        commit.getCommitTime()
                )
        );

        data.put(
                "parentCount",
                commit.getParentCount()
        );

        return data;
    }

    // ============================================================
    // Utility methods
    // ============================================================

    private String formatDateTime(
            int epochSeconds) {

        return DATE_TIME_FORMATTER.format(
                Instant.ofEpochSecond(
                        epochSeconds
                )
        );
    }

    private String formatDateOnly(
            int epochSeconds) {

        return DATE_FORMATTER.format(
                Instant.ofEpochSecond(
                        epochSeconds
                )
        );
    }

    private int toInt(Object value) {

        return value instanceof Number number
                ? number.intValue()
                : 0;
    }

    private long toLong(Object value) {

        return value instanceof Number number
                ? number.longValue()
                : 0L;
    }

    private String toStringValue(
            Object value) {

        return value == null
                ? ""
                : String.valueOf(value);
    }
}