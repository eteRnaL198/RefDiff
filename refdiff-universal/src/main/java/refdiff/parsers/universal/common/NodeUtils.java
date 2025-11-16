package refdiff.parsers.universal.common;

import org.treesitter.TSNode;

import refdiff.core.cst.Location;

import java.nio.charset.StandardCharsets;

public class NodeUtils {
    public static String getNodeText(TSNode node, byte[] sourceBytes) {
        if (node == null || node.isNull()) {
            return "";
        }
        return new String(sourceBytes, node.getStartByte(), node.getEndByte() - node.getStartByte(), StandardCharsets.UTF_8);
    }
    
    public static Location generateLocation(TSNode decl, TSNode body, String path) {
        return new Location(
            path,
            decl.getStartByte(),
            decl.getEndByte(),
            decl.getStartPoint().getRow() + 1,
            decl.getStartPoint().getColumn() + 1,
            body != null ? body.getStartByte() : decl.getStartByte(),
            body != null ? body.getEndByte() : decl.getEndByte()
        );
    }
}