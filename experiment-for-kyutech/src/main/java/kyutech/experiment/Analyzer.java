package kyutech.experiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
import refdiff.parsers.universal.UniversalPlugin;

public class Analyzer {
        private static int COMMIT_COUNT = 200;

        public static void main(String[] args) throws Exception {
            UniversalPlugin universalPlugin = new UniversalPlugin();
            RefDiff refDiffUniversal = new RefDiff(universalPlugin);
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmm"));

           // CLI option: --before-commit <sha>  or -b <sha>
           String beforeCommitSha = null;
           for (int i = 0; i < args.length; i++) {
               String a = args[i];
               if ("--before-commit".equals(a) || "-b".equals(a)) {
                   if (i + 1 < args.length) {
                       beforeCommitSha = args[i + 1];
                       i++;
                   }
               } else if (a.startsWith("--before-commit=")) {
                   beforeCommitSha = a.substring("--before-commit=".length());
               }
           }
            // Map: language -> (repoName -> repoDir)
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

                                    File tempFolder = new File("repo-for-analysis");
                                    File repoDir = new File(tempFolder, repoName);
                                    File clonedRepo = refDiffUniversal.cloneGitRepository(repoDir, repoUrl);
                                    clonedReposByLang.computeIfAbsent(lang, k -> new HashMap<>()).put(repoName, clonedRepo);
                                    System.out.println("Cloned " + repoName + " (" + lang + ") to " + clonedRepo.getAbsolutePath());
                            } catch (Exception e) {
                                    System.err.println("Failed to clone " + repoUrl + ": " + e.getMessage());
                            }
                    }
            }

            // Map: language -> (repoName -> commits)
            Map<String, Map<String, List<String>>> repoCommitsByLang = new HashMap<>();
            for (Map.Entry<String, Map<String, File>> langEntry : clonedReposByLang.entrySet()) {
                    String lang = langEntry.getKey();
                    Map<String, File> repos = langEntry.getValue();
                    for (Map.Entry<String, File> entry : repos.entrySet()) {
                            String repoName = entry.getKey();
                            File repoDir = entry.getValue();
                            try {
                                    List<String> commits;
                                    if (beforeCommitSha != null && !beforeCommitSha.isEmpty()) {
                                        commits = getCommitsBeforeCommit(repoDir, beforeCommitSha, COMMIT_COUNT);
                                    } else {
                                        commits = getRecentCommits(repoDir, COMMIT_COUNT);
                                    }
                                     repoCommitsByLang.computeIfAbsent(lang, k -> new HashMap<>()).put(repoName, commits);
                            } catch (Exception e) {
                                    System.err.println("Failed to get recent commits for " + repoName + ": " + e.getMessage());
                            }
                    }
            }

            System.out.println("\n\n----- Analyzing commits -----");
            for (Map.Entry<String, Map<String, File>> langEntry : clonedReposByLang.entrySet()) {
                    String lang = langEntry.getKey();
                    Map<String, File> repos = langEntry.getValue();
                    for (Map.Entry<String, File> entry : repos.entrySet()) {
                            String repoName = entry.getKey();
                            File repoDir = entry.getValue();
                            List<String> commits = repoCommitsByLang.getOrDefault(lang, Map.of()).get(repoName);

                            Path outPath = Paths.get("result", "analysis", "mid", lang, repoName, time + ".txt");
                            Files.createDirectories(outPath.getParent());

                            StringBuilder sb = new StringBuilder();

                            if (commits != null) {
                                for (String commitSha : commits) {
                                        try {
                                                CstDiff diff = refDiffUniversal.computeDiffForCommit(repoDir, commitSha);
                                                String result = buildRefactoringsText(repoName, commitSha, diff);
                                                sb.append(result);
                                                System.out.println(result);
                                        } catch (Exception e) {
                                                System.err.println("Failed to analyze commit " + commitSha + " in " + repoName + ": " + e.getMessage());
                                        }
                                }
                            } else {
                                sb.append("No commits retrieved for ").append(repoName).append(System.lineSeparator());
                            }

                            Files.write(outPath, sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    }
            }
    }

    private static String buildRefactoringsText(String repoName, String commitSha, CstDiff diff) {
        StringBuilder lines = new StringBuilder();
        lines.append("Commit: ").append(commitSha).append(System.lineSeparator());

        if (diff == null || diff.getRefactoringRelationships().isEmpty()) {
            lines.append("No refactorings found.").append(System.lineSeparator());
        } else {
            for (Relationship rel : diff.getRefactoringRelationships()) {
                lines.append(rel.getStandardDescription()).append(System.lineSeparator());
            }
        }
        lines.append(System.lineSeparator());

        return lines.toString();
    }

    /**
     * Get commits older than the given commit (exclude the given commit).
     * Uses "git log <commit>^ --pretty=format:%H -n <maxCount>".
     */
    private static List<String> getCommitsBeforeCommit(File repoDir, String commitSha, int maxCount) throws Exception {
        // ensure commitSha looks non-empty
        if (commitSha == null || commitSha.trim().isEmpty()) {
            throw new IllegalArgumentException("commitSha must be provided");
        }
        // use <commit>^ to start from the parent of the given commit (i.e., older commits)
        ProcessBuilder pb = new ProcessBuilder("git", "log", "--pretty=format:%H", commitSha + "^", "-n", String.valueOf(maxCount));
        pb.directory(repoDir);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        List<String> commits;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            commits = br.lines()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        }
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("git log exited with code " + exitCode + " for commit " + commitSha);
        }
        return commits;
    }

    private static List<String> getRecentCommits(File repoDir, int maxCount) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("git", "log", "--pretty=format:%H", "-n", String.valueOf(maxCount));
        pb.directory(repoDir);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        List<String> commits;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            commits = br.lines()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        }
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("git log exited with code " + exitCode);
        }
        return commits;
    }
}
