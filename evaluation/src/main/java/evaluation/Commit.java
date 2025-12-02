package evaluation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;


import refdiff.core.io.GitHelper;
import refdiff.parsers.universal.Language;

public class Commit {
  private static final String JAVA_COMMIT_URLS_FILE = "src/main/resources/java-commit-urls.txt";
  private static final String C_PRECISION_COMMIT_URLS_FILE = "src/main/resources/c-precision-commit-urls.txt";
  private static final String C_RECALL_COMMIT_URLS_FILE = "src/main/resources/c-recall-commit-urls.txt";
  private static final String JS_PRECISION_COMMIT_URLS_FILE = "src/main/resources/js-precision-commit-urls.txt";
  private static final String JS_RECALL_COMMIT_URLS_FILE = "src/main/resources/js-recall-commit-urls.txt";

  public static String[] getCommitUrls(Language lang, Metric metric) {
    if (lang == Language.JAVA && metric == null) {
      return loadCommitUrlsFromFile(JAVA_COMMIT_URLS_FILE);
    } else if (lang == Language.C && metric == Metric.PRECISION) {
      return loadCommitUrlsFromFile(C_PRECISION_COMMIT_URLS_FILE);
    } else if (lang == Language.C && metric == Metric.RECALL) {
      return loadCommitUrlsFromFile(C_RECALL_COMMIT_URLS_FILE);
    } else if (lang == Language.JAVASCRIPT && metric == Metric.PRECISION) {
      return loadCommitUrlsFromFile(JS_PRECISION_COMMIT_URLS_FILE);
    } else if (lang == Language.JAVASCRIPT && metric == Metric.RECALL) {
      return loadCommitUrlsFromFile(JS_RECALL_COMMIT_URLS_FILE);
    } else {
      throw new IllegalArgumentException("Unsupported combination of language and metric.");
    }
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

  public static File clone(File destBaseDir, String owner, String repoName) {
    String cloneUrl = String.format("https://github.com/%s/%s.git", owner, repoName);
    System.out.println("\nCloning " + repoName + " from " + cloneUrl);
    File repoDir = new File(destBaseDir, repoName);
    File clonedRepo = GitHelper.cloneBareRepository(repoDir, cloneUrl);
    return clonedRepo;
  }
}
