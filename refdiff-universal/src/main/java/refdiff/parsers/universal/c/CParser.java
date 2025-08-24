package refdiff.parsers.universal.c;

import org.treesitter.TSLanguage;
import org.treesitter.TSParser;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterC;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import java.util.Map;


import refdiff.core.io.SourceFileSet;
import refdiff.parsers.universal.common.CallGraphGenerator;
import refdiff.parsers.universal.common.SourceFileReader;
import refdiff.parsers.universal.common.Tokenizer;
import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.Location;
import refdiff.core.cst.TokenizedSource;


public class CParser {

  private int cstId = 0;

  public CstRoot parse(SourceFileSet folder) {
    TSParser parser = new TSParser();
    TSLanguage tsLang = new TreeSitterC();
    parser.setLanguage(tsLang);

    CstRoot root = new CstRoot();
    Map<String, String> sourceCodeMap = SourceFileReader.readAllSourceFiles(folder);

    for (Map.Entry<String, String> entry : sourceCodeMap.entrySet()) {
      String filePath = entry.getKey();
      String sourceCode = entry.getValue();
      TSTree tree = parser.parseString(null, sourceCode);
      byte[] sourceBytes = sourceCode.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      addNodes(tree, tsLang, root, filePath, sourceBytes);
      TokenizedSource tokenizedSource = Tokenizer.tokenize(tree, tsLang, filePath);
      root.addTokenizedFile(tokenizedSource);
    }

    CallGraphGenerator callGraphGenerator = new CallGraphGenerator(CNodeTypes.FUNCTION_DECLARATION);
    callGraphGenerator.generateCallGraph(root, sourceCodeMap);

    return root;
  }

  private void addNodes(TSTree tree, TSLanguage tsLang, CstRoot root, String path, byte[] sourceBytes) {
    String query = "[(translation_unit) (function_definition)] @node";
    TSQuery tsQuery = new TSQuery(tsLang, query);

    TSNode rootNode = tree.getRootNode();
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(tsQuery, rootNode);
    TSQueryMatch match = new TSQueryMatch();

    CstNode parent = null;
    while (cursor.nextMatch(match)) {
      TSQueryCapture[] captures = match.getCaptures();
      for (TSQueryCapture capture : captures) {
        TSNode tsNode = capture.getNode();
        
        switch(tsNode.getType()) {
          case "translation_unit": { // file
            CstNode cstNode = new CstNode(cstId++);
            cstNode.setType(CNodeTypes.FILE);
            int lineNumber = tsNode.getStartPoint().getRow() + 1;
            cstNode.setLocation(new Location(path, tsNode.getStartByte(), tsNode.getEndByte(), lineNumber, tsNode.getStartByte(), tsNode.getEndByte())); // TODO bodyと区別して計算

            Path filePath = Paths.get(path);
            Path parentPath = filePath.getParent();
            cstNode.setNamespace(parentPath != null ? parentPath.toString() + "/" : "");
            
            String fileName = filePath.getFileName().toString();
            cstNode.setLocalName(fileName);
            cstNode.setSimpleName(fileName);
            root.addNode(cstNode);
            parent = cstNode;
            break; }
          case "function_definition": {
            CstNode cstNode = new CstNode(cstId++);
            cstNode.setType(CNodeTypes.FUNCTION_DECLARATION);
            
            TSNode block = tsNode.getChildByFieldName("body");
            int lineNumber = tsNode.getStartPoint().getRow() + 1;
            cstNode.setLocation(new Location(path, tsNode.getStartByte(), tsNode.getEndByte(), lineNumber, block.getStartByte(), block.getEndByte())); // TODO bodyと区別して計算

            TSNode declarator = tsNode.getChildByFieldName("declarator");
            TSNode identifier = declarator.getChild(0);
            String functionName = new String(sourceBytes, identifier.getStartByte(), identifier.getEndByte() - identifier.getStartByte(), StandardCharsets.UTF_8);
            cstNode.setSimpleName(functionName);

            TSNode parameters = declarator.getChildByFieldName("parameters");
            StringBuilder localNameBuilder = new StringBuilder();
            localNameBuilder.append("(");
            if (parameters.getChildCount() != 2) { // Ignore the () case
              for (int i = 1; i < parameters.getChildCount() - 1; i++) { // Ignore the first ( and last )
                if (i > 2) {
                  localNameBuilder.append(", ");
                }
                TSNode parameter = parameters.getChild(i);
                if (parameter.getType().equals("variadic_parameter")) { // variable length arguments
                  localNameBuilder.append("...");
                } else if (parameter.getType().equals("parameter_declaration")) {
                  TSNode type = parameter.getChildByFieldName("type");
                  String typeName = new String(sourceBytes, type.getStartByte(), type.getEndByte() - type.getStartByte(), StandardCharsets.UTF_8);
                  localNameBuilder.append(typeName);
                }
              }
            }
            localNameBuilder.append(")");
            String signature = functionName + localNameBuilder.toString();
            cstNode.setLocalName(signature);

            // TODO Parentをちゃんと取る
            if (parent == null) {
              System.out.println("Parent is null");
            }
            parent.addNode(cstNode);
            break; }
          default:
            break;
        }
      }
    }
  }

}
