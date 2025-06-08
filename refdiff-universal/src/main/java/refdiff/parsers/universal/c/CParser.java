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
      addNodes(tree, tsLang, root, filePath, sourceCode);
      
      TokenizedSource tokenizedSource = Tokenizer.tokenize(tree, tsLang, filePath, sourceCode);// TODO tokenizeの引数にはrelative pathを渡す？
      root.addTokenizedFile(tokenizedSource);
    }

    CallGraphGenerator callGraphGenerator = new CallGraphGenerator(CNodeTypes.FUNCTION_DECLARATION);
    callGraphGenerator.generateCallGraph(root, sourceCodeMap);

    return root;
  }

  private void addNodes(TSTree tree, TSLanguage tsLang, CstRoot root, String path, String sourceCode) {
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
            cstNode.setLocation(Location.of(path, tsNode.getStartByte(), tsNode.getEndByte(), tsNode.getStartByte(), tsNode.getEndByte(), sourceCode)); // TODO bodyと区別して計算
            
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
            cstNode.setLocation(Location.of(path, tsNode.getStartByte(), tsNode.getEndByte(), block.getStartByte(), block.getEndByte(), sourceCode)); // TODO bodyと区別して計算

            TSNode declarator = tsNode.getChildByFieldName("declarator");
            TSNode identifier = declarator.getChild(0);
            String functionName = sourceCode.substring(identifier.getStartByte(), identifier.getEndByte());
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
                  String typeName = sourceCode.substring(type.getStartByte(), type.getEndByte());
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
