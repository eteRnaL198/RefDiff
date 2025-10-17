package executor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;


import refdiff.core.io.GitHelper;

public class Commit {
  private static String[] javaCommitUrls;
  private static String[] cPrecisionCommitUrls;
  private static String[] cRecallCommitUrls;

  private static final String JAVA_COMMIT_URLS_FILE = "src/main/resources/java-commit-urls.txt";
  private static final String C_PRECISION_COMMIT_URLS_FILE = "src/main/resources/c-precision-commit-urls.txt";
  private static final String C_RECALL_COMMIT_URLS_FILE = "src/main/resources/c-recall-commit-urls.txt";

  public static String[] getJavaCommitUrls() {
    if (javaCommitUrls != null) {
      return javaCommitUrls;
    }
    return loadCommitUrlsFromFile(JAVA_COMMIT_URLS_FILE);
  }

  public static String[] getCPrecisionCommitUrls() {
    if (cPrecisionCommitUrls != null) {
      return cPrecisionCommitUrls;
    }
    return loadCommitUrlsFromFile(C_PRECISION_COMMIT_URLS_FILE);
  }

  public static String[] getCRecallCommitUrls() {
    if (cRecallCommitUrls != null) {
      return cRecallCommitUrls;
    }
    return loadCommitUrlsFromFile(C_RECALL_COMMIT_URLS_FILE);
  }

  public static String[] getJsPrecisionCommitUrls() {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public static String[] getJsRecallCommitUrls() {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  private static String[] loadCommitUrlsFromFile(String filePath) {
    try {
      List<String> lines = Files.readAllLines(
          Paths.get(filePath),
          StandardCharsets.UTF_8
      );
      return lines.stream()
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .distinct() // Commit URLs can be duplicated in the file
          .toArray(String[]::new);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read commit URLs from " + filePath, e);
    }
  }

  public static Map<String, File> cloneRepos(String[] commitUrls, File clonedRepositoryBaseDir) throws Exception {
        Map<String, File> clonedRepos = new HashMap<>();
        for (String commitUrl : commitUrls) {
            String owner = extractOwner(commitUrl);
            String repoName = extractRepoName(commitUrl);
            String repoMapKey = owner + "/" + repoName;
            if (clonedRepos.containsKey(repoMapKey)) continue; // CommitUrls can have duplicates. If already cloned, skip it.
            File clonedRepo = clone(clonedRepositoryBaseDir, owner, repoName);
            clonedRepos.put(repoMapKey, clonedRepo);
        }
        return clonedRepos;
    }

  public static String extractOwner(String commitUrl) {
    String[] parts = commitUrl.split("/");
    if (parts.length < 7) {
      throw new IllegalArgumentException("Invalid commit URL: " + commitUrl);
    }
    return parts[3];
  }

  public static String extractRepoName(String commitUrl) {
    String[] parts = commitUrl.split("/");
    if (parts.length < 7) {
      throw new IllegalArgumentException("Invalid commit URL: " + commitUrl);
    }
    return parts[4];
  }

  public static String extractSha1(String commitUrl) {
    String[] parts = commitUrl.split("/");
    if (parts.length < 7) {
      throw new IllegalArgumentException("Invalid commit URL: " + commitUrl);
    }
    return parts[6];
  }

  private static File clone(File destBaseDir, String owner, String repoName) {
    String cloneUrl = String.format("https://github.com/%s/%s.git", owner, repoName);
    System.out.println("\nCloning " + repoName + " from " + cloneUrl);
    File repoDir = new File(destBaseDir, repoName);
    File clonedRepo = GitHelper.cloneBareRepository(repoDir, cloneUrl);
    return clonedRepo;
  }
}
