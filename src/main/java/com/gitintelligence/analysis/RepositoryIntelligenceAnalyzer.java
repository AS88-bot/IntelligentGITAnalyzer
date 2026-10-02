package com.gitintelligence.analysis;

import com.gitintelligence.code.RepositoryJavaScriptAnalyzer;
import com.gitintelligence.git.GitRepositoryAnalyzer;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;


@Service
public class RepositoryIntelligenceAnalyzer {

    private final GitRepositoryAnalyzer gitAnalyzer;
    private final RepositoryJavaScriptAnalyzer sourceAnalyzer;

    public RepositoryIntelligenceAnalyzer() {
        this(new GitRepositoryAnalyzer(), new RepositoryJavaScriptAnalyzer());
    }

    public RepositoryIntelligenceAnalyzer(
            GitRepositoryAnalyzer gitAnalyzer,
            RepositoryJavaScriptAnalyzer sourceAnalyzer) {
        this.gitAnalyzer = gitAnalyzer == null
                ? new GitRepositoryAnalyzer() : gitAnalyzer;
        this.sourceAnalyzer = sourceAnalyzer == null
                ? new RepositoryJavaScriptAnalyzer() : sourceAnalyzer;
    }

    public Map<String, Object> analyzeRepository(String repositoryPath) throws Exception {
        validateRepository(repositoryPath);
        try (Repository repository = gitAnalyzer.openRepository(repositoryPath)) {
            Map<String, Object> result = new LinkedHashMap<>();
            Map<String, Object> source = buildSourceSummary(repositoryPath);
            List<Map<String, Object>> files = buildFileIntelligence(repositoryPath, repository);
            List<Map<String, Object>> hotspots = topHotspots(files);
            List<Map<String, Object>> maintenance = topMaintenance(files);

            result.put("success", true);
            result.put("repositoryInfo", buildRepositoryInfo(repositoryPath, repository));
            result.put("gitSummary", gitAnalyzer.getRepositorySummary(repository));
            result.put("gitAnalytics", buildGitAnalytics(repository));
            result.put("sourceSummary", source);
            result.put("fileIntelligence", files);
            result.put("hotspots", hotspots);
            result.put("maintenanceCandidates", maintenance);
            result.put("repositoryHealth", buildHealth(repository, source, files, hotspots, maintenance));
            result.put("repositoryInsights", buildInsights(repository, source, files, hotspots, maintenance));
            result.put("recommendations", buildRecommendations(source, hotspots, maintenance));
            result.put("analysisMetadata", buildMetadata(repositoryPath));
            return result;
        }
    }

    public Map<String, Object> analyzeFile(String repositoryPath, String filePath) throws Exception {
        validateRepository(repositoryPath);
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("File path must not be blank.");
        }
        Path root = Paths.get(repositoryPath).toAbsolutePath().normalize();
        Path file = root.resolve(filePath).normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("File path must remain inside the repository.");
        }
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }
        String relative = root.relativize(file).toString().replace('\\', '/');
        if (!isSupported(relative)) {
            throw new IllegalArgumentException("Only .ts, .tsx, .js and .jsx files are supported.");
        }

        try (Repository repository = gitAnalyzer.openRepository(repositoryPath)) {
            Map<String, Object> source = sourceAnalyzer.analyzeFile(root, file);
            Map<String, Object> git = buildGitMetrics(repository, relative);
            Map<String, Object> result = new LinkedHashMap<>();
            int lines = intValue(source.get("lines"));
            int functions = intValue(source.get("functions"));
            int complexity = intValue(source.get("complexity"));
            int todo = intValue(source.get("todoCount"));
            int fixme = intValue(source.get("fixmeCount"));
            int changes = intValue(git.get("changeCount"));
            long churn = longValue(git.get("churn"));
            int hotspot = hotspotScore(lines, functions, complexity, changes, churn, todo, fixme);
            int maintenance = maintenanceScore(lines, functions, complexity, changes, churn, todo, fixme);

            result.put("success", true);
            result.put("repositoryPath", root.toString());
            result.put("file", relative);
            result.put("source", source);
            result.put("git", git);
            addScoredFields(result, git, hotspot, maintenance,
                    hotspotReasons(lines, functions, complexity, changes, churn, todo, fixme),
                    maintenanceReasons(lines, functions, complexity, changes, churn, todo, fixme));
            result.put("recommendations", fileRecommendations(source, git, hotspot, maintenance));
            return result;
        }
    }

    private Map<String, Object> buildRepositoryInfo(String path, Repository repository) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("repositoryPath", path);
        m.put("repositoryName", gitAnalyzer.getRepositoryName(repository));
        m.put("currentBranch", gitAnalyzer.getCurrentBranch(repository));
        m.put("branches", gitAnalyzer.getBranches(repository));
        m.put("commitCount", gitAnalyzer.getCommitCount(repository));
        m.put("latestCommitId", gitAnalyzer.getLatestCommitId(repository));
        m.put("latestCommitMessage", gitAnalyzer.getLatestCommitMessage(repository));
        m.put("latestCommitAuthor", gitAnalyzer.getLatestCommitAuthor(repository));
        m.put("latestCommitAuthorEmail", gitAnalyzer.getLatestCommitAuthorEmail(repository));
        m.put("latestCommitDate", gitAnalyzer.getLatestCommitDate(repository));
        m.put("contributors", gitAnalyzer.getContributors(repository));
        m.put("changedFileCount", gitAnalyzer.getAllChangedFiles(repository).size());
        return m;
    }

    private Map<String, Object> buildGitAnalytics(Repository r) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("commitTimeline", gitAnalyzer.getCommitTimeline(r));
        m.put("contributorActivity", gitAnalyzer.getCommitActivityByAuthor(r));
        m.put("contributorDetails", gitAnalyzer.getContributorActivityDetails(r));
        m.put("commitActivityByDate", gitAnalyzer.getCommitActivityByDate(r));
        m.put("linesChangedByDate", gitAnalyzer.getLinesChangedByDate(r));
        m.put("branches", gitAnalyzer.getBranches(r));
        m.put("fileChangeFrequency", gitAnalyzer.getFileChangeFrequency(r));
        m.put("fileChurnStatistics", gitAnalyzer.getFileChurnStatistics(r));
        m.put("fileExtensionStatistics", gitAnalyzer.getFileExtensionStatistics(r));
        m.put("directoryStatistics", gitAnalyzer.getDirectoryStatistics(r));
        m.put("largestCommits", gitAnalyzer.getLargestCommits(r, 10));
        return m;
    }

    private Map<String, Object> buildSourceSummary(String path) throws Exception {
        return new LinkedHashMap<>(sourceAnalyzer.getRepositorySummary(path));
    }

    private List<Map<String, Object>> buildFileIntelligence(String path, Repository r) throws Exception {
        List<Map<String, Object>> sourceFiles = sourceAnalyzer.analyzeRepository(path);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> src : sourceFiles) {
            Map<String, Object> f = new LinkedHashMap<>(src);
            String file = stringValue(src.get("file"));
            Map<String, Object> git = buildGitMetrics(r, file);
            int lines = intValue(src.get("lines"));
            int functions = intValue(src.get("functions"));
            int complexity = intValue(src.get("complexity"));
            int todo = intValue(src.get("todoCount"));
            int fixme = intValue(src.get("fixmeCount"));
            int changes = intValue(git.get("changeCount"));
            long churn = longValue(git.get("churn"));
            int hotspot = hotspotScore(lines, functions, complexity, changes, churn, todo, fixme);
            int maintenance = maintenanceScore(lines, functions, complexity, changes, churn, todo, fixme);
            f.put("git", git);
            f.put("gitChangeCount", changes);
            f.put("gitLinesAdded", longValue(git.get("linesAdded")));
            f.put("gitLinesRemoved", longValue(git.get("linesRemoved")));
            f.put("gitChurn", churn);
            f.put("changeCount", changes);
            f.put("churn", churn);
            addScoredFields(f, git, hotspot, maintenance,
                    hotspotReasons(lines, functions, complexity, changes, churn, todo, fixme),
                    maintenanceReasons(lines, functions, complexity, changes, churn, todo, fixme));
            f.put("recommendations", fileRecommendations(src, git, hotspot, maintenance));
            result.add(f);
        }
        result.sort(Comparator.comparing(x -> stringValue(x.get("file"))));
        return result;
    }

    private Map<String, Object> buildGitMetrics(Repository r, String file) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        long added = gitAnalyzer.getFileLinesAdded(r, file);
        long removed = gitAnalyzer.getFileLinesRemoved(r, file);
        m.put("changeCount", gitAnalyzer.getFileChangeCount(r, file));
        m.put("linesAdded", added);
        m.put("linesRemoved", removed);
        m.put("churn", added + removed);
        m.put("contributors", gitAnalyzer.getFileContributors(r, file));
        m.put("latestCommit", gitAnalyzer.getLatestCommitChangingFile(r, file));
        return m;
    }

    private void addScoredFields(Map<String, Object> target, Map<String, Object> git,
                                 int hotspot, int maintenance,
                                 List<String> hotspotReasons, List<String> maintenanceReasons) {
        target.put("gitChangeCount", intValue(git.get("changeCount")));
        target.put("gitLinesAdded", longValue(git.get("linesAdded")));
        target.put("gitLinesRemoved", longValue(git.get("linesRemoved")));
        target.put("gitChurn", longValue(git.get("churn")));
        target.put("hotspotScore", hotspot);
        target.put("hotspotCategory", hotspotCategory(hotspot));
        target.put("hotspotReasons", hotspotReasons);
        target.put("maintenanceScore", maintenance);
        target.put("maintenanceCategory", maintenanceCategory(maintenance));
        target.put("maintenanceReasons", maintenanceReasons);
    }

    private List<Map<String, Object>> topHotspots(List<Map<String, Object>> files) {
        List<Map<String, Object>> result = new ArrayList<>(files);
        result.sort(Comparator.comparingInt((Map<String,Object> x) -> intValue(x.get("hotspotScore")))
                .reversed().thenComparing(x -> stringValue(x.get("file"))));
        return result.size() > 10 ? new ArrayList<>(result.subList(0, 10)) : result;
    }

    private List<Map<String, Object>> topMaintenance(List<Map<String, Object>> files) {
        List<Map<String, Object>> result = new ArrayList<>(files);
        result.removeIf(x -> intValue(x.get("maintenanceScore")) <= 0);
        result.sort(Comparator.comparingInt((Map<String,Object> x) -> intValue(x.get("maintenanceScore")))
                .reversed().thenComparing(x -> stringValue(x.get("file"))));
        return result.size() > 10 ? new ArrayList<>(result.subList(0, 10)) : result;
    }

    private Map<String, Object> buildHealth(Repository r, Map<String, Object> source,
                                            List<Map<String,Object>> files,
                                            List<Map<String,Object>> hotspots,
                                            List<Map<String,Object>> maintenance) throws IOException {
        Map<String,Object> h = new LinkedHashMap<>();
        int highComplexity = 0, large = 0, frequent = 0, highChurn = 0;
        for (Map<String,Object> f : files) {
            if (intValue(f.get("complexity")) >= 20) highComplexity++;
            if (intValue(f.get("lines")) >= 500) large++;
            if (intValue(f.get("gitChangeCount")) >= 5) frequent++;
            if (longValue(f.get("gitChurn")) >= 500) highChurn++;
        }
        int signals = highComplexity + large + frequent + highChurn + hotspots.size() + maintenance.size();
        String status = files.isEmpty() ? "NO_SOURCE_DATA" : signals >= 12 ? "NEEDS_ATTENTION" : signals >= 6 ? "WATCH" : "STABLE";
        h.put("sourceFiles", intValue(source.get("sourceFiles")));
        h.put("totalLines", intValue(source.get("lines")));
        h.put("totalFunctions", intValue(source.get("functions")));
        h.put("totalImports", intValue(source.get("imports")));
        h.put("totalExports", intValue(source.get("exports")));
        h.put("totalReactComponents", intValue(source.get("reactComponents")));
        h.put("totalComplexity", intValue(source.get("totalComplexity")));
        h.put("commits", gitAnalyzer.getCommitCount(r));
        h.put("filesWithHighComplexity", highComplexity);
        h.put("highComplexityFiles", highComplexity);
        h.put("largeFiles", large);
        h.put("frequentlyChangedFiles", frequent);
        h.put("highChurnFiles", highChurn);
        h.put("highHotspotFiles", hotspots.stream().filter(x -> intValue(x.get("hotspotScore")) >= 8).count());
        h.put("maintenanceCandidates", maintenance.size());
        h.put("hotspotCount", hotspots.size());
        h.put("maintenanceCandidateCount", maintenance.size());
        h.put("averageLinesPerFile", source.get("averageLinesPerFile"));
        h.put("averageComplexityPerFile", source.get("averageComplexityPerFile"));
        h.put("status", status);
        h.put("statusDescription", healthDescription(status));
        return h;
    }

    private Map<String,Object> buildInsights(Repository r, Map<String,Object> source,
                                             List<Map<String,Object>> files,
                                             List<Map<String,Object>> hotspots,
                                             List<Map<String,Object>> maintenance) throws IOException {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("sourceFileCount", source.get("sourceFiles"));
        m.put("totalSourceLines", source.get("lines"));
        m.put("totalFunctions", source.get("functions"));
        m.put("totalComplexity", source.get("totalComplexity"));
        m.put("commitCount", gitAnalyzer.getCommitCount(r));
        m.put("totalLinesChanged", longValue(gitAnalyzer.getRepositorySummary(r).get("linesChanged")));
        m.put("topHotspot", first(hotspots));
        m.put("topMaintenanceCandidate", first(maintenance));
        m.put("largestSourceFile", largest(files, "lines"));
        m.put("mostFrequentlyChangedFile", largest(files, "gitChangeCount"));
        m.put("largestCommit", first(gitAnalyzer.getLargestCommits(r, 1)));
        m.put("topContributor", first(gitAnalyzer.getCommitActivityByAuthor(r)));
        return m;
    }

    private List<String> buildRecommendations(Map<String,Object> source,
                                               List<Map<String,Object>> hotspots,
                                               List<Map<String,Object>> maintenance) {
        List<String> r = new ArrayList<>();
        if (intValue(source.get("sourceFiles")) == 0) r.add("No supported TypeScript/JavaScript source files were detected.");
        if (hotspots.stream().anyMatch(x -> intValue(x.get("complexity")) >= 20)) r.add("Review high-complexity files and consider extracting smaller functions.");
        if (hotspots.stream().anyMatch(x -> intValue(x.get("lines")) >= 500)) r.add("Review large source files for clearer module boundaries.");
        if (intValue(source.get("todoCount")) > 0) r.add("Review TODO markers and convert important items into tracked work.");
        if (intValue(source.get("fixmeCount")) > 0) r.add("Review FIXME markers because they indicate known problem areas.");
        if (!maintenance.isEmpty()) r.add("Review maintenance candidates during technical-debt planning.");
        if (!hotspots.isEmpty()) r.add("Inspect the highest-scoring hotspots before major changes.");
        if (r.isEmpty()) r.add("No immediate repository-level recommendations were generated from the available metrics.");
        return r;
    }

    private List<String> fileRecommendations(Map<String,Object> source, Map<String,Object> git,
                                              int hotspot, int maintenance) {
        List<String> r = new ArrayList<>();
        if (intValue(source.get("lines")) >= 500) r.add("Consider splitting this large file into smaller modules.");
        if (intValue(source.get("functions")) >= 25) r.add("Consider grouping related functions into focused modules.");
        if (intValue(source.get("complexity")) >= 20) r.add("Review complex control flow and extract smaller functions.");
        if (intValue(git.get("changeCount")) >= 5) r.add("This file changes frequently; review whether its responsibilities are too broad.");
        if (longValue(git.get("churn")) >= 500) r.add("High historical churn suggests reviewing the file before large changes.");
        if (intValue(source.get("todoCount")) > 0) r.add("Review TODO markers in this file.");
        if (intValue(source.get("fixmeCount")) > 0) r.add("Review FIXME markers in this file.");
        if (hotspot >= 8) r.add("Treat this file as a hotspot during maintenance and refactoring.");
        if (maintenance >= 7) r.add("Include this file in technical-debt or maintenance planning.");
        return r;
    }

    private int hotspotScore(int lines,int functions,int complexity,int changes,long churn,int todo,int fixme) {
        int s = 0;
        if(lines>=500)s+=3;else if(lines>=250)s+=2;else if(lines>=100)s++;
        if(functions>=50)s+=3;else if(functions>=25)s+=2;else if(functions>=10)s++;
        if(complexity>=100)s+=4;else if(complexity>=50)s+=3;else if(complexity>=20)s+=2;else if(complexity>=10)s++;
        if(changes>=10)s+=3;else if(changes>=5)s+=2;else if(changes>=2)s++;
        if(churn>=1000)s+=4;else if(churn>=500)s+=3;else if(churn>=100)s+=2;else if(churn>=25)s++;
        return s + Math.min(todo,2) + Math.min(fixme,4);
    }

    private int maintenanceScore(int lines,int functions,int complexity,int changes,long churn,int todo,int fixme) {
        int s = 0;
        if(lines>=1000)s+=4;else if(lines>=500)s+=3;else if(lines>=250)s+=2;
        if(functions>=100)s+=4;else if(functions>=50)s+=3;else if(functions>=25)s+=2;
        if(complexity>=100)s+=4;else if(complexity>=50)s+=3;else if(complexity>=20)s+=2;
        if(changes>=10)s+=3;else if(changes>=5)s+=2;else if(changes>=2)s++;
        if(churn>=1000)s+=4;else if(churn>=500)s+=3;else if(churn>=100)s+=2;
        return s + Math.min(todo,2) + Math.min(fixme,4);
    }

    private String hotspotCategory(int s){return s>=12?"CRITICAL":s>=8?"HIGH":s>=4?"MODERATE":"LOW";}
    private String maintenanceCategory(int s){return s>=12?"HIGH":s>=7?"MODERATE":s>=4?"LOW":"MINIMAL";}

    private List<String> hotspotReasons(int lines,int functions,int complexity,int changes,long churn,int todo,int fixme) {
        List<String> r=new ArrayList<>();
        if(lines>=500)r.add("Large file");else if(lines>=250)r.add("Moderately large file");else if(lines>=100)r.add("Non-trivial file size");
        if(functions>=50)r.add("Very high function count");else if(functions>=25)r.add("High function count");else if(functions>=10)r.add("Elevated function count");
        if(complexity>=100)r.add("Very high complexity");else if(complexity>=50)r.add("High complexity");else if(complexity>=20)r.add("Elevated complexity");else if(complexity>=10)r.add("Moderate complexity");
        if(changes>=10)r.add("Frequently changed");else if(changes>=5)r.add("Changed often");else if(changes>=2)r.add("Multiple historical changes");
        if(churn>=1000)r.add("Very high Git churn");else if(churn>=500)r.add("High Git churn");else if(churn>=100)r.add("Elevated Git churn");else if(churn>=25)r.add("Some Git churn");
        if(todo>0)r.add("Contains TODO markers"); if(fixme>0)r.add("Contains FIXME markers");
        if(r.isEmpty())r.add("No strong hotspot signals"); return r;
    }

    private List<String> maintenanceReasons(int lines,int functions,int complexity,int changes,long churn,int todo,int fixme) {
        List<String> r=new ArrayList<>();
        if(lines>=1000)r.add("Very large file");else if(lines>=500)r.add("Large file");else if(lines>=250)r.add("Moderately large file");
        if(functions>=100)r.add("Very high function count");else if(functions>=50)r.add("High function count");else if(functions>=25)r.add("Many functions");
        if(complexity>=100)r.add("Very high complexity");else if(complexity>=50)r.add("High complexity");else if(complexity>=20)r.add("Elevated complexity");
        if(changes>=10)r.add("Frequently changed");else if(changes>=5)r.add("Changed often");else if(changes>=2)r.add("Repeatedly changed");
        if(churn>=1000)r.add("Very high historical churn");else if(churn>=500)r.add("High historical churn");else if(churn>=100)r.add("Elevated historical churn");
        if(todo>0)r.add("TODO markers need review"); if(fixme>0)r.add("FIXME markers need review");
        if(r.isEmpty())r.add("No strong maintenance signals"); return r;
    }

    private Map<String,Object> buildMetadata(String path){
        Map<String,Object> m=new LinkedHashMap<>(); m.put("version","2.0"); m.put("analyzer","RepositoryIntelligenceAnalyzer");
        m.put("sourceAnalyzer","RepositoryJavaScriptAnalyzer"); m.put("gitAnalyzer","GitRepositoryAnalyzer"); m.put("repositoryPath",path);
        m.put("supportedSourceTypes",List.of(".ts",".tsx",".js",".jsx")); return m;
    }

    private void validateRepository(String path){
        if(path==null||path.isBlank())throw new IllegalArgumentException("Repository path must not be blank.");
        Path p=Paths.get(path).toAbsolutePath().normalize();
        if(!Files.exists(p))throw new IllegalArgumentException("Repository path does not exist: "+path);
        if(!Files.isDirectory(p))throw new IllegalArgumentException("Repository path is not a directory: "+path);
    }

    private boolean isSupported(String f){return f.endsWith(".ts")||f.endsWith(".tsx")||f.endsWith(".js")||f.endsWith(".jsx");}
    private String stringValue(Object o){return o==null?"":String.valueOf(o);}
    private int intValue(Object o){if(o instanceof Number)return ((Number)o).intValue();try{return o==null?0:Integer.parseInt(String.valueOf(o));}catch(Exception e){return 0;}}
    private long longValue(Object o){if(o instanceof Number)return ((Number)o).longValue();try{return o==null?0L:Long.parseLong(String.valueOf(o));}catch(Exception e){return 0L;}}
    private Map<String,Object> first(List<Map<String,Object>> l){return l==null||l.isEmpty()?new LinkedHashMap<>():l.get(0);}
    private Map<String,Object> largest(List<Map<String,Object>> l,String key){Map<String,Object> best=new LinkedHashMap<>();for(Map<String,Object>x:l)if(best.isEmpty()||longValue(x.get(key))>longValue(best.get(key)))best=x;return best;}
    private String healthDescription(String s){if("NO_SOURCE_DATA".equals(s))return "No supported TypeScript/JavaScript source data was available.";if("NEEDS_ATTENTION".equals(s))return "Several size, complexity, churn, hotspot or maintenance signals were detected.";if("WATCH".equals(s))return "Some maintainability signals were detected and should be monitored.";return "The available metrics did not show a large concentration of warning signals.";}
}
