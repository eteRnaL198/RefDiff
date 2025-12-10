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

public class JsPlugin extends BasePlugin {

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
      } else {
        cstNode.setType(JsNodeTypes.FUNCTION);
        String funcName = NodeUtils.getNodeText(name, sourceBytes);
        cstNode.setSimpleName(funcName);
        cstNode.setLocalName(funcName);

        List<Parameter> cstParameters = new ArrayList<>();
        if (parameters != null) {
          extractParameters(parameters, sourceBytes, cstParameters, tsLang);
        }
        cstNode.setParameters(cstParameters);
      }
      addNodeToParent(cstNode);
    }
  }

  private void extractParameters(TSNode parametersHostNode, byte[] sourceBytes, List<Parameter> cstParameters, TSLanguage tsLang) {
    if (parametersHostNode == null) {
        return;
    }

    String hostNodeType = parametersHostNode.getType();
    if ("identifier".equals(hostNodeType)) { // Single parameter for arrow function: param => ...
        cstParameters.add(new Parameter(NodeUtils.getNodeText(parametersHostNode, sourceBytes)));
        return;
    }

    if (!"formal_parameters".equals(hostNodeType)) {
        return;
    }

    for (int i = 0; i < parametersHostNode.getNamedChildCount(); i++) {
        TSNode paramChildNode = parametersHostNode.getNamedChild(i);
        String paramName = queryParameterName(paramChildNode, sourceBytes, tsLang);
        if (paramName != null) {
            cstParameters.add(new Parameter(paramName));
        }
    }
  }

  private String queryParameterName(TSNode paramNode, byte[] sourceBytes, TSLanguage tsLang) {
    String nodeType = paramNode.getType();
    String querySrc;
    String nameCapture = "name";
    boolean isRest = false;

    switch (nodeType) {
        case "identifier":
            return NodeUtils.getNodeText(paramNode, sourceBytes);
        case "assignment_pattern":
            querySrc = "(assignment_pattern left: (identifier) @name)";
            break;
        case "rest_pattern":
            querySrc = "(rest_pattern (identifier) @name)";
            isRest = true;
            break;
        default:
            return null;
    }

    TSQuery query = new TSQuery(tsLang, querySrc);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, paramNode);
    TSQueryMatch match = new TSQueryMatch();

    if (cursor.nextMatch(match)) {
        for (TSQueryCapture capture : match.getCaptures()) {
            if (nameCapture.equals(query.getCaptureNameForId(capture.getIndex()))) {
                TSNode nameNode = capture.getNode();
                String name = NodeUtils.getNodeText(nameNode, sourceBytes);
                return isRest ? "..." + name : name;
            }
        }
    }

    return null;
  }
}
