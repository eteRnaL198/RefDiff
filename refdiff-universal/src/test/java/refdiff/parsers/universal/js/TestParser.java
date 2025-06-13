package refdiff.parsers.universal.js;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


import org.junit.Test;
import refdiff.core.cst.Location;
import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.io.SourceFileSet;
import refdiff.core.io.SourceFolder;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.universal.UniversalPlugin;

public class TestParser {
  private static final LanguagePlugin parser = new UniversalPlugin();
  private static final String TEST_DATA_BASE_PATH = "src/test/resources/js/grammar";

  @Test
  public void shouldParseClassDeclarations() throws Exception {
    Path baseFolderPath = Paths.get(TEST_DATA_BASE_PATH);
    SourceFileSet sources = SourceFolder.from(baseFolderPath, ".js");
    CstRoot cstRoot = parser.parse(sources);
    List<CstNode> classNodes = cstRoot.getNodes().stream()
        .filter(node -> JsNodeTypes.CLASS.equals(node.getType()))
        .collect(Collectors.toList());

    assertThat("Should find 2 class declarations", classNodes.size(), is(equalTo(2)));

    // Verify Animal class
    Optional<CstNode> animalNodeOpt = classNodes.stream()
        .filter(node -> "Animal".equals(node.getSimpleName()))
        .findFirst();
    CstNode animalNode = animalNodeOpt.get();
    assertThat("Animal node type", animalNode.getType(), is(equalTo(JsNodeTypes.CLASS)));
    assertThat("Animal node simple name", animalNode.getSimpleName(), is(equalTo("Animal")));
    assertThat("Animal node local name", animalNode.getLocalName(), is(equalTo("Animal")));
    assertThat("Animal node namespace", animalNode.getNamespace(), is(equalTo("class/")));
    Location animalLocation = animalNode.getLocation();
    assertThat("Animal location file path", animalLocation.getFile(), is(equalTo("class/class.js")));
    assertThat("Animal location class start line", animalLocation.getLine(), is(equalTo(2)));

    // Verify Dog class
    Optional<CstNode> dogNodeOpt = classNodes.stream()
        .filter(node -> "Dog".equals(node.getSimpleName()))
        .findFirst();
    assertTrue("Dog class node should be present", dogNodeOpt.isPresent());
    CstNode dogNode = dogNodeOpt.get();
    assertThat("Dog node type", dogNode.getType(), is(equalTo(JsNodeTypes.CLASS)));
    assertThat("Dog node simple name", dogNode.getSimpleName(), is(equalTo("Dog")));
    assertThat("Dog node local name", dogNode.getLocalName(), is(equalTo("Dog")));
    assertThat("Dog node namespace", dogNode.getNamespace(), is(equalTo("class/")));
    Location dogLocation = dogNode.getLocation();
    assertThat("Dog location file path", dogLocation.getFile(), is(equalTo("class/class.js")));
    assertThat("Dog location class start line", dogLocation.getLine(), is(equalTo(65)));
  }
}
