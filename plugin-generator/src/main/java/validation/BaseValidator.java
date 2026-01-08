package validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import plugingenerator.Language;
import refdiff.core.io.FilePathFilter;
import java.util.ArrayList;
import refdiff.core.io.SourceFolder;
import refdiff.core.io.SourceFileSet;
import refdiff.core.cst.CstRoot;
import refdiff.parsers.LanguagePlugin;
import java.nio.file.StandardOpenOption;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public abstract class BaseValidator {

  /**
   * Concrete subclasses must provide the parser plugin used for CST generation.
   */
  protected abstract LanguagePlugin getPlugin();

  /**
   * Concrete subclasses must provide the Language enum instance.
   */
  protected abstract Language getLanguage();

  /**
   * Concrete subclasses must provide the list of repository names to process.
   */
  protected abstract String[] getRepos();

  /**
   * Type equivalence check: implemented by subclasses for each language.
   */
  protected abstract boolean isTypeEqual(String tagKind, String nodeType);

  /**
   * Run the validation process. Subclasses can call this from their own main().
   */
  public void run() throws Exception {
    Language language = getLanguage();
    FilePathFilter fileFilter = new FilePathFilter(List.of(language.getExtensions()));

    Path baseRepoDir = Paths.get("repo");
    for (String repoName : getRepos()) {
      Path repoPath = baseRepoDir.resolve(repoName);
      if (!Files.exists(repoPath) || !Files.isDirectory(repoPath)) {
        System.out.println("Repository not found, skipping: " + repoPath);
        continue;
      }
      Path tagsOutputPath = Paths.get("output/tags/").resolve(language.getName()).resolve(repoName + ".ndjson");
      if (!Files.exists(tagsOutputPath.getParent())) {
        Files.createDirectories(tagsOutputPath.getParent());
      }
      if (Files.exists(tagsOutputPath)) {
        System.out.println("Tags file already exists, skipping ctags generation: " + tagsOutputPath);
      }
      System.out.println("Generating ctags for " + repoName + "...");
      String ctagsOutput = Tag.execCtags(repoPath, language, fileFilter);
      Files.writeString(tagsOutputPath, ctagsOutput, StandardOpenOption.CREATE);

      System.out.println("Validating CST against ctags for " + repoName + "...");
      List<Match> matches = validateCstWithTags(repoPath, tagsOutputPath);
      String now = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
      Path matchesOut = Paths.get("output/matching/").resolve(language.getName()).resolve(repoName + "-" + now + ".ndjson");
      if (!Files.exists(matchesOut.getParent())) {
        Files.createDirectories(matchesOut.getParent());
      }
      for (Match match : matches) {
        Files.writeString(matchesOut, match.toNdjson() + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      }
    }
  }

  private List<Match> validateCstWithTags(Path repoPath, Path tagsFilePath) {
    try {
      List<Tag> tags = Tag.parseJson(tagsFilePath);
      SourceFileSet sources = SourceFolder.from(repoPath, getLanguage().getExtensions());
      CstRoot root = getPlugin().parse(sources);
      List<Match> matches = new ArrayList<>();
      for (Tag tag : tags) {
        Match match = Match.create(tag, root, this::isTypeEqual);
        matches.add(match);
      }
      return matches;
    } catch (Exception e) {
      System.out.println("Validation failed: " + e.getMessage());
      e.printStackTrace();
      return new ArrayList<>();
    }
  }
}
