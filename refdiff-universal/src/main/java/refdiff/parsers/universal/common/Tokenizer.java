package refdiff.parsers.universal.common;

import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
import org.treesitter.TSTree;
import refdiff.core.cst.TokenPosition;
import refdiff.core.cst.TokenizedSource;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {

    public static TokenizedSource tokenize(TSTree tree, TSLanguage tsLang, String path) {
        String query = "_ @node";
        TSQuery tsQuery = new TSQuery(tsLang, query);
        TSQueryCursor cursor = new TSQueryCursor();
        TSNode rootNode = tree.getRootNode();
        cursor.exec(tsQuery, rootNode);
        TSQueryMatch match = new TSQueryMatch();

        List<TokenPosition> tokens = new ArrayList<>();
        while (cursor.nextMatch(match)) {
            TSQueryCapture[] captures = match.getCaptures();
            for (TSQueryCapture capture : captures) {
                TSNode node = capture.getNode();
                if (node.getChildCount() == 0) { // Leaf node
                    int start = node.getStartByte();
                    int end = node.getEndByte();
                    tokens.add(new TokenPosition(start, end));
                }
            }
        }
        return new TokenizedSource(path, tokens);
    }
}