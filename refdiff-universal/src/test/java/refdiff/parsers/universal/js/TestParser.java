package refdiff.parsers.universal.js;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
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
  public void shouldParseFileNodeCorrectly() throws Exception {
    Path fileTestPath = Paths.get(TEST_DATA_BASE_PATH, "file");
    SourceFileSet sources = SourceFolder.from(fileTestPath, ".js");
    CstRoot cstRoot = parser.parse(sources);

    List<CstNode> fileNodes = cstRoot.getNodes().stream()
        .filter(node -> JsNodeTypes.FILE.equals(node.getType()))
        .collect(Collectors.toList());

        System.out.println("File nodes: " + fileNodes.size());
    assertThat("Should find 1 file node", fileNodes.size(), is(equalTo(1)));

    CstNode fileNode = fileNodes.get(0);
    assertThat("File node type", fileNode.getType(), is(equalTo(JsNodeTypes.FILE)));
    assertThat("File node simple name", fileNode.getSimpleName(), is(equalTo("file.js")));
    assertThat("File node local name", fileNode.getLocalName(), is(equalTo("file.js")));
    assertThat("File node namespace", fileNode.getNamespace(), is(equalTo("dir/")));
    Location location = fileNode.getLocation();
    assertThat("Location file path", location.getFile(), is(equalTo("dir/file.js")));
    assertThat("Location start byte", location.getBegin(), is(equalTo(0)));
    assertThat("Location body start byte", location.getBodyBegin(), is(equalTo(0)));
    assertThat("Location start line", location.getLine(), is(equalTo(1)));
  }

  @Test
  public void shouldParseClassDeclarations() throws Exception {
    Path baseFolderPath = Paths.get(TEST_DATA_BASE_PATH, "class");
    SourceFileSet sources = SourceFolder.from(baseFolderPath, ".js");
    CstRoot cstRoot = parser.parse(sources);

    List<CstNode> classNodes = new ArrayList<>();
    cstRoot.forEachNode((node, _) -> {
      if (JsNodeTypes.CLASS.equals(node.getType())) {
        classNodes.add(node);
      }
    });

    assertThat("Should find 2 class declarations", classNodes.size(), is(equalTo(2)));

    // Verify Animal class
    Optional<CstNode> animalNodeOpt = classNodes.stream()
        .filter(node -> "Animal".equals(node.getSimpleName()))
        .findFirst();
    CstNode animalNode = animalNodeOpt.get();
    assertThat("Animal node type", animalNode.getType(), is(equalTo(JsNodeTypes.CLASS)));
    assertThat("Animal node simple name", animalNode.getSimpleName(), is(equalTo("Animal")));
    assertThat("Animal node local name", animalNode.getLocalName(), is(equalTo("Animal")));
    assertThat("Animal node namespace", animalNode.getNamespace(), is(equalTo("dir/")));
    Location animalLocation = animalNode.getLocation();
    assertThat("Animal location file path", animalLocation.getFile(), is(equalTo("dir/class.js")));
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
    assertThat("Dog node namespace", dogNode.getNamespace(), is(equalTo("dir/")));
    Location dogLocation = dogNode.getLocation();
    assertThat("Dog location file path", dogLocation.getFile(), is(equalTo("dir/class.js")));
    assertThat("Dog location class start line", dogLocation.getLine(), is(equalTo(65)));
  }
}
