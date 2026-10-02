package com.gitintelligence.code;

import java.io.File;
import java.util.List;
import java.util.Map;

public class RepositoryJavaAnalyzerTest {

    public static void main(String[] args) {

        String repositoryPath =
                "C:\\IGA_Work\\ibelieveyou";

        RepositoryJavaAnalyzer analyzer =
                new RepositoryJavaAnalyzer();

        try {

            System.out.println(
                    "=============================================="
            );

            System.out.println(
                    "JAVA REPOSITORY ANALYSIS"
            );

            System.out.println(
                    "=============================================="
            );

            System.out.println();

            // ====================================================
            // DISCOVER JAVA FILES
            // ====================================================

            List<File> javaFiles =
                    analyzer.findJavaFiles(
                            repositoryPath
                    );

            System.out.println(
                    "JAVA FILES DISCOVERED"
            );

            System.out.println(
                    "----------------------------------------------"
            );

            System.out.println();

            System.out.println(
                    "Total Java files: "
                            + javaFiles.size()
            );

            System.out.println();

            for (File file :
                    javaFiles) {

                System.out.println(
                        "  - "
                                + file.getAbsolutePath()
                );
            }

            System.out.println();

            // ====================================================
            // REPOSITORY SUMMARY
            // ====================================================

            System.out.println(
                    "REPOSITORY SUMMARY"
            );

            System.out.println(
                    "----------------------------------------------"
            );

            System.out.println();

            Map<String, Object> summary =
                    analyzer.getRepositorySummary(
                            repositoryPath
                    );

            for (Map.Entry<String, Object> entry :
                    summary.entrySet()) {

                System.out.println(
                        entry.getKey()
                                + ": "
                                + entry.getValue()
                );
            }

            System.out.println();

            // ====================================================
            // FILE-BY-FILE ANALYSIS
            // ====================================================

            System.out.println(
                    "FILE-BY-FILE ANALYSIS"
            );

            System.out.println(
                    "----------------------------------------------"
            );

            System.out.println();

            List<Map<String, Object>> files =
                    analyzer.analyzeRepository(
                            repositoryPath
                    );

            for (Map<String, Object> file :
                    files) {

                System.out.println(
                        "File: "
                                + file.get(
                                        "relativePath"
                                )
                );

                if (file.containsKey("error")) {

                    System.out.println(
                            "  ERROR: "
                                    + file.get(
                                            "error"
                                    )
                    );

                    System.out.println();

                    continue;
                }

                System.out.println(
                        "  Package: "
                                + file.get(
                                        "package"
                                )
                );

                System.out.println(
                        "  Classes: "
                                + file.get(
                                        "classCount"
                                )
                );

                System.out.println(
                        "  Interfaces: "
                                + file.get(
                                        "interfaceCount"
                                )
                );

                System.out.println(
                        "  Methods: "
                                + file.get(
                                        "methodCount"
                                )
                );

                System.out.println(
                        "  Fields: "
                                + file.get(
                                        "fieldCount"
                                )
                );

                System.out.println(
                        "  Imports: "
                                + file.get(
                                        "importCount"
                                )
                );

                System.out.println(
                        "  Method Calls: "
                                + file.get(
                                        "methodCallCount"
                                )
                );

                System.out.println(
                        "  Lines: "
                                + file.get(
                                        "lineCount"
                                )
                );

                System.out.println(
                        "  Complexity: "
                                + file.get(
                                        "totalComplexity"
                                )
                );

                System.out.println(
                        "  TODOs: "
                                + file.get(
                                        "todoCount"
                                )
                );

                System.out.println(
                        "  FIXMEs: "
                                + file.get(
                                        "fixmeCount"
                                )
                );

                System.out.println(
                        "  Comments: "
                                + file.get(
                                        "commentCount"
                                )
                );

                System.out.println(
                        "  Long methods: "
                                + file.get(
                                        "longMethodCount"
                                )
                );

                System.out.println(
                        "  Empty methods: "
                                + file.get(
                                        "emptyMethodCount"
                                )
                );

                System.out.println();
            }

            // ====================================================
            // MOST COMPLEX FILES
            // ====================================================

            System.out.println(
                    "MOST COMPLEX JAVA FILES"
            );

            System.out.println(
                    "----------------------------------------------"
            );

            System.out.println();

            List<Map<String, Object>>
                    complexFiles =
                    analyzer.getMostComplexFiles(
                            repositoryPath,
                            10
                    );

            for (Map<String, Object> file :
                    complexFiles) {

                System.out.println(
                        file.get(
                                "relativePath"
                        )
                                + " -> Complexity: "
                                + file.get(
                                        "totalComplexity"
                                )
                );
            }

            System.out.println();

            // ====================================================
            // LARGEST JAVA FILES
            // ====================================================

            System.out.println(
                    "LARGEST JAVA FILES"
            );

            System.out.println(
                    "----------------------------------------------"
            );

            System.out.println();

            List<Map<String, Object>>
                    largestFiles =
                    analyzer.getLargestJavaFiles(
                            repositoryPath,
                            10
                    );

            for (Map<String, Object> file :
                    largestFiles) {

                System.out.println(
                        file.get(
                                "relativePath"
                        )
                                + " -> Lines: "
                                + file.get(
                                        "lineCount"
                                )
                );
            }

            System.out.println();

            // ====================================================
            // MOST COMPLEX METHODS
            // ====================================================

            System.out.println(
                    "MOST COMPLEX METHODS"
            );

            System.out.println(
                    "----------------------------------------------"
            );

            System.out.println();

            List<Map<String, Object>>
                    complexMethods =
                    analyzer.getMostComplexMethods(
                            repositoryPath,
                            20
                    );

            for (Map<String, Object> method :
                    complexMethods) {

                System.out.println(
                        method.get("file")
                                + " | "
                                + method.get("method")
                                + " | Complexity: "
                                + method.get("complexity")
                                + " | "
                                + method.get("category")
                                + " | Lines: "
                                + method.get("lines")
                );
            }

            System.out.println();

            // ====================================================
            // PACKAGE STATISTICS
            // ====================================================

            System.out.println(
                    "PACKAGE STATISTICS"
            );

            System.out.println(
                    "----------------------------------------------"
            );

            System.out.println();

            Map<String, Integer>
                    packages =
                    analyzer.getPackageStatistics(
                            repositoryPath
                    );

            for (Map.Entry<String, Integer> entry :
                    packages.entrySet()) {

                System.out.println(
                        "  "
                                + entry.getKey()
                                + " -> "
                                + entry.getValue()
                                + " files"
                );
            }

            System.out.println();

            // ====================================================
            // IMPORT STATISTICS
            // ====================================================

            System.out.println(
                    "IMPORT STATISTICS"
            );

            System.out.println(
                    "----------------------------------------------"
            );

            System.out.println();

            Map<String, Integer>
                    imports =
                    analyzer.getImportStatistics(
                            repositoryPath
                    );

            for (Map.Entry<String, Integer> entry :
                    imports.entrySet()) {

                System.out.println(
                        "  "
                                + entry.getKey()
                                + " -> "
                                + entry.getValue()
                );
            }

            System.out.println();

            // ====================================================
            // TECHNICAL-DEBT CANDIDATES
            // ====================================================

            System.out.println(
                    "TECHNICAL-DEBT CANDIDATES"
            );

            System.out.println(
                    "----------------------------------------------"
            );

            System.out.println();

            List<Map<String, Object>>
                    debtCandidates =
                    analyzer
                            .getTechnicalDebtCandidates(
                                    repositoryPath
                            );

            if (debtCandidates.isEmpty()) {

                System.out.println(
                        "No technical-debt candidates "
                                + "matched the configured thresholds."
                );

            } else {

                for (Map<String, Object> candidate :
                        debtCandidates) {

                    System.out.println(
                            "File: "
                                    + candidate.get(
                                            "file"
                                    )
                    );

                    System.out.println(
                            "  Score: "
                                    + candidate.get(
                                            "score"
                                    )
                    );

                    System.out.println(
                            "  Complexity: "
                                    + candidate.get(
                                            "complexity"
                                    )
                    );

                    System.out.println(
                            "  Lines: "
                                    + candidate.get(
                                            "lines"
                                    )
                    );

                    System.out.println(
                            "  Methods: "
                                    + candidate.get(
                                            "methods"
                                    )
                    );

                    System.out.println(
                            "  Imports: "
                                    + candidate.get(
                                            "imports"
                                    )
                    );

                    System.out.println(
                            "  TODOs: "
                                    + candidate.get(
                                            "todos"
                                    )
                    );

                    System.out.println(
                            "  FIXMEs: "
                                    + candidate.get(
                                            "fixmes"
                                    )
                    );

                    System.out.println(
                            "  Long methods: "
                                    + candidate.get(
                                            "longMethods"
                                    )
                    );

                    System.out.println(
                            "  Reasons: "
                                    + candidate.get(
                                            "reasons"
                                    )
                    );

                    System.out.println();
                }
            }

            // ====================================================
            // COMPLETED
            // ====================================================

            System.out.println(
                    "=============================================="
            );

            System.out.println(
                    "JAVA REPOSITORY ANALYSIS COMPLETED"
            );

            System.out.println(
                    "=============================================="
            );

        } catch (Exception e) {

            System.out.println();

            System.out.println(
                    "JAVA REPOSITORY ANALYSIS FAILED"
            );

            System.out.println();

            e.printStackTrace();
        }
    }
}