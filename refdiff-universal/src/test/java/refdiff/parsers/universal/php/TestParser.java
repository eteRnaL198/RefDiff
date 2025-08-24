package refdiff.parsers.universal.php;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String TEST_DATA_BASE_PATH = "src/test/resources/php/syntax";

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
    ) {
        ExpectedNode(String name, String type, int line, String localName, String namespace, String fileName) {
            this(name, type, line, localName, namespace, fileName, List.of());
        }
    }

    @Test
    public void shouldParsePhpClassesAndFunctionsCorrectly() throws Exception {
        Path baseFolderPath = Paths.get(TEST_DATA_BASE_PATH);
        SourceFileSet sources = SourceFolder.from(baseFolderPath, ".php");
        CstRoot cstRoot = parser.parse(sources);

        List<CstNode> functionNodes = cstRoot.getNodes().stream()
            .filter(node -> PhpNodeTypes.METHOD.equals(node.getType()) || PhpNodeTypes.FUNCTION.equals(node.getType()))
            .collect(Collectors.toList());

        assertThat("Should find 27 function/method nodes", functionNodes.size(), is(equalTo(27)));

        List<ExpectedNode> expectedFunctions = Arrays.asList(
            new ExpectedNode("publicAbstractMethod", PhpNodeTypes.METHOD, 7, "publicAbstractMethod()", "sample.php/", "sample.php"),
            new ExpectedNode("protectedAbstractMethod", PhpNodeTypes.METHOD, 10, "protectedAbstractMethod($param)", "sample.php/", "sample.php", List.of("$param")),
            new ExpectedNode("interfaceMethod", PhpNodeTypes.METHOD, 17, "interfaceMethod($a)", "sample.php/", "sample.php", List.of("$a")),
            new ExpectedNode("traitMethod", PhpNodeTypes.METHOD, 24, "traitMethod($name)", "sample.php/", "sample.php", List.of("$name")),
            new ExpectedNode("privateTraitMethod", PhpNodeTypes.METHOD, 30, "privateTraitMethod()", "sample.php/", "sample.php"),
            new ExpectedNode("publicMethod", PhpNodeTypes.METHOD, 44, "publicMethod()", "sample.php/", "sample.php"),
            new ExpectedNode("protectedMethod", PhpNodeTypes.METHOD, 50, "protectedMethod($message)", "sample.php/", "sample.php", List.of("$message")),
            new ExpectedNode("privateMethod", PhpNodeTypes.METHOD, 56, "privateMethod()", "sample.php/", "sample.php"),
            new ExpectedNode("publicStaticMethod", PhpNodeTypes.METHOD, 64, "publicStaticMethod()", "sample.php/", "sample.php"),
            new ExpectedNode("protectedStaticMethod", PhpNodeTypes.METHOD, 70, "protectedStaticMethod()", "sample.php/", "sample.php"),
            new ExpectedNode("finalPublicMethod", PhpNodeTypes.METHOD, 76, "finalPublicMethod()", "sample.php/", "sample.php"),
            new ExpectedNode("finalPublicStaticMethod", PhpNodeTypes.METHOD, 82, "finalPublicStaticMethod()", "sample.php/", "sample.php"),
            new ExpectedNode("methodWithTypedArgument", PhpNodeTypes.METHOD, 90, "methodWithTypedArgument($a, $b)", "sample.php/", "sample.php", List.of("$a", "$b")),
            new ExpectedNode("methodWithDefaultValue", PhpNodeTypes.METHOD, 96, "methodWithDefaultValue($name)", "sample.php/", "sample.php", List.of("$name")),
            new ExpectedNode("methodByReference", PhpNodeTypes.METHOD, 102, "methodByReference(&$number)", "sample.php/", "sample.php", List.of("&$number")),
            new ExpectedNode("methodWithVariadicArguments", PhpNodeTypes.METHOD, 108, "methodWithVariadicArguments(...$names)", "sample.php/", "sample.php", List.of("...$names")),
            new ExpectedNode("complexArguments", PhpNodeTypes.METHOD, 114, "complexArguments($name, $id, ...$data)", "sample.php/", "sample.php", List.of("$name", "$id", "...$data")),
            new ExpectedNode("methodWithReturnType", PhpNodeTypes.METHOD, 122, "methodWithReturnType()", "sample.php/", "sample.php"),
            new ExpectedNode("methodWithVoidReturnType", PhpNodeTypes.METHOD, 128, "methodWithVoidReturnType()", "sample.php/", "sample.php"),
            new ExpectedNode("methodWithNullableReturnType", PhpNodeTypes.METHOD, 134, "methodWithNullableReturnType()", "sample.php/", "sample.php"),
            new ExpectedNode("publicAbstractMethod", PhpNodeTypes.METHOD, 142, "publicAbstractMethod()", "sample.php/", "sample.php"),
            new ExpectedNode("protectedAbstractMethod", PhpNodeTypes.METHOD, 147, "protectedAbstractMethod($param)", "sample.php/", "sample.php", List.of("$param")),
            new ExpectedNode("interfaceMethod", PhpNodeTypes.METHOD, 153, "interfaceMethod($a)", "sample.php/", "sample.php", List.of("$a")),
            new ExpectedNode("__construct", PhpNodeTypes.METHOD, 160, "__construct($name, $id)", "sample.php/", "sample.php", List.of("$name", "$id")),
            new ExpectedNode("createAnonymousClass", PhpNodeTypes.METHOD, 169, "createAnonymousClass()", "sample.php/", "sample.php"),
            new ExpectedNode("anonymousMethod", PhpNodeTypes.METHOD, 172, "anonymousMethod()", "sample.php/", "sample.php"),
            new ExpectedNode("globalFunction", PhpNodeTypes.FUNCTION, 183, "globalFunction($param)", "sample.php/", "sample.php", List.of("$param"))
        );

        for (ExpectedNode expected : expectedFunctions) {
            CstNode actualNode = findNode(functionNodes, expected.name(), expected.line());
            assertThat(actualNode.getType(), is(equalTo(expected.type())));
            assertThat(actualNode.getSimpleName(), is(equalTo(expected.name())));
            assertThat(actualNode.getLocalName(), is(equalTo(expected.localName())));
            if (expected.namespace() != null) {
                assertThat(actualNode.getNamespace(), is(equalTo(expected.namespace())));
            }
            Location location = actualNode.getLocation();
            assertThat(location.getFile(), is(equalTo(expected.fileName())));
            assertThat(location.getBeginLine(), is(equalTo(expected.line())));
            List<String> actualParamNames = actualNode.getParameters().stream()
                .map(Parameter::getName)
                .collect(Collectors.toList());
            assertThat(actualParamNames, is(equalTo(expected.params())));
        }
    }
}