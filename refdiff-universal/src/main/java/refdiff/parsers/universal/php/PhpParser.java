package refdiff.parsers.universal.php;

import org.treesitter.TSLanguage;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterPhp;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;

import java.util.List;
import java.util.ArrayList;

import refdiff.core.cst.CstNode;
import refdiff.core.cst.Parameter;
import refdiff.core.cst.Stereotype;
import refdiff.core.io.FilePathFilter;
import refdiff.parsers.universal.common.NodeUtils;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.universal.base.BasePlugin;

public class PhpParser extends BasePlugin {

  private int cstId = 0;

  public FilePathFilter getAllowedFilesFilter() {
    return new FilePathFilter(List.of(".php"));
  }

  @Override
  protected TSLanguage getLanguage() {
    return new TreeSitterPhp();
  }

  @Override
  protected String[] getCallableNodeTypes() {
    return new String[]{PhpNodeTypes.METHOD, PhpNodeTypes.FUNCTION};
  }

  @Override
  protected String[] getInheritableNodeTypes() {
    return new String[]{PhpNodeTypes.CLASS, PhpNodeTypes.INTERFACE};
  }

  @Override
  protected void buildCst(TSTree tree, TSLanguage tsLang, String path, byte[] sourceBytes) {
    String querySrc = """
    [
      (class_declaration
        name: (name) @name
        body: (declaration_list) @body
      ) @declaration
      (interface_declaration
          name: (name) @name
          body: (declaration_list) @body
      ) @declaration
      (trait_declaration
          name: (name) @name
          body: (declaration_list) @body
      ) @declaration
      (method_declaration
          name: (name) @name
          parameters: (formal_parameters)? @parameters
          body: (compound_statement)? @body
      ) @declaration
      (function_definition
          name: (name) @name
          parameters: (formal_parameters)? @parameters
          body: (compound_statement) @body
      ) @declaration
    ]
    """;
    TSQuery query = new TSQuery(tsLang, querySrc);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, tree.getRootNode());
    TSQueryMatch match = new TSQueryMatch();

    while (cursor.nextMatch(match)) {
        TSNode name = null;
        TSNode parameters = null;
        TSNode body = null;
        TSNode declaration = null;

        for (TSQueryCapture capture : match.getCaptures()) {
            TSNode capturedNode = capture.getNode();
            String captureName = query.getCaptureNameForId(capture.getIndex());
            switch (captureName) {
                case "name" -> name = capturedNode;
                case "parameters" -> parameters = capturedNode;
                case "body" -> body = capturedNode;
                case "declaration" -> declaration = capturedNode;
            }
        }

        if (declaration == null || name == null) {
            continue;
        }

        CstNode cstNode = new CstNode(cstId++);
        cstNode.setLocation(NodeUtils.generateLocation(declaration, body, path));
        cstNode.setSimpleName(NodeUtils.getNodeText(name, sourceBytes));

        switch (declaration.getType()) {
            case "class_declaration":
                cstNode.setType(PhpNodeTypes.CLASS);
                cstNode.setLocalName(NodeUtils.getNodeText(name, sourceBytes));
                break;
            case "interface_declaration":
                cstNode.setType(PhpNodeTypes.INTERFACE);
                cstNode.setLocalName(NodeUtils.getNodeText(name, sourceBytes));
                break;
            case "trait_declaration":
                cstNode.setType(PhpNodeTypes.TRAIT);
                cstNode.setLocalName(NodeUtils.getNodeText(name, sourceBytes));
                break;
            case "method_declaration":
                cstNode.setType(PhpNodeTypes.METHOD);
                List<Parameter> method_params = extractSignatureParameters(parameters, sourceBytes);
                cstNode.setParameters(method_params);
                cstNode.setLocalName(NodeUtils.getNodeText(name, sourceBytes) + NodeUtils.generateParams(method_params));
                break;
            case "function_definition":
                cstNode.setType(PhpNodeTypes.FUNCTION);
                List<Parameter> func_params = extractSignatureParameters(parameters, sourceBytes);
                cstNode.setParameters(func_params);
                cstNode.setLocalName(NodeUtils.getNodeText(name, sourceBytes) + NodeUtils.generateParams(func_params));
                break;
            default:
                System.out.println("Warning: Unhandled declaration type: " + declaration.getType());
                continue;
        }
        addNodeToParent(cstNode);
    }
  }

  private List<Parameter> extractSignatureParameters(TSNode parametersNode, byte[] sourceBytes) {
    List<Parameter> parameters = new ArrayList<>();
    if (parametersNode == null) {
        return parameters;
    }

    for (int i = 0; i < parametersNode.getChildCount(); i++) {
        TSNode parameterNode = parametersNode.getChild(i);
        TSNode typeNode = parameterNode.getChildByFieldName("type");

        if (typeNode != null) {
            String paramType = NodeUtils.getNodeText(typeNode, sourceBytes);
            parameters.add(new Parameter(paramType));
        } else if (parameterNode.getType().equals("property_promotion_parameter")) {
            // For property promotion, the type is not a named field but a direct child.
            for (int j = 0; j < parameterNode.getChildCount(); j++) {
                TSNode child = parameterNode.getChild(j);
                String childType = child.getType();
                if (childType.equals("primitive_type") || childType.equals("name")) {
                    parameters.add(new Parameter(NodeUtils.getNodeText(child, sourceBytes)));
                    break;
                }
            }
        }
    }
    return parameters;
  }
}
