package com.gitintelligence.code;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.List;
import java.util.Map;

public class JavaCodeAnalyzerTest {

    public static void main(String[] args) {

        String javaFilePath =
                "C:\\IGA_Work\\IntelligentGITAnalyzer\\src\\main\\java\\com\\gitintelligence\\git\\GitRepositoryAnalyzer.java";

        JavaCodeAnalyzer analyzer =
                new JavaCodeAnalyzer();

        try {

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "JAVA SOURCE CODE ANALYSIS"
            );

            System.out.println(
                    "======================================"
            );

            System.out.println();

            CompilationUnit compilationUnit =
                    analyzer.parseJavaFile(
                            javaFilePath
                    );

            System.out.println(
                    "Java file parsed successfully!"
            );

            System.out.println();

            // =================================================
            // BASIC INFORMATION
            // =================================================

            System.out.println(
                    "BASIC INFORMATION"
            );

            System.out.println(
                    "--------------------------------------"
            );

            System.out.println();

            System.out.println(
                    "Package: "
                            + analyzer.getPackageName(
                                    compilationUnit
                            )
            );

            System.out.println(
                    "Classes: "
                            + analyzer.getClassCount(
                                    compilationUnit
                            )
            );

            System.out.println(
                    "Methods: "
                            + analyzer.getMethodCount(
                                    compilationUnit
                            )
            );

            System.out.println(
                    "Fields: "
                            + analyzer.getFieldCount(
                                    compilationUnit
                            )
            );

            System.out.println(
                    "Imports: "
                            + analyzer.getImportCount(
                                    compilationUnit
                            )
            );

            System.out.println(
                    "Method Calls: "
                            + analyzer.getMethodCallCount(
                                    compilationUnit
                            )
            );

            System.out.println(
                    "Lines: "
                            + analyzer.getLineCount(
                                    compilationUnit
                            )
            );

            System.out.println();

            // =================================================
            // ACTUAL FIELDS
            // =================================================

            System.out.println(
                    "CLASS FIELDS"
            );

            System.out.println(
                    "--------------------------------------"
            );

            System.out.println();

            for (String field :
                    analyzer.getFieldNames(
                            compilationUnit
                    )) {

                System.out.println(
                        "  - " + field
                );
            }

            System.out.println();

            // =================================================
            // METHOD COMPLEXITY
            // =================================================

            System.out.println(
                    "METHOD COMPLEXITY"
            );

            System.out.println(
                    "--------------------------------------"
            );

            System.out.println();

            for (MethodDeclaration method :
                    compilationUnit.findAll(
                            MethodDeclaration.class
                    )) {

                int complexity =
                        analyzer.getMethodComplexity(
                                method
                        );

                System.out.println(
                        method.getNameAsString()
                                + " -> Complexity: "
                                + complexity
                                + " ("
                                + analyzer
                                .getComplexityCategory(
                                        complexity
                                )
                                + ")"
                );
            }

            System.out.println();

            System.out.println(
                    "Total Complexity: "
                            + analyzer.getTotalComplexity(
                                    compilationUnit
                            )
            );

            System.out.println();

            // =================================================
            // CLASS -> METHODS
            // =================================================

            System.out.println(
                    "CLASS -> METHODS"
            );

            System.out.println(
                    "--------------------------------------"
            );

            System.out.println();

            Map<String, List<String>>
                    classMethods =
                    analyzer.getClassMethods(
                            compilationUnit
                    );

            for (Map.Entry<String, List<String>> entry :
                    classMethods.entrySet()) {

                System.out.println(
                        "Class: "
                                + entry.getKey()
                );

                for (String method :
                        entry.getValue()) {

                    System.out.println(
                            "  - " + method
                    );
                }

                System.out.println();
            }

            // =================================================
            // IMPORTS
            // =================================================

            System.out.println(
                    "IMPORTS"
            );

            System.out.println(
                    "--------------------------------------"
            );

            System.out.println();

            for (String importName :
                    analyzer.getImports(
                            compilationUnit
                    )) {

                System.out.println(
                        "  - " + importName
                );
            }

            System.out.println();

            // =================================================
            // SUMMARY
            // =================================================

            System.out.println(
                    "JAVA FILE SUMMARY"
            );

            System.out.println(
                    "--------------------------------------"
            );

            System.out.println();

            Map<String, Object> summary =
                    analyzer.getJavaFileSummary(
                            compilationUnit
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

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "JAVA ANALYSIS COMPLETED SUCCESSFULLY"
            );

            System.out.println(
                    "======================================"
            );

        } catch (Exception e) {

            System.out.println();

            System.out.println(
                    "Java source analysis failed."
            );

            System.out.println();

            e.printStackTrace();
        }
    }
}