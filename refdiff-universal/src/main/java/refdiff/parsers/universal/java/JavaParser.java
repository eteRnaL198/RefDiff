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
import refdiff.core.cst.CstNodeRelationshipType;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.Location;
import refdiff.core.cst.Stereotype;
import refdiff.core.cst.TokenizedSource;
import refdiff.parsers.universal.common.CallGraphGenerator;
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
      String sourceCode = sourceCodeMap.get(filePath); // Get source code for context
      byte[] sourceBytes = sourceCode.getBytes(StandardCharsets.UTF_8);
      addNodes(tree, tsLang, root, filePath, sourceBytes);
      
      TokenizedSource tokenizedSource = Tokenizer.tokenize(tree, tsLang, filePath); // TODO: Should the argument for tokenize be a relative path?
      root.addTokenizedFile(tokenizedSource);
    }

    List<String> inheritanceRelatedNodeTypes = Arrays.asList(JavaNodeTypes.CLASS_DECLARATION, JavaNodeTypes.INTERFACE_DECLARATION);
    String inheritanceQuery = "[(superclass) (super_interfaces) (extends_interfaces)] @node";
    InheritanceTreeGenerator inheritanceTreeGenerator = new InheritanceTreeGenerator(inheritanceRelatedNodeTypes, inheritanceQuery);
    inheritanceTreeGenerator.buildInheritanceTree(root, sourceCodeMap, parsedTreeMap, tsLang);
    
    CallGraphGenerator callGraphGenerator = new CallGraphGenerator(JavaNodeTypes.METHOD_DECLARATION);
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
        packageName = new String(sourceBytes, capturedNode.getStartByte(),
            capturedNode.getEndByte() - capturedNode.getStartByte(), StandardCharsets.UTF_8);
        break;
      }
    }

    Stack<CstNode> parentStack = new Stack<>();
    walkTree(tree.getRootNode(), parentStack, root, path, sourceBytes, packageName);
  }

  private void walkTree(TSNode tsNode, Stack<CstNode> parentStack, CstRoot root, String path, byte[] sourceBytes, String packageName) {
    CstNode cstNode = null;
    boolean isContainer = false;

    switch(tsNode.getType()) {
      case "class_declaration": {
        cstNode = new CstNode(cstId++);
        cstNode.setType(JavaNodeTypes.CLASS_DECLARATION);

        TSNode body = tsNode.getChildByFieldName("body");
        int lineNumber = tsNode.getStartPoint().getRow() + 1;
        int endLineNumber = tsNode.getEndPoint().getRow() + 1;
        cstNode.setLocation(new Location(path, tsNode.getStartByte(), tsNode.getEndByte(), lineNumber, endLineNumber, body.getStartByte(), body.getEndByte()));
        
        TSNode identifier = tsNode.getChildByFieldName("name");
        String className = new String(sourceBytes, identifier.getStartByte(), identifier.getEndByte() - identifier.getStartByte(), StandardCharsets.UTF_8);
        cstNode.setLocalName(className);
        cstNode.setSimpleName(className);

        cstNode.setNamespace(packageName + ".");
        isContainer = true;
        break; }
      case "interface_declaration": {
        cstNode = new CstNode(cstId++);
        cstNode.setType(JavaNodeTypes.INTERFACE_DECLARATION);

        TSNode body = tsNode.getChildByFieldName("body");
        int lineNumber = tsNode.getStartPoint().getRow() + 1;
        int endLineNumber = tsNode.getEndPoint().getRow() + 1;
        cstNode.setLocation(new Location(path, tsNode.getStartByte(), tsNode.getEndByte(), lineNumber, endLineNumber, body.getStartByte(), body.getEndByte()));

        TSNode identifier = tsNode.getChildByFieldName("name");
        String interfaceName = new String(sourceBytes, identifier.getStartByte(), identifier.getEndByte() - identifier.getStartByte(), StandardCharsets.UTF_8);
        cstNode.setLocalName(interfaceName);
        cstNode.setSimpleName(interfaceName);

        cstNode.setNamespace(packageName + ".");
        cstNode.addStereotypes(Stereotype.ABSTRACT);
        isContainer = true;
        break; }
      case "constructor_declaration": {
        cstNode = new CstNode(cstId++);
        cstNode.setType(JavaNodeTypes.METHOD_DECLARATION);

        TSNode block = tsNode.getChildByFieldName("body");
        int lineNumber = tsNode.getStartPoint().getRow() + 1;
        int endLineNumber = tsNode.getEndPoint().getRow() + 1;
        cstNode.setLocation(new Location(path, tsNode.getStartByte(), tsNode.getEndByte(), lineNumber, endLineNumber, block.getStartByte(), block.getEndByte()));

        String constructorName = "new";
        cstNode.setSimpleName(constructorName);

        TSNode parameters = tsNode.getChildByFieldName("parameters");
        String paramsSignature = extractSignatureParameters(parameters, sourceBytes);
        cstNode.setLocalName(constructorName + paramsSignature);

        cstNode.addStereotypes(Stereotype.TYPE_CONSTRUCTOR);
        isContainer = true;
        break; }
      case "method_declaration": {
        cstNode = new CstNode(cstId++);
        cstNode.setType(JavaNodeTypes.METHOD_DECLARATION);

        int lineNumber = tsNode.getStartPoint().getRow() + 1;
        int endLineNumber = tsNode.getEndPoint().getRow() + 1;
        try {
          TSNode block = tsNode.getChildByFieldName("body");
          cstNode.setLocation(new Location(path, tsNode.getStartByte(), tsNode.getEndByte(), lineNumber,
              endLineNumber, block.getStartByte(), block.getEndByte()));
          } catch (TSException e) { // body is null for abstract methods
          cstNode.setLocation(new Location(path, tsNode.getStartByte(), tsNode.getEndByte(), lineNumber,
              endLineNumber, tsNode.getStartByte(), tsNode.getEndByte()));
          cstNode.addStereotypes(Stereotype.ABSTRACT);
        }

        TSNode identifier = tsNode.getChildByFieldName("name");
        String methodName = new String(sourceBytes, identifier.getStartByte(), identifier.getEndByte() - identifier.getStartByte(), StandardCharsets.UTF_8);
        cstNode.setSimpleName(methodName);

        TSNode parameters = tsNode.getChildByFieldName("parameters");
        String paramsSignature = extractSignatureParameters(parameters, sourceBytes);
        cstNode.setLocalName(methodName + paramsSignature);

        cstNode.addStereotypes(Stereotype.TYPE_MEMBER);
        isContainer = true;
        break; }
      default:
        break;
    }

    if (cstNode != null) {
      if (parentStack.isEmpty()) {
        root.addNode(cstNode);
      } else {
        parentStack.peek().addNode(cstNode);
      }
      if (isContainer) {
        parentStack.push(cstNode);
      }
    }

    for (int i = 0; i < tsNode.getChildCount(); i++) {
      walkTree(tsNode.getChild(i), parentStack, root, path, sourceBytes, packageName);
    }

    if (cstNode != null && isContainer) {
      parentStack.pop();
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
                paramTypeString = new String(sourceBytes, typeNode.getStartByte(), typeNode.getEndByte() - typeNode.getStartByte(), StandardCharsets.UTF_8).split("<")[0]; // Remove generic type parameters if any, e.g., List<String> -> List
            }
        } else if (parameter.getType().equals("spread_parameter")) {
            TSNode typeNode = parameter.getChild(0); // The first child is the type for spread parameters
            if (typeNode != null && !typeNode.isNull()) {
                paramTypeString = new String(sourceBytes, typeNode.getStartByte(), typeNode.getEndByte() - typeNode.getStartByte(), StandardCharsets.UTF_8).split("<")[0] + "..."; // For spread parameters, the type is followed by "..."
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
                " in source code: " + new String(sourceBytes, parametersNode.getStartByte(), parametersNode.getEndByte() - parametersNode.getStartByte(), StandardCharsets.UTF_8));
        }
    }
    paramsStr.append(String.join(", ", paramTypes));
    paramsStr.append(")");
    return paramsStr.toString();
  }

}