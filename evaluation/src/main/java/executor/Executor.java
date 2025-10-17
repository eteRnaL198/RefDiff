package executor;

import java.io.File;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import refdiff.core.RefDiff;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.Relationship;
import refdiff.parsers.universal.UniversalPlugin;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;

public class Executor {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Please provide the programming language and optionally the metric as arguments.");
            return;
        }
        Language lang = mapLanguage(args[0]);
        Metric metric = null;
        if (args.length >= 2) {
            metric = mapMetrics(args[1]);
        }

        String resultDir = "detection-result/";
        Files.createDirectories(Paths.get(resultDir));
        
        String[] commitUrls;
        if (lang == Language.JAVA) {
            commitUrls = Commit.getJavaCommitUrls();
        } else if (lang == Language.C && metric == Metric.PRECISION) {
            commitUrls = Commit.getCPrecisionCommitUrls();
        } else if (lang == Language.C && metric == Metric.RECALL) {
            commitUrls = Commit.getCRecallCommitUrls();
        } else if (lang == Language.JS && metric == Metric.PRECISION) {
            commitUrls = Commit.getJsPrecisionCommitUrls();
        } else if (lang == Language.JS && metric == Metric.RECALL) {
            commitUrls = Commit.getJsRecallCommitUrls();
        } else {
            throw new IllegalArgumentException("Unsupported combination of language and metric.");
        }

        File clonedRepositoryBaseDir = new File("repository");
        Map<String, File> repoMap = Commit.cloneRepos(commitUrls, clonedRepositoryBaseDir);
        
        UniversalPlugin universalPlugin = new UniversalPlugin();
        RefDiff refDiff = new RefDiff(universalPlugin);
        
        Executor executor = new Executor();
        String fileName = executor.getNowDateTime() + "-" + lang.toString().toLowerCase() + ".csv";
        executor.runForRepo(refDiff, repoMap, commitUrls, resultDir + fileName);
    }

    private void runForRepo(RefDiff refDiff, Map<String, File> repoMap, String[] commitUrls, String outputFilePath) throws Exception {
        Files.write(Paths.get(outputFilePath), new byte[0], StandardOpenOption.CREATE_NEW);

        int FILE_WRITING_COMMIT_COUNT = 10;
        int commitCount = 0;
        StringBuilder result = new StringBuilder();
        result.append("repository,commit,type,before,after\n"); // header
        for (String commitUrl : commitUrls) {
            String owner = Commit.extractOwner(commitUrl);
            String repoName = Commit.extractRepoName(commitUrl);
            String sha1 = Commit.extractSha1(commitUrl);
            String repoMapKey = owner + "/" + repoName;

            System.out.println("\nProcessing commit " + sha1 + " in repository " + repoName);

            File repo = repoMap.get(repoMapKey);
            // Detect refactorings
            try {
                CstDiff diff = refDiff.computeDiffForCommit(repo, sha1);
                for (Relationship rel : diff.getRefactoringRelationships()) {
                    result.append(String.format("\"%s\",\"%s\",%s\n",  repoName, sha1, rel.getStandardDescriptionForCsv()));
                }
            } catch (Exception e) {
                String errorMsg = String.format("Error processing commit %s in repository %s: %s\n", sha1, repoName, e.getMessage());
                Files.write(Paths.get(outputFilePath + ".error.txt"), String.format("%s\n", errorMsg).getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                System.err.println(errorMsg);
            }
            // Write results to file
            try {
                commitCount++;
                if (commitCount % FILE_WRITING_COMMIT_COUNT != 0 && commitCount != commitUrls.length) continue; // Write to file every FILE_WRITING_COMMIT_COUNT commits
                Files.write(Paths.get(outputFilePath), result.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                result.setLength(0); // clear the StringBuilder
            } catch (Exception e) {
                String errorMsg = String.format("Error writing to file %s: %s\n", outputFilePath, e.getMessage());
                System.err.println(errorMsg);
            }
        }
    }

    private String getNowDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMdd-HHmm");
        return now.format(formatter);
    }

    private enum Language {
        JAVA,
        C,
        JS,
    }

    private static Language mapLanguage(String lang) {
        switch (lang.toLowerCase()) {
            case "java":
                return Language.JAVA;
            case "c":
                return Language.C;
            case "javascript":
            case "js":
                return Language.JS;
            default:
                throw new IllegalArgumentException("Unsupported language: " + lang);
        }
    }

    private enum Metric {
        PRECISION,
        RECALL,
    }

    private static Metric mapMetrics(String metric) {
        switch (metric.toLowerCase()) {
            case "precision":
                return Metric.PRECISION;
            case "recall":
                return Metric.RECALL;
            default:
                throw new IllegalArgumentException("Unsupported metric: " + metric);
        }
    }
}
