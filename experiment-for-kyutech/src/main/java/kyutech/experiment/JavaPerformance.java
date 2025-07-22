package kyutech.experiment;

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

public class JavaPerformance {

	public static void main(String[] args) throws Exception {
		new JavaPerformance().runUniversalForRepo();
		// new ExperimentForKyutech().runRefdiffJava();
	}

	private static void printExtractMethodOrExtractMove(String headLine, CstDiff diff) {
		System.out.println(headLine);
		for (Relationship rel : diff.getRefactoringRelationships()) {
			if (rel.getType() == RelationshipType.EXTRACT || rel.getType() == RelationshipType.EXTRACT_MOVE) {
				System.out.println(rel.getStandardDescription());
			}
		}
	}



	private final String[] commitUrls = {
			/* 1~20 */
			// "https://github.com/icse18-refactorings/cassandra/commit/35668435090eb47cf8c5e704243510b6cee35a7b",
			// "https://github.com/icse18-refactorings/cordova-plugin-local-notifications/commit/51f498a96b2fa1822e392027982c20e950535fd1",
			// "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
			// "https://github.com/icse18-refactorings/facebook-android-sdk/commit/19d1936c3b07d97d88646aeae30de747715e3248",
			// "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
			// "https://github.com/icse18-refactorings/hive/commit/5f78f9ef1e6c798849d34cc66721e6c1d9709b6f",
			// "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
			// "https://github.com/icse18-refactorings/drools/commit/1bf2875e9d73e2d1cd3b58200d5300485f890ff5",
			// "https://github.com/icse18-refactorings/intellij-community/commit/d71154ed21e2d5c65bb0ddb000bcb04ca5735048",
			// "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
			// "https://github.com/icse18-refactorings/Aeron/commit/4b762c2c70f06b0c5d2cd85866424c46478c827b",
			// "https://github.com/icse18-refactorings/k-9/commit/23c49d834d3859fc76a604da32d1789d2e863303",
			// "https://github.com/icse18-refactorings/java-algorithms-implementation/commit/ab98bcacf6e5bf1c3a06f6bcca68f178f880ffc9",
			// "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
			// "https://github.com/icse18-refactorings/nutz/commit/de7efe40dad0f4bb900c4fffa80ed377745532b3",
			"https://github.com/icse18-refactorings/fabric8/commit/e068eb7f484f24dee285d29b8a910d9019592020",
			"https://github.com/icse18-refactorings/docx4j/commit/59b8e89e61432d1d8f25cb003b62b3ac004d1b6f",
			// "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
			// "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
			"https://github.com/icse18-refactorings/intellij-community/commit/cc0eaf7faa408a04b68e2b5820f3ebcc75420b5b",

			/* 21~30 */
			// "https://github.com/icse18-refactorings/languagetool/commit/bec15926deb49d2b3f7b979d4cfc819947a434ec",
			"https://github.com/icse18-refactorings/hive/commit/b8d2140fe4faccadcf1a6343ec8cd0cc58c315f9",
			"https://github.com/icse18-refactorings/intellij-community/commit/10f769a60c7c7b73982e978959d381df487bbe2d",
			"https://github.com/icse18-refactorings/voltdb/commit/e9efc045fbc6fa893c66a03b72b7eedb388cf96c",
			"https://github.com/icse18-refactorings/quasar/commit/56d4b999e8be70be237049708f019c278c356e71",
			"https://github.com/icse18-refactorings/feign/commit/b2b4085348de32f10903970dded99fdf0376a43c",
			"https://github.com/icse18-refactorings/jedis/commit/d4b4aecbc69bbd04ba87c4e32a52cff3d129906a",
			"https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
			// "https://github.com/icse18-refactorings/neo4j/commit/5fa74fbb4a307571e3807c1201b8b05d3d60a99b",
			"https://github.com/icse18-refactorings/gradle/commit/79c66ceab11dae0b9fd1dade7bb4120028738705",
			// "https://github.com/icse18-refactorings/hazelcast/commit/30c4ae09745d6062077925a54f27205b7401d8df", // たまにいける
		};


	
	private void runUniversalForRepo() throws Exception {
		System.out.println("\n\n----- Universal Plugin Java on Repository -----");

		File clonedRepositoryBaseDir = new File("repository");

		UniversalPlugin universalPlugin = new UniversalPlugin();
		RefDiff refDiffUniversal = new RefDiff(universalPlugin);

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
				File clonedRepo = refDiffUniversal.cloneGitRepository(repoDir, cloneUrl);
				clonedRepos.put(repoIdentifier, clonedRepo);
			}
		}

		// 2. Process each commit against the already cloned repositories.
		System.out.println("\n\n----- Analyzing commits -----");
		for (String commitUrl : commitUrls) {
			String[] parts = commitUrl.split("/");
			if (parts.length < 7) {
				continue;
			}
			String owner = parts[3];
			String repoName = parts[4];
			String sha1 = parts[6];
			String repoIdentifier = owner + "/" + repoName;

			File clonedRepo = clonedRepos.get(repoIdentifier);
			printExtractMethodOrExtractMove(
					String.format("\nRefactorings found in %s %s", repoName, sha1),
					refDiffUniversal.computeDiffForCommit(clonedRepo, sha1));
		}
	}

	private void runRefdiffJava() throws Exception {
		System.out.println("\n\n----- Universal Plugin Java on Repository -----");

		File clonedRepositoryBaseDir = new File("repository");

		JavaPlugin javaPlugin = new JavaPlugin(clonedRepositoryBaseDir);
		RefDiff refDiffJava = new RefDiff(javaPlugin);

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
				File clonedRepo = refDiffJava.cloneGitRepository(repoDir, cloneUrl);
				clonedRepos.put(repoIdentifier, clonedRepo);
			}
		}

		// 2. Process each commit against the already cloned repositories.
		System.out.println("\n\n----- Analyzing commits -----");
		for (String commitUrl : commitUrls) {
			String[] parts = commitUrl.split("/");
			if (parts.length < 7) {
				continue;
			}
			String owner = parts[3];
			String repoName = parts[4];
			String sha1 = parts[6];
			String repoIdentifier = owner + "/" + repoName;

			File clonedRepo = clonedRepos.get(repoIdentifier);
			printExtractMethodOrExtractMove(
					String.format("\nRefactorings found in %s %s", repoName, sha1),
					refDiffJava.computeDiffForCommit(clonedRepo, sha1));
		}
	}
}
