package com.gitintelligence.code;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JavaCodeAnalyzer {

    // =========================================================
    // PARSE JAVA FILE
    // =========================================================

    public CompilationUnit parseJavaFile(
            String filePath) throws Exception {

        File file = new File(filePath);

        if (!file.exists()) {
            throw new IllegalArgumentException(
                    "Java file not found: " + filePath
            );
        }

        if (!file.getName().endsWith(".java")) {
            throw new IllegalArgumentException(
                    "The specified file is not a Java file: "
                            + filePath
            );
        }

        return StaticJavaParser.parse(file);
    }

    // =========================================================
    // PACKAGE
    // =========================================================

    public String getPackageName(
            CompilationUnit compilationUnit) {

        return compilationUnit
                .getPackageDeclaration()
                .map(packageDeclaration ->
                        packageDeclaration
                                .getNameAsString())
                .orElse("[default package]");
    }

    // =========================================================
    // CLASSES
    // =========================================================

    public List<String> getClassNames(
            CompilationUnit compilationUnit) {

        List<String> classes =
                new ArrayList<>();

        for (ClassOrInterfaceDeclaration declaration :
                compilationUnit.findAll(
                        ClassOrInterfaceDeclaration.class
                )) {

            classes.add(
                    declaration.getNameAsString()
            );
        }

        return classes;
    }

    public int getClassCount(
            CompilationUnit compilationUnit) {

        return compilationUnit
                .findAll(
                        ClassOrInterfaceDeclaration.class
                )
                .size();
    }

    // =========================================================
    // METHODS
    // =========================================================

    public List<String> getMethodNames(
            CompilationUnit compilationUnit) {

        List<String> methods =
                new ArrayList<>();

        for (MethodDeclaration method :
                compilationUnit.findAll(
                        MethodDeclaration.class
                )) {

            methods.add(
                    method.getNameAsString()
            );
        }

        return methods;
    }

    public int getMethodCount(
            CompilationUnit compilationUnit) {

        return compilationUnit
                .findAll(
                        MethodDeclaration.class
                )
                .size();
    }

    public List<String> getMethodSignatures(
            CompilationUnit compilationUnit) {

        List<String> signatures =
                new ArrayList<>();

        for (MethodDeclaration method :
                compilationUnit.findAll(
                        MethodDeclaration.class
                )) {

            signatures.add(
                    method.getDeclarationAsString(
                            false,
                            false,
                            false
                    )
            );
        }

        return signatures;
    }

    // =========================================================
    // IMPORTS
    // =========================================================

    public List<String> getImports(
            CompilationUnit compilationUnit) {

        List<String> imports =
                new ArrayList<>();

        for (ImportDeclaration importDeclaration :
                compilationUnit.getImports()) {

            imports.add(
                    importDeclaration
                            .getNameAsString()
            );
        }

        return imports;
    }

    public int getImportCount(
            CompilationUnit compilationUnit) {

        return compilationUnit
                .getImports()
                .size();
    }

    // =========================================================
    // ACTUAL CLASS FIELDS
    // =========================================================

    public List<String> getFieldNames(
            CompilationUnit compilationUnit) {

        List<String> fields =
                new ArrayList<>();

        for (FieldDeclaration fieldDeclaration :
                compilationUnit.findAll(
                        FieldDeclaration.class
                )) {

            fieldDeclaration
                    .getVariables()
                    .forEach(variable ->
                            fields.add(
                                    variable
                                            .getNameAsString()
                            ));
        }

        return fields;
    }

    public int getFieldCount(
            CompilationUnit compilationUnit) {

        int count = 0;

        for (FieldDeclaration fieldDeclaration :
                compilationUnit.findAll(
                        FieldDeclaration.class
                )) {

            count +=
                    fieldDeclaration
                            .getVariables()
                            .size();
        }

        return count;
    }

    // =========================================================
    // METHOD CALLS
    // =========================================================

    public List<String> getMethodCalls(
            CompilationUnit compilationUnit) {

        List<String> calls =
                new ArrayList<>();

        for (MethodCallExpr call :
                compilationUnit.findAll(
                        MethodCallExpr.class
                )) {

            calls.add(
                    call.getNameAsString()
            );
        }

        return calls;
    }

    public int getMethodCallCount(
            CompilationUnit compilationUnit) {

        return compilationUnit
                .findAll(
                        MethodCallExpr.class
                )
                .size();
    }

    // =========================================================
    // SOURCE SIZE
    // =========================================================

    public int getLineCount(
            CompilationUnit compilationUnit) {

        return compilationUnit
                .getRange()
                .map(range ->
                        range.end.line -
                                range.begin.line +
                                1)
                .orElse(0);
    }

    // =========================================================
    // CLASS -> METHODS
    // =========================================================

    public Map<String, List<String>>
    getClassMethods(
            CompilationUnit compilationUnit) {

        Map<String, List<String>> result =
                new LinkedHashMap<>();

        for (ClassOrInterfaceDeclaration classDeclaration :
                compilationUnit.findAll(
                        ClassOrInterfaceDeclaration.class
                )) {

            List<String> methods =
                    new ArrayList<>();

            for (MethodDeclaration method :
                    classDeclaration.getMethods()) {

                methods.add(
                        method.getNameAsString()
                );
            }

            result.put(
                    classDeclaration
                            .getNameAsString(),
                    methods
            );
        }

        return result;
    }

    // =========================================================
    // CYCLOMATIC COMPLEXITY
    // =========================================================

    public int getMethodComplexity(
            MethodDeclaration method) {

        int complexity = 1;

        complexity +=
                method.findAll(
                        IfStmt.class
                ).size();

        complexity +=
                method.findAll(
                        ForStmt.class
                ).size();

        complexity +=
                method.findAll(
                        ForEachStmt.class
                ).size();

        complexity +=
                method.findAll(
                        WhileStmt.class
                ).size();

        complexity +=
                method.findAll(
                        DoStmt.class
                ).size();

        complexity +=
                method.findAll(
                        CatchClause.class
                ).size();

        for (SwitchEntry switchEntry :
                method.findAll(
                        SwitchEntry.class
                )) {

            if (!switchEntry.getLabels().isEmpty()) {
                complexity++;
            }
        }

        return complexity;
    }

    public Map<String, Integer>
    getMethodComplexities(
            CompilationUnit compilationUnit) {

        Map<String, Integer> result =
                new LinkedHashMap<>();

        for (MethodDeclaration method :
                compilationUnit.findAll(
                        MethodDeclaration.class
                )) {

            String methodName =
                    method.getNameAsString();

            String key =
                    methodName;

            if (result.containsKey(key)) {
                key =
                        method.getDeclarationAsString(
                                false,
                                false,
                                false
                        );
            }

            result.put(
                    key,
                    getMethodComplexity(method)
            );
        }

        return result;
    }

    public int getTotalComplexity(
            CompilationUnit compilationUnit) {

        int total = 0;

        for (MethodDeclaration method :
                compilationUnit.findAll(
                        MethodDeclaration.class
                )) {

            total +=
                    getMethodComplexity(method);
        }

        return total;
    }

    // =========================================================
    // COMPLEXITY CATEGORY
    // =========================================================

    public String getComplexityCategory(
            int complexity) {

        if (complexity <= 5) {
            return "LOW";
        }

        if (complexity <= 10) {
            return "MODERATE";
        }

        if (complexity <= 20) {
            return "HIGH";
        }

        return "VERY HIGH";
    }

    // =========================================================
    // JAVA FILE SUMMARY
    // =========================================================

    public Map<String, Object>
    getJavaFileSummary(
            CompilationUnit compilationUnit) {

        Map<String, Object> summary =
                new LinkedHashMap<>();

        summary.put(
                "package",
                getPackageName(
                        compilationUnit
                )
        );

        summary.put(
                "classes",
                getClassNames(
                        compilationUnit
                )
        );

        summary.put(
                "classCount",
                getClassCount(
                        compilationUnit
                )
        );

        summary.put(
                "methods",
                getMethodNames(
                        compilationUnit
                )
        );

        summary.put(
                "methodCount",
                getMethodCount(
                        compilationUnit
                )
        );

        summary.put(
                "fields",
                getFieldNames(
                        compilationUnit
                )
        );

        summary.put(
                "fieldCount",
                getFieldCount(
                        compilationUnit
                )
        );

        summary.put(
                "imports",
                getImports(
                        compilationUnit
                )
        );

        summary.put(
                "importCount",
                getImportCount(
                        compilationUnit
                )
        );

        summary.put(
                "methodCallCount",
                getMethodCallCount(
                        compilationUnit
                )
        );

        summary.put(
                "lineCount",
                getLineCount(
                        compilationUnit
                )
        );

        summary.put(
                "totalComplexity",
                getTotalComplexity(
                        compilationUnit
                )
        );

        summary.put(
                "methodComplexities",
                getMethodComplexities(
                        compilationUnit
                )
        );

        summary.put(
                "classMethods",
                getClassMethods(
                        compilationUnit
                )
        );

        return summary;
    }
}