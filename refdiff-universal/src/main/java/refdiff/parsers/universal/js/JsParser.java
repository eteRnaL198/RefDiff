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

  private void addNodes(TSTree tree, TSLanguage tsLang, CstRoot cstRoot, String filePath, String sourceCode) {
    TSNode fileTsNode = tree.getRootNode(); // This is the (program) node for JS
    CstNode fileCstNode = new CstNode(cstId++);
    fileCstNode.setType(JsNodeTypes.FILE);
    fileCstNode.setLocation(refdiff.core.cst.Location.of(
        filePath,
        fileTsNode.getStartByte(), fileTsNode.getEndByte(),
        fileTsNode.getStartByte(), fileTsNode.getEndByte(),
        sourceCode));
    String nameForFileNode = getFileNameFromFilePath(filePath);
    fileCstNode.setSimpleName(nameForFileNode);
    fileCstNode.setLocalName(nameForFileNode);
    fileCstNode.setNamespace(getNamespaceFromFilePath(filePath));
    cstRoot.addNode(fileCstNode);

    // Determine the parent for elements within this file (classes, functions, etc.)
    // If fileCstNode was created, it's the parent. Otherwise, fallback to globalRoot.
    CstNode parentCstNode = fileCstNode;

    String classQuerySrc = """
        (class_declaration
          name: (identifier) @class_name
          body: (class_body) @class_body) @class_node
        """;
    TSQuery classTsQuery = new TSQuery(tsLang, classQuerySrc);
    TSQueryCursor classCursor = new TSQueryCursor();
    classCursor.exec(classTsQuery, fileTsNode);
    TSQueryMatch classMatch = new TSQueryMatch();

    while (classCursor.nextMatch(classMatch)) {
      TSQueryCapture[] captures = classMatch.getCaptures();
      TSNode classDeclarationNode = null;
      TSNode nameIdentifierNode = null;
      TSNode bodyNode = null;

      for (TSQueryCapture capture : captures) {
        String captureName = classTsQuery.getCaptureNameForId(capture.getIndex());
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
        System.err.println("Warning: Could not capture all required parts (class_node, class_name, class_body) for a class declaration in " + filePath + " at match offset " + classMatch.getId());
        if (classDeclarationNode == null) System.err.println("  Missing @class_node");
        if (nameIdentifierNode == null) System.err.println("  Missing @class_name");
        if (bodyNode == null) System.err.println("  Missing @class_body");
        continue;
      }

      CstNode classCstNode = new CstNode(cstId++);
      classCstNode.setType(JsNodeTypes.CLASS);
      classCstNode.setLocation(refdiff.core.cst.Location.of(
          filePath,
          classDeclarationNode.getStartByte(), classDeclarationNode.getEndByte(),
          bodyNode.getStartByte(), bodyNode.getEndByte(),
          sourceCode));
      String className = sourceCode.substring(nameIdentifierNode.getStartByte(), nameIdentifierNode.getEndByte());
      classCstNode.setSimpleName(className);
      classCstNode.setLocalName(className);
      classCstNode.setNamespace(getNamespaceFromFilePath(filePath));
      parentCstNode.addNode(classCstNode); // Add class as child of the FILE node (or globalRoot as fallback)
    }
  }

  private String getNamespaceFromFilePath(String filePath) {
    int lastSlash = filePath.lastIndexOf('/');
    int lastBackslash = filePath.lastIndexOf('\\');
    int lastSeparator = Math.max(lastSlash, lastBackslash);

    if (lastSeparator != -1) {
      return filePath.substring(0, lastSeparator + 1);
    }
    return ""; // No directory path found, likely just a filename
  }

  private String getFileNameFromFilePath(String filePath) {
    int lastSlash = filePath.lastIndexOf('/');
    int lastBackslash = filePath.lastIndexOf('\\');
    int lastSeparator = Math.max(lastSlash, lastBackslash);

    if (lastSeparator != -1) {
      return filePath.substring(lastSeparator + 1);
    }
    // If no separator is found, the filePath itself is the filename
    return filePath;
  }
}
