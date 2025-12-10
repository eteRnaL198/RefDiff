package refdiff.parsers.universal.js;

import java.util.ArrayList;
import java.util.List;

import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJavascript;

import refdiff.core.cst.CstNode;
import refdiff.core.cst.Parameter;
import refdiff.core.io.FilePathFilter;
import refdiff.parsers.universal.base.BasePlugin;
import refdiff.parsers.universal.common.FilePathUtils;
import refdiff.parsers.universal.common.NodeUtils;

public class JsParser extends BasePlugin {

  private int cstId = 0;

  @Override
  public FilePathFilter getAllowedFilesFilter() {
    return new FilePathFilter(List.of(".js", ".jsx"), List.of(".min.js"));
  }

  @Override
  protected TSLanguage getLanguage() {
    return new TreeSitterJavascript();
  }

  @Override
  protected String[] getCallableNodeTypes() {
    return new String[] { JsNodeTypes.FUNCTION };
  }

  @Override
  protected String[] getInheritableNodeTypes() {
    return new String[] { JsNodeTypes.CLASS };
  }

  @Override
  protected void buildCst(TSTree tree, TSLanguage tsLang, String path, byte[] sourceBytes) {
    String querySrc = """
        [
          (program) @program
          (class_declaration
            name: (identifier) @name
            body: (class_body) @body) @declaration
          (function_declaration
            name: (identifier) @name
            parameters: (formal_parameters) @parameters
            body: (statement_block) @body) @declaration
          (generator_function_declaration
            name: (identifier) @name
            parameters: (formal_parameters) @parameters
            body: (statement_block) @body) @declaration
          (lexical_declaration
            (variable_declarator
              name: (identifier) @name
              value: (function_expression
                parameters: (formal_parameters) @parameters
                body: (statement_block) @body))) @declaration
          (lexical_declaration
            (variable_declarator
              name: (identifier) @name
              value: (arrow_function
                parameters: (formal_parameters) @parameters
                body: (_) @body))) @declaration
          (lexical_declaration
            (variable_declarator
              name: (identifier) @name
              value: (arrow_function
                parameter: (identifier) @parameters
                body: (_) @body))) @declaration
        ]""";

    TSQuery tsQuery = new TSQuery(tsLang, querySrc);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(tsQuery, tree.getRootNode());

    TSQueryMatch match = new TSQueryMatch();
    while (cursor.nextMatch(match)) {
      TSNode name = null;
      TSNode parameters = null;
      TSNode body = null;
      TSNode declaration = null;
      TSNode program = null;

      for (TSQueryCapture capture : match.getCaptures()) {
        TSNode capturedNode = capture.getNode();
        String captureName = tsQuery.getCaptureNameForId(capture.getIndex());
        switch (captureName) {
        case "name":
          name = capturedNode;
          break;
        case "parameters":
          parameters = capturedNode;
          break;
        case "body":
          body = capturedNode;
          break;
        case "declaration":
          declaration = capturedNode;
          break;
        case "program":
          program = capturedNode;
          break;
        }
      }

      CstNode cstNode = new CstNode(cstId++);

      if (program != null) {
        cstNode.setType(JsNodeTypes.FILE);
        cstNode.setSimpleName(FilePathUtils.extractFileNameFromFilePath(path));
        cstNode.setLocalName(FilePathUtils.extractFileNameFromFilePath(path));
        cstNode.setNamespace(FilePathUtils.extractDirectoryFromFilePath(path));
        cstNode.setLocation(NodeUtils.generateLocation(program, null, path));
        addNodeToParent(cstNode);
        continue;
      }

      if (declaration == null) {
        continue;
      }

      String nodeType = declaration.getType();
      cstNode.setLocation(NodeUtils.generateLocation(declaration, body, path));
      
      if (nodeType.equals("class_declaration")) {
        cstNode.setType(JsNodeTypes.CLASS);
        String className = NodeUtils.getNodeText(name, sourceBytes);
        cstNode.setSimpleName(className);
        cstNode.setLocalName(className);
        cstNode.setNamespace(FilePathUtils.extractDirectoryFromFilePath(path));
      } else if (nodeType.equals("function_declaration")
          || nodeType.equals("generator_function_declaration")
          || nodeType.equals("lexical_declaration")) {
        cstNode.setType(JsNodeTypes.FUNCTION);
        String funcName = NodeUtils.getNodeText(name, sourceBytes);
        cstNode.setSimpleName(funcName);
        cstNode.setLocalName(funcName);

        List<Parameter> cstParameters = new ArrayList<>();
        if (parameters != null) {
          extractParameters(parameters, sourceBytes, cstParameters);
        }
        cstNode.setParameters(cstParameters);
      }
      addNodeToParent(cstNode);
    }
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
          return "..." + NodeUtils.getNodeText(nameNode, sourceBytes);
        }
      }
    } else if ("assignment_pattern".equals(nodeType)) { // e.g. name = "Guest"
      TSNode leftNode = paramNode.getChildByFieldName("left");
      if (leftNode != null && "identifier".equals(leftNode.getType())) {
        return NodeUtils.getNodeText(leftNode, sourceBytes);
      }
    }
    // Array/Object patterns (destructuring) could be handled here if needed
    // For now, they will result in null and won't be added as simple named
    // parameters.
    return null;
  }
}
