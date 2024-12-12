package refdiff.parsers.universal;

import org.treesitter.TSLanguage;
import org.treesitter.TSParser;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJava;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
import org.treesitter.TSRange;


public class UniversalParser {
  public static void main(String[] args) {
        TSParser parser = new TSParser();
        TSLanguage java = new TreeSitterJava();

        parser.setLanguage(java);
        String sourceCode = "public class Main { public static void main(String[] args) { String str = \"Hello World\"; System.out.println(str);  } }";
        TSTree tree = parser.parseString(null, sourceCode);
        
        TSNode rootNode = tree.getRootNode();
        System.out.println(rootNode.toString());
        System.out.println(rootNode.getNamedChild(0).getNamedChild(0));

        String query = "(method_declaration name: (identifier) @method)";
        TSQuery tsQuery = new TSQuery(java, query);
        TSQueryCursor cursor = new TSQueryCursor();
        cursor.exec(tsQuery, rootNode);
        TSQueryMatch match = new TSQueryMatch();
        while (cursor.nextMatch(match)) {
          // System.out.println(match.toString());
        }

        String newSourceCode = "public class Main { public static void main(String[] args) { System.out.println(\"Hello, World!\"); System.out.println(\"Hello, World!\"); } }";
        TSTree newTree = parser.parseString(null, newSourceCode);
        TSRange[] ranges = TSTree.getChangedRanges(tree, newTree);
        for (TSRange range : ranges) {
          System.out.println(range.getStartPoint().getColumn() + " " + range.getEndPoint().getColumn());
          System.out.println(newSourceCode.substring(range.getStartPoint().getColumn(), range.getEndPoint().getColumn()));
        }

  }
}