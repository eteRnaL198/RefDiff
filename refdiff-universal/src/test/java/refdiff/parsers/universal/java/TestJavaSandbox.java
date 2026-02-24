package refdiff.parsers.universal.java;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.Location;
import refdiff.core.cst.Parameter;
import refdiff.core.diff.CstComparator;
import refdiff.core.diff.CstDiff;
import refdiff.core.io.SourceFileSet;
import refdiff.core.io.SourceFolder;
import refdiff.parsers.LanguagePlugin;

public class TestJavaSandbox {
  private static final LanguagePlugin parser = new JavaPlugin();
  private static final String TEST_DATA_BASE_PATH = "src/test/resources/java/sandbox/";

  // @Test
  public void shouldParseClassDeclarationsCorrectly() throws Exception {
    Path baseFolderPath = Paths.get(TEST_DATA_BASE_PATH);
    SourceFileSet sources = SourceFolder.from(baseFolderPath, ".java");
    CstRoot cstRoot = parser.parse(sources);

    cstRoot.forEachNode((node, depth) -> {
        System.out.println(node.getSimpleName() + " : (" + node.getLocation().getBegin() + "," + node.getLocation().getEnd() + "), (" + node.getLocation().getBodyBegin() + "," + node.getLocation().getBodyEnd() + ")");
    });
  }
  @Test
  public void shouldCompareSandboxBeforeAndAfter() throws Exception {
    Path baseFolderPath = Paths.get(TEST_DATA_BASE_PATH);
    SourceFolder sourcesBefore = SourceFolder.from(baseFolderPath.resolve("before"), ".java");
    SourceFolder sourcesAfter = SourceFolder.from(baseFolderPath.resolve("after"), ".java");
    CstComparator comparator = new CstComparator(parser);
    CstDiff diff = comparator.compare(sourcesBefore, sourcesAfter);
    diff.getRelationships().forEach(relationship -> {
        System.out.println(relationship.getType() + " : " + relationship.getNodeBefore().getSimpleName() + " -> " + relationship.getNodeAfter().getSimpleName());
    });
  }
}
