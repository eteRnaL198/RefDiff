package refdiff.parsers.universal.js;

import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.TokenizedSource;
import refdiff.core.io.SourceFileSet;
import refdiff.parsers.universal.common.SourceFileReader;
import refdiff.parsers.universal.common.Tokenizer;

import java.util.HashMap;
import java.util.Map;

import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJavascript;

public class JsParser {

  private int cstId = 0;

  public CstRoot parse(SourceFileSet folder) {
    TSParser parser = new TSParser();
    TSLanguage tsLang = new TreeSitterJavascript();
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
      addNodes(tree, tsLang, root, filePath, sourceCode);
      
      TokenizedSource tokenizedSource = Tokenizer.tokenize(tree, tsLang, filePath, sourceCode); // TODO: Should the argument for tokenize be a relative path?
      root.addTokenizedFile(tokenizedSource);
    }

    return root;
  }

  private void addNodes(TSTree tree, TSLanguage tsLang, CstRoot root, String filePath, String sourceCode) {
    String query = """
        (class_declaration
          name: (identifier) @class_name
          body: (class_body) @class_body) @class_node
        """;
    TSQuery tsQuery = new TSQuery(tsLang, query);

    TSNode rootNode = tree.getRootNode();
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(tsQuery, rootNode);
    TSQueryMatch match = new TSQueryMatch();

    while (cursor.nextMatch(match)) {
      TSQueryCapture[] captures = match.getCaptures();
      TSNode classDeclarationNode = null;
      TSNode nameIdentifierNode = null;
      TSNode bodyNode = null;

      for (TSQueryCapture capture : captures) {
        String captureName = tsQuery.getCaptureNameForId(capture.getIndex());
        TSNode capturedNode = capture.getNode();
        if ("class_node".equals(captureName)) {
            classDeclarationNode = capturedNode;
        } else if ("class_name".equals(captureName)) {
            nameIdentifierNode = capturedNode;
        } else if ("class_body".equals(captureName)) {
            bodyNode = capturedNode;
        }
      }

      if (classDeclarationNode == null || nameIdentifierNode == null || bodyNode == null) {
          System.err.println("Warning: Could not capture all required parts (class_node, class_name, class_body) for a class declaration in " + filePath + " at match offset " + match.getId());
          if (classDeclarationNode == null) System.err.println("  Missing @class_node");
          if (nameIdentifierNode == null) System.err.println("  Missing @class_name");
          if (bodyNode == null) System.err.println("  Missing @class_body");
        continue;
      }

      CstNode cstNode = new CstNode(cstId++);
      cstNode.setType(JsNodeTypes.CLASS);
      cstNode.setLocation(refdiff.core.cst.Location.of(
          filePath,
          classDeclarationNode.getStartByte(), classDeclarationNode.getEndByte(),
          bodyNode.getStartByte(), bodyNode.getEndByte(),
          sourceCode));

      String className = sourceCode.substring(nameIdentifierNode.getStartByte(), nameIdentifierNode.getEndByte());
      cstNode.setSimpleName(className);
      cstNode.setLocalName(className);

      String namespace = "";
      int lastSeparatorIndex = filePath.lastIndexOf('/');
      if (lastSeparatorIndex != -1) {
        namespace = filePath.substring(0, lastSeparatorIndex + 1);
      }
      cstNode.setNamespace(namespace);

      root.addNode(cstNode);
    }
  }
}
