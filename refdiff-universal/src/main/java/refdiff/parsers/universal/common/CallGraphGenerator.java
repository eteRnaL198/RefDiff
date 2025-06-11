package refdiff.parsers.universal.common;

import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.CstNodeRelationship;
import refdiff.core.cst.CstNodeRelationshipType;
import refdiff.core.diff.CstRootHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallGraphGenerator {

    private final String callableNodeType;

    public CallGraphGenerator(String callableNodeType) {
        this.callableNodeType = callableNodeType;
    }

    /**
     * Generates call relationships (e.g., method calls) between callable nodes within the provided CST root.
     * It first identifies all nodes of the specified {@code callableNodeType} (e.g., method declarations)
     * and then analyzes the tokens within each callable node to find references to other callable nodes.
     * <p>
     * Note: This implementation assumes that the CST root contains tokens for each node,
     * typically populated by {@link refdiff.core.diff.CstRootHelper#retrieveTokens(CstRoot, String, CstNode, boolean)}.
     * @param root The CstRoot containing the nodes and to which relationships will be added.
     * @param sourceCodeMap A map where keys are file paths and values are the source code content of those files.
     */
    public void generateCallGraph(CstRoot root, Map<String, String> sourceCodeMap) {
        List<CstNode> callableNodes = new ArrayList<>();
        for (CstNode topLevelNode : root.getNodes()) {
            callableNodes.addAll(collectCallableNodesRecursively(topLevelNode));
        }

        Map<String, List<CstNode>> calleeCandidatesMap = new HashMap<>(); // メソッドがオーバーロードされている場合、同名メソッドが複数存在するためValueはListにしている
        for (CstNode callableNode : callableNodes) {
            calleeCandidatesMap.computeIfAbsent(callableNode.getSimpleName(), _ -> new ArrayList<>()).add(callableNode);
        }
        for (CstNode callerNode : callableNodes) {
            addCallRelationship(callerNode, root, sourceCodeMap, calleeCandidatesMap);
        }
    }

    /**
     * Recursively collects all callable nodes (method declarations) starting from the given CstNode.
     * @param node The CstNode to start collection from.
     * @return A list of CstNodes representing method declarations.
     */
    private List<CstNode> collectCallableNodesRecursively(CstNode node) {
        // TODO: CstRoot.forEachNode()で取れるかも
        List<CstNode> collectedNodes = new ArrayList<>();
        for (CstNode child : node.getNodes()) {
            collectedNodes.addAll(collectCallableNodesRecursively(child));
        }
        // Check the current node after processing children.
        if (node.getType().equals(this.callableNodeType)) {
            collectedNodes.add(node);
        }
        return collectedNodes;
    }

    private void addCallRelationship(CstNode callerNode, CstRoot root, Map<String, String> sourceCodeMap, Map<String, List<CstNode>> calleeCandidatesMap) {
        String path = callerNode.getLocation().getFile();
        String sourceCode = sourceCodeMap.get(path);

        if (sourceCode == null) {
            // This should ideally not happen if SourceFileReader.readAllSourceFiles ensures all files are read
            // or throws an exception. This warning is a safeguard.
            System.err.println("Warning: Source code not found in map for path: " + path +
                               " for CstNode: " + callerNode.getSimpleName() +
                               " (id: " + callerNode.getId() + "). No call relationships will be added for this node.");
            sourceCode = ""; // Use empty string to prevent NullPointerException later
        }
        
        List<String> tokens = CstRootHelper.retrieveTokens(root, sourceCode, callerNode, true);
        for (String token : tokens) {
            if (calleeCandidatesMap.containsKey(token)) {
                for (CstNode callee : calleeCandidatesMap.get(token)) {
                     root.getRelationships().add(new CstNodeRelationship(CstNodeRelationshipType.USE, callerNode.getId(), callee.getId()));
                }
            }
        }
    }
}