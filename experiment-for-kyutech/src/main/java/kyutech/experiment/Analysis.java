package kyutech.experiment;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import refdiff.core.RefDiff;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.Relationship;
import refdiff.parsers.universal.UniversalPlugin;

public class Analysis {

	public static void main(String[] args) throws Exception {
		runUniversalForRepo();
	}

	private static final String[] REPO_URLS = {
		// Java
		// "https://github.com/iluwatar/java-design-patterns.git",
		// "https://github.com/spring-projects/spring-boot.git",
		// "https://github.com/Stirling-Tools/Stirling-PDF.git",

		// C
		// "https://github.com/torvalds/linux.git",
		// "https://github.com/Genymobile/scrcpy.git",
		// "https://github.com/netdata/netdata.git",
		// "https://github.com/redis/redis.git",
		// "https://github.com/obsproject/obs-studio.git",
		// "https://github.com/curl/curl.git",
		// "https://github.com/tmux/tmux.git",

		// JavaScript
		// "https://github.com/facebook/react.git",
		// "https://github.com/airbnb/javascript.git",
		// "https://github.com/vercel/next.js.git",
		"https://github.com/nodejs/node.git",


		// Ruby
		// "https://github.com/rails/rails.git",
		// "https://github.com/maybe-finance/maybe.git",
	};

	private static void printRefactorings(String headLine, CstDiff diff) {
		if (diff.getRefactoringRelationships().isEmpty()) {
			return;
		}
		System.out.println(headLine);
		for (Relationship rel : diff.getRefactoringRelationships()) {
			System.out.println(rel.getStandardDescription());
		}
	}

	private static void runUniversalForRepo() throws Exception {
		File tempFolder = new File("repo-for-analysis");

		UniversalPlugin universalPlugin = new UniversalPlugin();
		RefDiff refDiffUniversal = new RefDiff(universalPlugin);

		Map<String, File> clonedRepos = new HashMap<>();
		for (String repoUrl : REPO_URLS) {
			try {
				String[] parts = repoUrl.split("/");
				String repoNameWithGit = parts[parts.length - 1];
				String repoName = repoNameWithGit.substring(0, repoNameWithGit.lastIndexOf('.'));

				System.out.println("\nCloning " + repoName + " from " + repoUrl);
				File repoDir = new File(tempFolder, repoName);
				File clonedRepo = refDiffUniversal.cloneGitRepository(repoDir, repoUrl);
				clonedRepos.put(repoName, clonedRepo);
				System.out.println("Cloned " + repoName + " to " + clonedRepo.getAbsolutePath());
			} catch (Exception e) {
				System.err.println("Failed to clone " + repoUrl + ": " + e.getMessage());
			}
		}

		System.out.println("\n\n----- Analyzing commits -----");
		for (Map.Entry<String, File> entry : clonedRepos.entrySet()) {
			String repoName = entry.getKey();
			File repoDir = entry.getValue();
			
			java.nio.file.Path commitsFilePath = Paths.get(repoDir.getAbsolutePath(), "commits.txt");
			if (!Files.exists(commitsFilePath)) {
				System.err.println("commits.txt not found for " + repoName + ". Please run 'get-commits.sh' first.");
				continue;
			}

			List<String> commits = new ArrayList<>();
			try {
				commits = Files.readAllLines(commitsFilePath);
			} catch (Exception e) {
				System.err.println("Failed to read commits.txt for " + repoName + ": " + e.getMessage());
				continue;
			}

			if (commits.isEmpty()) {
				System.err.println("No commits found in commits.txt for " + repoName + ". Skipping analysis.");
				continue;
			}

			for (String commitSha : commits) {
				if (commitSha == null || commitSha.trim().isEmpty()) {
					continue;
				}
				try {
					printRefactorings(
							"\nRefactorings found in " + repoName + " " + commitSha,
							refDiffUniversal.computeDiffForCommit(repoDir, commitSha.trim()));
				} catch (Exception e) {
					System.err.println("Failed to analyze commit " + commitSha + " in " + repoName + ": " + e.getMessage());
				}
			}
		}
	}
}
