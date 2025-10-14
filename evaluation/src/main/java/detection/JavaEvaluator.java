package detection;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import refdiff.core.RefDiff;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.Relationship;
import refdiff.core.diff.RelationshipType;
import refdiff.parsers.universal.UniversalPlugin;
import refdiff.parsers.java.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;

public class JavaEvaluator {

    public static void main(String[] args) throws Exception {
        JavaEvaluator javaEvaluator = new JavaEvaluator();

        File clonedRepositoryBaseDir = new File("repository");
        // Require output directory id be specified with -o <id> (no default).
        String dirName = null;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("-h".equals(a) || "--help".equals(a)) {
                System.out.println("Usage: JavaEvaluator -o <outputDirName>");
                System.out.println("Example: JavaEvaluator -o 1012-2215");
                return;
            }
            if ("-o".equals(a) || "--output".equals(a)) {
                if (i + 1 < args.length) {
                    dirName = args[i + 1];
                    i++; // skip next
                } else {
                    System.err.println("Error: missing value for -o/--output");
                    System.exit(1);
                }
            }
        }
        if (dirName == null) {
            System.err.println("Error: output id must be specified with -o <id>");
            System.err.println("Usage: JavaEvaluator -o <outputDirName>");
            System.exit(1);
        }
        String resultDir = String.format("detection-result/%s", dirName);
        Files.createDirectories(Paths.get(resultDir));
        Map<String, File> repoMap = javaEvaluator.cloneRepos(clonedRepositoryBaseDir);

        UniversalPlugin universalPlugin = new UniversalPlugin();
        RefDiff refDiffUniversal = new RefDiff(universalPlugin);
        // javaEvaluator.runForRepo(refDiffUniversal, repoMap, resultDir + "/universal.csv");

        JavaPlugin javaPlugin = new JavaPlugin(clonedRepositoryBaseDir); // needs base dir in constructor
        RefDiff refDiffJava = new RefDiff(javaPlugin);
        javaEvaluator.runForRepo(refDiffJava, repoMap, resultDir + "/java.csv");
    }

    private void runForRepo(RefDiff refDiff, Map<String, File> repoMap, String outputFilePath) throws Exception {
        String[] commitUrls = CommitUrl.getUniqueCommitUrls();

        // 最初にファイルを空にする
        Files.write(Paths.get(outputFilePath), new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        int FILE_WRITING_COMMIT_COUNT = 10;
        int commitCount = 0;
        StringBuilder result = new StringBuilder();
        result.append("repository,commit,type,before,after\n"); // header
        for (String commitUrl : commitUrls) {
            String owner = CommitUrl.extractOwner(commitUrl);
            String repoName = CommitUrl.extractRepoName(commitUrl);
            String sha1 = CommitUrl.extractSha1(commitUrl);
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
                Files.write(Paths.get(outputFilePath), errorMsg.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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

    private Map<String, File> cloneRepos(File clonedRepositoryBaseDir) throws Exception {
        String[] commitUrls = CommitUrl.getUniqueCommitUrls();
        Map<String, File> clonedRepos = new HashMap<>();
        for (String commitUrl : commitUrls) {
            String owner = CommitUrl.extractOwner(commitUrl);
            String repoName = CommitUrl.extractRepoName(commitUrl);
            String repoMapKey = owner + "/" + repoName;

            if (clonedRepos.containsKey(repoMapKey)) continue; // If already cloned, skip it.
            File clonedRepo = CommitUrl.clone(clonedRepositoryBaseDir, owner, repoName);
            clonedRepos.put(repoMapKey, clonedRepo);
        }
        return clonedRepos;
    }
}
