package refdiff.parsers.universal.java;

import static org.junit.Assert.assertThat;
import static refdiff.test.util.CstDiffMatchers.containsOnly;
import static refdiff.test.util.CstDiffMatchers.node;
import static refdiff.test.util.CstDiffMatchers.relationship;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import refdiff.core.diff.CstComparator;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.RelationshipType;
import refdiff.core.diff.Relationship;
import refdiff.core.io.SourceFolder;
import refdiff.parsers.LanguagePlugin;

public class TestJavaCstComparator {

    private static final LanguagePlugin parser = new JavaPlugin();
    private static final String TEST_DATA_BASE_PATH = "src/test/resources/java/refactor";

    private CstDiff diff(String folderName) throws Exception {
        Path baseFolderPath = Paths.get(TEST_DATA_BASE_PATH, folderName);
        SourceFolder sourcesBefore = SourceFolder.from(baseFolderPath.resolve("v0"), ".java");
        SourceFolder sourcesAfter = SourceFolder.from(baseFolderPath.resolve("v1"), ".java");
        CstComparator comparator = new CstComparator(parser);
        return comparator.compare(sourcesBefore, sourcesAfter);
    }

    @Test
    public void shouldMatchMoveClass() throws Exception {
        CstDiff diff = diff("moveClass");
        assertThat(diff, containsOnly(
            relationship(RelationshipType.MOVE, node("Foo/Foo"), node("Moved/Foo")),
            relationship(RelationshipType.SAME, node("Foo/Foo", "main(String[])"), node("Moved/Foo", "main(String[])"))
        ));
    }

    @Test
    public void shouldMatchMoveMethod() throws Exception {
        CstDiff diff = diff("moveMethod");
        assertThat(diff, containsOnly(
            relationship(RelationshipType.SAME, node("Foo"), node("Foo")),
            relationship(RelationshipType.MOVE, node("Foo", "hello()"), node("Bar", "hello()")),
            relationship(RelationshipType.SAME, node("Foo", "main(String[])"), node("Foo", "main(String[])"))
        ));
    }

    @Test
    public void shouldMatchMoveAndRenameClass() throws Exception {
        CstDiff diff = diff("moveAndRenameClass");
        assertThat(diff, containsOnly(
            relationship(RelationshipType.SAME, node("Foo/Foo", "main(String[])"), node("Bar/Bar", "main(String[])")),
            relationship(RelationshipType.MOVE_RENAME, node("Foo/Foo"), node("Bar/Bar"))
        ));
    }

    @Test
    public void shouldMatchRenameClass() throws Exception {
        CstDiff diff = diff("renameClass");
        assertThat(diff, containsOnly(
            relationship(RelationshipType.SAME, node("Foo", "main(String[])"), node("Bar", "main(String[])")),
            relationship(RelationshipType.RENAME, node("Foo"), node("Bar"))
        ));
    }

    @Test
    public void shouldMatchRenameMethod() throws Exception {
        CstDiff diff = diff("renameMethod");
        assertThat(diff, containsOnly(
            relationship(RelationshipType.RENAME, node("User", "isOkay()"), node("User", "isAdult()")),
            relationship(RelationshipType.SAME, node("User"), node("User")),
            relationship(RelationshipType.RENAME, node("Foo", "hello()"), node("Foo", "greet()")),
            relationship(RelationshipType.SAME, node("Foo"), node("Foo")),
            relationship(RelationshipType.SAME, node("User", "new(int)"), node("User", "new(int)"))
        ));
    }

    @Test
    public void shouldMatchExtractInterface() throws Exception {
        CstDiff diff = diff("extractInterface");
        assertThat(diff, containsOnly(
            relationship(RelationshipType.EXTRACT_SUPER, node("Foo"), node("Bar")),
            relationship(RelationshipType.SAME, node("Foo"), node("Foo"))
        ));
    }

    @Test
    public void shouldMatchExtractSuperclass() throws Exception {
        CstDiff diff = diff("extractSuperclass");
        assertThat(diff, containsOnly(
            relationship(RelationshipType.EXTRACT_SUPER, node("Foo"), node("Bar")),
            relationship(RelationshipType.SAME, node("Foo"), node("Foo"))
        ));
    }

    @Test
    public void shouldMatchPullUpMethod() throws Exception {
        CstDiff diff = diff("pullUpMethod");
        assertThat(diff, containsOnly(
            relationship(RelationshipType.SAME, node("Foo"), node("Foo")),
            relationship(RelationshipType.PULL_UP, node("Foo", "greet()"), node("Bar", "greet()")),
            relationship(RelationshipType.SAME, node("Bar"), node("Bar"))
        ));
    }

    @Test
    public void shouldMatchPushDownMethod() throws Exception {
        CstDiff diff = diff("pushDownMethod");
        assertThat(diff, containsOnly(
            relationship(RelationshipType.SAME, node("Bar"), node("Bar")),
            relationship(RelationshipType.SAME, node("Foo"), node("Foo")),
            relationship(RelationshipType.PUSH_DOWN, node("Bar", "greet()"), node("Foo", "greet()"))
        ));
    }

    @Test
    public void shouldMatchExtractMethod() throws Exception {
        CstDiff diff = diff("extractMethod");
        assertThat(diff, containsOnly(
            relationship(RelationshipType.SAME, node("Foo"), node("Foo")),
            relationship(RelationshipType.EXTRACT, node("Foo", "main(String[])"), node("Foo", "hello(String[])")),
            relationship(RelationshipType.SAME, node("Foo", "main(String[])"), node("Foo", "main(String[])"))
        ));
    }

    @Test
    public void shouldMatchExtractAndMoveMethod() throws Exception {
        CstDiff diff = diff("extractAndMoveMethod");
        assertThat(diff, containsOnly(
            relationship(RelationshipType.EXTRACT_MOVE, node("Foo", "main(String[])"), node("Bar", "hello(String[])")),
            relationship(RelationshipType.SAME, node("Foo"), node("Foo")),
            relationship(RelationshipType.SAME, node("Foo", "main(String[])"), node("Foo", "main(String[])"))
        ));
    }

    @Test
    public void shouldMatchInlineMethod() throws Exception {
        CstDiff diff = diff("InlineMethod"); // Folder name is "InlineMethod"
        assertThat(diff, containsOnly(
            relationship(RelationshipType.INLINE, node("Foo", "hello(String[])"), node("Foo", "main(String[])")),
            relationship(RelationshipType.SAME, node("Foo", "main(String[])"), node("Foo", "main(String[])")),
            relationship(RelationshipType.SAME, node("Foo"), node("Foo"))
        ));
    }

    @Test
    public void shouldMatchInternalMoveMethod() throws Exception {
        CstDiff diff = diff("InternalMoveMethod"); // Folder name is "InternalMoveMethod"
        assertThat(diff, containsOnly(
            relationship(RelationshipType.SAME, node("Foo"), node("Foo")),
            relationship(RelationshipType.SAME, node("Foo", "Bar"), node("Foo", "Bar")),
            relationship(RelationshipType.INTERNAL_MOVE, node("Foo", "Bar", "hello(String[])"), node("Foo", "hello(String[])")),
            relationship(RelationshipType.SAME, node("Foo", "main(String[])"), node("Foo", "main(String[])"))
        ));
    }
}