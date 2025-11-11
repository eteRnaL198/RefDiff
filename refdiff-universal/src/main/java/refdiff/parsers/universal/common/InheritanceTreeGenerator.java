package refdiff.parsers.universal.common;

import org.treesitter.TSLanguage;
import org.treesitter.TSTree;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;

import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.CstNodeRelationship;
import refdiff.core.cst.CstNodeRelationshipType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InheritanceTreeGenerator {

    private final List<String> participatingNodeTypes;
    private final String inheritanceConstructQueryString;

    /**
     * Constructs an InheritanceTreeGenerator.
     * @param typeDefiningNodeTypes A list of CST node type strings that represent entities participating in inheritance (e.g., "CLASS_DECLARATION", "INTERFACE_DECLARATION").
     * @param inheritanceConstructQueryString The Tree-sitter query string to find nodes that define inheritance relationships (e.g., superclass clauses, implements clauses).
     */
    public InheritanceTreeGenerator(List<String> typeDefiningNodeTypes, String inheritanceConstructQueryString) {
        this.participatingNodeTypes = new ArrayList<>(typeDefiningNodeTypes); // Defensive copy
        this.inheritanceConstructQueryString = inheritanceConstructQueryString;
    }

    public void buildInheritanceTree(CstRoot root, Map<String, String> sourceCodeMap, Map<String, TSTree> parsedTreeMap, TSLanguage tsLang) {
        List<CstNode> participatingTypeNodes = new ArrayList<>();
        for (CstNode topLevelNode : root.getNodes()) {
            collectParticipatingTypeNodesRecursively(topLevelNode, participatingTypeNodes);
        }
        Map<String, CstNode> participatingTypeNodeMap = new HashMap<>();
        for (CstNode participatingTypeNode : participatingTypeNodes) {
            participatingTypeNodeMap.put(participatingTypeNode.getSimpleName(), participatingTypeNode); // TODO: Using simplename for the hashmap key, so duplicates can easily overwrite. Using fully qualified names or similar might be better.
        }

        for (Map.Entry<String, TSTree> treeEntry : parsedTreeMap.entrySet()) {
            TSTree tree = treeEntry.getValue();
            String filePath = treeEntry.getKey();
            String sourceCode = sourceCodeMap.get(filePath);
            byte[] sourceBytes = sourceCode.getBytes();
            addInheritanceRelationshipsForFile(root, participatingTypeNodeMap, tree, tsLang, sourceBytes);
        }
    }

    private void collectParticipatingTypeNodesRecursively(CstNode node, List<CstNode> collectedNodes) {
        String nodeType = node.getType();
        if (this.participatingNodeTypes.contains(nodeType)) {
            collectedNodes.add(node);
        }
        for (CstNode child : node.getNodes()) {
            collectParticipatingTypeNodesRecursively(child, collectedNodes);
        }
    }

    private void addInheritanceRelationshipsForFile(CstRoot root, Map<String, CstNode> participatingTypeNodeMap, TSTree tree, TSLanguage tsLang, byte[] sourceBytes) {
        TSQuery tsQuery = new TSQuery(tsLang, this.inheritanceConstructQueryString);
        TSQueryCursor cursor = new TSQueryCursor();
        TSNode rootNode = tree.getRootNode();
        cursor.exec(tsQuery, rootNode);
        TSQueryMatch match = new TSQueryMatch();

        while (cursor.nextMatch(match)) {
            for (TSQueryCapture capture : match.getCaptures()) {
                TSNode tsNode = capture.getNode();
                switch (tsNode.getType()) {
                    case "superclass": { // e.g., class A extends B
                        TSNode superclass = tsNode;
                        TSNode extendsToken = superclass.getChild(0);
                        TSNode superclassNameNode = superclass.getChild(1);
                        if (superclassNameNode.getChildCount() > 1) {
                            superclassNameNode = superclassNameNode.getChild(0); // In case of generic superclass, e.g., extends Base<T>
                        }
                        String superclassName = NodeUtils.getNodeText(superclassNameNode, sourceBytes).trim();
                        TSNode identifier = superclass.getParent().getChildByFieldName("name");
                        String className = NodeUtils.getNodeText(identifier, sourceBytes);
                        if (participatingTypeNodeMap.containsKey(className) && participatingTypeNodeMap.containsKey(superclassName)) {
                            root.getRelationships().add(new CstNodeRelationship(CstNodeRelationshipType.SUBTYPE, participatingTypeNodeMap.get(className).getId(), participatingTypeNodeMap.get(superclassName).getId()));
                        }
                        break;
                    }
                    case "super_interfaces":   // e.g., class A implements B, C
                    case "extends_interfaces": { // e.g., interface A extends B, C
                        TSNode interfacesNode = tsNode;
                        TSNode subTypeNameNode = interfacesNode.getParent().getChildByFieldName("name");
                        String subTypeName = NodeUtils.getNodeText(subTypeNameNode, sourceBytes);
                        TSNode implementsOrExtends = interfacesNode.getChild(0); // e.g., 'implements' or 'extends' token
                        List<String> superTypeNames = java.util.Arrays.stream(
                            NodeUtils.getNodeText(implementsOrExtends, sourceBytes)
                            .split(",")) // Split by commas
                            .map(String::trim)
                            .map(s -> s.split("<")[0]) // Remove generic type parameters if any, e.g., B<T> -> B
                            .toList();
                        for (String name : superTypeNames) {
                            if (participatingTypeNodeMap.containsKey(subTypeName) && participatingTypeNodeMap.containsKey(name)) {
                                root.getRelationships().add(new CstNodeRelationship(CstNodeRelationshipType.SUBTYPE, participatingTypeNodeMap.get(subTypeName).getId(), participatingTypeNodeMap.get(name).getId()));
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
}