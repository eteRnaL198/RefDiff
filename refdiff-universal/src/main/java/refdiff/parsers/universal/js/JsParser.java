package refdiff.parsers.universal.js;

import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstNodeRelationship;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.Location;
import refdiff.core.cst.TokenizedSource;
import refdiff.core.cst.Parameter;
import refdiff.core.io.FilePathFilter;
import refdiff.core.io.SourceFileSet;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.universal.common.CallGraphGenerator;
import refdiff.parsers.universal.common.NodeUtils;
import refdiff.parsers.universal.common.SourceFileReader;
import refdiff.parsers.universal.common.Tokenizer;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.nio.charset.StandardCharsets;

import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJavascript;

public class JsParser implements LanguagePlugin {

  private int cstId = 0;

  public FilePathFilter getAllowedFilesFilter() {
    return new FilePathFilter(List.of(".js", ".jsx"), List.of(".min.js"));
  }

  public CstRoot parse(SourceFileSet folder) {
    TSParser parser = new TSParser();
    TSLanguage tsLang = new TreeSitterJavascript();
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

      TokenizedSource tokenizedSource = Tokenizer.tokenize(tree, tsLang, filePath); // TODO: Should the argument for tokenize be a relative path?
      root.addTokenizedFile(tokenizedSource);
    }

    CallGraphGenerator callGraphGenerator = new CallGraphGenerator(JsNodeTypes.FUNCTION);
    callGraphGenerator.generateCallGraph(root, sourceCodeMap);

    return root;
  }

  private void addNodes(TSTree tree, TSLanguage tsLang, CstRoot cstRoot, String filePath, byte[] sourceBytes) {
    TSNode fileTsNode = tree.getRootNode(); // This is the (program) node for JS
    CstNode fileCstNode = new CstNode(cstId++);
    fileCstNode.setType(JsNodeTypes.FILE);
    int lineNumber = fileTsNode.getStartPoint().getRow() + 1;
    int endLineNumber = fileTsNode.getEndPoint().getRow() + 1;
    fileCstNode.setLocation(new Location(filePath,fileTsNode.getStartByte(), fileTsNode.getEndByte(),lineNumber,endLineNumber, fileTsNode.getEndByte()));
    String nameForFileNode = getFileNameFromFilePath(filePath);
    fileCstNode.setSimpleName(nameForFileNode);
    fileCstNode.setLocalName(nameForFileNode);
    fileCstNode.setNamespace(getNamespaceFromFilePath(filePath));
    cstRoot.addNode(fileCstNode);

    // Determine the parent for elements within this file (classes, functions, etc.)
    // If fileCstNode was created, it's the parent. Otherwise, fallback to globalRoot.
    CstNode parentCstNode = fileCstNode;

    String classQuerySrc = """
        (class_declaration
          name: (identifier) @class_name
          body: (class_body) @class_body) @class_node
        """;
    TSQuery classTsQuery = new TSQuery(tsLang, classQuerySrc);
    TSQueryCursor classCursor = new TSQueryCursor();
    classCursor.exec(classTsQuery, fileTsNode);
    TSQueryMatch classMatch = new TSQueryMatch();

    while (classCursor.nextMatch(classMatch)) {
      TSQueryCapture[] captures = classMatch.getCaptures();
      TSNode classDeclarationNode = null;
      TSNode nameIdentifierNode = null;
      TSNode bodyNode = null;

      for (TSQueryCapture capture : captures) {
        String captureName = classTsQuery.getCaptureNameForId(capture.getIndex());
        TSNode capturedNode = capture.getNode();
        if ("class_node".equals(captureName)) {
          classDeclarationNode = capturedNode;
        } else if ("class_name".equals(captureName)) {
          nameIdentifierNode = capturedNode;
        } else if ("class_body".equals(captureName)) {
          bodyNode = capturedNode;
        }
      }
      if (classDeclarationNode == null || nameIdentifierNode == null || bodyNode == null) {
        System.err.println("Warning: Could not capture all required parts (class_node, class_name, class_body) for a class declaration in " + filePath + " at match offset " + classMatch.getId());
        if (classDeclarationNode == null) System.err.println("  Missing @class_node");
        if (nameIdentifierNode == null) System.err.println("  Missing @class_name");
        if (bodyNode == null) System.err.println("  Missing @class_body");
        continue;
      }

      CstNode classCstNode = new CstNode(cstId++);
      classCstNode.setType(JsNodeTypes.CLASS);
      lineNumber = classDeclarationNode.getStartPoint().getRow() + 1;
      endLineNumber = classDeclarationNode.getEndPoint().getRow() + 1;
      classCstNode.setLocation(new Location(
        filePath,
        classDeclarationNode.getStartByte(), classDeclarationNode.getEndByte(),
        lineNumber,
        endLineNumber,
        bodyNode.getStartByte(), bodyNode.getEndByte()
      ));
      String className = NodeUtils.getNodeText(nameIdentifierNode, sourceBytes);
      classCstNode.setSimpleName(className);
      classCstNode.setLocalName(className);
      classCstNode.setNamespace(getNamespaceFromFilePath(filePath));
      parentCstNode.addNode(classCstNode); // Add class as child of the FILE node (or globalRoot as fallback)
    }

    // Query for various function definitions
    String functionQuerySrc = String.join("\n",
        "[", // Top-level alternatives
        "  (function_declaration", // Pattern 1: Normal function declaration (async or not)
        "    name: (identifier) @name.id",
        "    parameters: (formal_parameters) @params.node",
        "    body: (statement_block) @body.node) @function_def", // @function_def captures function_declaration
        "",
        "  (generator_function_declaration", // Pattern 2: Generator function
        "    name: (identifier) @name.id",
        "    parameters: (formal_parameters) @params.node",
        "    body: (statement_block) @body.node) @function_def", // @function_def captures generator_function_declaration
        "",
        "  (lexical_declaration", // Pattern 3: Variable assigned function expression
        "    (variable_declarator",
        "      name: (identifier) @name.id", // name.id is the variable identifier
        "      value: (function_expression",
        "               name: (identifier)? @name.id.expr", // Optional: internal name of function expr
        "               parameters: (formal_parameters) @params.node",
        "               body: (statement_block) @body.node) @function_def))", // @function_def captures function_expression node
        "",
        "  (lexical_declaration", // Pattern 4a: Arrow function with formal_parameters (e.g., (a,b) => ..., () => ...)
        "    (variable_declarator",
        "      name: (identifier) @name.id", // name.id is the variable identifier
        "      value: (arrow_function",
        "               parameters: (formal_parameters) @params.node",
        "               body: (_) @body.node) @function_def))", // @function_def captures arrow_function node
        "",
        "  (lexical_declaration", // Pattern 4b: Arrow function with single identifier parameter (e.g., a => ...)
        "    (variable_declarator",
        "      name: (identifier) @name.id", // name.id is the variable identifier
        "      value: (arrow_function",
        "               parameter: (identifier) @params.node",
        "               body: (_) @body.node) @function_def))", // @function_def captures arrow_function node
        "]" // End of alternatives
    );

    TSQuery funcTsQuery = new TSQuery(tsLang, functionQuerySrc);
    TSQueryCursor funcCursor = new TSQueryCursor();
    funcCursor.exec(funcTsQuery, fileTsNode); // fileTsNode is the (program) node
    TSQueryMatch funcMatch = new TSQueryMatch();

    while (funcCursor.nextMatch(funcMatch)) {
      TSNode funcDefinitionNode = null; // Node for the function's full span (e.g., function_declaration, arrow_function)
      TSNode nameIdentifierNode = null;
      TSNode parametersHostNode = null; // Node containing parameters (formal_parameters) or the single param identifier
      TSNode bodyValueNode = null;

      for (TSQueryCapture capture : funcMatch.getCaptures()) {
        TSNode capturedNode = capture.getNode();
        String captureName = funcTsQuery.getCaptureNameForId(capture.getIndex());

        switch (captureName) {
            case "function_def": // Unified capture name for the main function definition node
                funcDefinitionNode = capturedNode;
                break;
            case "name.id": // Covers name from func_decl, or var name for assigned expr/arrow
                nameIdentifierNode = capturedNode;
                break;
            // name.id.expr is an optional capture for named function expressions, currently name.id takes precedence.
            case "params.node": // formal_parameters from func_decl, gen_decl, func_expr
            case "params.node.formal": // formal_parameters from arrow_func like (a,b)=> or ()=>
                parametersHostNode = capturedNode;
                break;
            case "params.node.single": // single identifier param from arrow_func like a=>
                parametersHostNode = capturedNode; // The identifier itself is the "host"
                break;
            case "body.node":
                bodyValueNode = capturedNode;
                break;
        }
      }
      if (funcDefinitionNode == null || nameIdentifierNode == null || bodyValueNode == null) {
        System.err.println("Warning: Could not capture all required parts (definition, name, body) for a function in " + filePath + " at match offset " + funcMatch.getId());
        continue;
      }

      CstNode funcCstNode = new CstNode(cstId++);
      funcCstNode.setType(JsNodeTypes.FUNCTION);
      lineNumber = funcDefinitionNode.getStartPoint().getRow() + 1;
      endLineNumber = funcDefinitionNode.getEndPoint().getRow() + 1;
      funcCstNode.setLocation(new Location(
          filePath,
          funcDefinitionNode.getStartByte(), funcDefinitionNode.getEndByte(),
          lineNumber,
          endLineNumber,
          bodyValueNode.getStartByte(), bodyValueNode.getEndByte()
        ));
      String funcName = NodeUtils.getNodeText(nameIdentifierNode, sourceBytes);
      funcCstNode.setSimpleName(funcName);
      funcCstNode.setLocalName(funcName);
      List<refdiff.core.cst.Parameter> cstParameters = new ArrayList<>();
      if (parametersHostNode != null) {
          extractParameters(parametersHostNode, sourceBytes, cstParameters);
      }
      funcCstNode.setParameters(cstParameters);

      parentCstNode.addNode(funcCstNode); // Add function as child of the FILE node
    }
  }

  private String getNamespaceFromFilePath(String filePath) {
    int lastSlash = filePath.lastIndexOf('/');
    int lastBackslash = filePath.lastIndexOf('\\');
    int lastSeparator = Math.max(lastSlash, lastBackslash);

    if (lastSeparator != -1) {
      return filePath.substring(0, lastSeparator + 1);
    }
    return ""; // No directory path found, likely just a filename
  }

  private String getFileNameFromFilePath(String filePath) {
    int lastSlash = filePath.lastIndexOf('/');
    int lastBackslash = filePath.lastIndexOf('\\');
    int lastSeparator = Math.max(lastSlash, lastBackslash);

    if (lastSeparator != -1) {
      return filePath.substring(lastSeparator + 1);
    }
    // If no separator is found, the filePath itself is the filename
    return filePath;
  }

  private void extractParameters(TSNode parametersHostNode, byte[] sourceBytes, List<Parameter> cstParameters) {
    String hostNodeType = parametersHostNode.getType();

    if ("formal_parameters".equals(hostNodeType)) {
        for (int i = 0; i < parametersHostNode.getChildCount(); i++) {
            TSNode paramElementNode = parametersHostNode.getChild(i);
            if (paramElementNode.isNamed()) { // Process only named nodes like identifier, rest_pattern, etc.
                String paramName = extractParameterNameInternal(paramElementNode, sourceBytes);
                if (paramName != null) {
                    cstParameters.add(new Parameter(paramName));
                }
            }
        }
    } else if ("identifier".equals(hostNodeType)) { // Single parameter for arrow function: param => ...
        String paramName = NodeUtils.getNodeText(parametersHostNode, sourceBytes);
        cstParameters.add(new Parameter(paramName));
    }
  }

  private String extractParameterNameInternal(TSNode paramNode, byte[] sourceBytes) {
      String nodeType = paramNode.getType();
      if ("identifier".equals(nodeType)) {
          return NodeUtils.getNodeText(paramNode, sourceBytes);
      } else if ("rest_pattern".equals(nodeType)) {
        if (paramNode.getNamedChildCount() > 0) {
            TSNode nameNode = paramNode.getNamedChild(0); // (rest_pattern (identifier))
            if (nameNode != null && "identifier".equals(nameNode.getType())) {
                return NodeUtils.getNodeText(nameNode, sourceBytes);
            }
        }
      } else if ("assignment_pattern".equals(nodeType)) { // e.g. name = "Guest"
        TSNode leftNode = paramNode.getChildByFieldName("left");
        if (leftNode != null && "identifier".equals(leftNode.getType())) {
            return NodeUtils.getNodeText(leftNode, sourceBytes);
        }
      }
      // Array/Object patterns (destructuring) could be handled here if needed
      // For now, they will result in null and won't be added as simple named parameters.
      return null;
  }
}
