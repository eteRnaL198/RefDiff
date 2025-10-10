package evaluation;

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
import evaluation.CommitUrl;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;

public class JavaEvaluator {

    public static void main(String[] args) throws Exception {
        File clonedRepositoryBaseDir = new File("repository");
        String id = "1011-0116";
        String resultDir = String.format("result/%s", id);
        Files.createDirectories(Paths.get(resultDir));

        // Universal plugin
        UniversalPlugin universalPlugin = new UniversalPlugin();
        RefDiff refDiffUniversal = new RefDiff(universalPlugin);
        new JavaEvaluator().runForRepo(refDiffUniversal, clonedRepositoryBaseDir, "Universal Plugin Java on Repository", resultDir + "/universal.txt");

        // Java plugin (needs base dir in constructor)
        JavaPlugin javaPlugin = new JavaPlugin(clonedRepositoryBaseDir);
        RefDiff refDiffJava = new RefDiff(javaPlugin);
        new JavaEvaluator().runForRepo(refDiffJava, clonedRepositoryBaseDir, "Java Plugin on Repository", resultDir + "/java.txt");
    }


    private static String extractMethodOrExtractMoveAsString(String headLine, CstDiff diff) {
            StringBuilder sb = new StringBuilder();
            sb.append(headLine).append(System.lineSeparator());
            for (Relationship rel : diff.getRefactoringRelationships()) {
                    if (rel.getType() == RelationshipType.EXTRACT || rel.getType() == RelationshipType.EXTRACT_MOVE) {
                            sb.append(rel.getStandardDescription()).append(System.lineSeparator());
                    }
            }
            return sb.toString();
    }

    private void runForRepo(RefDiff refDiff, File clonedRepositoryBaseDir, String header, String outputFilePath) throws Exception {
        System.out.println("\n\n----- " + header + " -----");
        String[] commitUrls = CommitUrl.getCommitUrls();

        // 1. Clone all unique repositories first.
        Map<String, File> clonedRepos = new HashMap<>();
        for (String commitUrl : commitUrls) {
            String[] parts = commitUrl.split("/");
            if (parts.length < 7) {
                System.err.println("Invalid commit URL: " + commitUrl);
                continue;
            }
            String owner = parts[3];
            String repoName = parts[4];
            String repoIdentifier = owner + "/" + repoName;

            // If not already cloned, clone it.
            if (!clonedRepos.containsKey(repoIdentifier)) {
                String cloneUrl = String.format("https://github.com/%s/%s.git", owner, repoName);
                System.out.println("\nCloning " + repoName + " from " + cloneUrl);
                File repoDir = new File(clonedRepositoryBaseDir, repoName);
                File clonedRepo = refDiff.cloneGitRepository(repoDir, cloneUrl);
                clonedRepos.put(repoIdentifier, clonedRepo);
            }
        }

        // 2. Process each commit against the already cloned repositories.
        System.out.println("\n\n----- Analyzing commits -----");
        // 最初にファイルを空にする
        Files.write(Paths.get(outputFilePath), new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        for (String commitUrl : commitUrls) {
            String[] parts = commitUrl.split("/");
            if (parts.length < 7) {
                continue;
            }
            String owner = parts[3];
            String repoName = parts[4];
            String sha1 = parts[6];
            String repoIdentifier = owner + "/" + repoName;

            System.out.println("\nProcessing commit " + sha1 + " in repository " + repoName);

            File clonedRepo = clonedRepos.get(repoIdentifier);
            try {
                String result = extractMethodOrExtractMoveAsString(
                    String.format("\nRefactorings found in %s %s", repoName, sha1),
                    refDiff.computeDiffForCommit(clonedRepo, sha1));
                // TODO 100コミットごとにまとめて追記でもいいかも
                // 1コミットごとに追記
                Files.write(Paths.get(outputFilePath), result.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) {
                String errorMsg = String.format("Error processing commit %s in repository %s: %s\n", sha1, repoName, e.getMessage());
                Files.write(Paths.get(outputFilePath), errorMsg.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                System.err.println(errorMsg);
            }
        }
    }
}
