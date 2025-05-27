package refdiff.parsers.universal.common;

import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.CstNodeRelationship;
import refdiff.core.cst.CstNodeRelationshipType;
import refdiff.core.diff.CstRootHelper;
import refdiff.core.io.SourceFile;
import refdiff.core.io.SourceFileSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CallGraphGenerator {

    private final String callableNodeType;

    public CallGraphGenerator(String callableNodeType) {
        this.callableNodeType = callableNodeType;
    }

    public void generateCallGraph(CstRoot root, SourceFileSet folder, List<SourceFile> files) {
        List<CstNode> allCallableNodes = new ArrayList<>();
        // Iterate over top-level nodes (e.g., classes/interfaces) and collect all callable methods.
        for (CstNode topLevelNode : root.getNodes()) {
            allCallableNodes.addAll(collectCallableNodesRecursively(topLevelNode));
        }

        Map<String, CstNode> callableNodeMap = new HashMap<>();
        for (CstNode callableNode : allCallableNodes) {
            // TODO: simplenameをhashmapに持たせてるので重複しやすく上書きされる。namespaceなどを使うといいかも
            callableNodeMap.put(callableNode.getSimpleName(), callableNode);
        }

        for (CstNode callerNode : allCallableNodes) {
            addCallRelationship(callerNode, root, folder, files, callableNodeMap);
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

    private void addCallRelationship(CstNode callerNode, CstRoot root, SourceFileSet folder, List<SourceFile> files, Map<String, CstNode> callableNodeMap) {
        String path = callerNode.getLocation().getFile();
        String sourceCode = "";
        try {
            Optional<SourceFile> sourceFileOptional = files.stream().filter(f -> f.getPath().equals(path)).findFirst();
            if (sourceFileOptional.isPresent()) {
                sourceCode = folder.readContent(sourceFileOptional.get());
            } else {
                System.err.println("Warning: Source file not found for path: " + path +
                                   " when processing CstNode: " + callerNode.getSimpleName() +
                                   " (id: " + callerNode.getId() + "). No call relationships will be added for this node.");
                // Original code would proceed with empty sourceCode, leading to no tokens found.
            }
        } catch (IOException e) {
            System.err.println("Warning: IOException while reading source file: " + path +
                               " for CstNode: " + callerNode.getSimpleName() +
                               " (id: " + callerNode.getId() + "). No call relationships will be added for this node.");
            e.printStackTrace(); // Maintain original behavior of printing stack trace.
            // Original code would proceed with empty sourceCode.
        }

        List<String> tokens = CstRootHelper.retrieveTokens(root, sourceCode, callerNode, true);
        for (String token : tokens) {
            if (callableNodeMap.containsKey(token)) {
                CstNode callee = callableNodeMap.get(token);
                root.getRelationships().add(new CstNodeRelationship(CstNodeRelationshipType.USE, callerNode.getId(), callee.getId()));
            }
        }
    }
}