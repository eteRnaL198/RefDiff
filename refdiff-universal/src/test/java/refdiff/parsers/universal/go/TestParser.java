package refdiff.parsers.universal.go;

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
    private static final String TEST_DATA_BASE_PATH = "src/test/resources/go/syntax";

    // Helper method to find a node, similar to the JS and Ruby tests
    private CstNode findNode(List<CstNode> nodes, String name, int line) {
        return nodes.stream()
            .filter(node -> name.equals(node.getSimpleName()) && node.getLocation().getLine() == line)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Node with name '" + name + "' at line " + line + " not found."));
    }

    // Record for expected node data, similar to the JS and Ruby tests
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
    public void shouldParseGoFunctionsAndStructsCorrectly() throws Exception {
        Path baseFolderPath = Paths.get(TEST_DATA_BASE_PATH);
        SourceFileSet sources = SourceFolder.from(baseFolderPath, ".go");
        CstRoot cstRoot = parser.parse(sources);

        List<CstNode> allGoNodes = new ArrayList<>();
        cstRoot.forEachNode((node, _) -> {
            allGoNodes.add(node);
        });

        // Expected nodes from keyvalue.go, runner.go, and updater_windows.go
        List<ExpectedNode> expectedNodes = Arrays.asList(
            // keyvalue.go
            new ExpectedNode("KeyValue", GoNodeTypes.TYPE_DECLARATION, 8, "KeyValue", "gguf", "keyvalue.go"),
            
            new ExpectedNode("Valid", GoNodeTypes.METHOD, 13, "Valid()", "gguf.KeyValue", "keyvalue.go", List.of()),
            new ExpectedNode("Value", GoNodeTypes.TYPE_DECLARATION, 17, "Value", "gguf", "keyvalue.go"),
            
            new ExpectedNode("value", GoNodeTypes.FUNCTION, 21, "value(v, kinds)", "gguf", "keyvalue.go", List.of("v", "kinds")),
            new ExpectedNode("values", GoNodeTypes.FUNCTION, 29, "values(v, kinds)", "gguf", "keyvalue.go", List.of("v", "kinds")),
            new ExpectedNode("Int", GoNodeTypes.METHOD, 43, "Int()", "gguf.Value", "keyvalue.go", List.of()),
            new ExpectedNode("Ints", GoNodeTypes.METHOD, 48, "Ints()", "gguf.Value", "keyvalue.go", List.of()),
            new ExpectedNode("Uint", GoNodeTypes.METHOD, 53, "Uint()", "gguf.Value", "keyvalue.go", List.of()),
            new ExpectedNode("Uints", GoNodeTypes.METHOD, 58, "Uints()", "gguf.Value", "keyvalue.go", List.of()),
            new ExpectedNode("Float", GoNodeTypes.METHOD, 63, "Float()", "gguf.Value", "keyvalue.go", List.of()),
            new ExpectedNode("Floats", GoNodeTypes.METHOD, 68, "Floats()", "gguf.Value", "keyvalue.go", List.of()),
            new ExpectedNode("Bool", GoNodeTypes.METHOD, 73, "Bool()", "gguf.Value", "keyvalue.go", List.of()),
            new ExpectedNode("Bools", GoNodeTypes.METHOD, 78, "Bools()", "gguf.Value", "keyvalue.go", List.of()),
            new ExpectedNode("String", GoNodeTypes.METHOD, 83, "String()", "gguf.Value", "keyvalue.go", List.of()),
            new ExpectedNode("Strings", GoNodeTypes.METHOD, 88, "Strings()", "gguf.Value", "keyvalue.go", List.of()),

            // runner.go
            new ExpectedNode("Sequence", GoNodeTypes.TYPE_DECLARATION, 41, "Sequence", "ollamarunner", "runner.go"),
            

            new ExpectedNode("NewSequenceParams", GoNodeTypes.TYPE_DECLARATION, 97, "NewSequenceParams", "ollamarunner", "runner.go"),
            

            new ExpectedNode("NewSequence", GoNodeTypes.METHOD, 105, "NewSequence(prompt, images, params)", "ollamarunner.Server", "runner.go", List.of("prompt", "images", "params")),
            new ExpectedNode("inputs", GoNodeTypes.METHOD, 184, "inputs(prompt, images)", "ollamarunner.Server", "runner.go", List.of("prompt", "images")),

            new ExpectedNode("Server", GoNodeTypes.TYPE_DECLARATION, 261, "Server", "ollamarunner", "runner.go"),
            

            new ExpectedNode("allNil", GoNodeTypes.METHOD, 307, "allNil()", "ollamarunner.Server", "runner.go", List.of()),
            new ExpectedNode("flushPending", GoNodeTypes.FUNCTION, 316, "flushPending(seq)", "ollamarunner", "runner.go", List.of("seq")),
            new ExpectedNode("removeSequence", GoNodeTypes.METHOD, 342, "removeSequence(seqIndex, reason)", "ollamarunner.Server", "runner.go", List.of("seqIndex", "reason")),
            new ExpectedNode("run", GoNodeTypes.METHOD, 354, "run(ctx)", "ollamarunner.Server", "runner.go", List.of("ctx")),
            new ExpectedNode("processBatch", GoNodeTypes.METHOD, 370, "processBatch()", "ollamarunner.Server", "runner.go", List.of()),
            new ExpectedNode("completion", GoNodeTypes.METHOD, 589, "completion(w, r)", "ollamarunner.Server", "runner.go", List.of("w", "r")),
            new ExpectedNode("health", GoNodeTypes.METHOD, 713, "health(w, r)", "ollamarunner.Server", "runner.go", List.of("w", "r")),

            new ExpectedNode("multiLPath", GoNodeTypes.TYPE_DECLARATION, 723, "multiLPath", "ollamarunner", "runner.go"),
            new ExpectedNode("Set", GoNodeTypes.METHOD, 725, "Set(value)", "ollamarunner.multiLPath", "runner.go", List.of("value")),
            new ExpectedNode("String", GoNodeTypes.METHOD, 730, "String()", "ollamarunner.multiLPath", "runner.go", List.of()),

            new ExpectedNode("reserveWorstCaseGraph", GoNodeTypes.METHOD, 734, "reserveWorstCaseGraph()", "ollamarunner.Server", "runner.go", List.of()),
            new ExpectedNode("initModel", GoNodeTypes.METHOD, 831, "initModel(mpath, params, lpath, parallel, kvCacheType, kvSize, multiUserCache)", "ollamarunner.Server", "runner.go", List.of("mpath", "params", "lpath", "parallel", "kvCacheType", "kvSize", "multiUserCache")),
            new ExpectedNode("load", GoNodeTypes.METHOD, 868, "load(ctx, mpath, params, lpath, parallel, kvCacheType, kvSize, multiUserCache)", "ollamarunner.Server", "runner.go", List.of("ctx", "mpath", "params", "lpath", "parallel", "kvCacheType", "kvSize", "multiUserCache")),
            new ExpectedNode("Execute", GoNodeTypes.FUNCTION, 897, "Execute(args)", "ollamarunner", "runner.go", List.of("args")),

            // updater_windows.go
            new ExpectedNode("DoUpgrade", GoNodeTypes.FUNCTION, 13, "DoUpgrade(cancel, done)", "lifecycle", "updater_windows.go", List.of("cancel", "done"))
        );

        for (ExpectedNode expected : expectedNodes) {
            CstNode actualNode = findNode(allGoNodes, expected.name(), expected.line());

            assertThat("Type for " + expected.name() + " at line " + expected.line(), actualNode.getType(), is(equalTo(expected.type())));
            assertThat("SimpleName for " + expected.name() + " at line " + expected.line(), actualNode.getSimpleName(), is(equalTo(expected.name())));
            assertThat("LocalName for " + expected.name() + " at line " + expected.line(), actualNode.getLocalName(), is(equalTo(expected.localName())));
            if (expected.namespace() != null) {
                assertThat("Namespace for " + expected.name() + " at line " + expected.line(), actualNode.getNamespace(), is(equalTo(expected.namespace())));
            }
            Location actualLocation = actualNode.getLocation();
            assertThat("Location file for " + expected.name() + " at line " + expected.line(), actualLocation.getFile(), is(equalTo(expected.fileName())));
            assertThat("Location line for " + expected.name() + " at line " + expected.line(), actualLocation.getLine(), is(equalTo(expected.line())));

            List<String> actualParamNames = actualNode.getParameters().stream()
                .map(Parameter::getName)
                .collect(Collectors.toList());
            assertThat("Parameters for " + expected.name() + " at line " + expected.line(), actualParamNames, is(equalTo(expected.params())));
        }
    }
}
