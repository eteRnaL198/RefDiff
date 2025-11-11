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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.Location;
import refdiff.core.cst.Parameter;
import refdiff.core.cst.TokenizedSource;
import refdiff.core.io.SourceFileSet;
import refdiff.parsers.universal.common.CallGraphGenerator;
import refdiff.parsers.universal.common.Parser;
import refdiff.parsers.universal.common.NodeUtils;
import refdiff.parsers.universal.common.SourceFileReader;
import refdiff.parsers.universal.common.Tokenizer;

public class CParser implements Parser {

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

    CallGraphGenerator callGraphGenerator = new CallGraphGenerator(CNodeTypes.FUNCTION);
    callGraphGenerator.generateCallGraph(root, sourceCodeMap);

    return root;
  }

  private void addNodes(TSTree tree, TSLanguage tsLang, CstRoot cstRoot, String filePath, byte[] sourceBytes) {
    TSNode rootNode = tree.getRootNode();

    String querySrc = """
        (translation_unit) @file

        (function_definition
          declarator: [
            (function_declarator
                declarator: (identifier) @function_name
                parameters: (parameter_list)? @function_params)
            (pointer_declarator
                declarator: (function_declarator
                    declarator: (identifier) @function_name
                    parameters: (parameter_list)? @function_params))
          ]
          body: (compound_statement)? @function_body
        ) @function_node

        (declaration
          declarator: [
            (function_declarator
                declarator: (identifier) @function_name
                parameters: (parameter_list)? @function_params)
            (pointer_declarator
                declarator: (function_declarator
                    declarator: (identifier) @function_name
                    parameters: (parameter_list)? @function_params))
          ]
        ) @function_node
        """;

    TSQuery tsQuery = new TSQuery(tsLang, querySrc);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(tsQuery, rootNode);

    TSQueryMatch match = new TSQueryMatch();
    CstNode fileCstNode = null;

    while (cursor.nextMatch(match)) {
        if (match.getPatternIndex() == 0) { // File match
            TSNode fileTsNode = match.getCaptures()[0].getNode();
            fileCstNode = new CstNode(cstId++);
            fileCstNode.setType(CNodeTypes.FILE);
            fileCstNode.setSimpleName(getFileNameFromFilePath(filePath));
            fileCstNode.setLocalName(getFileNameFromFilePath(filePath));
            fileCstNode.setNamespace(getNamespaceFromFilePath(filePath));
            int startLine = fileTsNode.getStartPoint().getRow() + 1;
            int endLine = fileTsNode.getEndPoint().getRow() + 1;
            fileCstNode.setLocation(new Location(filePath, fileTsNode.getStartByte(), fileTsNode.getEndByte(), startLine, endLine, fileTsNode.getStartByte(), fileTsNode.getEndByte()));
            cstRoot.addNode(fileCstNode);
        } else { // Function match
            if (fileCstNode == null) {
                fileCstNode = new CstNode(cstId++);
                fileCstNode.setType(CNodeTypes.FILE);
                fileCstNode.setSimpleName(getFileNameFromFilePath(filePath));
                fileCstNode.setLocalName(getFileNameFromFilePath(filePath));
                fileCstNode.setNamespace(getNamespaceFromFilePath(filePath));
                fileCstNode.setLocation(new Location(filePath, rootNode.getStartByte(), rootNode.getEndByte(), rootNode.getStartPoint().getRow() + 1, rootNode.getEndPoint().getRow() + 1, rootNode.getStartByte(), rootNode.getEndByte()));
                cstRoot.addNode(fileCstNode);
            }

            TSNode functionNode = null;
            TSNode nameNode = null;
            TSNode paramsNode = null;
            TSNode bodyNode = null;

            for (TSQueryCapture capture : match.getCaptures()) {
                TSNode capturedNode = capture.getNode();
                String captureName = tsQuery.getCaptureNameForId(capture.getIndex());
                switch (captureName) {
                    case "function_node": functionNode = capturedNode; break;
                    case "function_name": nameNode = capturedNode; break;
                    case "function_params": paramsNode = capturedNode; break;
                    case "function_body": bodyNode = capturedNode; break;
                }
            }

            if (functionNode == null || nameNode == null) {
                continue;
            }

            CstNode functionCstNode = new CstNode(cstId++);
            functionCstNode.setType(CNodeTypes.FUNCTION);

            String functionName = getNodeText(nameNode, sourceBytes);
            functionCstNode.setSimpleName(functionName);
            functionCstNode.setNamespace(null);

            int defStartByte = functionNode.getStartByte();
            int defEndByte = functionNode.getEndByte();
            int bodyStartByte;
            int bodyEndByte;

            if (bodyNode == null) {
                bodyStartByte = defStartByte;
                bodyEndByte = defEndByte;
            } else {
                bodyStartByte = bodyNode.getStartByte();
                bodyEndByte = bodyNode.getEndByte();
            }

            int funcLineNumber = functionNode.getStartPoint().getRow() + 1;
            int funcEndLineNumber = functionNode.getEndPoint().getRow() + 1;
            functionCstNode.setLocation(new Location(filePath, defStartByte, defEndByte, funcLineNumber, funcEndLineNumber, bodyStartByte, bodyEndByte));

            List<Parameter> cstParameters = new ArrayList<>();
            List<String> paramTypes = new ArrayList<>();
            extractParams(paramsNode, sourceBytes, cstParameters, paramTypes, tsLang);

            functionCstNode.setParameters(cstParameters);
            String localName = functionName + "(" + String.join(", ", paramTypes) + ")";
            functionCstNode.setLocalName(localName);

            fileCstNode.addNode(functionCstNode);
        }
    }
  }

  private void extractParams(TSNode paramsNode, byte[] sourceBytes, List<Parameter> cstParameters, List<String> paramTypes, TSLanguage tsLang) {
    if (paramsNode == null) {
        return;
    }

    String paramQuerySrc = """
        [
            (parameter_declaration
                type: (_) @param_type
                declarator: (_) @param_declarator
            )
            (variadic_parameter) @variadic
        ]
        """;
    
    TSQuery paramQuery = new TSQuery(tsLang, paramQuerySrc);
    TSQueryCursor paramCursor = new TSQueryCursor();
    paramCursor.exec(paramQuery, paramsNode);
    TSQueryMatch paramMatch = new TSQueryMatch();

    while (paramCursor.nextMatch(paramMatch)) {
        TSNode typeNode = null;
        TSNode declaratorNode = null;
        boolean isVariadic = false;

        for (TSQueryCapture capture : paramMatch.getCaptures()) {
            String captureName = paramQuery.getCaptureNameForId(capture.getIndex());
            switch (captureName) {
                case "param_type": typeNode = capture.getNode(); break;
                case "param_declarator": declaratorNode = capture.getNode(); break;
                case "variadic": isVariadic = true; break;
            }
        }

        if (isVariadic) {
            paramTypes.add("...");
            cstParameters.add(new Parameter("..."));
        } else if (typeNode != null) {
            String typeStr = NodeUtils.getNodeText(typeNode, sourceBytes);
            if (typeStr.startsWith("struct ")) {
                typeStr = typeStr.substring(7);
            }
            if (typeStr.equals("void") && paramsNode.getNamedChildCount() == 1) {
                continue;
            }
            paramTypes.add(typeStr);

            String paramName = null;
            if (declaratorNode != null) {
                paramName = NodeUtils.getNodeText(declaratorNode, sourceBytes);
            }
            
            if (paramName != null) {
                cstParameters.add(new Parameter(paramName));
            } else {
                cstParameters.add(new Parameter(""));
            }
        }
    }
  }

  private String getNamespaceFromFilePath(String filePath) {
    int lastSlash = filePath.lastIndexOf('/');
    int lastBackslash = filePath.lastIndexOf('\\');
    int lastSeparator = Math.max(lastSlash, lastBackslash);

    if (lastSeparator != -1) {
      return filePath.substring(0, lastSeparator + 1);
    }
    return "";
  }

  private String getFileNameFromFilePath(String filePath) {
    int lastSlash = filePath.lastIndexOf('/');
    int lastBackslash = filePath.lastIndexOf('\\');
    int lastSeparator = Math.max(lastSlash, lastBackslash);

    if (lastSeparator != -1) {
      return filePath.substring(lastSeparator + 1);
    }
    return filePath;
  }
}