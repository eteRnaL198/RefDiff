package refdiff.parsers.universal.c;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import refdiff.core.diff.CstComparator;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.Relationship;
import refdiff.core.io.SourceFolder;
import refdiff.parsers.universal.UniversalPlugin;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class TestCstComparator {

    private CstComparator comparator;

    @Before
    public void setUp() {
        comparator = new CstComparator(new UniversalPlugin());
    }

    private List<String> getRelationshipDescriptions(CstDiff diff) {
        return diff.getRelationships().stream()
            .map(Relationship::getStandardDescription)
            .sorted() // Ensure consistent order for comparison
            .collect(Collectors.toList());
    }

    @Test
    public void testChangeSignature() {
        String basePath = "src/test/resources/c";
        SourceFolder v0 = SourceFolder.from(Paths.get(basePath, "changeSignature/v0"), ".c");
        SourceFolder v1 = SourceFolder.from(Paths.get(basePath, "changeSignature/v1"), ".c");
        CstDiff diff = comparator.compare(v0, v1);
        List<String> actual = getRelationshipDescriptions(diff);
        List<String> expected = List.of(
            "CHANGE_SIGNATURE\t{Function print() at main.c:3}\t{Function print(char, int) at main.c:3}",
            "SAME\t{File main.c at main.c:1}\t{File main.c at main.c:1}",
            "SAME\t{Function main() at main.c:9}\t{Function main() at main.c:9}"
        ).stream().sorted().collect(Collectors.toList());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMoveFile() {
        String basePath = "src/test/resources/c";
        SourceFolder v0 = SourceFolder.from(Paths.get(basePath, "moveFile/v0"), ".c");
        SourceFolder v1 = SourceFolder.from(Paths.get(basePath, "moveFile/v1"), ".c");
        CstDiff diff = comparator.compare(v0, v1);
        List<String> actual = getRelationshipDescriptions(diff);
        List<String> expected = List.of(
            "MOVE_FILE\t{File main.c at main.c:1}\t{File main.c at foo/main.c:1}",
            "SAME\t{Function main() at main.c:3}\t{Function main() at foo/main.c:3}"
        ).stream().sorted().collect(Collectors.toList());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMoveFunction() {
        String basePath = "src/test/resources/c";
        SourceFolder v0 = SourceFolder.from(Paths.get(basePath, "moveFunction/v0"), ".c");
        SourceFolder v1 = SourceFolder.from(Paths.get(basePath, "moveFunction/v1"), ".c");
        CstDiff diff = comparator.compare(v0, v1);
        List<String> actual = getRelationshipDescriptions(diff);
        List<String> expected = List.of(
            "MOVE_FUNCTION\t{Function foo() at main.c:3}\t{Function foo() at foo.c:3}",
            "SAME\t{File main.c at main.c:1}\t{File main.c at main.c:1}",
            "SAME\t{Function main() at main.c:7}\t{Function main() at main.c:3}",
            "ADD\t{File foo.c at foo.c:1}\t{}"
        ).stream().sorted().collect(Collectors.toList());
        // Note: The issue description for Move Function has a slight difference in expected output.
        // It shows "ADD	{File foo.c at foo.c:1}" while the example shows "ADD	{File foo.c at foo.c:1}\t{}".
        // I'm using the latter format as it's consistent with other ADD/REMOVE operations.
        // Also, the example output for main() in v1 is main.c:3, not main.c:7.
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testRenameFile() {
        String basePath = "src/test/resources/c";
        SourceFolder v0 = SourceFolder.from(Paths.get(basePath, "renameFile/v0"), ".c");
        SourceFolder v1 = SourceFolder.from(Paths.get(basePath, "renameFile/v1"), ".c");
        CstDiff diff = comparator.compare(v0, v1);
        List<String> actual = getRelationshipDescriptions(diff);
        List<String> expected = List.of(
            "RENAME_FILE\t{File main.c at main.c:1}\t{File foo.c at foo.c:1}",
            "SAME\t{Function main() at main.c:3}\t{Function main() at foo.c:3}"
        ).stream().sorted().collect(Collectors.toList());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testRenameFunction() {
        String basePath = "src/test/resources/c";
        SourceFolder v0 = SourceFolder.from(Paths.get(basePath, "renameFunction/v0"), ".c");
        SourceFolder v1 = SourceFolder.from(Paths.get(basePath, "renameFunction/v1"), ".c");
        CstDiff diff = comparator.compare(v0, v1);
        List<String> actual = getRelationshipDescriptions(diff);
        List<String> expected = List.of(
            "RENAME_FUNCTION\t{Function foo() at main.c:3}\t{Function bar() at main.c:3}",
            "SAME\t{File main.c at main.c:1}\t{File main.c at main.c:1}",
            "SAME\t{Function main() at main.c:7}\t{Function main() at main.c:7}"
        ).stream().sorted().collect(Collectors.toList());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMoveAndRenameFile() {
        String basePath = "src/test/resources/c";
        SourceFolder v0 = SourceFolder.from(Paths.get(basePath, "moveAndRenameFile/v0"), ".c");
        SourceFolder v1 = SourceFolder.from(Paths.get(basePath, "moveAndRenameFile/v1"), ".c");
        CstDiff diff = comparator.compare(v0, v1);
        List<String> actual = getRelationshipDescriptions(diff);
        List<String> expected = List.of(
            "MOVE_RENAME_FILE\t{File main.c at main.c:1}\t{File bar.c at foo/bar.c:1}",
            "SAME\t{Function main() at main.c:3}\t{Function main() at foo/bar.c:3}"
        ).stream().sorted().collect(Collectors.toList());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMoveAndRenameFunction() {
        String basePath = "src/test/resources/c";
        SourceFolder v0 = SourceFolder.from(Paths.get(basePath, "moveAndRenameFunction/v0"), ".c");
        SourceFolder v1 = SourceFolder.from(Paths.get(basePath, "moveAndRenameFunction/v1"), ".c");
        CstDiff diff = comparator.compare(v0, v1);
        List<String> actual = getRelationshipDescriptions(diff);
        List<String> expected = List.of(
            "MOVE_RENAME_FUNCTION\t{Function foo() at main.c:3}\t{Function bar() at foo.c:3}",
            "SAME\t{File main.c at main.c:1}\t{File main.c at main.c:1}",
            "SAME\t{Function main() at main.c:7}\t{Function main() at main.c:3}",
             "ADD\t{File foo.c at foo.c:1}\t{}"
        ).stream().sorted().collect(Collectors.toList());
        // Similar to Move Function, adjusted ADD and main() line number.
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testExtractFunction() {
        String basePath = "src/test/resources/c";
        SourceFolder v0 = SourceFolder.from(Paths.get(basePath, "extractFunction/v0"), ".c");
        SourceFolder v1 = SourceFolder.from(Paths.get(basePath, "extractFunction/v1"), ".c");
        CstDiff diff = comparator.compare(v0, v1);
        List<String> actual = getRelationshipDescriptions(diff);
        List<String> expected = List.of(
            "EXTRACT_FUNCTION\t{Function extracted() at main.c:3}\t{Function main() at main.c:7}",
            "SAME\t{File main.c at main.c:1}\t{File main.c at main.c:1}",
            "SAME\t{Function main() at main.c:3}\t{Function main() at main.c:7}"
        ).stream().sorted().collect(Collectors.toList());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testInlineFunction() {
        String basePath = "src/test/resources/c";
        SourceFolder v0 = SourceFolder.from(Paths.get(basePath, "inlineFunction/v0"), ".c");
        SourceFolder v1 = SourceFolder.from(Paths.get(basePath, "inlineFunction/v1"), ".c");
        CstDiff diff = comparator.compare(v0, v1);
        List<String> actual = getRelationshipDescriptions(diff);
        List<String> expected = List.of(
            "INLINE_FUNCTION\t{Function foo() at main.c:3}\t{Function main() at main.c:7}",
            "SAME\t{File main.c at main.c:1}\t{File main.c at main.c:1}",
            "SAME\t{Function main() at main.c:7}\t{Function main() at main.c:3}"
        ).stream().sorted().collect(Collectors.toList());
        // Example output for main() in v1 is main.c:3, not main.c:7
        Assert.assertEquals(expected, actual);
    }
}
