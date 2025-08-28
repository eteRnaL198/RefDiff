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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;

public class JavaPerformance {

    public static void main(String[] args) throws Exception {
        File clonedRepositoryBaseDir = new File("repository");

        // Universal plugin
        UniversalPlugin universalPlugin = new UniversalPlugin();
        RefDiff refDiffUniversal = new RefDiff(universalPlugin);
        String universalResult = new JavaPerformance().runForRepo(refDiffUniversal, clonedRepositoryBaseDir, "Universal Plugin Java on Repository");

        // Java plugin (needs base dir in constructor)
        File clonedRepositoryBaseDirForJavaPlugin = new File("repository");
        JavaPlugin javaPlugin = new JavaPlugin(clonedRepositoryBaseDirForJavaPlugin);
        RefDiff refDiffJava = new RefDiff(javaPlugin);
        String javaResult = new JavaPerformance().runForRepo(refDiffJava, clonedRepositoryBaseDirForJavaPlugin, "Java Plugin on Repository");

        // write results to separate files
        Files.createDirectories(Paths.get("result/javaPerformance"));
        Files.write(Paths.get("result/javaPerformance/universal.txt"),
                universalResult.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.write(Paths.get("result/javaPerformance/java.txt"),
                javaResult.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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

    private final String[] commitUrls = {
            /* 1 - 50 */
            "https://github.com/icse18-refactorings/cassandra/commit/35668435090eb47cf8c5e704243510b6cee35a7b",
            "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
            "https://github.com/icse18-refactorings/facebook-android-sdk/commit/19d1936c3b07d97d88646aeae30de747715e3248",
            "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
            "https://github.com/icse18-refactorings/hive/commit/5f78f9ef1e6c798849d34cc66721e6c1d9709b6f",
            "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
            "https://github.com/icse18-refactorings/drools/commit/1bf2875e9d73e2d1cd3b58200d5300485f890ff5",
            "https://github.com/icse18-refactorings/intellij-community/commit/d71154ed21e2d5c65bb0ddb000bcb04ca5735048",
            "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
            "https://github.com/icse18-refactorings/Aeron/commit/4b762c2c70f06b0c5d2cd85866424c46478c827b",
            "https://github.com/icse18-refactorings/java-algorithms-implementation/commit/ab98bcacf6e5bf1c3a06f6bcca68f178f880ffc9",
            "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
            "https://github.com/icse18-refactorings/nutz/commit/de7efe40dad0f4bb900c4fffa80ed377745532b3",
            "https://github.com/icse18-refactorings/docx4j/commit/59b8e89e61432d1d8f25cb003b62b3ac004d1b6f",
            "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
            "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
            "https://github.com/icse18-refactorings/intellij-community/commit/cc0eaf7faa408a04b68e2b5820f3ebcc75420b5b",
            "https://github.com/icse18-refactorings/hive/commit/b8d2140fe4faccadcf1a6343ec8cd0cc58c315f9",
            "https://github.com/icse18-refactorings/jedis/commit/d4b4aecbc69bbd04ba87c4e32a52cff3d129906a",
            "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
            "https://github.com/icse18-refactorings/neo4j/commit/5fa74fbb4a307571e3807c1201b8b05d3d60a99b",
            "https://github.com/icse18-refactorings/gradle/commit/79c66ceab11dae0b9fd1dade7bb4120028738705",
            "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
            "https://github.com/icse18-refactorings/Osmand/commit/c45b9e6615181b7d8f4d7b5b1cc141169081c02c",
            "https://github.com/icse18-refactorings/grails-core/commit/480537e0f8aaf50a7648bf445b33230aa32a9b44",
            "https://github.com/icse18-refactorings/jetty.project/commit/1f3be625e62f44d929c01f6574678eea05754474",
            "https://github.com/icse18-refactorings/jfinal/commit/881baed894540031bd55e402933bcad28b74ca88",
            "https://github.com/icse18-refactorings/intellij-community/commit/7c59f2a4f9b03a9e48ca15554291a03477aa19c1",
            "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
            "https://github.com/icse18-refactorings/voltdb/commit/c1359c843bd03a694f846c8140e24ed646bbb913",
            "https://github.com/icse18-refactorings/java-driver/commit/1edac0e92080e7c5e971b2d56c8753bf44ea8a6c",
            "https://github.com/icse18-refactorings/Android-IMSI-Catcher-Detector/commit/e235f884f2e0bc258da77b9c80492ad33386fa86",
            "https://github.com/icse18-refactorings/android_frameworks_base/commit/910397f2390d6821a006991ed6035c76cbc74897",
            "https://github.com/icse18-refactorings/orientdb/commit/b40adc25008b6f608ee3eb3422c8884fff987337",
            "https://github.com/icse18-refactorings/languagetool/commit/01cddc5afb590b4d36cb784637a8ea8aa31d3561",
            "https://github.com/icse18-refactorings/realm-java/commit/6cf596df183b3c3a38ed5dd9bb3b0100c6548ebb",
            "https://github.com/icse18-refactorings/open-keychain/commit/de50b3becb31c367f867382ff9cd898ba1628350",
            "https://github.com/icse18-refactorings/orientdb/commit/f50f234b24e6ada29c82ce57830118508bf55d51",
            "https://github.com/icse18-refactorings/gradle/commit/681dc6346ce3cf5be5c5985faad120a18949cee0",
            "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
            "https://github.com/icse18-refactorings/netty/commit/9d347ffb91f34933edb7b1124f6b70c3fc52e220",
            "https://github.com/icse18-refactorings/jackson-databind/commit/da29a040ebae664274b28117b157044af0f525fa",
            "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
            "https://github.com/icse18-refactorings/languagetool/commit/01cddc5afb590b4d36cb784637a8ea8aa31d3561",
            "https://github.com/icse18-refactorings/clojure/commit/309c03055b06525c275b278542c881019424760e",
            "https://github.com/icse18-refactorings/Android-IMSI-Catcher-Detector/commit/e235f884f2e0bc258da77b9c80492ad33386fa86",
            "https://github.com/icse18-refactorings/spring-boot/commit/1cfc6f64f64353bc5530a8ce8cdacfc3eba3e7b2",
            "https://github.com/icse18-refactorings/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07",
            "https://github.com/icse18-refactorings/intellij-community/commit/61215911ef28ca783c5106d7c01e74cf3000a866",
            "https://github.com/icse18-refactorings/voltdb/commit/c9b2006381301c99b66c50c4b31f329caac06137",
        };


    // 共通化された処理: RefDiffインスタンスとベースディレクトリ、ヘッダー文字列を受け取る
    private String runForRepo(RefDiff refDiff, File clonedRepositoryBaseDir, String header) throws Exception {
        System.out.println("\n\n----- " + header + " -----");

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
        StringBuilder sb = new StringBuilder();
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
                    sb.append(result);
            } catch (Exception e) {
                System.err.println("Error processing commit " + sha1 + " in repository " + repoName + ": " + e.getMessage());
            }
        }
        return sb.toString();
    }
}
