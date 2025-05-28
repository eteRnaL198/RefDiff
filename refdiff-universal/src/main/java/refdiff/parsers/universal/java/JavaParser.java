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
import java.util.HashMap;


import refdiff.core.io.SourceFileSet;
import refdiff.parsers.universal.common.Tokenizer;
import refdiff.parsers.universal.common.SourceFileReader;
import refdiff.core.cst.CstNode;
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
      addNodes(tree, tsLang, root, filePath, sourceCode);
      
      TokenizedSource tokenizedSource = Tokenizer.tokenize(tree, tsLang, filePath, sourceCode); // TODO: Should the argument for tokenize be a relative path?
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

  private void addNodes(TSTree tree, TSLanguage tsLang, CstRoot root, String path, String sourceCode) {
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
            cstNode.setLocation(Location.of(path, tsNode.getStartByte(), tsNode.getEndByte(), body.getStartByte(), body.getEndByte(), sourceCode));

            TSNode identifier = tsNode.getChildByFieldName("name");
            String className = sourceCode.substring(identifier.getStartByte(), identifier.getEndByte());
            cstNode.setLocalName(className);
            cstNode.setSimpleName(className);

            String packageName = "";
            TSNode program = tsNode.getParent();
            for (int i = 0; i < program.getChildCount(); i++) { // package_declarationの他に block_commentやimport_declarationなどもあるのでフィルタリングしている
              TSNode child = program.getChild(i);
              if (child.getType().equals("package_declaration")) {
                TSNode scopedIdentifier = child.getChild(1);
                packageName = sourceCode.substring(scopedIdentifier.getStartByte(), child.getEndByte());
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
            cstNode.setLocation(Location.of(path, tsNode.getStartByte(), tsNode.getEndByte(), body.getStartByte(), body.getEndByte(), sourceCode));

            TSNode identifier = tsNode.getChildByFieldName("name");
            String interfaceName = sourceCode.substring(identifier.getStartByte(), identifier.getEndByte());
            cstNode.setLocalName(interfaceName);
            cstNode.setSimpleName(interfaceName);

            String packageName = "";
            TSNode program = tsNode.getParent();
            for (int i = 0; i < program.getChildCount(); i++) { // package_declarationの他に block_commentやimport_declarationなどもあるのでフィルタリングしている
              TSNode child = program.getChild(i);
              if (child.getType().equals("package_declaration")) {
                TSNode scopedIdentifier = child.getChild(1);
                packageName = sourceCode.substring(scopedIdentifier.getStartByte(), child.getEndByte());
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
            cstNode.setLocation(Location.of(path, tsNode.getStartByte(), tsNode.getEndByte(), block.getStartByte(), block.getEndByte(), sourceCode));

            TSNode identifier = tsNode.getChildByFieldName("name");
            String constructorName = sourceCode.substring(identifier.getStartByte(), identifier.getEndByte());
            cstNode.setLocalName(constructorName);
            cstNode.setSimpleName(constructorName);

            cstNode.addStereotypes(Stereotype.TYPE_CONSTRUCTOR);

            // TODO Parentをちゃんと取る
            if (parent == null) {
              System.out.println("Parent is null");
            }
            parent.addNode(cstNode);
            break; }
          case "method_declaration": {
            CstNode cstNode = new CstNode(cstId++);
            cstNode.setType(JavaNodeTypes.METHOD_DECLARATION);

            TSNode block = tsNode.getChildByFieldName("body");
            if (block.isNull()) {
              cstNode.setLocation(Location.of(path, tsNode.getStartByte(), tsNode.getEndByte(), tsNode.getEndByte(), tsNode.getEndByte(), sourceCode)); // Abstract method has no body
            } else {
              cstNode.setLocation(Location.of(path, tsNode.getStartByte(), tsNode.getEndByte(), block.getStartByte(), block.getEndByte(), sourceCode));
            }

            TSNode identifier = tsNode.getChildByFieldName("name");
            String methodName = sourceCode.substring(identifier.getStartByte(), identifier.getEndByte());
            cstNode.setLocalName(methodName);
            cstNode.setSimpleName(methodName);

            cstNode.addStereotypes(Stereotype.TYPE_MEMBER);

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
