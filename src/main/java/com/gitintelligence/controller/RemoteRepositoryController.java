package com.gitintelligence.controller;

import com.gitintelligence.analysis.RepositoryIntelligenceAnalyzer;
import com.gitintelligence.git.RemoteRepositoryService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class RemoteRepositoryController {

    private final RepositoryIntelligenceAnalyzer analyzer;
    private final RemoteRepositoryService remoteRepositoryService;

    public RemoteRepositoryController(
            RepositoryIntelligenceAnalyzer analyzer,
            RemoteRepositoryService remoteRepositoryService) {

        this.analyzer = analyzer;
        this.remoteRepositoryService =
                remoteRepositoryService;
    }

    @GetMapping("/api/repository/analyze-remote")
    public Map<String, Object> analyzeRemoteRepository(
            @RequestParam String repositoryUrl) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        if (repositoryUrl == null ||
                repositoryUrl.isBlank()) {

            response.put("success", false);
            response.put(
                    "message",
                    "GitHub repository URL is required."
            );

            return response;
        }

        try {

            Path repositoryPath =
                    remoteRepositoryService
                            .getOrCloneRepository(
                                    repositoryUrl
                            );

            Map<String, Object> analysis =
                    analyzer.analyzeRepository(
                            repositoryPath.toString()
                    );

            response.putAll(analysis);

            response.put(
                    "repositoryUrl",
                    repositoryUrl
            );

            response.put(
                    "repositoryPath",
                    repositoryPath.toString()
            );

            response.put(
                    "remoteRepository",
                    true
            );

            return response;

        } catch (Exception e) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "repositoryUrl",
                    repositoryUrl
            );

            response.put(
                    "message",
                    "Unable to clone or analyze the repository: "
                            + e.getMessage()
            );

            return response;
        }
    }
}