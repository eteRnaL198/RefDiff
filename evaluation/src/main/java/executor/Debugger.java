package executor;

import java.nio.file.Path;
import java.nio.file.Paths;

import refdiff.core.diff.CstComparator;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.RelationshipType;
import refdiff.core.diff.Relationship;
import refdiff.core.io.SourceFolder;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.universal.UniversalPlugin;

public class Debugger {
    private static final String TEST_DATA_BASE_PATH = "src/test/resources/";
    
    private static final LanguagePlugin parser = new UniversalPlugin();

    private CstDiff diff(String folderName) throws Exception {
        Path baseFolderPath = Paths.get(TEST_DATA_BASE_PATH, folderName);
        SourceFolder sourcesBefore = SourceFolder.from(baseFolderPath.resolve("v0"), ".java");
        SourceFolder sourcesAfter = SourceFolder.from(baseFolderPath.resolve("v1"), ".java");
        CstComparator comparator = new CstComparator(parser);
        return comparator.compare(sourcesBefore, sourcesAfter);
    }

    public static void main(String[] args) throws Exception {
        Debugger detector = new Debugger();
        // CstDiff diff = detector.diff("BuildCraft");
        CstDiff diff = detector.diff("crate");
        for (Relationship r : diff.getRelationships()) {
            // RelationshipType type = r.getType();
            // System.out.println(type);
            System.out.println(r);
        }
    }
}
