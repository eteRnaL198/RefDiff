package refdiff.parsers.universal.common;

import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.CstNodeRelationship;
import refdiff.core.cst.CstNodeRelationshipType;
import refdiff.core.diff.CstRootHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CallGraphGenerator {

    private final Set<String> callableNodeTypes;

    public CallGraphGenerator(String... callableNodeTypes) {
        this.callableNodeTypes = new HashSet<>(Arrays.asList(callableNodeTypes));
    }

    /**
     * Generates call relationships (e.g., method calls) between callable nodes within the provided CST root.
     * It first identifies all nodes of the specified {@code callableNodeTypes} (e.g., method declarations)
     * and then analyzes the tokens within each callable node to find references to other callable nodes.
     * <p>
     * Note: This implementation assumes that the CST root contains tokens for each node,
     * typically populated by {@link refdiff.core.diff.CstRootHelper#retrieveTokens(CstRoot, String, CstNode, boolean)}.
     * @param root The CstRoot containing the nodes and to which relationships will be added.
     * @param sourceCodeMap A map where keys are file paths and values are the source code content of those files.
     */
    public void generateCallGraph(CstRoot root, Map<String, String> sourceCodeMap) {
        List<CstNode> callableNodes = new ArrayList<>();
        root.forEachNode((node, _) -> {
            if (this.callableNodeTypes.contains(node.getType())) {
                callableNodes.add(node);
            }
        });

        Map<String, List<CstNode>> calleeCandidatesMap = new HashMap<>(); // メソッドがオーバーロードされている場合、同名メソッドが複数存在するためValueはListにしている
        for (CstNode callableNode : callableNodes) {
            calleeCandidatesMap.computeIfAbsent(callableNode.getSimpleName(), _ -> new ArrayList<>()).add(callableNode);
        }
        for (CstNode callerNode : callableNodes) {
            addCallRelationship(callerNode, root, sourceCodeMap, calleeCandidatesMap);
        }
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