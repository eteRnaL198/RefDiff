package refdiff.parsers.universal;

import org.treesitter.TSLanguage;
import org.treesitter.TSParser;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJava;
import org.treesitter.TreeSitterC;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
import org.treesitter.TSRange;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import refdiff.core.io.SourceFileSet;
import refdiff.core.io.SourceFolder;
import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstNodeRelationship;
import refdiff.core.cst.CstNodeRelationshipType;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.Location;
import refdiff.core.cst.TokenizedSource;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.CstRootHelper;
import refdiff.core.cst.TokenPosition;
import refdiff.core.io.SourceFile;


public class UniversalParser {
  public CstRoot parse(SourceFileSet folder) {
    TSParser parser = new TSParser();

    TSLanguage tsLang; // TODO 言語切替
    String firstFilePath = folder.getSourceFiles().get(0).getPath();
    if (firstFilePath.endsWith(".java")) {
      tsLang = new TreeSitterJava();
    } else if (firstFilePath.endsWith(".c")) {
      tsLang = new TreeSitterC();
    } else {
      throw new IllegalArgumentException("Unsupported file type: " + firstFilePath);
    }
    parser.setLanguage(tsLang);

    CstRoot root = new CstRoot();
    List<SourceFile> files = folder.getSourceFiles();

    /* Create CstNode for each file */
    for (SourceFile file : files) {
      String sourceCode = "";
      try {
        sourceCode = folder.readContent(file);
      } catch (IOException e) {
        e.printStackTrace();
      }

      TSTree tree = parser.parseString(null, sourceCode);
      addNodes(tree, tsLang, root, file.toString(), sourceCode);
      TokenizedSource tokenizedSource = tokenize(tree, tsLang, file.getPath(), sourceCode); // TODO tokenizeの引数にはrelative pathを渡す？
      root.addTokenizedFile(tokenizedSource);
    }

    /* Create call graph */
    List<CstNode> callableNodes = new ArrayList<>();
    for (CstNode node : root.getNodes()) {
      callableNodes.addAll(getCallableNodes(node));
    }
    Map<String, CstNode> callableNodeMap = new HashMap<>(); 
    for (CstNode callableNode : callableNodes) {
      callableNodeMap.put(callableNode.getSimpleName(), callableNode); //TODO simplenameをhashmapに持たせてるので重複しやすく上書きされる。namespaceなどを使うといいかも
    }
    for (CstNode node : callableNodes) {
      addReferences(node, root, folder, files, callableNodeMap);
    }

    return root;
  }

  private List<CstNode> getCallableNodes(CstNode node) {
    List<CstNode> callableNodes = new ArrayList<>();
    for (CstNode child : node.getNodes()) {
      callableNodes.addAll(getCallableNodes(child));
    }
    String nodeType = node.getType();
    if (nodeType.equals("method") || nodeType.equals("function")) {
      callableNodes.add(node);
    }
    return callableNodes;
  }

  private void addReferences(CstNode node, CstRoot root, SourceFileSet folder, List<SourceFile> files, Map<String, CstNode> callableNodeMap) {
    String path = node.getLocation().getFile();
    String sourceCode = "";
    try {
      Optional<SourceFile> sourceFile = files.stream().filter(f -> f.getPath().equals(path)).findFirst();
      if (sourceFile.isPresent()) {
        sourceCode = folder.readContent(sourceFile.get());
      } else {
        throw new IllegalArgumentException("Source file not found for path: " + path);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    List<String> tokens = CstRootHelper.retrieveTokens(root, sourceCode, node, true);
    for (String token : tokens) {
      if (callableNodeMap.containsKey(token)) {
      CstNode callee = callableNodeMap.get(token);
      root.getRelationships().add(new CstNodeRelationship(CstNodeRelationshipType.USE, node.getId(), callee.getId()));
      }
    }
  }

  private void addNodes(TSTree tree, TSLanguage tsLang, CstRoot root, String path, String sourceCode) {
    String query; // TODO 言語切替
    if (path.endsWith(".java")) {
      query = "[(class_declaration) (method_declaration)] @node";
    } else if (path.endsWith(".c")) {
      query = "[(translation_unit) (function_definition)] @node";
    } else {
      throw new IllegalArgumentException("Unsupported file type: " + path);
    }
    TSQuery tsQuery = new TSQuery(tsLang, query);

    TSNode rootNode = tree.getRootNode();
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(tsQuery, rootNode);
    TSQueryMatch match = new TSQueryMatch();

    String[] splittedSourceCode = sourceCode.split("\n");
    int id = 0;
    CstNode parent = null;
    while (cursor.nextMatch(match)) {
      TSQueryCapture[] captures = match.getCaptures();
      for (TSQueryCapture capture : captures) {
        TSNode tsNode = capture.getNode();
        
        switch(tsNode.getType()) {
          case "class_declaration": { // for Java
            CstNode cstNode = new CstNode(id++);
            cstNode.setType("class");

            TSNode body = tsNode.getChild(3);
            cstNode.setLocation(Location.of(path, tsNode.getStartByte(), tsNode.getEndByte(), body.getStartByte(), body.getEndByte(), sourceCode));

            TSNode identifier = tsNode.getChild(2);
            int idntfrLine = identifier.getStartPoint().getRow();
            int idntfrStart = identifier.getStartPoint().getColumn();
            int idntfrEnd = identifier.getEndPoint().getColumn();
            String className = splittedSourceCode[idntfrLine].substring(idntfrStart, idntfrEnd);
            cstNode.setLocalName(className);
            cstNode.setSimpleName(className);

            TSNode packageDecl = tsNode.getPrevSibling(); // package is declared before class
            TSNode packageIdentifier = packageDecl.getChild(1);
            int pkg_line = packageIdentifier.getStartPoint().getRow();
            int pkg_start = packageIdentifier.getStartPoint().getColumn();
            int pkg_end = packageIdentifier.getEndPoint().getColumn();
            String packageName = splittedSourceCode[pkg_line].substring(pkg_start, pkg_end);
            cstNode.setNamespace(packageName + ".");
            root.addNode(cstNode);
            parent = cstNode;
            break; }
          case "method_declaration": { // for Java
            CstNode cstNode = new CstNode(id++);
            cstNode.setType("method");

            TSNode block = tsNode.getChild(4);
            cstNode.setLocation(Location.of(path, tsNode.getStartByte(), tsNode.getEndByte(), block.getStartByte(), block.getEndByte(), sourceCode));

            TSNode identifier = tsNode.getChild(2);
            int idntfrLine = identifier.getStartPoint().getRow();
            int idntfrStart = identifier.getStartPoint().getColumn();
            int idntfrEnd = identifier.getEndPoint().getColumn();
            String methodName = splittedSourceCode[idntfrLine].substring(idntfrStart, idntfrEnd);
            cstNode.setLocalName(methodName);
            cstNode.setSimpleName(methodName);

            // TODO Parentをちゃんと取る
            if (parent == null) {
              System.out.println("Parent is null");
            }
            parent.addNode(cstNode);
            break; }
          case "translation_unit": { // for C
            CstNode cstNode = new CstNode(id++);
            cstNode.setType("file");
            cstNode.setLocation(Location.of(path, tsNode.getStartByte(), tsNode.getEndByte(), tsNode.getStartByte(), tsNode.getEndByte(), sourceCode)); // TODO bodyと区別して計算
            cstNode.setLocalName(path);
            cstNode.setSimpleName(path);
            root.addNode(cstNode);
            parent = cstNode;
            break; }
          case "function_definition": { // for C
            CstNode cstNode = new CstNode(id++);
            cstNode.setType("function");
            
            TSNode block = tsNode.getChild(2);
            cstNode.setLocation(Location.of(path, tsNode.getStartByte(), tsNode.getEndByte(), block.getStartByte(), block.getEndByte(), sourceCode)); // TODO bodyと区別して計算

            TSNode declarator = tsNode.getChild(1);
            TSNode identifier = declarator.getChild(0);
            int idntfrLine = identifier.getStartPoint().getRow();
            int idntfrStart = identifier.getStartPoint().getColumn();
            int idntfrEnd = identifier.getEndPoint().getColumn();
            String functionName = splittedSourceCode[idntfrLine].substring(idntfrStart, idntfrEnd);
            cstNode.setLocalName(functionName);
            cstNode.setSimpleName(functionName);
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

  private TokenizedSource tokenize(TSTree tree, TSLanguage tsLang, String path, String sourceCode) {
    String query = "_ @node";
    TSQuery tsQuery = new TSQuery(tsLang, query);
    TSQueryCursor cursor = new TSQueryCursor();
    TSNode rootNode = tree.getRootNode();
    cursor.exec(tsQuery, rootNode);
    TSQueryMatch match = new TSQueryMatch();
    
    String[] splittedSourceCode = sourceCode.split("\n");
    int[] offsets = new int[splittedSourceCode.length]; // Used to calculate the start and end positions in the string source code.
    int offset = 0;
    for (int i = 0; i < splittedSourceCode.length; i++) {
      offsets[i] = offset;
      offset += splittedSourceCode[i].length() + 1;
    }
    List<TokenPosition> tokens = new ArrayList<>();
    while (cursor.nextMatch(match)) {
      TSQueryCapture[] captures = match.getCaptures();
      for (TSQueryCapture capture : captures) {
        TSNode node = capture.getNode();
        if (node.getChildCount() == 0) { // Leaf node
          int startRow = node.getStartPoint().getRow();
          int startColumn = node.getStartPoint().getColumn();
          int endColumn = node.getEndPoint().getColumn();
          int start = offsets[startRow] + startColumn;
          int end = offsets[startRow] + endColumn;
          tokens.add(new TokenPosition(start, end));
        }
      }
    }
    return new TokenizedSource(path.toString(), tokens);
  }

  private void parseWithCtags(Path[] paths) {
    for (Path inputPath : paths) {
      CstRoot root = new CstRoot();
      List<Map<String, Object>> mapList = runCtags(inputPath);
      for (Map<String, Object> map : mapList) {
        switch (map.get("kind").toString()) {
          case "class":
            CstNode node = new CstNode(0);
            node.setType("class");
            node.setLocalName(map.get("name").toString());
            node.setSimpleName(map.get("name").toString());
            node.setNamespace("");
            root.addNode(node);
            break;
          case "method":
            // Handle variable kind
            break;
          default:

            break;
        }
      }
      root.getNodes().forEach(node -> {
        System.out.println(node.getLocalName());
      });
    }
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> runCtags(final Path inputPath) {
    String[] cmd = { "ctags", "--output-format=json", "--fields=NnesKS", "-o", "-", inputPath.toString() };
    ObjectMapper mapper = new ObjectMapper();
    List<Map<String, Object>> mapList = new ArrayList<>();
    try {
      Process process = new ProcessBuilder(cmd).start();
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        Map<String, Object> map = mapper.readValue(line, Map.class);
        mapList.add(map);
      }
      reader.close();
      process.waitFor();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
    return mapList;
  }
}