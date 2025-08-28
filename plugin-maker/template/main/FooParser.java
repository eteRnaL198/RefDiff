package refdiff.parsers.universal.foo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterFoo;

import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.Location;
import refdiff.core.cst.Parameter;
import refdiff.core.cst.TokenizedSource;
import refdiff.core.io.SourceFileSet;
import refdiff.parsers.universal.common.CallGraphGenerator;
import refdiff.parsers.universal.common.SourceFileReader;
import refdiff.parsers.universal.common.Tokenizer;
import refdiff.parsers.universal.common.Parser;

import java.util.ArrayList;

public class FooParser implements Parser {
  private int cstId = 0;
  public CstRoot parse(SourceFileSet folder) {
    TSParser parser = new TSParser();
    TSLanguage tsLang = new TreeSitterFoo();
    parser.setLanguage(tsLang);

    CstRoot root = new CstRoot();

    Map<String, String> sourceCodeMap = SourceFileReader.readAllSourceFiles(folder);
    Map<String, TSTree> parsedTreeMap = new HashMap<>();
    for (Map.Entry<String, String> sourceEntry : sourceCodeMap.entrySet()) {
      TSTree tree = parser.parseString(null, sourceEntry.getValue());
      parsedTreeMap.put(sourceEntry.getKey(), tree);
    }

    for (Map.Entry<String, TSTree> treeEntry : parsedTreeMap.entrySet()) {
      String filePath = treeEntry.getKey();
      TSTree tree = treeEntry.getValue();
      String sourceCode = sourceCodeMap.get(filePath);
      byte[] sourceBytes = sourceCode.getBytes(StandardCharsets.UTF_8);
      addNodes(tree, tsLang, root, filePath, sourceBytes);

      TokenizedSource tokenizedSource = Tokenizer.tokenize(tree, tsLang, filePath);
      root.addTokenizedFile(tokenizedSource);
    }

    CallGraphGenerator callGraphGenerator = new CallGraphGenerator(FooNodeTypes.METHOD, FooNodeTypes.FUNCTION);
    callGraphGenerator.generateCallGraph(root, sourceCodeMap);

    return root;
  }

  private void addNodes(TSTree tree, TSLanguage tsLang, CstRoot cstRoot, String filePath, byte[] sourceBytes) {
  }

}
