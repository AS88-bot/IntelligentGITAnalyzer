package com.gitintelligence.controller;

import com.gitintelligence.analysis.RepositoryIntelligenceAnalyzer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/repository")
@CrossOrigin(origins = "http://localhost:5173")
public class RepositoryIntelligenceController {

    private final RepositoryIntelligenceAnalyzer analyzer;

    public RepositoryIntelligenceController(
            RepositoryIntelligenceAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @GetMapping("/analyze")
    public ResponseEntity<?> analyzeRepository(
            @RequestParam("path") String path) {
        try {
            Map<String, Object> result =
                    analyzer.analyzeRepository(path);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(errorResponse(
                            "Invalid repository request",
                            e.getMessage()
                    ));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse(
                            "Repository analysis failed",
                            e.getMessage()
                    ));
        }
    }

    @GetMapping("/file")
    public ResponseEntity<?> analyzeFile(
            @RequestParam("repositoryPath") String repositoryPath,
            @RequestParam("file") String filePath) {

        try {
            Map<String, Object> result =
                    analyzer.analyzeFile(
                            repositoryPath,
                            filePath
                    );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(errorResponse(
                            "Invalid file request",
                            e.getMessage()
                    ));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse(
                            "File analysis failed",
                            e.getMessage()
                    ));
        }
    }

    @GetMapping("/source")
    public ResponseEntity<?> getSourceFile(
            @RequestParam("repositoryPath") String repositoryPath,
            @RequestParam("file") String filePath) {

        try {
            if (repositoryPath == null ||
                    repositoryPath.isBlank()) {

                throw new IllegalArgumentException(
                        "Repository path is required."
                );
            }

            if (filePath == null ||
                    filePath.isBlank()) {

                throw new IllegalArgumentException(
                        "File path is required."
                );
            }

            Path repositoryRoot =
                    Paths.get(repositoryPath)
                            .toAbsolutePath()
                            .normalize();

            if (!Files.exists(repositoryRoot)) {
                throw new IllegalArgumentException(
                        "Repository path does not exist: "
                                + repositoryPath
                );
            }

            if (!Files.isDirectory(repositoryRoot)) {
                throw new IllegalArgumentException(
                        "Repository path is not a directory: "
                                + repositoryPath
                );
            }

            Path requestedFile =
                    repositoryRoot
                            .resolve(filePath)
                            .normalize();

            if (!requestedFile.startsWith(repositoryRoot)) {
                throw new IllegalArgumentException(
                        "File path is outside the repository."
                );
            }

            if (!Files.exists(requestedFile)) {
                throw new IllegalArgumentException(
                        "File does not exist: "
                                + filePath
                );
            }

            if (!Files.isRegularFile(requestedFile)) {
                throw new IllegalArgumentException(
                        "Requested path is not a file: "
                                + filePath
                );
            }

            String extension =
                    getFileExtension(filePath);

            if (!isSupportedSourceFile(extension)) {
                throw new IllegalArgumentException(
                        "Unsupported source file type: "
                                + extension
                );
            }

            String content =
                    Files.readString(
                            requestedFile,
                            StandardCharsets.UTF_8
                    );

            long sizeBytes =
                    Files.size(requestedFile);

            long lineCount =
                    content.isEmpty()
                            ? 0
                            : content.lines().count();

            Map<String, Object> result =
                    new LinkedHashMap<>();

            result.put("success", true);
            result.put(
                    "repositoryPath",
                    repositoryRoot.toString()
            );
            result.put(
                    "file",
                    filePath.replace('\\', '/')
            );
            result.put(
                    "extension",
                    extension
            );
            result.put(
                    "content",
                    content
            );
            result.put(
                    "lines",
                    lineCount
            );
            result.put(
                    "sizeBytes",
                    sizeBytes
            );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(errorResponse(
                            "Invalid source file request",
                            e.getMessage()
                    ));

        } catch (IOException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse(
                            "Source file could not be read",
                            e.getMessage()
                    ));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse(
                            "Source file loading failed",
                            e.getMessage()
                    ));
        }
    }

    /*
     * =========================================================
     * FEATURE 6
     * REPOSITORY CODE SEARCH
     * =========================================================
     */

    @GetMapping("/search")
    public ResponseEntity<?> searchRepository(
            @RequestParam("repositoryPath") String repositoryPath,
            @RequestParam("query") String query) {

        try {
            if (repositoryPath == null ||
                    repositoryPath.isBlank()) {

                throw new IllegalArgumentException(
                        "Repository path is required."
                );
            }

            if (query == null ||
                    query.isBlank()) {

                throw new IllegalArgumentException(
                        "Search query is required."
                );
            }

            String trimmedQuery =
                    query.trim();

            if (trimmedQuery.length() < 1) {
                throw new IllegalArgumentException(
                        "Search query cannot be empty."
                );
            }

            Path repositoryRoot =
                    Paths.get(repositoryPath)
                            .toAbsolutePath()
                            .normalize();

            if (!Files.exists(repositoryRoot)) {
                throw new IllegalArgumentException(
                        "Repository path does not exist: "
                                + repositoryPath
                );
            }

            if (!Files.isDirectory(repositoryRoot)) {
                throw new IllegalArgumentException(
                        "Repository path is not a directory: "
                                + repositoryPath
                );
            }

            List<Map<String, Object>> matches =
                    new ArrayList<>();

            try (Stream<Path> paths =
                         Files.walk(repositoryRoot)) {

                paths
                        .filter(Files::isRegularFile)
                        .filter(path ->
                                isSearchableFile(
                                        repositoryRoot,
                                        path
                                ))
                        .forEach(path ->
                                searchFile(
                                        repositoryRoot,
                                        path,
                                        trimmedQuery,
                                        matches
                                )
                        );
            }

            Map<String, Object> result =
                    new LinkedHashMap<>();

            result.put("success", true);
            result.put(
                    "repositoryPath",
                    repositoryRoot.toString()
            );
            result.put(
                    "query",
                    trimmedQuery
            );
            result.put(
                    "totalMatches",
                    matches.size()
            );
            result.put(
                    "filesMatched",
                    countUniqueFiles(matches)
            );
            result.put(
                    "results",
                    matches
            );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(errorResponse(
                            "Invalid search request",
                            e.getMessage()
                    ));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse(
                            "Repository search failed",
                            e.getMessage()
                    ));
        }
    }

    private void searchFile(
            Path repositoryRoot,
            Path file,
            String query,
            List<Map<String, Object>> matches) {

        try {
            String content =
                    Files.readString(
                            file,
                            StandardCharsets.UTF_8
                    );

            String[] lines =
                    content.split(
                            "\\r?\\n",
                            -1
                    );

            String lowerQuery =
                    query.toLowerCase();

            for (int i = 0; i < lines.length; i++) {

                String line =
                        lines[i];

                if (line
                        .toLowerCase()
                        .contains(lowerQuery)) {

                    Path relativePath =
                            repositoryRoot
                                    .relativize(file);

                    Map<String, Object> match =
                            new LinkedHashMap<>();

                    match.put(
                            "file",
                            relativePath
                                    .toString()
                                    .replace('\\', '/')
                    );

                    match.put(
                            "line",
                            i + 1
                    );

                    match.put(
                            "content",
                            line
                    );

                    match.put(
                            "extension",
                            getFileExtension(
                                    file.toString()
                            )
                    );

                    matches.add(match);
                }
            }

        } catch (IOException ignored) {
            /*
             * Some files may not be readable as UTF-8.
             * Those files are skipped rather than failing
             * the entire repository search.
             */
        }
    }

    private boolean isSearchableFile(
            Path repositoryRoot,
            Path file) {

        Path relative =
                repositoryRoot.relativize(file);

        for (Path part : relative) {

            String directory =
                    part.toString();

            if (directory.equals(".git") ||
                    directory.equals("node_modules") ||
                    directory.equals("dist") ||
                    directory.equals("build") ||
                    directory.equals("target") ||
                    directory.equals(".idea") ||
                    directory.equals(".next") ||
                    directory.equals("coverage")) {

                return false;
            }
        }

        String extension =
                getFileExtension(
                        file.toString()
                );

        return isSupportedSourceFile(extension);
    }

    private int countUniqueFiles(
            List<Map<String, Object>> matches) {

        return (int) matches
                .stream()
                .map(match ->
                        String.valueOf(
                                match.get("file")
                        )
                )
                .distinct()
                .count();
    }

    private String getFileExtension(
            String filePath) {

        String normalized =
                filePath.replace('\\', '/');

        int slash =
                normalized.lastIndexOf('/');

        String fileName =
                slash >= 0
                        ? normalized.substring(
                                slash + 1
                        )
                        : normalized;

        int dot =
                fileName.lastIndexOf('.');

        if (dot <= 0 ||
                dot == fileName.length() - 1) {

            return "";
        }

        return fileName
                .substring(dot + 1)
                .toLowerCase();
    }

    private boolean isSupportedSourceFile(
            String extension) {

        return extension.equals("ts")
                || extension.equals("tsx")
                || extension.equals("js")
                || extension.equals("jsx")
                || extension.equals("java")
                || extension.equals("css")
                || extension.equals("html")
                || extension.equals("json");
    }

    private Map<String, Object> errorResponse(
            String error,
            String message) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "success",
                false
        );

        response.put(
                "error",
                error
        );

        response.put(
                "message",
                message == null
                        ? "Unknown error"
                        : message
        );

        return response;
    }
}