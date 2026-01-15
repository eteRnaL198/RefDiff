package refdiff.parsers.universal.js;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;


import org.junit.jupiter.api.Test;
import refdiff.core.cst.Location;
import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.io.SourceFileSet;
import refdiff.core.cst.Parameter;
import refdiff.core.io.SourceFolder;
import refdiff.parsers.LanguagePlugin;

public class TestJsValidation {
  private static final LanguagePlugin parser = new JsPlugin();
  private static final String TEST_DATA_BASE_PATH = "src/test/resources/js/syntax";

  private CstNode findNode(List<CstNode> nodes, String name, int line, String file) {
    return nodes.stream()
        .filter(node -> name.equals(node.getSimpleName()) && node.getLocation().getBeginLine() == line && node.getLocation().getFile().equals(file))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Node with name '" + name + "' at line " + line + " not found."));
  }

  private record ExpectedNode(
      String name,
      String type,
      int line,
      String fileName
  ) {
      ExpectedNode(String name, String type, int line, String fileName) {
          this.name = name;
          this.type = type;
          this.line = line;
          this.fileName = fileName;
      }
  }
  
  @Test
  public void shouldParse() throws Exception {
    Path baseFolderPath = Paths.get(TEST_DATA_BASE_PATH, "validation");
    SourceFileSet sources = SourceFolder.from(baseFolderPath, ".js");
    CstRoot cstRoot = parser.parse(sources);

    List<CstNode> actualNodes = new ArrayList<>();
    cstRoot.forEachNode((node, depth) -> {
      actualNodes.add(node);
    });

    List<ExpectedNode> expectedNodes = Arrays.asList(
        new ExpectedNode("AbstractButton3", JsNodeTypes.CLASS, 1, "benchmark.js"),
        new ExpectedNode("render", JsNodeTypes.FUNCTION, 2, "benchmark.js"),
        new ExpectedNode("onClick", JsNodeTypes.FUNCTION, 11, "benchmark.js"),

        new ExpectedNode("render", JsNodeTypes.FUNCTION, 1, "benchmark1.js"),

        new ExpectedNode("ReactImage0", JsNodeTypes.FUNCTION, 1, "benchmark2.js"),

        new ExpectedNode("_load", JsNodeTypes.FUNCTION, 1, "flags.js"),

        new ExpectedNode("exports", JsNodeTypes.FUNCTION, 1, "print-prerelease-summary.js"),
        
        new ExpectedNode("exports", JsNodeTypes.FUNCTION, 1, "parse-params.js"),

        new ExpectedNode("exports", JsNodeTypes.FUNCTION, 1, "check-out-packages.js")

    );

    for (ExpectedNode expected : expectedNodes) {
        CstNode actualNode = findNode(actualNodes, expected.name(), expected.line(), expected.fileName()  );
        assertThat(actualNode.getType(), is(equalTo(expected.type())));
        assertThat(actualNode.getSimpleName(), is(equalTo(expected.name())));
        Location location = actualNode.getLocation();
        assertThat(location.getFile(), is(equalTo(expected.fileName())));
        assertThat(location.getBeginLine(), is(equalTo(expected.line())));
    }
  }
}
