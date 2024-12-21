package refdiff.parsers.universal;

import org.treesitter.TSLanguage;
import org.treesitter.TSParser;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJava;
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
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.Location;
import refdiff.core.cst.TokenizedSource;
import refdiff.core.diff.CstDiff;
import refdiff.core.cst.TokenPosition;
import refdiff.core.io.SourceFile;


public class UniversalParser {
  public CstRoot parse(SourceFileSet folder) {
    TSParser parser = new TSParser();
    TSLanguage java = new TreeSitterJava();
    parser.setLanguage(java);

    CstRoot root = new CstRoot();
    List<SourceFile> files = folder.getSourceFiles();
    for (SourceFile file : files) {
      String sourceCode = "";
      try {
        sourceCode = folder.readContent(file);
      } catch (IOException e) {
        e.printStackTrace();
      }

      TSTree tree = parser.parseString(null, sourceCode);
      addNodes(tree, root, file.getPath(), sourceCode);
      TokenizedSource tokenizedSource = tokenize(tree, file.getPath(), sourceCode);
      root.addTokenizedFile(tokenizedSource);
    }
    return root;
  }

  private void addNodes(TSTree tree, CstRoot root, String path, String sourceCode) { //TODO tokenizeとaddNodeでクエリ2回投げるのではなく1回にまとめた方が速い？
    TSNode rootNode = tree.getRootNode();
    TSLanguage java = new TreeSitterJava();
    String query = "[(class_declaration) (method_declaration)] @node"; 
    TSQuery tsQuery = new TSQuery(java, query); // TODO 引数に渡すのでもいいかも
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
          case "class_declaration": {
            CstNode cstNode = new CstNode(id++);
            cstNode.setType("class");

            TSNode body = tsNode.getChild(3);
            cstNode.setLocation(Location.of(path, tsNode.getStartPoint().getRow(), tsNode.getEndPoint().getRow(), body.getStartPoint().getRow(), body.getEndPoint().getRow(), sourceCode)); // TODO beginとendを tokenizedと同じ、StringのSourceCodeでの値にする

            TSNode identifier = tsNode.getChild(2);
            int idntfr_line = identifier.getStartPoint().getRow();
            int idntfr_start = identifier.getStartPoint().getColumn();
            int idntfr_end = identifier.getEndPoint().getColumn();
            String className = splittedSourceCode[idntfr_line].substring(idntfr_start, idntfr_end);
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
          case "method_declaration": {
            CstNode cstNode = new CstNode(id++);
            cstNode.setType("method");

            TSNode block = tsNode.getChild(4);
            cstNode.setLocation(Location.of(path, tsNode.getStartPoint().getRow(), tsNode.getEndPoint().getRow(), block.getStartPoint().getRow(), block.getEndPoint().getRow(), sourceCode)); // TODO beginとendを tokenizedと同じ、StringのSourceCodeでの値にする

            TSNode identifier = tsNode.getChild(2);
            int idntfr_line = identifier.getStartPoint().getRow();
            int idntfr_start = identifier.getStartPoint().getColumn();
            int idntfr_end = identifier.getEndPoint().getColumn();
            String methodName = splittedSourceCode[idntfr_line].substring(idntfr_start, idntfr_end);
            cstNode.setLocalName(methodName);
            cstNode.setSimpleName(methodName);

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

  private TokenizedSource tokenize(TSTree tree, String path, String sourceCode) {
    TSNode rootNode = tree.getRootNode();
    TSLanguage java = new TreeSitterJava();
    String query = "_ @node";
    TSQuery tsQuery = new TSQuery(java, query); // TODO 引数に渡すのでもいいかも
    TSQueryCursor cursor = new TSQueryCursor();
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