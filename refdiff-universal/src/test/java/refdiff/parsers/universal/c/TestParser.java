package refdiff.parsers.universal.c;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.Location;
import refdiff.core.cst.Parameter;
import refdiff.core.io.SourceFileSet;
import refdiff.core.io.SourceFolder;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.universal.UniversalPlugin;

public class TestParser {
    private static final LanguagePlugin parser = new UniversalPlugin();

    private CstNode findNode(List<CstNode> nodes, String name, int line) {
        return nodes.stream()
            .filter(node -> name.equals(node.getSimpleName()) && node.getLocation().getBeginLine() == line)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Node with name '" + name + "' at line " + line + " not found."));
    }

    private record ExpectedNode(
        String name,
        String type,
        int line,
        String localName,
        String namespace,
        String fileName,
        List<String> params
    ) {}

    @Test
    public void shouldParseCFunctionsCorrectly() throws Exception {
        // Setup: Cファイルを読み込む
        Path basePath = Paths.get("test-data/c/parser");
		SourceFolder sources = SourceFolder.from(basePath, Paths.get("dir1/hello.c"));
        CstRoot cstRoot = parser.parse(sources);

        // function 型のノードをすべて抽出
        List<CstNode> functionNodes = new ArrayList<>();
        cstRoot.forEachNode((node, _) -> {
            if (CNodeTypes.FUNCTION.equals(node.getType())) {
                functionNodes.add(node);
            }
        });

        // 検証1: 関数定義の総数が正しいか
        assertThat("Should find 15 function nodes", functionNodes.size(), is(equalTo(15)));

        // 検証2: 各関数が正しく解析されているか
        // 新しいCソースコードに対応する期待値リスト
        List<ExpectedNode> expectedNodes = Arrays.asList(
            new ExpectedNode("main", CNodeTypes.FUNCTION, 54, "main()", null, "functions.c", List.of()),
            new ExpectedNode("function_no_args_no_return", CNodeTypes.FUNCTION, 160, "function_no_args_no_return()", null, "functions.c", List.of()),
            new ExpectedNode("function_with_args_no_return", CNodeTypes.FUNCTION, 164, "function_with_args_no_return(int, int)", null, "functions.c", List.of("a", "b")),
            new ExpectedNode("add", CNodeTypes.FUNCTION, 170, "add(int, int)", null, "functions.c", List.of("a", "b")),
            new ExpectedNode("old_style_add", CNodeTypes.FUNCTION, 177, "old_style_add(int, int)", null, "functions.c", List.of("a", "b")),
            new ExpectedNode("internal_function", CNodeTypes.FUNCTION, 188, "internal_function()", null, "functions.c", List.of()),
            new ExpectedNode("multiply", CNodeTypes.FUNCTION, 193, "multiply(int, int)", null, "functions.c", List.of("a", "b")),
            new ExpectedNode("increment", CNodeTypes.FUNCTION, 201, "increment(int*)", null, "functions.c", List.of("num_ptr")),
            new ExpectedNode("get_global_variable_address", CNodeTypes.FUNCTION, 207, "get_global_variable_address()", null, "functions.c", List.of()),
            new ExpectedNode("create_person", CNodeTypes.FUNCTION, 215, "create_person(const char*, int)", null, "functions.c", List.of("name", "age")),
            new ExpectedNode("celebrate_birthday", CNodeTypes.FUNCTION, 222, "celebrate_birthday(Person*)", null, "functions.c", List.of("p")),
            new ExpectedNode("subtract", CNodeTypes.FUNCTION, 231, "subtract(int, int)", null, "functions.c", List.of("a", "b")),
            new ExpectedNode("perform_calc", CNodeTypes.FUNCTION, 234, "perform_calc(int, int, ArithmeticOperation)", null, "functions.c", List.of("a", "b", "func")),
            new ExpectedNode("factorial", CNodeTypes.FUNCTION, 244, "factorial(int)", null, "functions.c", List.of("n")),
            new ExpectedNode("sum_all", CNodeTypes.FUNCTION, 255, "sum_all(int, ...)", null, "functions.c", List.of("count"))
        );

        for (ExpectedNode expected : expectedNodes) {
            CstNode actualNode = findNode(functionNodes, expected.name(), expected.line());

            assertThat("Type for " + expected.name(), actualNode.getType(), is(equalTo(expected.type())));
            assertThat("SimpleName for " + expected.name(), actualNode.getSimpleName(), is(equalTo(expected.name())));
            
            // ★修正点: localName (シグネチャ) を正しく検証する
            assertThat("LocalName for " + expected.name(), actualNode.getLocalName(), is(equalTo(expected.localName())));
            
            if (expected.namespace() != null) {
                assertThat("Namespace for " + expected.name(), actualNode.getNamespace(), is(equalTo(expected.namespace())));
            }

            Location actualLocation = actualNode.getLocation();
            assertThat("Location file for " + expected.name(), Paths.get(actualLocation.getFile()).getFileName().toString(), is(equalTo(expected.fileName())));
            assertThat("Location line for " + expected.name(), actualLocation.getBeginLine(), is(equalTo(expected.line())));

            List<String> actualParamNames = actualNode.getParameters().stream()
                .map(Parameter::getName)
                .collect(Collectors.toList());
            assertThat("Parameters for " + expected.name(), actualParamNames, is(equalTo(expected.params())));
        }
    }
}