package investigation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import refdiff.core.RefDiff;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.Relationship;
import refdiff.core.io.GitHelper;
import refdiff.parsers.universal.UniversalPlugin;
import refdiff.parsers.universal.UniversalPlugin.Language;

public class Executor {
    private static int COMMIT_DEPTH = 1000;
    // private static int COMMIT_COUNT = 100;
    private static final int BATCH_SIZE = 10;

    private int commitCount = 0;
    private String currentCommitSha = "";

    public static void main(String[] args) throws Exception {
        // CLI option: --start-commit <sha> or -s <sha>
        String startCommitSha = null;
        String language = null; // Add a variable to store the language
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("--start-commit".equals(a) || "-s".equals(a)) {
                if (i + 1 < args.length) {
                    startCommitSha = args[i + 1];
                    i++;
                }
            } else if (a.startsWith("--start-commit=")) {
                startCommitSha = a.substring("--start-commit=".length());
            } else if ("--language".equals(a) || "-l".equals(a)) { // Handle --language or -l
                if (i + 1 < args.length) {
                    language = args[i + 1];
                    i++;
                }
            } else if (a.startsWith("--language=")) {
                language = a.substring("--language=".length());
            }
        }

        // Validate the language argument
        if (language == null) {
            throw new IllegalArgumentException("Language must be specified using --language or -l.");
        }

        Language langEnum = mapLanguage(language); // Convert to Language enum
        new Executor().execute(startCommitSha, langEnum);
    }

    private void incrementCommitCount() {
        commitCount++;
    }

    private void setCurrentCommitSha(String sha) {
        currentCommitSha = sha;
    }

    private void execute(String startCommitSha, Language language) throws Exception {
        UniversalPlugin plugin = new UniversalPlugin(language);
        RefDiff refDiffUniversal = new RefDiff(plugin);

        Map<String, Map<String, File>> clonedReposByLang = getRepos();

        System.out.println("\n\n----- Detect refactorings -----");
        for (Map.Entry<String, Map<String, File>> langEntry : clonedReposByLang.entrySet()) {
            String lang = langEntry.getKey();
            Map<String, File> repos = langEntry.getValue();
            for (Map.Entry<String, File> entry : repos.entrySet()) {
                String repoName = entry.getKey();
                File repoDir = entry.getValue();

                Path outPath = Paths.get("result", lang, repoName + "-" + getNowDateTime() + ".csv");
                Files.createDirectories(outPath.getParent());

                StringBuilder sb = new StringBuilder();
                setCurrentCommitSha(startCommitSha != null ? startCommitSha : "HEAD");
                for (int attempt = 1; attempt <= 5; attempt++) {
                    try {
                        refDiffUniversal.computeDiffForCommitHistory(repoDir, currentCommitSha, COMMIT_DEPTH, (commit, diff) -> {
                            String commitSha = commit.getName();
                            for (Relationship rel : diff.getRefactoringRelationships()) {
                                sb.append(String.format("\"%s\",%s\n", commitSha, rel.getDescriptionWithLocInCsv()));
                            }
                            if ((commitCount + 1) % BATCH_SIZE == 0 || commitCount + 1 == COMMIT_DEPTH) {
                                try {
                                    Files.write(outPath, sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                    sb.setLength(0); // clear the StringBuilder
                                } catch (IOException e) {
                                    System.err.println("Failed to write results for " + repoName + ": " + e.getMessage());
                                }
                            }
                            incrementCommitCount();
                            setCurrentCommitSha(commitSha);
                        });
                    } catch (Exception e) {
                        System.err.println("Error processing repository " + repoName + " (attempt " + attempt + "): " + e.getMessage());
                        try {
                            Files.write(outPath, sb.toString().getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            sb.setLength(0); // clear the StringBuilder
                        } catch (IOException ioException) {
                            System.err.println("Failed to write error log for " + repoName + ": " + ioException.getMessage());
                        }
                        if (attempt == 5) {
                            System.err.println("Max attempts reached for repository " + repoName + ". Skipping.");
                        } else {
                            System.out.println("Retrying...");
                            Thread.sleep(2000); // wait before retrying
                            continue;
                        }
                    }
                    break; // exit the retry loop if successful
                }

                // Write any remaining results
                if (sb.length() > 0) {
                    Files.write(outPath, sb.toString().getBytes(StandardCharsets.UTF_8), 
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
            }
        }
    }

    /**
     * Map: language -> (repoName -> repoDir)
     */
    private Map<String, Map<String, File>> getRepos() {
        Map<String, Map<String, File>> clonedReposByLang = new HashMap<>();
        for (Map.Entry<String, String[]> langEntry : RepoConfig.REPOS_BY_LANGUAGE.entrySet()) {
            String lang = langEntry.getKey();
            String[] repoUrls = langEntry.getValue();
            if (repoUrls == null) {
                continue;
            }
            for (String repoUrl : repoUrls) {
                try {
                    if (repoUrl == null || repoUrl.trim().isEmpty()) {
                        continue;
                    }
                    String[] parts = repoUrl.split("/");
                    String repoNameWithGit = parts[parts.length - 1];
                    String repoName;
                    if (repoNameWithGit.endsWith(".git")) {
                        repoName = repoNameWithGit.substring(0, repoNameWithGit.length() - 4);
                    } else {
                        repoName = repoNameWithGit;
                    }

                    File tempFolder = new File("repo");
                    File repoDir = new File(tempFolder, repoName);
                    File clonedRepo = GitHelper.cloneBareRepository(repoDir, repoUrl);
                    clonedReposByLang.computeIfAbsent(lang, _ -> new HashMap<>()).put(repoName, clonedRepo);
                    System.out.println("Cloned " + repoName + " (" + lang + ") to " + clonedRepo.getAbsolutePath());
                } catch (Exception e) {
                    System.err.println("Failed to clone " + repoUrl + ": " + e.getMessage());
                }
            }
        }
        return clonedReposByLang;
    }

    private static String getNowDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMdd-HHmm");
        return now.format(formatter);
    }

    private static Language mapLanguage(String lang) {
        switch (lang.toLowerCase()) {
            case "java":
                return Language.JAVA;
            case "c":
                return Language.C;
            case "javascript":
                return Language.JAVASCRIPT;
            case "go":
                return Language.GO;
            case "python":
                return Language.PYTHON;
            case "ruby":
                return Language.RUBY;
            case "php":
                return Language.PHP;
            default:
                throw new IllegalArgumentException("Unsupported language: " + lang);
        }
    }
}
