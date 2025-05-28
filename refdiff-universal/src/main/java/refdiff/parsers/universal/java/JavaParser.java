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

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;


import refdiff.core.io.SourceFileSet;
import refdiff.parsers.universal.common.Tokenizer;
import refdiff.parsers.universal.common.SourceFileReader;
import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstNodeRelationship;
import refdiff.core.cst.CstNodeRelationshipType;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.Location;
import refdiff.core.cst.Stereotype;
import refdiff.core.cst.TokenizedSource;
import refdiff.parsers.universal.common.CallGraphGenerator;


public class JavaParser {

  private int cstId = 0;

  public CstRoot parse(SourceFileSet folder) {
    TSParser parser = new TSParser();
    TSLanguage tsLang = new TreeSitterJava();
    parser.setLanguage(tsLang);

    CstRoot root = new CstRoot();
    Map<String, String> sourceCodeMap = SourceFileReader.readAllSourceFiles(folder);

    for (Map.Entry<String, String> entry : sourceCodeMap.entrySet()) {
      String filePath = entry.getKey();
      String sourceCode = entry.getValue();
      TSTree tree = parser.parseString(null, sourceCode);
      addNodes(tree, tsLang, root, filePath, sourceCode);

      TokenizedSource tokenizedSource = Tokenizer.tokenize(tree, tsLang, filePath, sourceCode); // TODO: Should the argument for tokenize be a relative path?
      root.addTokenizedFile(tokenizedSource);
    }

    /* Create Hierarchy Graph */
    List<CstNode> classOrInterfaceNodes = new ArrayList<>();
    for (CstNode node : root.getNodes()) {
      classOrInterfaceNodes.addAll(getClassOrInterfaceNodes(node));
    }
    Map<String, CstNode> classOrInterfaceNodeMap = new HashMap<>();
    for (CstNode classOrInterfaceNode : classOrInterfaceNodes) {
      classOrInterfaceNodeMap.put(classOrInterfaceNode.getSimpleName(), classOrInterfaceNode); //TODO simplenameをhashmapに持たせてるので重複しやすく上書きされる。namespaceなどを使うといいかも
    }
    for (Map.Entry<String, String> entry : sourceCodeMap.entrySet()) {
      String sourceCode = entry.getValue();
      TSTree tree = parser.parseString(null, sourceCode); // TODO parse済みのtreeを使いたい
      addInheritanceRelationship(root, classOrInterfaceNodeMap, tree, tsLang, sourceCode);
    }

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

  private void addInheritanceRelationship(CstRoot root, Map<String, CstNode> classOrInterfaceNodeMap, TSTree tree, TSLanguage tsLang, String sourceCode) {
    String query = "[(superclass) (super_interfaces) (extends_interfaces)] @node";
    TSQuery tsQuery = new TSQuery(tsLang, query);
    TSQueryCursor cursor = new TSQueryCursor();
    TSNode rootNode = tree.getRootNode();
    cursor.exec(tsQuery, rootNode);
    TSQueryMatch match = new TSQueryMatch();

    while (cursor.nextMatch(match)) {
      TSQueryCapture[] captures = match.getCaptures();
      for (TSQueryCapture capture : captures) {
        TSNode tsNode = capture.getNode();
        switch (tsNode.getType()) {
          case "superclass": {
            TSNode superclass = tsNode; // (superclass (type_identifier)) は extends Bar の2つを含む
            TSNode extendsToken = superclass.getChild(0); // type_identifier は extends のこと
            String superclassName = sourceCode.substring(extendsToken.getEndByte(), superclass.getEndByte()).trim(); // 先頭の空白を削除
    
            TSNode identifier = superclass.getParent().getChildByFieldName("name");
            String className = sourceCode.substring(identifier.getStartByte(), identifier.getEndByte());
    
            if (!classOrInterfaceNodeMap.containsKey(superclassName)) { // 入力として与えられたフォルダにsuperclassの定義ファイルが含まれていなかった場合
              continue;
            }
            root.getRelationships().add(new CstNodeRelationship(CstNodeRelationshipType.SUBTYPE, classOrInterfaceNodeMap.get(className).getId(), classOrInterfaceNodeMap.get(superclassName).getId()));
            break; }
          case "super_interfaces": { // class implements interfaces
            TSNode interfaces = tsNode;
            TSNode subClass = interfaces.getParent().getChildByFieldName("name");
            String subClassName = sourceCode.substring(subClass.getStartByte(), subClass.getEndByte());

            TSNode implementsToken = interfaces.getChild(0); // (super_interfaces (type_identifier)) type_identifier は implements のこと
            List<String> interfaceNames = java.util.Arrays.stream(
              sourceCode.substring(implementsToken.getEndByte(), interfaces.getEndByte()).split(",")).map(String::trim).toList(); // "Foo, Bar, Baz" -> ["Foo", "Bar", "Baz"]
            for (String name : interfaceNames) {
              if (!classOrInterfaceNodeMap.containsKey(name)) { // 入力として与えられたフォルダにinterfaceの定義ファイルが含まれていなかった場合
                continue;
              }
              root.getRelationships().add(new CstNodeRelationship(CstNodeRelationshipType.SUBTYPE, classOrInterfaceNodeMap.get(subClassName).getId(), classOrInterfaceNodeMap.get(name).getId()));
            }
            break; }
          case "extends_interfaces": { // interface extends interfaces
            TSNode extendsInterfaces = tsNode;
            TSNode subInterface = extendsInterfaces.getParent().getChildByFieldName("name");
            String subInterfaceName = sourceCode.substring(subInterface.getStartByte(), subInterface.getEndByte());

            TSNode extendsToken = extendsInterfaces.getChild(0); // (extends_interfaces (type_identifier)) type_identifier は extends のこと
            List<String> extendedNames = java.util.Arrays.stream(
              sourceCode.substring(extendsToken.getEndByte(), extendsInterfaces.getEndByte()).split(",")).map(String::trim).toList(); // "Foo, Bar, Baz" -> ["Foo", "Bar", "Baz"]
            
            for (String name : extendedNames) {
              if (!classOrInterfaceNodeMap.containsKey(name)) { // 入力として与えられたフォルダにinterfaceの定義ファイルが含まれていなかった場合
                continue;
              }
              root.getRelationships().add(new CstNodeRelationship(CstNodeRelationshipType.SUBTYPE, classOrInterfaceNodeMap.get(subInterfaceName).getId(), classOrInterfaceNodeMap.get(name).getId()));
            }
            break; }
          default:
            break;
        }
      }
    }
    return;
  }

  private List<CstNode> getClassOrInterfaceNodes(CstNode node) { // TODO CstRoot.forEachNode()で取れるかも
    List<CstNode> classOrInterfaceNodes = new ArrayList<>();
    for (CstNode child : node.getNodes()) {
      classOrInterfaceNodes.addAll(getClassOrInterfaceNodes(child));
    }
    String nodeType = node.getType();
    if (nodeType.equals(JavaNodeTypes.CLASS_DECLARATION) || nodeType.equals(JavaNodeTypes.INTERFACE_DECLARATION)) {
      classOrInterfaceNodes.add(node);
    }
    return classOrInterfaceNodes;
  }

}
