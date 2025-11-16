package refdiff.parsers.universal.java;

import org.treesitter.TSException;
import org.treesitter.TSLanguage;
import org.treesitter.TSParser;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJava;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;

import java.util.Map;
import java.util.Stack;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;


import refdiff.core.io.SourceFileSet;
import refdiff.parsers.universal.common.Tokenizer;
import refdiff.parsers.universal.common.SourceFileReader;
import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.Location;
import refdiff.core.cst.Stereotype;
import refdiff.core.cst.TokenizedSource;
import refdiff.parsers.universal.common.CallGraphGenerator;
import refdiff.parsers.universal.common.NodeUtils;
import refdiff.parsers.universal.common.InheritanceTreeGenerator;
import refdiff.parsers.universal.common.Parser;

public class JavaParser implements Parser {

  private int cstId = 0;

  public CstRoot parse(SourceFileSet folder) {
    TSParser parser = new TSParser();
    TSLanguage tsLang = new TreeSitterJava(); // Keep tsLang instance for reuse
    parser.setLanguage(tsLang);

    CstRoot root = new CstRoot();
    Map<String, String> sourceCodeMap = SourceFileReader.readAllSourceFiles(folder);

    // Parse all files once and store TSTrees
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
      
      TokenizedSource tokenizedSource = Tokenizer.tokenize(tree, tsLang, filePath); // TODO: Should the argument for tokenize be a relative path?
      root.addTokenizedFile(tokenizedSource);
    }

    List<String> inheritanceRelatedNodeTypes = Arrays.asList(JavaNodeTypes.CLASS, JavaNodeTypes.INTERFACE);
    String inheritanceQuery = "[(superclass) (super_interfaces) (extends_interfaces)] @node";
    InheritanceTreeGenerator inheritanceTreeGenerator = new InheritanceTreeGenerator(inheritanceRelatedNodeTypes, inheritanceQuery);
    inheritanceTreeGenerator.buildInheritanceTree(root, sourceCodeMap, parsedTreeMap, tsLang);
    
    CallGraphGenerator callGraphGenerator = new CallGraphGenerator(JavaNodeTypes.METHOD);
    callGraphGenerator.generateCallGraph(root, sourceCodeMap);

    return root;
  }

  private void addNodes(TSTree tree, TSLanguage tsLang, CstRoot root, String path, byte[] sourceBytes) {
    String packageQuerySrc = "(package_declaration  [(identifier) (scoped_identifier)] @package_name)"; // e.g., package foo; or package foo.bar;
    TSQuery packageQuery = new TSQuery(tsLang, packageQuerySrc);
    TSQueryCursor packageCursor = new TSQueryCursor();
    packageCursor.exec(packageQuery, tree.getRootNode());
    String packageName = "";
    TSQueryMatch packageMatch = new TSQueryMatch();
    if (packageCursor.nextMatch(packageMatch)) {
      for (TSQueryCapture capture : packageMatch.getCaptures()) {
        TSNode capturedNode = capture.getNode();
        packageName = NodeUtils.getNodeText(capturedNode, sourceBytes);
        break;
      }
    }

    String querySrc = """
    [
      (class_declaration
        name: (identifier) @name
        body: (class_body) @body
      ) @declaration
      (interface_declaration
          name: (identifier) @name
          body: (interface_body) @body
      ) @declaration
      (enum_declaration
          name: (identifier) @name
          body: (enum_body) @body
      ) @declaration
      (constructor_declaration
          name: (identifier) @name
          parameters: (formal_parameters) @parameters
          body: (constructor_body) @body
      ) @declaration
      (method_declaration
          name: (identifier) @name
          parameters: (formal_parameters) @parameters
          body: ((block) @body)?
      ) @declaration
    ]    
    """;
    TSQuery query = new TSQuery(tsLang, querySrc);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, tree.getRootNode());
    TSQueryMatch match = new TSQueryMatch();
    Stack<CstNode> parentStack = new Stack<>();
    while (cursor.nextMatch(match)) {
      TSNode name = null;
      TSNode parameters = null;
      TSNode body = null;
      TSNode declaration = null;
      for (TSQueryCapture capture : match.getCaptures()) {
        TSNode capturedNode = capture.getNode();
        String captureName = query.getCaptureNameForId(capture.getIndex());
        switch (captureName) {
          case "name" -> name = capturedNode;
          case "parameters" -> parameters = capturedNode;
          case "body" -> body = capturedNode;
          case "declaration" -> declaration = capturedNode;
        }
      }

      CstNode cstNode = new CstNode(cstId++);
      cstNode.setLocation(NodeUtils.generateLocation(declaration, body, path));
      cstNode.setSimpleName(NodeUtils.getNodeText(name, sourceBytes));
      cstNode.setLocalName(NodeUtils.getNodeText(name, sourceBytes));
      switch (declaration.getType()) {
        case "class_declaration":
          cstNode.setType(JavaNodeTypes.CLASS);
          cstNode.setNamespace(packageName + "."); // TODO 簡略化
          break;
        case "interface_declaration":
          cstNode.setType(JavaNodeTypes.INTERFACE);
          cstNode.setNamespace(packageName + ".");
          cstNode.addStereotypes(Stereotype.ABSTRACT);
          break;
        case "enum_declaration":
          cstNode.setType(JavaNodeTypes.ENUM);
          cstNode.setNamespace(packageName + ".");
          break;
        case "constructor_declaration":
          cstNode.setType(JavaNodeTypes.METHOD);
          String constructorName = "new";
          cstNode.setSimpleName(constructorName);
          String constructorParamsSignature = extractSignatureParameters(parameters, sourceBytes);
          cstNode.setLocalName(constructorName + constructorParamsSignature);
          cstNode.addStereotypes(Stereotype.TYPE_CONSTRUCTOR);
          break;
        case "method_declaration":
          cstNode.setType(JavaNodeTypes.METHOD);
          String methodParamsSignature = extractSignatureParameters(parameters, sourceBytes);
          cstNode.setLocalName(NodeUtils.getNodeText(name, sourceBytes) + methodParamsSignature);
          cstNode.addStereotypes(Stereotype.TYPE_MEMBER);
          break;
        default:
          System.out.println("Warning: Unhandled declaration type: " + declaration.getType() + " at " + declaration.getStartPoint().getRow() + "-" + declaration.getEndPoint().getRow() + " in source code: " + NodeUtils.getNodeText(declaration, sourceBytes));
          break;
      }

      // Determine parent-child relationship based on source code location
      while (!parentStack.isEmpty() && parentStack.peek().getLocation().getEnd() < cstNode.getLocation().getBegin()) {
        parentStack.pop();
      }
      if (parentStack.isEmpty()) {
        root.getNodes().add(cstNode);
      } else {
        CstNode parentNode = parentStack.peek();
        parentNode.addNode(cstNode);
      }
      parentStack.push(cstNode);

    }
  }

  /**
   * Extracts parameter types from a parameters TSNode and builds a signature string.
   * e.g., "(String, int[])"
   * @param parametersNode The TSNode representing the parameters list (e.g., content of formal_parameters).
   * @param sourceBytes The source code string to extract type names.
   * @return A string representing the parameter signature.
   */
  private String extractSignatureParameters(TSNode parametersNode, byte[] sourceBytes) {
    StringBuilder paramsStr = new StringBuilder();
    paramsStr.append("(");

    List<String> paramTypes = new ArrayList<>();
    for (int i = 0; i < parametersNode.getNamedChildCount(); i++) {
        TSNode parameter = parametersNode.getNamedChild(i);
        String paramTypeString = null;

        if (parameter.getType().equals("formal_parameter")) {
            TSNode typeNode = parameter.getChildByFieldName("type");
            if (typeNode != null && !typeNode.isNull()) {
                paramTypeString = NodeUtils.getNodeText(typeNode, sourceBytes).split("<")[0]; // Remove generic type parameters if any, e.g., List<String> -> List
            }
        } else if (parameter.getType().equals("spread_parameter")) {
            TSNode typeNode = parameter.getChild(0); // The first child is the type for spread parameters
            if (typeNode != null && !typeNode.isNull()) {
                paramTypeString = NodeUtils.getNodeText(typeNode, sourceBytes).split("<")[0] + "..."; // For spread parameters, the type is followed by "..."
            }
        } else if (parameter.getType().equals("receiver_parameter")) {
            // Receiver parameters (e.g., `Outer.this`) are generally not included in RefDiff's localName.
            // If they need to be included, this part can be adjusted.
            continue; // Skipping receiver parameters for localName.
        } else if (parameter.getType().equals("block_comment") || parameter.getType().equals("line_comment")) {
            continue; // Skip comments within parameters
        }

        if (paramTypeString != null) {
          paramTypes.add(paramTypeString);
        } else {
            System.out.println("Warning: Could not determine type for parameter: " + parameter.getType() +
                " at " + parameter.getStartPoint().getRow() + "-" + parameter.getEndPoint().getRow() +
                " in source code: " + NodeUtils.getNodeText(parametersNode, sourceBytes));
        }
    }
    paramsStr.append(String.join(", ", paramTypes));
    paramsStr.append(")");
    return paramsStr.toString();
  }

}