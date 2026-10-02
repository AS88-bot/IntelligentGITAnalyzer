package com.gitintelligence.code;

import java.util.List;
import java.util.Map;

public class RepositoryJavaScriptAnalyzerTest {

    public static void main(String[] args) {

        String repositoryPath =
                "C:\\IGA_Work\\ibelieveyou";

        RepositoryJavaScriptAnalyzer analyzer =
                new RepositoryJavaScriptAnalyzer();

        try {

            System.out.println(
                    "==================================================");

            System.out.println(
                    "TYPESCRIPT / JAVASCRIPT REPOSITORY ANALYSIS");

            System.out.println(
                    "==================================================");

            // ==================================================
            // SOURCE FILES
            // ==================================================

            System.out.println();
            System.out.println(
                    "SOURCE FILES DISCOVERED");

            System.out.println(
                    "--------------------------------------------------");

            List<Map<String, Object>> files =
                    analyzer.analyzeRepository(
                            repositoryPath);

            System.out.println(
                    "Total source files: "
                            + files.size());

            for (Map<String, Object> file :
                    files) {

                System.out.println();

                System.out.println(
                        "  - "
                                + file.get("file"));
            }

            // ==================================================
            // REPOSITORY SUMMARY
            // ==================================================

            System.out.println();
            System.out.println(
                    "REPOSITORY SUMMARY");

            System.out.println(
                    "--------------------------------------------------");

            Map<String, Object> summary =
                    analyzer.getRepositorySummary(
                            repositoryPath);

            printMap(summary);

            // ==================================================
            // FILE-BY-FILE ANALYSIS
            // ==================================================

            System.out.println();
            System.out.println(
                    "FILE-BY-FILE ANALYSIS");

            System.out.println(
                    "--------------------------------------------------");

            for (Map<String, Object> file :
                    files) {

                System.out.println();

                System.out.println(
                        "File: "
                                + file.get("file"));

                System.out.println();

                System.out.println(
                        "  Extension: "
                                + file.get("extension"));

                System.out.println(
                        "  Lines: "
                                + file.get("lines"));

                System.out.println(
                        "  Code lines: "
                                + file.get("codeLines"));

                System.out.println(
                        "  Comment lines: "
                                + file.get("commentLines"));

                System.out.println(
                        "  Blank lines: "
                                + file.get("blankLines"));

                System.out.println(
                        "  Functions: "
                                + file.get("functions"));

                System.out.println(
                        "  Imports: "
                                + file.get("imports"));

                System.out.println(
                        "  Exports: "
                                + file.get("exports"));

                System.out.println(
                        "  React components: "
                                + file.get(
                                        "reactComponents"));

                System.out.println(
                        "  TODOs: "
                                + file.get("todoCount"));

                System.out.println(
                        "  FIXMEs: "
                                + file.get("fixmeCount"));

                System.out.println(
                        "  Complexity: "
                                + file.get("complexity")
                                + " ("
                                + file.get(
                                        "complexityCategory")
                                + ")");

                System.out.println(
                        "  Source size: "
                                + file.get(
                                        "sourceSizeBytes")
                                + " bytes");
            }

            // ==================================================
            // COMPLETION
            // ==================================================

            System.out.println();
            System.out.println(
                    "==================================================");

            System.out.println(
                    "SOURCE CODE ANALYSIS COMPLETED SUCCESSFULLY");

            System.out.println(
                    "==================================================");

        } catch (Exception exception) {

            System.out.println();
            System.out.println(
                    "ANALYSIS FAILED");

            System.out.println(
                    "--------------------------------------------------");

            exception.printStackTrace();
        }
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
}