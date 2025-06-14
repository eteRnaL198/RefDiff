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
import refdiff.core.cst.Parameter;
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

  @Test
  public void shouldParseFunctionDeclarations() throws Exception {
    Path baseFolderPath = Paths.get(TEST_DATA_BASE_PATH, "function");
    SourceFileSet sources = SourceFolder.from(baseFolderPath, ".js");
    CstRoot cstRoot = parser.parse(sources);

    List<CstNode> actualFunctionNodes = new ArrayList<>();
    cstRoot.forEachNode((node, _) -> {
      if (JsNodeTypes.FUNCTION.equals(node.getType())) {
        actualFunctionNodes.add(node);
      }
    });

    assertThat("Should find 17 function declarations", actualFunctionNodes.size(), is(equalTo(17)));

    // 1. classicFunction
    CstNode classicFuncNode = actualFunctionNodes.stream()
        .filter(node -> "classicFunction".equals(node.getSimpleName()))
        .findFirst().orElseThrow(() -> new AssertionError("classicFunction not found"));
    assertThat(classicFuncNode.getType(), is(equalTo(JsNodeTypes.FUNCTION)));
    assertThat(classicFuncNode.getSimpleName(), is(equalTo("classicFunction")));
    assertThat(classicFuncNode.getLocalName(), is(equalTo("classicFunction")));
    assertThat(classicFuncNode.getNamespace(), is(equalTo(""))); // Relative to baseFolderPath
    assertThat(classicFuncNode.getLocation().getFile(), is(equalTo("function.js")));
    assertThat(classicFuncNode.getLocation().getLine(), is(equalTo(2)));
    List<String> classicFuncParamNames = classicFuncNode.getParameters().stream()
        .map(Parameter::getName)
        .collect(Collectors.toList());
    assertThat(classicFuncParamNames, is(equalTo(List.of("param1", "param2"))));

    // 2. anonymousFunction (assigned to const)
    CstNode anonymousFuncNode = actualFunctionNodes.stream()
        .filter(node -> "anonymousFunction".equals(node.getSimpleName()))
        .findFirst().orElseThrow(() -> new AssertionError("anonymousFunction not found"));
    assertThat(anonymousFuncNode.getSimpleName(), is(equalTo("anonymousFunction")));
    assertThat(anonymousFuncNode.getLocation().getLine(), is(equalTo(10)));
    List<String> anonymousFuncParamNames = anonymousFuncNode.getParameters().stream()
        .map(Parameter::getName)
        .collect(Collectors.toList());
    assertThat(anonymousFuncParamNames, is(equalTo(List.of("a", "b"))));

    // 3. arrowFunctionSimple
    CstNode arrowFuncSimpleNode = actualFunctionNodes.stream()
        .filter(node -> "arrowFunctionSimple".equals(node.getSimpleName()))
        .findFirst().orElseThrow(() -> new AssertionError("arrowFunctionSimple not found"));
    assertThat(arrowFuncSimpleNode.getType(), is(equalTo(JsNodeTypes.FUNCTION)));
    assertThat(arrowFuncSimpleNode.getSimpleName(), is(equalTo("arrowFunctionSimple")));
    assertThat(arrowFuncSimpleNode.getLocalName(), is(equalTo("arrowFunctionSimple")));
    assertThat(arrowFuncSimpleNode.getNamespace(), is(equalTo("")));
    assertThat(arrowFuncSimpleNode.getLocation().getFile(), is(equalTo("function.js")));
    assertThat(arrowFuncSimpleNode.getLocation().getLine(), is(equalTo(16)));
    List<String> arrowFuncSimpleParamNames = arrowFuncSimpleNode.getParameters().stream()
        .map(Parameter::getName)
        .collect(Collectors.toList());
    assertThat(arrowFuncSimpleParamNames, is(equalTo(List.of("x", "y"))));

    // 4. arrowFunctionSingleParam (no parentheses for param)
    CstNode arrowFuncSingleParamNode = actualFunctionNodes.stream()
        .filter(node -> "arrowFunctionSingleParam".equals(node.getSimpleName()))
        .findFirst().orElseThrow(() -> new AssertionError("arrowFunctionSingleParam not found"));
    assertThat(arrowFuncSingleParamNode.getSimpleName(), is(equalTo("arrowFunctionSingleParam")));
    assertThat(arrowFuncSingleParamNode.getLocation().getLine(), is(equalTo(19)));
    List<String> arrowFuncSingleParamNames = arrowFuncSingleParamNode.getParameters().stream()
        .map(Parameter::getName)
        .collect(Collectors.toList());
    assertThat(arrowFuncSingleParamNames, is(equalTo(List.of("param"))));

    // 5. processArguments (rest parameters)
    CstNode processArgsNode = actualFunctionNodes.stream()
        .filter(node -> "processArguments".equals(node.getSimpleName()))
        .findFirst().orElseThrow(() -> new AssertionError("processArguments not found"));
    assertThat(processArgsNode.getSimpleName(), is(equalTo("processArguments")));
    assertThat(processArgsNode.getLocation().getLine(), is(equalTo(50)));
    List<String> processArgsParamNames = processArgsNode.getParameters().stream()
        .map(Parameter::getName)
        .collect(Collectors.toList());
    assertThat(processArgsParamNames, is(equalTo(List.of("firstArg", "restArgs"))));
    
    // 6. greet (default parameters)
    CstNode greetNode = actualFunctionNodes.stream()
        .filter(node -> "greet".equals(node.getSimpleName()))
        .findFirst().orElseThrow(() -> new AssertionError("greet not found"));
    assertThat(greetNode.getSimpleName(), is(equalTo("greet")));
    assertThat(greetNode.getLocation().getLine(), is(equalTo(58)));
    List<String> greetParamNames = greetNode.getParameters().stream()
        .map(Parameter::getName)
        .collect(Collectors.toList());
    assertThat(greetParamNames, is(equalTo(List.of("name"))));

    // 7. innerFunction (nested function)
    // TODO 子要素になっているかを確認する
    CstNode innerFuncNode = actualFunctionNodes.stream()
        .filter(node -> "innerFunction".equals(node.getSimpleName()))
        .findFirst().orElseThrow(() -> new AssertionError("innerFunction not found"));
    assertThat(innerFuncNode.getSimpleName(), is(equalTo("innerFunction")));
    assertThat(innerFuncNode.getLocation().getLine(), is(equalTo(41)));
    List<String> innerFuncParamNames = innerFuncNode.getParameters().stream()
        .map(Parameter::getName)
        .collect(Collectors.toList());
    assertThat(innerFuncParamNames, is(equalTo(List.of("innerParam"))));
    assertTrue("innerFunction's parent should be a FILE node", 
        innerFuncNode.getParent().isPresent() && 
        JsNodeTypes.FILE.equals(innerFuncNode.getParent().get().getType()));

    // Verify other functions exist (simplified check)
    assertTrue(actualFunctionNodes.stream().anyMatch(n -> "arrowFunctionNoParam".equals(n.getSimpleName()) && n.getLocation().getLine() == 22));
    assertTrue(actualFunctionNodes.stream().anyMatch(n -> "arrowFunctionBlockBody".equals(n.getSimpleName()) && n.getLocation().getLine() == 25));
    assertTrue(actualFunctionNodes.stream().anyMatch(n -> "higherOrderFunction".equals(n.getSimpleName()) && n.getLocation().getLine() == 32));
    assertTrue(actualFunctionNodes.stream().anyMatch(n -> "outerFunction".equals(n.getSimpleName()) && n.getLocation().getLine() == 39));
    assertTrue(actualFunctionNodes.stream().anyMatch(n -> "performAsyncOperation".equals(n.getSimpleName()) && n.getLocation().getLine() == 65));
    assertTrue(actualFunctionNodes.stream().anyMatch(n -> "idGenerator".equals(n.getSimpleName()) && n.getLocation().getLine() == 91));
    assertTrue(actualFunctionNodes.stream().anyMatch(n -> "fibonacciSequence".equals(n.getSimpleName()) && n.getLocation().getLine() == 101));
    assertTrue(actualFunctionNodes.stream().anyMatch(n -> "functionWithErrorHandling".equals(n.getSimpleName()) && n.getLocation().getLine() == 114));
    assertTrue(actualFunctionNodes.stream().anyMatch(n -> "functionWithIIFE".equals(n.getSimpleName()) && n.getLocation().getLine() == 132));
    assertTrue(actualFunctionNodes.stream().anyMatch(n -> "labeledLoopFunction".equals(n.getSimpleName()) && n.getLocation().getLine() == 145));
  }
}
