package com.gitintelligence.code;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RepositoryJavaAnalyzer {

    private final JavaCodeAnalyzer codeAnalyzer;

    public RepositoryJavaAnalyzer() {
        this.codeAnalyzer = new JavaCodeAnalyzer();
    }

    // ============================================================
    // JAVA FILE DISCOVERY
    // ============================================================

    public List<File> findJavaFiles(String repositoryPath) {

        File repository =
                new File(repositoryPath);

        if (!repository.exists()) {
            throw new IllegalArgumentException(
                    "Repository path does not exist: "
                            + repositoryPath
            );
        }

        if (!repository.isDirectory()) {
            throw new IllegalArgumentException(
                    "Repository path is not a directory: "
                            + repositoryPath
            );
        }

        List<File> javaFiles =
                new ArrayList<>();

        collectJavaFiles(
                repository,
                javaFiles
        );

        return javaFiles;
    }

    private void collectJavaFiles(
            File directory,
            List<File> javaFiles) {

        File[] files =
                directory.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {

            if (file.isDirectory()) {

                String name =
                        file.getName();

                // Ignore generated/build/dependency folders
                if (name.equals(".git")
                        || name.equals("node_modules")
                        || name.equals("target")
                        || name.equals("build")
                        || name.equals("dist")
                        || name.equals(".idea")) {

                    continue;
                }

                collectJavaFiles(
                        file,
                        javaFiles
                );

            } else if (
                    file.getName().endsWith(".java")) {

                javaFiles.add(file);
            }
        }
    }

    // ============================================================
    // FILE ANALYSIS
    // ============================================================

    public Map<String, Object> analyzeJavaFile(
            File file) throws Exception {

        CompilationUnit compilationUnit =
                codeAnalyzer.parseJavaFile(
                        file.getAbsolutePath()
                );

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put(
                "file",
                file.getAbsolutePath()
        );

        result.put(
                "relativePath",
                file.getPath()
        );

        result.put(
                "package",
                codeAnalyzer.getPackageName(
                        compilationUnit
                )
        );

        result.put(
                "classes",
                codeAnalyzer.getClassNames(
                        compilationUnit
                )
        );

        result.put(
                "classCount",
                codeAnalyzer.getClassCount(
                        compilationUnit
                )
        );

        result.put(
                "interfaceCount",
                getInterfaceCount(
                        compilationUnit
                )
        );

        result.put(
                "methods",
                codeAnalyzer.getMethodNames(
                        compilationUnit
                )
        );

        result.put(
                "methodCount",
                codeAnalyzer.getMethodCount(
                        compilationUnit
                )
        );

        result.put(
                "fields",
                codeAnalyzer.getFieldNames(
                        compilationUnit
                )
        );

        result.put(
                "fieldCount",
                codeAnalyzer.getFieldCount(
                        compilationUnit
                )
        );

        result.put(
                "imports",
                codeAnalyzer.getImports(
                        compilationUnit
                )
        );

        result.put(
                "importCount",
                codeAnalyzer.getImportCount(
                        compilationUnit
                )
        );

        result.put(
                "methodCallCount",
                codeAnalyzer.getMethodCallCount(
                        compilationUnit
                )
        );

        result.put(
                "lineCount",
                codeAnalyzer.getLineCount(
                        compilationUnit
                )
        );

        result.put(
                "totalComplexity",
                codeAnalyzer.getTotalComplexity(
                        compilationUnit
                )
        );

        result.put(
                "methodComplexities",
                codeAnalyzer.getMethodComplexities(
                        compilationUnit
                )
        );

        result.put(
                "classMethods",
                codeAnalyzer.getClassMethods(
                        compilationUnit
                )
        );

        result.put(
                "todoCount",
                getTodoCount(
                        compilationUnit
                )
        );

        result.put(
                "fixmeCount",
                getFixmeCount(
                        compilationUnit
                )
        );

        result.put(
                "commentCount",
                getCommentCount(
                        compilationUnit
                )
        );

        result.put(
                "longMethodCount",
                getLongMethodCount(
                        compilationUnit
                )
        );

        result.put(
                "emptyMethodCount",
                getEmptyMethodCount(
                        compilationUnit
                )
        );

        return result;
    }

    // ============================================================
    // INTERFACES
    // ============================================================

    public int getInterfaceCount(
            CompilationUnit compilationUnit) {

        int count = 0;

        for (ClassOrInterfaceDeclaration declaration :
                compilationUnit.findAll(
                        ClassOrInterfaceDeclaration.class)) {

            if (declaration.isInterface()) {
                count++;
            }
        }

        return count;
    }

    // ============================================================
    // TODO / FIXME
    // ============================================================

    public int getTodoCount(
            CompilationUnit compilationUnit) {

        int count = 0;

        for (Comment comment :
                compilationUnit.getAllContainedComments()) {

            String text =
                    comment.getContent()
                            .toUpperCase();

            if (text.contains("TODO")) {
                count++;
            }
        }

        return count;
    }

    public int getFixmeCount(
            CompilationUnit compilationUnit) {

        int count = 0;

        for (Comment comment :
                compilationUnit.getAllContainedComments()) {

            String text =
                    comment.getContent()
                            .toUpperCase();

            if (text.contains("FIXME")) {
                count++;
            }
        }

        return count;
    }

    public int getCommentCount(
            CompilationUnit compilationUnit) {

        return compilationUnit
                .getAllContainedComments()
                .size();
    }

    // ============================================================
    // LONG METHODS
    // ============================================================

    public int getLongMethodCount(
            CompilationUnit compilationUnit) {

        int count = 0;

        for (MethodDeclaration method :
                compilationUnit.findAll(
                        MethodDeclaration.class)) {

            int lines =
                    method.getRange()
                            .map(range ->
                                    range.end.line
                                            - range.begin.line
                                            + 1)
                            .orElse(0);

            if (lines > 50) {
                count++;
            }
        }

        return count;
    }

    public int getEmptyMethodCount(
            CompilationUnit compilationUnit) {

        int count = 0;

        for (MethodDeclaration method :
                compilationUnit.findAll(
                        MethodDeclaration.class)) {

            if (method.getBody().isPresent()
                    && method.getBody()
                            .get()
                            .getStatements()
                            .isEmpty()) {

                count++;
            }
        }

        return count;
    }

    // ============================================================
    // REPOSITORY ANALYSIS
    // ============================================================

    public List<Map<String, Object>> analyzeRepository(
            String repositoryPath) throws Exception {

        List<File> javaFiles =
                findJavaFiles(
                        repositoryPath
                );

        List<Map<String, Object>> results =
                new ArrayList<>();

        for (File file : javaFiles) {

            try {

                Map<String, Object> result =
                        analyzeJavaFile(file);

                result.put(
                        "relativePath",
                        getRelativePath(
                                repositoryPath,
                                file
                        )
                );

                results.add(result);

            } catch (Exception e) {

                Map<String, Object> error =
                        new LinkedHashMap<>();

                error.put(
                        "file",
                        file.getAbsolutePath()
                );

                error.put(
                        "error",
                        e.getMessage()
                );

                results.add(error);
            }
        }

        return results;
    }

    // ============================================================
    // REPOSITORY SUMMARY
    // ============================================================

    public Map<String, Object> getRepositorySummary(
            String repositoryPath) throws Exception {

        List<Map<String, Object>> files =
                analyzeRepository(
                        repositoryPath
                );

        Map<String, Object> summary =
                new LinkedHashMap<>();

        int javaFiles = 0;
        int classes = 0;
        int interfaces = 0;
        int methods = 0;
        int fields = 0;
        int imports = 0;
        int methodCalls = 0;
        int lines = 0;
        int complexity = 0;
        int todos = 0;
        int fixmes = 0;
        int comments = 0;
        int longMethods = 0;
        int emptyMethods = 0;
        int failedFiles = 0;

        for (Map<String, Object> file :
                files) {

            if (file.containsKey("error")) {
                failedFiles++;
                continue;
            }

            javaFiles++;

            classes += getInt(
                    file,
                    "classCount"
            );

            interfaces += getInt(
                    file,
                    "interfaceCount"
            );

            methods += getInt(
                    file,
                    "methodCount"
            );

            fields += getInt(
                    file,
                    "fieldCount"
            );

            imports += getInt(
                    file,
                    "importCount"
            );

            methodCalls += getInt(
                    file,
                    "methodCallCount"
            );

            lines += getInt(
                    file,
                    "lineCount"
            );

            complexity += getInt(
                    file,
                    "totalComplexity"
            );

            todos += getInt(
                    file,
                    "todoCount"
            );

            fixmes += getInt(
                    file,
                    "fixmeCount"
            );

            comments += getInt(
                    file,
                    "commentCount"
            );

            longMethods += getInt(
                    file,
                    "longMethodCount"
            );

            emptyMethods += getInt(
                    file,
                    "emptyMethodCount"
            );
        }

        summary.put(
                "repositoryPath",
                repositoryPath
        );

        summary.put(
                "javaFiles",
                javaFiles
        );

        summary.put(
                "classes",
                classes
        );

        summary.put(
                "interfaces",
                interfaces
        );

        summary.put(
                "methods",
                methods
        );

        summary.put(
                "fields",
                fields
        );

        summary.put(
                "imports",
                imports
        );

        summary.put(
                "methodCalls",
                methodCalls
        );

        summary.put(
                "lines",
                lines
        );

        summary.put(
                "totalComplexity",
                complexity
        );

        summary.put(
                "todoCount",
                todos
        );

        summary.put(
                "fixmeCount",
                fixmes
        );

        summary.put(
                "commentCount",
                comments
        );

        summary.put(
                "longMethodCount",
                longMethods
        );

        summary.put(
                "emptyMethodCount",
                emptyMethods
        );

        summary.put(
                "failedFiles",
                failedFiles
        );

        if (javaFiles > 0) {

            summary.put(
                    "averageLinesPerFile",
                    (double) lines / javaFiles
            );

            summary.put(
                    "averageMethodsPerFile",
                    (double) methods / javaFiles
            );

            summary.put(
                    "averageComplexityPerFile",
                    (double) complexity / javaFiles
            );
        }

        return summary;
    }

    // ============================================================
    // COMPLEX FILES
    // ============================================================

    public List<Map<String, Object>> getMostComplexFiles(
            String repositoryPath,
            int limit) throws Exception {

        List<Map<String, Object>> files =
                analyzeRepository(
                        repositoryPath
                );

        files.removeIf(
                file -> file.containsKey("error")
        );

        files.sort(
                Comparator.comparingInt(
                        file ->
                                -getInt(
                                        file,
                                        "totalComplexity"
                                )
                )
        );

        return limitList(
                files,
                limit
        );
    }

    // ============================================================
    // LARGEST FILES
    // ============================================================

    public List<Map<String, Object>> getLargestJavaFiles(
            String repositoryPath,
            int limit) throws Exception {

        List<Map<String, Object>> files =
                analyzeRepository(
                        repositoryPath
                );

        files.removeIf(
                file -> file.containsKey("error")
        );

        files.sort(
                Comparator.comparingInt(
                        file ->
                                -getInt(
                                        file,
                                        "lineCount"
                                )
                )
        );

        return limitList(
                files,
                limit
        );
    }

    // ============================================================
    // MOST COMPLEX METHODS
    // ============================================================

    public List<Map<String, Object>> getMostComplexMethods(
            String repositoryPath,
            int limit) throws Exception {

        List<Map<String, Object>> methods =
                new ArrayList<>();

        List<File> javaFiles =
                findJavaFiles(
                        repositoryPath
                );

        for (File file :
                javaFiles) {

            CompilationUnit unit =
                    codeAnalyzer.parseJavaFile(
                            file.getAbsolutePath()
                    );

            for (MethodDeclaration method :
                    unit.findAll(
                            MethodDeclaration.class)) {

                Map<String, Object> data =
                        new LinkedHashMap<>();

                int complexity =
                        codeAnalyzer.getMethodComplexity(
                                method
                        );

                int lines =
                        method.getRange()
                                .map(range ->
                                        range.end.line
                                                - range.begin.line
                                                + 1)
                                .orElse(0);

                data.put(
                        "file",
                        getRelativePath(
                                repositoryPath,
                                file
                        )
                );

                data.put(
                        "method",
                        method.getNameAsString()
                );

                data.put(
                        "signature",
                        method.getDeclarationAsString(
                                false,
                                false,
                                false
                        )
                );

                data.put(
                        "complexity",
                        complexity
                );

                data.put(
                        "category",
                        codeAnalyzer
                                .getComplexityCategory(
                                        complexity
                                )
                );

                data.put(
                        "lines",
                        lines
                );

                methods.add(data);
            }
        }

        methods.sort(
                Comparator.comparingInt(
                        method ->
                                -getInt(
                                        method,
                                        "complexity"
                                )
                )
        );

        return limitList(
                methods,
                limit
        );
    }

    // ============================================================
    // PACKAGE STATISTICS
    // ============================================================

    public Map<String, Integer> getPackageStatistics(
            String repositoryPath) throws Exception {

        Map<String, Integer> result =
                new LinkedHashMap<>();

        List<Map<String, Object>> files =
                analyzeRepository(
                        repositoryPath
                );

        for (Map<String, Object> file :
                files) {

            if (file.containsKey("error")) {
                continue;
            }

            String packageName =
                    String.valueOf(
                            file.get("package")
                    );

            int current =
                    result.getOrDefault(
                            packageName,
                            0
                    );

            result.put(
                    packageName,
                    current + 1
            );
        }

        return sortDescending(result);
    }

    // ============================================================
    // IMPORT STATISTICS
    // ============================================================

    public Map<String, Integer> getImportStatistics(
            String repositoryPath) throws Exception {

        Map<String, Integer> result =
                new LinkedHashMap<>();

        List<Map<String, Object>> files =
                analyzeRepository(
                        repositoryPath
                );

        for (Map<String, Object> file :
                files) {

            if (file.containsKey("error")) {
                continue;
            }

            Object importsObject =
                    file.get("imports");

            if (!(importsObject instanceof List<?>)) {
                continue;
            }

            for (Object importObject :
                    (List<?>) importsObject) {

                String importName =
                        String.valueOf(
                                importObject
                        );

                result.put(
                        importName,
                        result.getOrDefault(
                                importName,
                                0
                        ) + 1
                );
            }
        }

        return sortDescending(result);
    }

    // ============================================================
    // TECHNICAL-DEBT CANDIDATES
    // ============================================================

    public List<Map<String, Object>>
    getTechnicalDebtCandidates(
            String repositoryPath) throws Exception {

        List<Map<String, Object>> candidates =
                new ArrayList<>();

        List<Map<String, Object>> files =
                analyzeRepository(
                        repositoryPath
                );

        for (Map<String, Object> file :
                files) {

            if (file.containsKey("error")) {
                continue;
            }

            int complexity =
                    getInt(
                            file,
                            "totalComplexity"
                    );

            int lines =
                    getInt(
                            file,
                            "lineCount"
                    );

            int methods =
                    getInt(
                            file,
                            "methodCount"
                    );

            int imports =
                    getInt(
                            file,
                            "importCount"
                    );

            int todos =
                    getInt(
                            file,
                            "todoCount"
                    );

            int fixmes =
                    getInt(
                            file,
                            "fixmeCount"
                    );

            int longMethods =
                    getInt(
                            file,
                            "longMethodCount"
                    );

            int score = 0;

            List<String> reasons =
                    new ArrayList<>();

            if (complexity > 20) {

                score += 3;

                reasons.add(
                        "High total complexity"
                );
            }

            if (lines > 500) {

                score += 3;

                reasons.add(
                        "Large Java file"
                );
            }

            if (methods > 30) {

                score += 2;

                reasons.add(
                        "Large number of methods"
                );
            }

            if (imports > 20) {

                score += 1;

                reasons.add(
                        "High import count"
                );
            }

            if (todos > 0) {

                score += 1;

                reasons.add(
                        "Contains TODO comments"
                );
            }

            if (fixmes > 0) {

                score += 2;

                reasons.add(
                        "Contains FIXME comments"
                );
            }

            if (longMethods > 0) {

                score += 2;

                reasons.add(
                        "Contains long methods"
                );
            }

            if (score > 0) {

                Map<String, Object> candidate =
                        new LinkedHashMap<>();

                candidate.put(
                        "file",
                        file.get("relativePath")
                );

                candidate.put(
                        "score",
                        score
                );

                candidate.put(
                        "complexity",
                        complexity
                );

                candidate.put(
                        "lines",
                        lines
                );

                candidate.put(
                        "methods",
                        methods
                );

                candidate.put(
                        "imports",
                        imports
                );

                candidate.put(
                        "todos",
                        todos
                );

                candidate.put(
                        "fixmes",
                        fixmes
                );

                candidate.put(
                        "longMethods",
                        longMethods
                );

                candidate.put(
                        "reasons",
                        reasons
                );

                candidates.add(candidate);
            }
        }

        candidates.sort(
                Comparator.comparingInt(
                        candidate ->
                                -getInt(
                                        candidate,
                                        "score"
                                )
                )
        );

        return candidates;
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private int getInt(
            Map<String, Object> map,
            String key) {

        Object value =
                map.get(key);

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        return 0;
    }

    private List<Map<String, Object>> limitList(
            List<Map<String, Object>> list,
            int limit) {

        if (limit <= 0) {
            return new ArrayList<>();
        }

        int end =
                Math.min(
                        limit,
                        list.size()
                );

        return new ArrayList<>(
                list.subList(
                        0,
                        end
                )
        );
    }

    private Map<String, Integer> sortDescending(
            Map<String, Integer> map) {

        List<Map.Entry<String, Integer>>
                entries =
                new ArrayList<>(
                        map.entrySet()
                );

        entries.sort(
                Map.Entry.<String, Integer>
                        comparingByValue()
                        .reversed()
        );

        Map<String, Integer> result =
                new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry :
                entries) {

            result.put(
                    entry.getKey(),
                    entry.getValue()
            );
        }

        return result;
    }

    private String getRelativePath(
            String repositoryPath,
            File file) {

        String repository =
                new File(
                        repositoryPath
                ).getAbsolutePath();

        String absolute =
                file.getAbsolutePath();

        if (absolute.startsWith(repository)) {

            String relative =
                    absolute.substring(
                            repository.length()
                    );

            if (relative.startsWith(
                    File.separator)) {

                relative =
                        relative.substring(1);
            }

            return relative;
        }

        return absolute;
    }
}