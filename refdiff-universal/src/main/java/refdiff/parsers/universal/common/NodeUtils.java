package refdiff.parsers.universal.common;

import org.treesitter.TSNode;
import java.nio.charset.StandardCharsets;

public class NodeUtils {
    public static String getNodeText(TSNode node, byte[] sourceBytes) {
        if (node == null || node.isNull()) {
            return "";
        }
        return new String(sourceBytes, node.getStartByte(), node.getEndByte() - node.getStartByte(), StandardCharsets.UTF_8);
    }
}