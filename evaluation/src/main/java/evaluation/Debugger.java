package evaluation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.Repository;

import refdiff.core.diff.CstComparator;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.CstRootHelper;
import refdiff.core.diff.Relationship;
import refdiff.core.io.SourceFile;
import refdiff.core.io.FilePathFilter;
import refdiff.core.io.GitHelper;
import refdiff.core.io.SourceFileSet;
import refdiff.core.io.SourceFolder;
import refdiff.core.util.PairBeforeAfter;
import refdiff.parsers.LanguagePlugin;
// import refdiff.parsers.java.JavaPlugin;
import refdiff.parsers.universal.java.JavaPlugin;
import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;

public class Debugger {
    private static final Boolean IS_FOR_REPO = true;
    // private static final Boolean IS_FOR_REPO = false;
    private static final String COMMIT_URL = 
        // "https://github.com/refdiff-study/toxcore/commit/2465f486acd90ed8395c8a83a13af09ecd024c98";
        // "https://github.com/icse18-refactorings/hazelcast/commit/4d05a3b1168441216dcaea8282c39338285182af";
        "https://github.com/icse18-refactorings/wildfly/commit/37d842bfed9779e662321a5ee43c36b058386843";

    private static final String DIR_NAME = "wildfly";

    // private static final LanguagePlugin plugin = new JavaPlugin(new File("repository/wildfly"));
    private static final LanguagePlugin plugin = new JavaPlugin();
    // private static final LanguagePlugin plugin = new CPlugin();
    // private static final LanguagePlugin plugin = new refdiff.parsers.c.CPlugin(); // refdiff-c
    // private static final LanguagePlugin plugin = new JsPlugin();

    private CstDiff diff(LanguagePlugin plugin, Path path) throws Exception {
        SourceFolder sourcesBefore = SourceFolder.from(path.resolve("v0"), ".java");
        SourceFolder sourcesAfter = SourceFolder.from(path.resolve("v1"), ".java");
        CstComparator comparator = new CstComparator(plugin);
        return comparator.compare(sourcesBefore, sourcesAfter);
    }

    private CstDiff diff(LanguagePlugin plugin, File gitRepository, String commitSha1) throws Exception {
        Repository repo = GitHelper.openRepository(gitRepository);
        FilePathFilter fileFilter = plugin.getAllowedFilesFilter();
        PairBeforeAfter<SourceFileSet> beforeAndAfter = GitHelper.getSourcesBeforeAndAfterCommit(repo, commitSha1, fileFilter); 
        // LanguagePlugin javaPlugin = new JavaPlugin(gitRepository);
        // CstComparator comparator = new CstComparator(javaPlugin);
        CstComparator comparator = new CstComparator(plugin);
        return comparator.compare(beforeAndAfter);
    }

    private PairBeforeAfter<SourceFileSet> getSourceFileSets(LanguagePlugin plugin, Path path) {
        SourceFolder sourcesBefore = SourceFolder.from(path.resolve("v0"), ".java");
        SourceFolder sourcesAfter = SourceFolder.from(path.resolve("v1"), ".java");
        return new PairBeforeAfter<>(sourcesBefore, sourcesAfter);
    }

    private PairBeforeAfter<SourceFileSet> getSourceFileSets(LanguagePlugin plugin, File gitRepository, String commitSha1) throws Exception {
        Repository repo = GitHelper.openRepository(gitRepository);
        FilePathFilter fileFilter = plugin.getAllowedFilesFilter();
        return GitHelper.getSourcesBeforeAndAfterCommit(repo, commitSha1, fileFilter);
    }

    private static List<String> getNodeTokens(CstRoot root, SourceFileSet sourceFiles, Map<String, String> sourceCache, CstNode node) {
        if (node == null) {
            return List.of();
        }
        String filePath = node.getLocation().getFile();
        String sourceCode = sourceCache.computeIfAbsent(filePath, path -> {
            try {
                return sourceFiles.readContent(new SourceFile(Paths.get(path)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return CstRootHelper.retrieveTokens(root, sourceCode, node, false);
    }

    public static void main(String[] args) throws Exception {
        Debugger debugger = new Debugger();
        CstDiff diff = null;
        PairBeforeAfter<SourceFileSet> sourceFiles = null;

        if (IS_FOR_REPO) {
            File baseDir = new File("repository");
            File repo = CommitUrl.clone(baseDir, CommitUrl.extractOwner(COMMIT_URL), CommitUrl.extractRepoName(COMMIT_URL));
            String commitSha1 = CommitUrl.extractSha1(COMMIT_URL);
            diff = debugger.diff(plugin, repo, commitSha1);
            sourceFiles = debugger.getSourceFileSets(plugin, repo, commitSha1);
        } else {
            Path path = Paths.get("src/test/resources/", DIR_NAME);
            diff = debugger.diff(plugin, path);
            sourceFiles = debugger.getSourceFileSets(plugin, path);
        }
        
        Map<String, String> beforeCache = new HashMap<>();
        Map<String, String> afterCache = new HashMap<>();

        for (Relationship r : diff.getRelationships()) {
            // if (r.isRefactoring()) {
                System.out.println(r.toString());
                List<String> beforeTokens = getNodeTokens(diff.getBefore(), sourceFiles.getBefore(), beforeCache, r.getNodeBefore());
                List<String> afterTokens = getNodeTokens(diff.getAfter(), sourceFiles.getAfter(), afterCache, r.getNodeAfter());
                System.out.println("  before tokens: " + beforeTokens);
                System.out.println("  after tokens: " + afterTokens);
            // }
        }
    }
}
