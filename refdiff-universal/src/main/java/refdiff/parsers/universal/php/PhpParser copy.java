// package refdiff.parsers.universal.php;

// import java.nio.charset.StandardCharsets;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import org.treesitter.TSLanguage;
// import org.treesitter.TSNode;
// import org.treesitter.TSParser;
// import org.treesitter.TSQuery;
// import org.treesitter.TSQueryCapture;
// import org.treesitter.TSQueryCursor;
// import org.treesitter.TSQueryMatch;
// import org.treesitter.TSTree;
// import org.treesitter.TreeSitterPhp;
// import refdiff.core.cst.CstNode;
// import refdiff.core.cst.CstRoot;
// import refdiff.core.cst.Location;
// import refdiff.core.cst.Parameter;
// import refdiff.core.cst.TokenizedSource;
// import refdiff.core.io.SourceFileSet;
// import refdiff.parsers.universal.common.CallGraphGenerator;
// import refdiff.parsers.universal.common.SourceFileReader;
// import refdiff.parsers.universal.common.Tokenizer;
// import refdiff.parsers.universal.common.NodeUtils;
// import refdiff.parsers.universal.common.Parser;

// public class PhpParser implements Parser {
//   private int cstId = 0;

//   public CstRoot parse(SourceFileSet folder) {
//     TSParser parser = new TSParser();
//     TSLanguage tsLang = new TreeSitterPhp();
//     parser.setLanguage(tsLang);

//     CstRoot root = new CstRoot();

//     Map<String, String> sourceCodeMap = SourceFileReader.readAllSourceFiles(folder);
//     Map<String, TSTree> parsedTreeMap = new HashMap<>();
//     for (Map.Entry<String, String> sourceEntry : sourceCodeMap.entrySet()) {
//       try {
//         TSTree tree = parser.parseString(null, sourceEntry.getValue());
//         parsedTreeMap.put(sourceEntry.getKey(), tree);
//       } catch (Throwable t) {
//         System.err.println("Caught throwable: " + t.getMessage());
//         t.printStackTrace();
//         throw t;
//       }
//     }

//     for (Map.Entry<String, TSTree> treeEntry : parsedTreeMap.entrySet()) {
//       String filePath = treeEntry.getKey();
//       TSTree tree = treeEntry.getValue();
//       String sourceCode = sourceCodeMap.get(filePath);
//       byte[] sourceBytes = sourceCode.getBytes(StandardCharsets.UTF_8);
//       addNodes(tree, tsLang, root, filePath, sourceBytes);

//       TokenizedSource tokenizedSource = Tokenizer.tokenize(tree, tsLang, filePath);
//       root.addTokenizedFile(tokenizedSource);
//     }

//     CallGraphGenerator callGraphGenerator = new CallGraphGenerator(PhpNodeTypes.METHOD, PhpNodeTypes.FUNCTION);
//     callGraphGenerator.generateCallGraph(root, sourceCodeMap);

//     return root;
//   }

//   private void addNodes(TSTree tree, TSLanguage tsLang, CstRoot cstRoot, String filePath, byte[] sourceBytes) {
//     String querySrc =
//         "[ "
//             + "(function_definition name: (name) @name parameters: (formal_parameters) @params body: (compound_statement) @body) @function "
//             + "(method_declaration name: (name) @name parameters: (formal_parameters) @params body: (compound_statement) @body) @method "
//             + "(method_declaration name: (name) @name parameters: (formal_parameters) @params) @method "
//             + // closures / anonymous functions assigned to variables
//             "(assignment_expression left: (variable_name) @name right: (anonymous_function parameters: (formal_parameters) @params body: (compound_statement) @body) @function) "
//             + // arrow functions assigned to variables (body is an expression)
//             "(assignment_expression left: (variable_name) @name right: (arrow_function parameters: (formal_parameters) @params body: (expression) @body) @function) "
//             + "]";

//     TSQuery query = new TSQuery(tsLang, querySrc);
//     TSQueryCursor cursor = new TSQueryCursor();
//     cursor.exec(query, tree.getRootNode());

//     TSQueryMatch match = new TSQueryMatch();
//     while (cursor.nextMatch(match)) {
//       TSNode nameNode = null;
//       TSNode paramsNode = null;
//       TSNode bodyNode = null;
//       TSNode defNode = null;
//       String nodeType = null;

//       for (TSQueryCapture capture : match.getCaptures()) {
//         TSNode capturedNode = capture.getNode();
//         String captureName = query.getCaptureNameForId(capture.getIndex());

//         switch (captureName) {
//           case "function":
//             defNode = capturedNode;
//             nodeType = PhpNodeTypes.FUNCTION;
//             break;
//           case "method":
//             defNode = capturedNode;
//             nodeType = PhpNodeTypes.METHOD;
//             break;
//           case "name":
//             // Prefer the left-hand variable name when closures are assigned to variables.
//             nameNode = capturedNode;
//             break;
//           case "params":
//             paramsNode = capturedNode;
//             break;
//           case "body":
//             bodyNode = capturedNode;
//             break;
//         }
//       }

//       if (defNode != null && nameNode != null) {
//         CstNode cstNode = new CstNode(cstId++);
//         cstNode.setType(nodeType);

//         String simpleName = NodeUtils.getNodeText(nameNode, sourceBytes);
//         // strip leading $ for variable-based names
//         if (simpleName.startsWith("$")) {
//           simpleName = simpleName.substring(1);
//         }
//         cstNode.setSimpleName(simpleName);

//         cstNode.setNamespace(filePath + "/");

//         // only consider reference modifiers when the method/function actually has a body
//         boolean hasBody = bodyNode != null;
//         List<Parameter> cstParameters = extractParameters(paramsNode, sourceBytes, hasBody);
//         cstNode.setParameters(cstParameters);
        
//         StringBuilder localNameBuilder = new StringBuilder();
//         localNameBuilder.append(simpleName);
//         localNameBuilder.append("(");
//         List<String> paramNames = new ArrayList<>();
//         for (Parameter p : cstParameters) {
//           // Keep parameter names as provided (including leading $ and variadic '...' prefix)
//           paramNames.add(p.getName());
//         }
//         localNameBuilder.append(String.join(", ", paramNames));
//         localNameBuilder.append(")");
//         cstNode.setLocalName(localNameBuilder.toString());
        
//         // Determine the full node span. In some cases (assignment -> anonymous_function),
//         // the captured defNode may be the anonymous function itself or the assignment expression;
//         // prefer the outer node when available so the location covers the whole definition.
//         TSNode wholeDefNode = defNode;
//         String defType = defNode.getType();
//         if ("anonymous_function".equals(defType) || "arrow_function".equals(defType) || "closure".equals(defType)) {
//           // try to use parent if closure is assigned (assignment_expression captured as defNode in some patterns)
//           TSNode parent = defNode.getParent();
//           if (parent != null && !"program".equals(parent.getType())) {
//             wholeDefNode = parent;
//           }
//         }

//         int startByte = wholeDefNode.getStartByte();
//         int endByte = wholeDefNode.getEndByte();
//         int startLine = wholeDefNode.getStartPoint().getRow() + 1;
//         int endLine = wholeDefNode.getEndPoint().getRow() + 1;
//         int bodyStartByte = (bodyNode != null) ? bodyNode.getStartByte() : endByte;
//         int bodyEndByte = (bodyNode != null) ? bodyNode.getEndByte() : endByte;
//         cstNode.setLocation(new Location(filePath, startByte, endByte, startLine, endLine, bodyStartByte, bodyEndByte));

//         cstRoot.addNode(cstNode);
//       }
//     }
//   }

//   private List<Parameter> extractParameters(TSNode paramsNode, byte[] sourceBytes, boolean includeReference) {
//       List<Parameter> parameters = new ArrayList<>();
//       if (paramsNode == null) {
//           return parameters;
//       }

//       for (int i = 0; i < paramsNode.getNamedChildCount(); i++) {
//           TSNode paramNode = paramsNode.getNamedChild(i);
//           String type = paramNode.getType();
//           String prefix = "";
//           TSNode nameNode = null;

//           if ("simple_parameter".equals(type) || "variadic_parameter".equals(type) || "property_promotion_parameter".equals(type)) {
//               nameNode = paramNode.getChildByFieldName("name");
//               if (nameNode == null) {
//                   // fallback: try to find a descendant variable_name
//                   for (int j = 0; j < paramNode.getNamedChildCount() && nameNode == null; j++) {
//                       TSNode child = paramNode.getNamedChild(j);
//                       if ("variable_name".equals(child.getType())) {
//                           nameNode = child;
//                       }
//                   }
//               }

//               // Inspect raw text between parameter start and name start to detect '&' or '...'
//               if (nameNode != null) {
//                   int preStart = paramNode.getStartByte();
//                   int preEnd = nameNode.getStartByte();
//                   if (preEnd > preStart) {
//                       String preText = new String(sourceBytes, preStart, preEnd - preStart, StandardCharsets.UTF_8);
//                       if (preText.contains("...")) {
//                           prefix = "...";
//                       } else if (preText.contains("&") && includeReference) {
//                           prefix = "&";
//                       }
//                   }
//               }

//               // For variadic_parameter explicit type, ensure prefix is '...'
//               if ("variadic_parameter".equals(type)) {
//                   prefix = "...";
//               }
//           }

//           if (nameNode != null && "variable_name".equals(nameNode.getType())) {
//               String name = NodeUtils.getNodeText(nameNode, sourceBytes);
//               parameters.add(new Parameter(prefix + name));
//           }
//       }
//       return parameters;
//   }
 
// }
