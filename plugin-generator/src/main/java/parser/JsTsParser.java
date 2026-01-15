package parser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import plugingenerator.Language;
import plugingenerator.TreeSitterParser;

public class JsTsParser {
  private static final String[] REPO_URLS = new String[] {
    // "https://github.com/facebook/react/blob/main/scripts/bench/benchmarks/pe-class-components/benchmark.js#L454",
    // "https://github.com/facebook/react/blob/main/scripts/bench/benchmarks/hacker-news/benchmark.js#L307",
    // "https://github.com/facebook/react/blob/main/scripts/bench/benchmarks/pe-functional-components/benchmark.js#L4",
    // "https://github.com/facebook/react/blob/main/scripts/flags/flags.js#L150",
    // "https://github.com/facebook/react/blob/main/scripts/release/shared-commands/print-prerelease-summary.js#L9",
    "https://github.com/facebook/react/blob/main/scripts/release/shared-commands/parse-params.js#L44,"
  };
  
  public static void main(String[] args) throws Exception {
    for (String repoUrl : REPO_URLS) {
      System.out.println("Validating repo: " + repoUrl);
      Path repoPath = getRepoPath(repoUrl);
      String ast = TreeSitterParser.parse(repoPath, Language.JS.getTSLanguage());
      Path outPath = Path.of("./output/ast/%s/%s/".formatted(Language.JS.getName(), extractRepoName(repoUrl)));
      try {
        Files.createDirectories(outPath);
      } catch (Exception e) {
        throw new RuntimeException("Failed to create output directory: " + outPath, e);
      }
      Files.writeString(outPath.resolve(repoPath.getFileName().toString() + ".txt"), ast);
      System.out.println("AST written to: " + outPath);
    }
  }

  private static Path getRepoPath(String repoUrl) {
    // https://github.com/{owner}/{repo}/blob/{branch}/path/to/File.ext#L454
    Pattern p = Pattern.compile("^https?://(?:www\\.)?github\\.com/([^/]+)/([^/]+)/(?:blob)/[^/]+/(.*)$");
    Matcher m = p.matcher(repoUrl);
    if (!m.find()) return null;
    String repo = m.group(2);
    String rest = m.group(3);
    if (rest != null) {
      int hash = rest.indexOf('#');
      if (hash != -1) rest = rest.substring(0, hash);
      int q = rest.indexOf('?');
      if (q != -1) rest = rest.substring(0, q);
    }
    return Path.of("./repo/" + repo + (rest == null || rest.isEmpty() ? "" : "/" + rest));
  }

  private static String extractRepoName(String repoUrl) {
    // https://github.com/{owner}/{repo}/blob/{branch}/path/to/File.ext#L15
    Pattern p = Pattern.compile("^https?://(?:www\\.)?github\\.com/([^/]+)/([^/]+)/(?:blob)/[^/]+/(.*)$");
    Matcher m = p.matcher(repoUrl);
    if (!m.find())
      return null;
    String repo = m.group(2);
    return repo;
  }
}
