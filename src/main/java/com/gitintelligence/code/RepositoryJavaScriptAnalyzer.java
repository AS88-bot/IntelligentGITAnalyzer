package com.gitintelligence.code;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RepositoryJavaScriptAnalyzer {

    // ============================================================
    // FILE EXTENSIONS
    // ============================================================

    private static final String[] SOURCE_EXTENSIONS = {
            ".ts",
            ".tsx",
            ".js",
            ".jsx"
    };

    // ============================================================
    // DIRECTORIES TO IGNORE
    // ============================================================

    private static final String[] IGNORED_DIRECTORIES = {
            ".git",
            "node_modules",
            "dist",
            "build",
            "target",
            ".idea",
            ".next",
            "coverage"
    };

    // ============================================================
    // REGEX PATTERNS
    // ============================================================

    private static final Pattern FUNCTION_PATTERN =
            Pattern.compile(
                    "\\bfunction\\s+[A-Za-z_$][A-Za-z0-9_$]*\\s*\\("
            );

    private static final Pattern ARROW_FUNCTION_PATTERN =
            Pattern.compile(
                    "(?:const|let|var)\\s+[A-Za-z_$][A-Za-z0-9_$]*\\s*=\\s*(?:async\\s*)?(?:\\([^)]*\\)|[A-Za-z_$][A-Za-z0-9_$]*)\\s*=>"
            );

    private static final Pattern NAMED_ARROW_PATTERN =
            Pattern.compile(
                    "\\b[A-Za-z_$][A-Za-z0-9_$]*\\s*=\\s*(?:async\\s*)?(?:\\([^)]*\\)|[A-Za-z_$][A-Za-z0-9_$]*)\\s*=>"
            );

    private static final Pattern REACT_COMPONENT_FUNCTION_PATTERN =
            Pattern.compile(
                    "\\bfunction\\s+([A-Z][A-Za-z0-9_$]*)\\s*\\("
            );

    private static final Pattern REACT_COMPONENT_ARROW_PATTERN =
            Pattern.compile(
                    "(?:const|let|var)\\s+([A-Z][A-Za-z0-9_$]*)\\s*=\\s*(?:async\\s*)?(?:\\([^)]*\\)|[A-Za-z_$][A-Za-z0-9_$]*)\\s*=>"
            );

    // ============================================================
    // PUBLIC REPOSITORY ANALYSIS
    // ============================================================

    public List<Map<String, Object>> analyzeRepository(
            String repositoryPath) throws Exception {

        File repositoryDirectory =
                new File(repositoryPath);

        if (!repositoryDirectory.exists()) {
            throw new IllegalArgumentException(
                    "Repository path does not exist: "
                            + repositoryPath);
        }

        if (!repositoryDirectory.isDirectory()) {
            throw new IllegalArgumentException(
                    "Repository path is not a directory: "
                            + repositoryPath);
        }

        List<Path> sourceFiles =
                discoverSourceFiles(
                        repositoryDirectory.toPath());

        List<Map<String, Object>> analyses =
                new ArrayList<>();

        for (Path sourceFile : sourceFiles) {

            try {

                Map<String, Object> analysis =
                        analyzeFile(
                                repositoryDirectory.toPath(),
                                sourceFile);

                analyses.add(analysis);

            } catch (Exception exception) {

                Map<String, Object> failed =
                        new LinkedHashMap<>();

                String relativePath =
                        repositoryDirectory
                                .toPath()
                                .relativize(sourceFile)
                                .toString()
                                .replace(
                                        "\\",
                                        "/");

                failed.put(
                        "file",
                        relativePath);

                failed.put(
                        "path",
                        sourceFile.toString());

                failed.put(
                        "extension",
                        getExtension(
                                sourceFile.toString()));

                failed.put(
                        "lines",
                        0);

                failed.put(
                        "blankLines",
                        0);

                failed.put(
                        "commentLines",
                        0);

                failed.put(
                        "codeLines",
                        0);

                failed.put(
                        "functions",
                        0);

                failed.put(
                        "imports",
                        0);

                failed.put(
                        "exports",
                        0);

                failed.put(
                        "reactComponents",
                        0);

                failed.put(
                        "todoCount",
                        0);

                failed.put(
                        "fixmeCount",
                        0);

                failed.put(
                        "complexity",
                        0);

                failed.put(
                        "complexityCategory",
                        "UNKNOWN");

                failed.put(
                        "sourceSizeBytes",
                        0L);

                failed.put(
                        "failed",
                        true);

                failed.put(
                        "error",
                        exception.getMessage());

                analyses.add(failed);
            }
        }

        return analyses;
    }

    // ============================================================
    // COMPATIBILITY METHOD
    // ============================================================

    /*
     * This method is included so that other classes can use either:
     *
     * analyzeRepository(...)
     *
     * or:
     *
     * getFileAnalyses(...)
     *
     * without compilation problems.
     */

    public List<Map<String, Object>> getFileAnalyses(
            String repositoryPath) throws Exception {

        return analyzeRepository(repositoryPath);
    }

    // ============================================================
    // SOURCE FILE DISCOVERY
    // ============================================================

    private List<Path> discoverSourceFiles(
            Path repositoryDirectory) throws Exception {

        List<Path> result =
                new ArrayList<>();

        try (Stream<Path> stream =
                     Files.walk(repositoryDirectory)) {

            List<Path> paths =
                    stream
                            .filter(Files::isRegularFile)
                            .filter(this::isSourceFile)
                            .filter(path ->
                                    !isIgnoredPath(
                                            path))
                            .collect(
                                    Collectors.toList());

            result.addAll(paths);
        }

        result.sort(
                new Comparator<Path>() {

                    @Override
                    public int compare(
                            Path first,
                            Path second) {

                        return first.toString()
                                .compareToIgnoreCase(
                                        second.toString());
                    }
                });

        return result;
    }

    // ============================================================
    // SOURCE FILE CHECK
    // ============================================================

    private boolean isSourceFile(
            Path path) {

        String name =
                path.getFileName()
                        .toString()
                        .toLowerCase();

        for (String extension :
                SOURCE_EXTENSIONS) {

            if (name.endsWith(extension)) {
                return true;
            }
        }

        return false;
    }

    // ============================================================
    // IGNORED PATH
    // ============================================================

    private boolean isIgnoredPath(
            Path path) {

        for (Path part :
                path) {

            String directory =
                    part.toString();

            for (String ignored :
                    IGNORED_DIRECTORIES) {

                if (directory.equalsIgnoreCase(
                        ignored)) {

                    return true;
                }
            }
        }

        return false;
    }

    // ============================================================
    // FILE ANALYSIS
    // ============================================================

    public Map<String, Object> analyzeFile(
            Path repositoryDirectory,
            Path sourceFile) throws Exception {

        String content =
                Files.readString(
                        sourceFile,
                        StandardCharsets.UTF_8);

        String[] lines =
                content.split(
                        "\\R",
                        -1);

        int totalLines =
                lines.length;

        int blankLines = 0;
        int commentLines = 0;
        int codeLines = 0;

        boolean insideBlockComment =
                false;

        for (String line :
                lines) {

            String trimmed =
                    line.trim();

            if (trimmed.isEmpty()) {

                blankLines++;

                continue;
            }

            if (insideBlockComment) {

                commentLines++;

                if (trimmed.contains("*/")) {
                    insideBlockComment = false;
                }

                continue;
            }

            if (trimmed.startsWith("/*")) {

                commentLines++;

                if (!trimmed.contains("*/")
                        || trimmed.indexOf("/*")
                        == trimmed.lastIndexOf("/*")
                                && trimmed.indexOf("*/")
                                < trimmed.indexOf("/*")) {

                    insideBlockComment = true;
                }

                continue;
            }

            if (trimmed.startsWith("//")
                    || trimmed.startsWith("*")) {

                commentLines++;

                continue;
            }

            codeLines++;
        }

        int functions =
                countFunctions(content);

        int imports =
                countImports(lines);

        int exports =
                countExports(lines);

        int reactComponents =
                countReactComponents(content);

        int todoCount =
                countOccurrences(
                        content,
                        "TODO");

        int fixmeCount =
                countOccurrences(
                        content,
                        "FIXME");

        int complexity =
                calculateComplexity(
                        content);

        String relativePath =
                repositoryDirectory
                        .relativize(sourceFile)
                        .toString()
                        .replace(
                                "\\",
                                "/");

        Map<String, Object> result =
                new LinkedHashMap<>();

        /*
         * These keys are deliberately standardized.
         * RepositoryIntelligenceAnalyzer reads these exact keys.
         */

        result.put(
                "file",
                relativePath);

        result.put(
                "path",
                sourceFile.toString());

        result.put(
                "extension",
                getExtension(
                        sourceFile.toString()));

        result.put(
                "lines",
                totalLines);

        result.put(
                "blankLines",
                blankLines);

        result.put(
                "commentLines",
                commentLines);

        result.put(
                "codeLines",
                codeLines);

        result.put(
                "functions",
                functions);

        result.put(
                "imports",
                imports);

        result.put(
                "exports",
                exports);

        result.put(
                "reactComponents",
                reactComponents);

        result.put(
                "todoCount",
                todoCount);

        result.put(
                "fixmeCount",
                fixmeCount);

        result.put(
                "complexity",
                complexity);

        result.put(
                "complexityCategory",
                getComplexityCategory(
                        complexity));

        result.put(
                "sourceSizeBytes",
                Files.size(sourceFile));

        result.put(
                "failed",
                false);

        return result;
    }

    // ============================================================
    // FUNCTION COUNT
    // ============================================================

    private int countFunctions(
            String content) {

        int count = 0;

        Matcher functionMatcher =
                FUNCTION_PATTERN.matcher(
                        content);

        while (functionMatcher.find()) {
            count++;
        }

        Matcher arrowMatcher =
                ARROW_FUNCTION_PATTERN.matcher(
                        content);

        while (arrowMatcher.find()) {
            count++;
        }

        /*
         * Catch arrow functions that don't use const/let/var.
         * Avoid double-counting locations already counted above.
         */

        Matcher namedArrowMatcher =
                NAMED_ARROW_PATTERN.matcher(
                        content);

        while (namedArrowMatcher.find()) {

            int position =
                    namedArrowMatcher.start();

            String previousText =
                    content.substring(
                            Math.max(
                                    0,
                                    position - 20),
                            position);

            if (!previousText.contains(
                    "const ")
                    && !previousText.contains(
                            "let ")
                    && !previousText.contains(
                            "var ")) {

                count++;
            }
        }

        return count;
    }

    // ============================================================
    // IMPORT COUNT
    // ============================================================

    private int countImports(
            String[] lines) {

        int count = 0;

        for (String line :
                lines) {

            String trimmed =
                    line.trim();

            if (trimmed.startsWith("import ")
                    || trimmed.startsWith(
                            "import{")) {

                count++;

                continue;
            }

            if (trimmed.contains(
                    "require(")) {

                count++;
            }
        }

        return count;
    }

    // ============================================================
    // EXPORT COUNT
    // ============================================================

    private int countExports(
            String[] lines) {

        int count = 0;

        for (String line :
                lines) {

            String trimmed =
                    line.trim();

            if (trimmed.startsWith("export ")
                    || trimmed.startsWith(
                            "export{")) {

                count++;
            }
        }

        return count;
    }

    // ============================================================
    // REACT COMPONENT COUNT
    // ============================================================

    private int countReactComponents(
            String content) {

        int count = 0;

        Matcher functionMatcher =
                REACT_COMPONENT_FUNCTION_PATTERN
                        .matcher(content);

        while (functionMatcher.find()) {
            count++;
        }

        Matcher arrowMatcher =
                REACT_COMPONENT_ARROW_PATTERN
                        .matcher(content);

        while (arrowMatcher.find()) {

            String componentName =
                    arrowMatcher.group(1);

            if (componentName != null
                    && Character.isUpperCase(
                            componentName.charAt(0))) {

                count++;
            }
        }

        return count;
    }

    // ============================================================
    // COMPLEXITY
    // ============================================================

    private int calculateComplexity(
            String content) {

        /*
         * Lightweight heuristic.
         *
         * Starting complexity = 1.
         *
         * Adds complexity for:
         * if
         * for
         * while
         * switch
         * catch
         * &&
         * ||
         * ternary ?
         */

        int complexity = 1;

        complexity +=
                countKeyword(
                        content,
                        "if");

        complexity +=
                countKeyword(
                        content,
                        "for");

        complexity +=
                countKeyword(
                        content,
                        "while");

        complexity +=
                countKeyword(
                        content,
                        "switch");

        complexity +=
                countKeyword(
                        content,
                        "catch");

        complexity +=
                countOccurrences(
                        content,
                        "&&");

        complexity +=
                countOccurrences(
                        content,
                        "||");

        complexity +=
                countTernaryOperators(
                        content);

        return complexity;
    }

    // ============================================================
    // KEYWORD COUNT
    // ============================================================

    private int countKeyword(
            String content,
            String keyword) {

        Pattern pattern =
                Pattern.compile(
                        "\\b"
                                + Pattern.quote(keyword)
                                + "\\b");

        Matcher matcher =
                pattern.matcher(content);

        int count = 0;

        while (matcher.find()) {
            count++;
        }

        return count;
    }

    // ============================================================
    // OCCURRENCE COUNT
    // ============================================================

    private int countOccurrences(
            String content,
            String value) {

        if (content == null
                || value == null
                || value.isEmpty()) {

            return 0;
        }

        int count = 0;
        int position = 0;

        while (true) {

            int index =
                    content.indexOf(
                            value,
                            position);

            if (index < 0) {
                break;
            }

            count++;

            position =
                    index + value.length();
        }

        return count;
    }

    // ============================================================
    // TERNARY COUNT
    // ============================================================

    private int countTernaryOperators(
            String content) {

        int count = 0;

        for (int i = 0;
             i < content.length();
             i++) {

            char current =
                    content.charAt(i);

            if (current != '?') {
                continue;
            }

            /*
             * Ignore optional chaining:
             *
             * object?.property
             */

            if (i + 1 < content.length()
                    && content.charAt(i + 1)
                    == '.') {

                continue;
            }

            count++;
        }

        return count;
    }

    // ============================================================
    // COMPLEXITY CATEGORY
    // ============================================================

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

    // ============================================================
    // EXTENSION
    // ============================================================

    public String getExtension(
            String filePath) {

        if (filePath == null) {
            return "";
        }

        int lastDot =
                filePath.lastIndexOf('.');

        if (lastDot < 0) {
            return "";
        }

        return filePath
                .substring(lastDot + 1)
                .toLowerCase();
    }

    // ============================================================
    // REPOSITORY SUMMARY
    // ============================================================

    public Map<String, Object> getRepositorySummary(
            String repositoryPath) throws Exception {

        List<Map<String, Object>> files =
                analyzeRepository(
                        repositoryPath);

        Map<String, Object> summary =
                new LinkedHashMap<>();

        int typescriptFiles = 0;
        int javascriptFiles = 0;

        int lines = 0;
        int blankLines = 0;
        int commentLines = 0;
        int codeLines = 0;

        int functions = 0;
        int imports = 0;
        int exports = 0;
        int reactComponents = 0;

        int todoCount = 0;
        int fixmeCount = 0;
        int totalComplexity = 0;
        int failedFiles = 0;

        for (Map<String, Object> file :
                files) {

            String extension =
                    String.valueOf(
                            file.get("extension"));

            if ("ts".equals(extension)
                    || "tsx".equals(extension)) {

                typescriptFiles++;

            } else if ("js".equals(extension)
                    || "jsx".equals(extension)) {

                javascriptFiles++;
            }

            lines +=
                    getInteger(
                            file,
                            "lines");

            blankLines +=
                    getInteger(
                            file,
                            "blankLines");

            commentLines +=
                    getInteger(
                            file,
                            "commentLines");

            codeLines +=
                    getInteger(
                            file,
                            "codeLines");

            functions +=
                    getInteger(
                            file,
                            "functions");

            imports +=
                    getInteger(
                            file,
                            "imports");

            exports +=
                    getInteger(
                            file,
                            "exports");

            reactComponents +=
                    getInteger(
                            file,
                            "reactComponents");

            todoCount +=
                    getInteger(
                            file,
                            "todoCount");

            fixmeCount +=
                    getInteger(
                            file,
                            "fixmeCount");

            totalComplexity +=
                    getInteger(
                            file,
                            "complexity");

            if (Boolean.TRUE.equals(
                    file.get("failed"))) {

                failedFiles++;
            }
        }

        int fileCount =
                files.size();

        summary.put(
                "repositoryPath",
                repositoryPath);

        summary.put(
                "sourceFiles",
                fileCount);

        summary.put(
                "typescriptFiles",
                typescriptFiles);

        summary.put(
                "javascriptFiles",
                javascriptFiles);

        summary.put(
                "lines",
                lines);

        summary.put(
                "blankLines",
                blankLines);

        summary.put(
                "commentLines",
                commentLines);

        summary.put(
                "codeLines",
                codeLines);

        summary.put(
                "functions",
                functions);

        summary.put(
                "imports",
                imports);

        summary.put(
                "exports",
                exports);

        summary.put(
                "reactComponents",
                reactComponents);

        summary.put(
                "todoCount",
                todoCount);

        summary.put(
                "fixmeCount",
                fixmeCount);

        summary.put(
                "totalComplexity",
                totalComplexity);

        summary.put(
                "failedFiles",
                failedFiles);

        if (fileCount > 0) {

            summary.put(
                    "averageLinesPerFile",
                    (double) lines / fileCount);

            summary.put(
                    "averageFunctionsPerFile",
                    (double) functions / fileCount);

            summary.put(
                    "averageComplexityPerFile",
                    (double) totalComplexity / fileCount);

        } else {

            summary.put(
                    "averageLinesPerFile",
                    0.0);

            summary.put(
                    "averageFunctionsPerFile",
                    0.0);

            summary.put(
                    "averageComplexityPerFile",
                    0.0);
        }

        return summary;
    }

    // ============================================================
    // INTEGER HELPER
    // ============================================================

    private int getInteger(
            Map<String, Object> map,
            String key) {

        if (map == null) {
            return 0;
        }

        Object value =
                map.get(key);

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value == null) {
            return 0;
        }

        try {

            return Integer.parseInt(
                    value.toString());

        } catch (NumberFormatException exception) {

            return 0;
        }
    }
}