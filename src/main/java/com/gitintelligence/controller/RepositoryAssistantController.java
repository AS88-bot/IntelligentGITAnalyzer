package com.gitintelligence.controller;

import com.gitintelligence.analysis.RepositoryIntelligenceAnalyzer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class RepositoryAssistantController {

    private final RepositoryIntelligenceAnalyzer analyzer;

    public RepositoryAssistantController(
            RepositoryIntelligenceAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @GetMapping("/api/repository/assistant")
    public Map<String, Object> askAssistant(
            @RequestParam String repositoryPath,
            @RequestParam String question) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (repositoryPath == null || repositoryPath.isBlank()) {
            response.put("success", false);
            response.put("message", "Repository path is required.");
            return response;
        }

        if (question == null || question.isBlank()) {
            response.put("success", false);
            response.put("message", "Question is required.");
            return response;
        }

        try {
            Map<String, Object> analysis =
                    analyzer.analyzeRepository(repositoryPath);

            Object successValue = analysis.get("success");

            if (successValue instanceof Boolean
                    && !((Boolean) successValue)) {

                response.put("success", false);
                response.put(
                        "message",
                        analysis.getOrDefault(
                                "message",
                                "Repository analysis failed."
                        )
                );

                return response;
            }

            String normalizedQuestion =
                    question.trim().toLowerCase();

            String answer;
            Map<String, Object> supportingData;

            if (containsAny(
                    normalizedQuestion,
                    "hotspot",
                    "hot spots",
                    "attention",
                    "risky",
                    "risk",
                    "problematic")) {

                answer = answerHotspots(analysis);
                supportingData = extractMap(
                        analysis,
                        "hotspots"
                );

            } else if (containsAny(
                    normalizedQuestion,
                    "maintenance",
                    "technical debt",
                    "technical-debt",
                    "debt",
                    "maintain")) {

                answer = answerMaintenance(analysis);
                supportingData = extractMap(
                        analysis,
                        "maintenanceCandidates"
                );

            } else if (containsAny(
                    normalizedQuestion,
                    "complexity",
                    "complex")) {

                answer = answerComplexity(analysis);
                supportingData = extractMap(
                        analysis,
                        "fileIntelligence"
                );

            } else if (containsAny(
                    normalizedQuestion,
                    "churn",
                    "changed",
                    "changes",
                    "change activity")) {

                answer = answerChurn(analysis);
                supportingData = extractMap(
                        analysis,
                        "gitAnalytics"
                );

            } else if (containsAny(
                    normalizedQuestion,
                    "contributor",
                    "contributors",
                    "developer",
                    "developers",
                    "author",
                    "authors")) {

                answer = answerContributors(analysis);
                supportingData = extractMap(
                        analysis,
                        "gitAnalytics"
                );

            } else if (containsAny(
                    normalizedQuestion,
                    "repository",
                    "overview",
                    "summary",
                    "health",
                    "project")) {

                answer = answerRepository(analysis);
                supportingData = extractMap(
                        analysis,
                        "repositoryHealth"
                );

            } else {

                answer = answerGeneral(analysis);
                supportingData = new LinkedHashMap<>();
            }

            response.put("success", true);
            response.put("repositoryPath", repositoryPath);
            response.put("question", question);
            response.put("answer", answer);
            response.put("supportingData", supportingData);

            return response;

        } catch (Exception e) {

            response.put("success", false);
            response.put(
                    "message",
                    "Unable to answer the question: "
                            + e.getMessage()
            );

            return response;
        }
    }

    private boolean containsAny(
            String text,
            String... keywords) {

        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private Map<String, Object> extractMap(
            Map<String, Object> source,
            String key) {

        Object value = source.get(key);

        if (value instanceof Map<?, ?>) {

            Map<String, Object> result =
                    new LinkedHashMap<>();

            ((Map<?, ?>) value).forEach(
                    (k, v) -> result.put(
                            String.valueOf(k),
                            v
                    )
            );

            return result;
        }

        Map<String, Object> result =
                new LinkedHashMap<>();

        if (value != null) {
            result.put("value", value);
        }

        return result;
    }

    private String answerHotspots(
            Map<String, Object> analysis) {

        Object hotspots =
                analysis.get("hotspots");

        if (!(hotspots instanceof List<?>)) {
            return "No hotspot information is currently available.";
        }

        List<?> list = (List<?>) hotspots;

        if (list.isEmpty()) {
            return "No repository hotspots were detected.";
        }

        StringBuilder answer = new StringBuilder();

        answer.append(
                "The repository has "
        );

        answer.append(list.size());

        answer.append(
                " detected hotspot"
        );

        if (list.size() != 1) {
            answer.append("s");
        }

        answer.append(
                ". These files deserve attention because their "
                        + "complexity, size, change frequency, churn, "
                        + "or maintenance indicators are relatively high."
        );

        return answer.toString();
    }

    private String answerMaintenance(
            Map<String, Object> analysis) {

        Object candidates =
                analysis.get("maintenanceCandidates");

        if (!(candidates instanceof List<?>)) {
            return "No maintenance candidate information is currently available.";
        }

        List<?> list = (List<?>) candidates;

        if (list.isEmpty()) {
            return "No significant maintenance candidates were detected.";
        }

        return "The analysis identified "
                + list.size()
                + " maintenance candidate"
                + (list.size() == 1 ? "" : "s")
                + ". These files should be reviewed for refactoring, "
                + "complexity reduction, or technical-debt improvement.";
    }

    private String answerComplexity(
            Map<String, Object> analysis) {

        Map<String, Object> fileIntelligence =
                extractMap(
                        analysis,
                        "fileIntelligence"
                );

        Object value =
                fileIntelligence.get("totalComplexity");

        if (value != null) {
            return "The repository source analysis reports a total "
                    + "complexity of "
                    + value
                    + ". Higher-complexity files should be reviewed "
                    + "alongside their size and change frequency.";
        }

        return "Complexity information is available in the repository "
                + "source analysis, but a total complexity value was not found.";
    }

    private String answerChurn(
            Map<String, Object> analysis) {

        Map<String, Object> gitAnalytics =
                extractMap(
                        analysis,
                        "gitAnalytics"
                );

        Object changeFrequency =
                gitAnalytics.get("fileChangeFrequency");

        if (changeFrequency != null) {
            return "The repository contains file-change frequency "
                    + "information. Files with repeated changes and high "
                    + "line churn can indicate areas that deserve closer "
                    + "maintenance attention.";
        }

        return "Git change and churn information is available in the "
                + "repository analysis.";
    }

    private String answerContributors(
            Map<String, Object> analysis) {

        Map<String, Object> gitAnalytics =
                extractMap(
                        analysis,
                        "gitAnalytics"
                );

        Object contributors =
                gitAnalytics.get("contributors");

        if (contributors instanceof List<?>) {

            List<?> list =
                    (List<?>) contributors;

            return "The repository analysis contains contributor "
                    + "activity for "
                    + list.size()
                    + " contributor"
                    + (list.size() == 1 ? "" : "s")
                    + ".";
        }

        return "Contributor activity is available in the Git analytics "
                + "section of the repository analysis.";
    }

    private String answerRepository(
            Map<String, Object> analysis) {

        Map<String, Object> repositoryInfo =
                extractMap(
                        analysis,
                        "repositoryInfo"
                );

        Map<String, Object> gitSummary =
                extractMap(
                        analysis,
                        "gitSummary"
                );

        Object repositoryName =
                repositoryInfo.get("repositoryName");

        Object branch =
                repositoryInfo.get("currentBranch");

        Object commitCount =
                gitSummary.get("commitCount");

        StringBuilder answer =
                new StringBuilder();

        answer.append("Repository overview: ");

        if (repositoryName != null) {
            answer.append(repositoryName);
        } else {
            answer.append("repository");
        }

        if (branch != null) {
            answer.append(
                    " is currently analyzed on branch "
            );
            answer.append(branch);
        }

        if (commitCount != null) {
            answer.append(
                    " with "
            );
            answer.append(commitCount);
            answer.append(
                    " commit"
            );

            if (!"1".equals(String.valueOf(commitCount))) {
                answer.append("s");
            }
        }

        answer.append(".");

        return answer.toString();
    }

    private String answerGeneral(
            Map<String, Object> analysis) {

        return "I analyzed the repository and can answer questions "
                + "about repository health, hotspots, maintenance "
                + "candidates, complexity, Git changes, contributors, "
                + "and source-code structure. Try asking about one of "
                + "those areas.";
    }
}