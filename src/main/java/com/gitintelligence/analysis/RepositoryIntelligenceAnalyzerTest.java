package com.gitintelligence.analysis;

import java.util.List;
import java.util.Map;

public class RepositoryIntelligenceAnalyzerTest {

    public static void main(String[] args) {

        String repositoryPath =
                "C:\\IGA_Work\\ibelieveyou";

        System.out.println();
        System.out.println(
                "==================================================");

        System.out.println(
                "REPOSITORY INTELLIGENCE ANALYSIS");

        System.out.println(
                "==================================================");

        RepositoryIntelligenceAnalyzer analyzer =
                new RepositoryIntelligenceAnalyzer();

        try {

            Map<String, Object> result =
                    analyzer.analyzeRepository(
                            repositoryPath);

            // -----------------------------------------------------
            // REPOSITORY INFORMATION
            // -----------------------------------------------------

            System.out.println();
            System.out.println(
                    "REPOSITORY INFORMATION");

            System.out.println(
                    "--------------------------------------------------");

            Map<String, Object> repositoryInfo =
                    castMap(
                            result.get(
                                    "repositoryInfo"));

            printMap(repositoryInfo);

            // -----------------------------------------------------
            // GIT SUMMARY
            // -----------------------------------------------------

            System.out.println();
            System.out.println(
                    "GIT SUMMARY");

            System.out.println(
                    "--------------------------------------------------");

            Map<String, Object> gitSummary =
                    castMap(
                            result.get(
                                    "gitSummary"));

            printMap(gitSummary);

            // -----------------------------------------------------
            // SOURCE SUMMARY
            // -----------------------------------------------------

            System.out.println();
            System.out.println(
                    "SOURCE CODE SUMMARY");

            System.out.println(
                    "--------------------------------------------------");

            Map<String, Object> sourceSummary =
                    castMap(
                            result.get(
                                    "sourceSummary"));

            printMap(sourceSummary);

            // -----------------------------------------------------
            // FILE INTELLIGENCE
            // -----------------------------------------------------

            System.out.println();
            System.out.println(
                    "FILE INTELLIGENCE");

            System.out.println(
                    "--------------------------------------------------");

            List<Map<String, Object>> files =
                    castList(
                            result.get(
                                    "fileIntelligence"));

            for (Map<String, Object> file :
                    files) {

                printFile(file);
            }

            // -----------------------------------------------------
            // HOTSPOTS
            // -----------------------------------------------------

            System.out.println();
            System.out.println(
                    "TOP CODE HOTSPOTS");

            System.out.println(
                    "--------------------------------------------------");

            List<Map<String, Object>> hotspots =
                    castList(
                            result.get(
                                    "hotspots"));

            int hotspotLimit =
                    Math.min(
                            10,
                            hotspots.size());

            for (int i = 0;
                    i < hotspotLimit;
                    i++) {

                Map<String, Object> file =
                        hotspots.get(i);

                System.out.println(
                        (i + 1)
                                + ". "
                                + file.get("file"));

                System.out.println(
                        "   Hotspot Score: "
                                + file.get(
                                        "hotspotScore"));

                System.out.println(
                        "   Category: "
                                + file.get(
                                        "hotspotCategory"));

                System.out.println(
                        "   Lines: "
                                + file.get(
                                        "lines"));

                System.out.println(
                        "   Functions: "
                                + file.get(
                                        "functions"));

                System.out.println(
                        "   Complexity: "
                                + file.get(
                                        "complexity"));

                System.out.println(
                        "   Git Changes: "
                                + file.get(
                                        "gitChangeCount"));

                System.out.println(
                        "   Git Churn: "
                                + file.get(
                                        "gitChurn"));

                System.out.println(
                        "   Reasons: "
                                + file.get(
                                        "hotspotReasons"));

                System.out.println();
            }

            // -----------------------------------------------------
            // MAINTENANCE CANDIDATES
            // -----------------------------------------------------

            System.out.println(
                    "MAINTENANCE CANDIDATES");

            System.out.println(
                    "--------------------------------------------------");

            List<Map<String, Object>> maintenanceCandidates =
                    castList(
                            result.get(
                                    "maintenanceCandidates"));

            if (maintenanceCandidates.isEmpty()) {

                System.out.println(
                        "No maintenance candidates detected.");

            } else {

                int limit =
                        Math.min(
                                10,
                                maintenanceCandidates.size());

                for (int i = 0;
                        i < limit;
                        i++) {

                    Map<String, Object> file =
                            maintenanceCandidates.get(i);

                    System.out.println(
                            (i + 1)
                                    + ". "
                                    + file.get(
                                            "file"));

                    System.out.println(
                            "   Maintenance Score: "
                                    + file.get(
                                            "maintenanceScore"));

                    System.out.println(
                            "   Category: "
                                    + file.get(
                                            "maintenanceCategory"));

                    System.out.println(
                            "   Lines: "
                                    + file.get(
                                            "lines"));

                    System.out.println(
                            "   Functions: "
                                    + file.get(
                                            "functions"));

                    System.out.println(
                            "   Complexity: "
                                    + file.get(
                                            "complexity"));

                    System.out.println(
                            "   Git Churn: "
                                    + file.get(
                                            "gitChurn"));

                    System.out.println();
                }
            }

            // -----------------------------------------------------
            // REPOSITORY HEALTH
            // -----------------------------------------------------

            System.out.println(
                    "REPOSITORY HEALTH INDICATORS");

            System.out.println(
                    "--------------------------------------------------");

            Map<String, Object> health =
                    castMap(
                            result.get(
                                    "repositoryHealth"));

            printMap(health);

            // -----------------------------------------------------
            // COMPLETED
            // -----------------------------------------------------

            System.out.println();
            System.out.println(
                    "==================================================");

            System.out.println(
                    "REPOSITORY INTELLIGENCE ANALYSIS COMPLETED SUCCESSFULLY");

            System.out.println(
                    "==================================================");

        } catch (Exception exception) {

            System.out.println();
            System.out.println(
                    "REPOSITORY INTELLIGENCE ANALYSIS FAILED");

            System.out.println(
                    "--------------------------------------------------");

            System.out.println(
                    "Error: "
                            + exception.getMessage());

            exception.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(
            Object value) {

        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(
            Object value) {

        return (List<Map<String, Object>>) value;
    }

    private static void printMap(
            Map<String, Object> map) {

        for (Map.Entry<String, Object> entry :
                map.entrySet()) {

            System.out.println(
                    entry.getKey()
                            + ": "
                            + entry.getValue());
        }
    }

    private static void printFile(
            Map<String, Object> file) {

        System.out.println(
                "File: "
                        + file.get("file"));

        System.out.println(
                "  Lines: "
                        + file.get("lines"));

        System.out.println(
                "  Code lines: "
                        + file.get("codeLines"));

        System.out.println(
                "  Functions: "
                        + file.get("functions"));

        System.out.println(
                "  Complexity: "
                        + file.get("complexity")
                        + " ("
                        + file.get(
                                "complexityCategory")
                        + ")");

        System.out.println(
                "  React components: "
                        + file.get(
                                "reactComponents"));

        System.out.println(
                "  Git changes: "
                        + file.get(
                                "gitChangeCount"));

        System.out.println(
                "  Git lines added: "
                        + file.get(
                                "gitLinesAdded"));

        System.out.println(
                "  Git lines removed: "
                        + file.get(
                                "gitLinesRemoved"));

        System.out.println(
                "  Git churn: "
                        + file.get(
                                "gitChurn"));

        System.out.println(
                "  Hotspot score: "
                        + file.get(
                                "hotspotScore"));

        System.out.println(
                "  Hotspot category: "
                        + file.get(
                                "hotspotCategory"));

        System.out.println(
                "  Maintenance score: "
                        + file.get(
                                "maintenanceScore"));

        System.out.println(
                "  Maintenance category: "
                        + file.get(
                                "maintenanceCategory"));

        System.out.println();
    }
}