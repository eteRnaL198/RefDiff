package refdiff.parsers.universal.ruby;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
    private static final String TEST_DATA_BASE_PATH = "src/test/resources/ruby/parse";

    private CstNode findMethod(List<CstNode> nodes, String name, int line) {
        return nodes.stream()
            .filter(node -> name.equals(node.getSimpleName()) && node.getLocation().getLine() == line)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Method with name '" + name + "' at line " + line + " not found."));
    }

    private record ExpectedMethod(
        String name,
        int line,
        List<String> params
    ) {}

    @Test
    public void shouldParseMethodDefinitionsCorrectly() throws Exception {
        Path baseFolderPath = Paths.get(TEST_DATA_BASE_PATH);
        SourceFileSet sources = SourceFolder.from(baseFolderPath, ".rb");
        CstRoot cstRoot = parser.parse(sources);

        List<CstNode> methodNodes = cstRoot.getNodes().stream()
            .filter(node -> RubyNodeTypes.METHOD.equals(node.getType()))
            .collect(Collectors.toList());

        // Expected methods based on the query for `def` and `singleton_method`
        // Dynamic methods (define_method) and accessors (attr_*) are not included.
        assertThat("Should find 27 method nodes", methodNodes.size(), is(equalTo(27)));

        List<ExpectedMethod> expectedMethodsData = Arrays.asList(
            new ExpectedMethod("instance_method_example", 7, List.of()),
            new ExpectedMethod("class_method_example", 12, List.of()),
            new ExpectedMethod("another_class_method_example", 21, List.of()),
            new ExpectedMethod("greet", 40, List.of()),
            new ExpectedMethod("class_specific_method", 52, List.of()),
            new ExpectedMethod("initialize", 96,  List.of("name", "email", "age")), // User's initialize
            new ExpectedMethod("display_age", 102,  List.of()),
            new ExpectedMethod("hello", 125,  List.of()),
            new ExpectedMethod("method_missing", 141,  List.of("method_name", "*args", "&block")),
            new ExpectedMethod("respond_to_missing?", 151,  List.of("method_name", "include_private")),
            new ExpectedMethod("initialize", 173, List.of()), // ProcRunner's initialize
            new ExpectedMethod("run_proc", 177,  List.of("val")),
            new ExpectedMethod("run_lambda", 181,  List.of("val")),
            new ExpectedMethod("run_instance_lambda", 185,  List.of("val")),
            new ExpectedMethod("run_class_proc", 189,  List.of("val")),
            new ExpectedMethod("public_method", 205,  List.of()),
            new ExpectedMethod("protected_method", 213,  List.of()),
            new ExpectedMethod("protected_method_called_from_public", 217,  List.of()),
            new ExpectedMethod("private_method", 223,List.of()),
            new ExpectedMethod("private_method_called_from_public", 227, List.of()),
            new ExpectedMethod("call_protected_from_subclass", 233,List.of("other")),
            new ExpectedMethod("call_own_private", 237, List.of()),
            new ExpectedMethod("top_level_method_example", 260, List.of("name")),
            new ExpectedMethod("optional_param_method", 283, List.of("a")),
            new ExpectedMethod("keyword_param_method", 287, List.of("a")),
            new ExpectedMethod("hash_splat_param_method", 291, List.of("**options")),
            new ExpectedMethod("all_param_types", 295, List.of("required", "optional", "*splat", "keyword_req", "keyword_opt", "**hash_splat", "&block"))
        );

        for (ExpectedMethod expected : expectedMethodsData) {
            CstNode actualNode = findMethod(methodNodes, expected.name(), expected.line());

            assertThat("Type for " + expected.name(), actualNode.getType(), is(equalTo(RubyNodeTypes.METHOD)));
            assertThat("SimpleName for " + expected.name(), actualNode.getSimpleName(), is(equalTo(expected.name())));
            assertThat("LocalName for " + expected.name(), actualNode.getLocalName(), is(equalTo(expected.name())));

            Location actualLocation = actualNode.getLocation();
            assertThat("Location file for " + expected.name(), actualLocation.getFile(), is(equalTo("method.rb")));
            assertThat("Location line for " + expected.name(), actualLocation.getLine(), is(equalTo(expected.line())));

            List<String> actualParamNames = actualNode.getParameters().stream()
                .map(Parameter::getName)
                .collect(Collectors.toList());
            assertThat("Parameters for " + expected.name(), actualParamNames, is(equalTo(expected.params())));
        }
    }
}
