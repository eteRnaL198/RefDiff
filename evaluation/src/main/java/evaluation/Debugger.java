package evaluation;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jgit.lib.Repository;

import refdiff.core.diff.CstComparator;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.RelationshipType;
import refdiff.core.diff.Relationship;
import refdiff.core.io.FilePathFilter;
import refdiff.core.io.GitHelper;
import refdiff.core.io.SourceFileSet;
import refdiff.core.io.SourceFolder;
import refdiff.core.util.PairBeforeAfter;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.c.CPlugin;
import refdiff.parsers.universal.UniversalPlugin;
import refdiff.parsers.universal.UniversalPlugin.Language;

public class Debugger {
    // private static final Boolean IS_FOR_REPO = true;
    private static final Boolean IS_FOR_REPO = false;
    private static final String COMMIT_URL = 
        "https://github.com/refdiff-study/toxcore/commit/2465f486acd90ed8395c8a83a13af09ecd024c98";

    private static final String DIR_NAME = "hazelcast";

    private static final Language LANG = Language.JAVA;
    // private static final Language LANG = Language.C;
    // private static final Language LANG = Language.JAVASCRIPT;

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
        CstComparator comparator = new CstComparator(plugin);
        return comparator.compare(beforeAndAfter);
    }

    public static void main(String[] args) throws Exception {
        Debugger debugger = new Debugger();
        LanguagePlugin plugin = new UniversalPlugin(LANG);
        // LanguagePlugin plugin = new CPlugin();
        CstDiff diff = null;

        if (IS_FOR_REPO) {
            File baseDir = new File("repository");
            File repo = Commit.clone(baseDir, Commit.extractOwner(COMMIT_URL), Commit.extractRepoName(COMMIT_URL));
            diff = debugger.diff(plugin, repo, Commit.extractSha1(COMMIT_URL));
        } else {
            Path path = Paths.get("src/test/resources/", DIR_NAME);
            diff = debugger.diff(plugin, path);
        }
        
        for (Relationship r : diff.getRelationships()) {
            if (r.isRefactoring()) {
                System.out.println(r.toString());
            }
        }
    }
}
