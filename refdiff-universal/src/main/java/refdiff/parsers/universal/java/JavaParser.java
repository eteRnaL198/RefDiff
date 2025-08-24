package refdiff.parsers.universal.java;

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


public class JavaParser {

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
                paramTypeString = new String(sourceBytes, typeNode.getStartByte(), typeNode.getEndByte() - typeNode.getStartByte(), StandardCharsets.UTF_8);
            }
        } else if (parameter.getType().equals("spread_parameter")) {
            TSNode typeNode = parameter.getChild(0); // The first child is the type for spread parameters
            if (typeNode != null && !typeNode.isNull()) {
                paramTypeString = new String(sourceBytes, typeNode.getStartByte(), typeNode.getEndByte() - typeNode.getStartByte(), StandardCharsets.UTF_8) + "..."; // For spread parameters, the type is followed by "..."
            }
        } else if (parameter.getType().equals("receiver_parameter")) {
            // Receiver parameters (e.g., `Outer.this`) are generally not included in RefDiff's localName.
            // If they need to be included, this part can be adjusted.
            continue; // Skipping receiver parameters for localName.
        }

        if (paramTypeString != null) {
            paramTypes.add(paramTypeString);
        } else {
            System.err.println("Warning: Could not determine type for parameter: " + parameter.getType() + 
                               " at " + parameter.getStartByte() + "-" + parameter.getEndByte() + 
                               " in source code: " + sourceBytes);
        }
    }
    paramsStr.append(String.join(", ", paramTypes));
    paramsStr.append(")");
    return paramsStr.toString();
  }

  private void addNodes(TSTree tree, TSLanguage tsLang, CstRoot root, String path, byte[] sourceBytes) {
    String query;
    query = "[(class_declaration) (interface_declaration) (constructor_declaration) (method_declaration)] @node";
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
          case "class_declaration": {
            CstNode cstNode = new CstNode(cstId++);
            cstNode.setType(JavaNodeTypes.CLASS_DECLARATION);

            TSNode body = tsNode.getChildByFieldName("body");
            int lineNumber = tsNode.getStartPoint().getRow() + 1;
            cstNode.setLocation(new Location(path, tsNode.getStartByte(), tsNode.getEndByte(), lineNumber, body.getStartByte(), body.getEndByte()));

            TSNode identifier = tsNode.getChildByFieldName("name");
            String className = new String(sourceBytes, identifier.getStartByte(), identifier.getEndByte() - identifier.getStartByte(), StandardCharsets.UTF_8);
            cstNode.setLocalName(className);
            cstNode.setSimpleName(className);

            String packageName = "";
            TSNode program = tsNode.getParent();
            for (int i = 0; i < program.getChildCount(); i++) { // package_declarationの他に block_commentやimport_declarationなどもあるのでフィルタリングしている
              TSNode child = program.getChild(i);
              if (child.getType().equals("package_declaration")) {
                TSNode scopedIdentifier = child.getChild(1);
                packageName = new String(sourceBytes, scopedIdentifier.getStartByte(), scopedIdentifier.getEndByte() - scopedIdentifier.getStartByte(), StandardCharsets.UTF_8);
              }
            }
            cstNode.setNamespace(packageName + ".");
            root.addNode(cstNode);
            parent = cstNode;
            break; }
          case "interface_declaration": {
            CstNode cstNode = new CstNode(cstId++);
            cstNode.setType(JavaNodeTypes.INTERFACE_DECLARATION);

            TSNode body = tsNode.getChildByFieldName("body");
            int lineNumber = body.getStartPoint().getRow() + 1;
            cstNode.setLocation(new Location(path, tsNode.getStartByte(), tsNode.getEndByte(), lineNumber, body.getStartByte(), body.getEndByte()));

            TSNode identifier = tsNode.getChildByFieldName("name");
            String interfaceName = new String(sourceBytes, identifier.getStartByte(), identifier.getEndByte() - identifier.getStartByte(), StandardCharsets.UTF_8);
            cstNode.setLocalName(interfaceName);
            cstNode.setSimpleName(interfaceName);

            String packageName = "";
            TSNode program = tsNode.getParent();
            for (int i = 0; i < program.getChildCount(); i++) { // package_declarationの他に block_commentやimport_declarationなどもあるのでフィルタリングしている
              TSNode child = program.getChild(i);
              if (child.getType().equals("package_declaration")) {
                TSNode scopedIdentifier = child.getChild(1);
                packageName = new String(sourceBytes, scopedIdentifier.getStartByte(), scopedIdentifier.getEndByte() - scopedIdentifier.getStartByte(), StandardCharsets.UTF_8);
              }
            }
            cstNode.setNamespace(packageName + ".");
            cstNode.addStereotypes(Stereotype.ABSTRACT);
            root.addNode(cstNode);
            parent = cstNode;
            break; }
          case "constructor_declaration": {
            CstNode cstNode = new CstNode(cstId++);
            cstNode.setType(JavaNodeTypes.METHOD_DECLARATION);

            TSNode block = tsNode.getChildByFieldName("body");
            int lineNumber = block.getStartPoint().getRow() + 1;
            cstNode.setLocation(new Location(path, tsNode.getStartByte(), tsNode.getEndByte(), lineNumber, block.getStartByte(), block.getEndByte()));

            TSNode identifier = tsNode.getChildByFieldName("name");
            String constructorName = new String(sourceBytes, identifier.getStartByte(), identifier.getEndByte() - identifier.getStartByte(), StandardCharsets.UTF_8);
            cstNode.setSimpleName(constructorName);

            TSNode parameters = tsNode.getChildByFieldName("parameters");
            String paramsSignature = extractSignatureParameters(parameters, sourceBytes);
            cstNode.setLocalName(constructorName + paramsSignature);

            cstNode.addStereotypes(Stereotype.TYPE_CONSTRUCTOR);

            // TODO Parentをちゃんと取る
            if (parent == null) {
              System.out.println("Parent is null: " + path + " for constructor: " + constructorName + paramsSignature);
            }
            parent.addNode(cstNode);
            break; }
          case "method_declaration": {
            CstNode cstNode = new CstNode(cstId++);
            cstNode.setType(JavaNodeTypes.METHOD_DECLARATION);

            TSNode block = tsNode.getChildByFieldName("body");
            int lineNumber = tsNode.getStartPoint().getRow() + 1;
            if (block.isNull()) {
              cstNode.setLocation(new Location(path, tsNode.getStartByte(), tsNode.getEndByte(), lineNumber, block.getStartByte(), block.getEndByte()));
            } else {
              cstNode.setLocation(new Location(path, tsNode.getStartByte(), tsNode.getEndByte(), lineNumber, block.getStartByte(), block.getEndByte()));
            }

            TSNode identifier = tsNode.getChildByFieldName("name");
            String methodName = new String(sourceBytes, identifier.getStartByte(), identifier.getEndByte() - identifier.getStartByte(), StandardCharsets.UTF_8);
            cstNode.setSimpleName(methodName);

            TSNode parameters = tsNode.getChildByFieldName("parameters");
            String paramsSignature = extractSignatureParameters(parameters, sourceBytes);
            cstNode.setLocalName(methodName + paramsSignature);

            cstNode.addStereotypes(Stereotype.TYPE_MEMBER);

            // TODO Parentをちゃんと取る
            if (parent == null) {
              System.out.println("Parent is null: " + path + " for method: " + methodName + paramsSignature);
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
