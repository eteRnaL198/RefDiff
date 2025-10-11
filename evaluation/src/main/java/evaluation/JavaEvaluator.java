package evaluation;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.revwalk.DepthWalk.Commit;

import refdiff.core.RefDiff;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.Relationship;
import refdiff.core.diff.RelationshipType;
import refdiff.core.io.GitHelper;
import refdiff.parsers.universal.UniversalPlugin;
import refdiff.parsers.java.JavaPlugin;
import evaluation.CommitUrl;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;

public class JavaEvaluator {

    public static void main(String[] args) throws Exception {
        JavaEvaluator javaEvaluator = new JavaEvaluator();

        File clonedRepositoryBaseDir = new File("repository");
        String id = "1011-1405";
        String resultDir = String.format("result/%s", id);
        Files.createDirectories(Paths.get(resultDir));
        Map<String, File> repoMap = javaEvaluator.cloneRepos(clonedRepositoryBaseDir);

        UniversalPlugin universalPlugin = new UniversalPlugin();
        RefDiff refDiffUniversal = new RefDiff(universalPlugin);
        javaEvaluator.runForRepo(refDiffUniversal, repoMap, resultDir + "/universal.txt");

        JavaPlugin javaPlugin = new JavaPlugin(clonedRepositoryBaseDir); // needs base dir in constructor
        RefDiff refDiffJava = new RefDiff(javaPlugin);
        javaEvaluator.runForRepo(refDiffJava, repoMap, resultDir + "/java.txt");
    }

    private void runForRepo(RefDiff refDiff, Map<String, File> repoMap, String outputFilePath) throws Exception {
        String[] commitUrls = CommitUrl.getUniqueCommitUrls();

        // 最初にファイルを空にする
        Files.write(Paths.get(outputFilePath), new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        int FILE_WRITING_COMMIT_COUNT = 10;
        int commitCount = 0;
        StringBuilder result = new StringBuilder();
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
                    result.append(String.format("%s %s %s%n", repoName, sha1, rel.getStandardDescription()));
                }
            } catch (Exception e) {
                String errorMsg = String.format("Error processing commit %s in repository %s: %s\n", sha1, repoName, e.getMessage());
                Files.write(Paths.get(outputFilePath), errorMsg.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                System.err.println(errorMsg);
            }
            // Write results to file
            try {
                commitCount++;
                if (commitCount % FILE_WRITING_COMMIT_COUNT != 0) continue; // Write to file every FILE_WRITING_COMMIT_COUNT commits
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
