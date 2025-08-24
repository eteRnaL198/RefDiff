package refdiff.parsers.universal.go;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterGo;

import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.Location;
import refdiff.core.cst.Parameter;
import refdiff.core.cst.TokenizedSource;
import refdiff.core.io.SourceFileSet;
import refdiff.parsers.universal.common.CallGraphGenerator;
import refdiff.parsers.universal.common.SourceFileReader;
import refdiff.parsers.universal.common.Tokenizer;

import java.util.ArrayList;

public class GoParser {
  private int cstId = 0;
  public CstRoot parse(SourceFileSet folder) {
    TSParser parser = new TSParser();
    TSLanguage tsLang = new TreeSitterGo();
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

    CallGraphGenerator callGraphGenerator = new CallGraphGenerator(GoNodeTypes.METHOD, GoNodeTypes.FUNCTION);
    callGraphGenerator.generateCallGraph(root, sourceCodeMap);

    return root;
  }

  private void addNodes(TSTree tree, TSLanguage tsLang, CstRoot cstRoot, String filePath, byte[] sourceCode) {
    String packageQuerySrc = "(package_clause (package_identifier) @package_name)";
    TSQuery packageQuery = new TSQuery(tsLang, packageQuerySrc);
    TSQueryCursor packageCursor = new TSQueryCursor();
    packageCursor.exec(packageQuery, tree.getRootNode());
    String namespace = "main"; 
    TSQueryMatch packageMatch = new TSQueryMatch();
    if (packageCursor.nextMatch(packageMatch)) {
        for (TSQueryCapture capture : packageMatch.getCaptures()) {
            TSNode capturedNode = capture.getNode();
            namespace = new String(sourceCode, capturedNode.getStartByte(), capturedNode.getEndByte() - capturedNode.getStartByte(), StandardCharsets.UTF_8);
            break;
        }
    }

    String querySrc = "[ " +
        "(function_declaration name: (identifier) @name parameters: (parameter_list) @params body: (block) @body) @function" +
        " (method_declaration receiver: (parameter_list) @receiver name: (field_identifier) @name parameters: (parameter_list) @params body: (block) @body) @method" +
        " (function_declaration name: (identifier) @name parameters: (parameter_list) @params result: (_) @result body: (block) @body) @function" +
        " (method_declaration receiver: (parameter_list) @receiver name: (field_identifier) @name parameters: (parameter_list) @params result: (_) @result body: (block) @body) @method" +
    "]";

    TSQuery query = new TSQuery(tsLang, querySrc);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, tree.getRootNode());

    TSQueryMatch match = new TSQueryMatch();
    while(cursor.nextMatch(match)) {
        TSNode nameNode = null;
        TSNode paramsNode = null;
        TSNode bodyNode = null;
        TSNode defNode = null;
        String nodeType = GoNodeTypes.FUNCTION;

        for (TSQueryCapture capture : match.getCaptures()) {
            TSNode capturedNode = capture.getNode();
            String captureName = query.getCaptureNameForId(capture.getIndex());

            switch (captureName) {
                case "function":
                    defNode = capturedNode;
                    nodeType = GoNodeTypes.FUNCTION;
                    break;
                case "method":
                    defNode = capturedNode;
                    nodeType = GoNodeTypes.METHOD;
                    break;
                case "name":
                    nameNode = capturedNode;
                    break;
                case "params":
                    paramsNode = capturedNode;
                    break;
                case "body":
                    bodyNode = capturedNode;
                    break;
            }
        }

        if (defNode != null && nameNode != null && bodyNode != null) {
            CstNode cstNode = new CstNode(cstId++);
            cstNode.setType(nodeType);
            
            String simpleName = new String(sourceCode, nameNode.getStartByte(), nameNode.getEndByte() - nameNode.getStartByte(), StandardCharsets.UTF_8);
            cstNode.setSimpleName(simpleName);
            cstNode.setNamespace(namespace + ".");

            int startByte = defNode.getStartByte();
            int endByte = defNode.getEndByte();
            int bodyStartByte = bodyNode.getStartByte();
            int bodyEndByte = bodyNode.getEndByte();
            int startLine = defNode.getStartPoint().getRow() + 1;

            cstNode.setLocation(new Location(filePath, startByte, endByte, startLine, bodyStartByte, bodyEndByte));

            String paramsString = "()";
            if (paramsNode != null) {
                paramsString = new String(sourceCode, paramsNode.getStartByte(), paramsNode.getEndByte() - paramsNode.getStartByte(), StandardCharsets.UTF_8);
            }
            cstNode.setLocalName(simpleName + paramsString);
            cstNode.setParameters(extractParameters(paramsNode, sourceCode));
            
            cstRoot.addNode(cstNode);
        }
    }
  }

  private List<Parameter> extractParameters(TSNode paramsNode, byte[] sourceCode) {
      List<Parameter> parameters = new ArrayList<>();
      if (paramsNode == null) {
          return parameters;
      }

      for (int i = 0; i < paramsNode.getNamedChildCount(); i++) {
          TSNode paramDecl = paramsNode.getNamedChild(i);
          String nodeType = paramDecl.getType();

          if (nodeType.equals("parameter_declaration")) {
              int namedChildCount = paramDecl.getNamedChildCount();
              if (namedChildCount > 0) {
                  for (int j = 0; j < namedChildCount - 1; j++) {
                      TSNode nameNode = paramDecl.getNamedChild(j);
                      if (nameNode.getType().equals("identifier")) {
                          String paramName = new String(sourceCode, nameNode.getStartByte(), nameNode.getEndByte() - nameNode.getStartByte(), StandardCharsets.UTF_8);
                          parameters.add(new Parameter(paramName));
                      }
                  }
              }
          } else if (nodeType.equals("variadic_parameter_declaration")) {
              if (paramDecl.getNamedChildCount() > 0) {
                  TSNode nameNode = paramDecl.getNamedChild(0);
                  String paramName = new String(sourceCode, nameNode.getStartByte(), nameNode.getEndByte() - nameNode.getStartByte(), StandardCharsets.UTF_8);
                  parameters.add(new Parameter(paramName));
              }
          }
      }
      return parameters;
  }
}