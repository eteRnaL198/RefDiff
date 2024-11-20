package refdiff.parsers.universal;


import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;

import io.github.treesitter.jtreesitter.InputEncoding;
import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Parser;
import io.github.treesitter.jtreesitter.Tree;
import io.github.treesitter.jtreesitter.Node;


public class UniversalParser {
  public static void main(String[] args) {
    // String libraryPath = "/Users/ikuya/Documents/TokyoTech/修論研究/Sandbox/tree-sitter-java/libtree-sitter-java.dylib";
    // SymbolLookup symbols = SymbolLookup.libraryLookup(libraryPath, Arena.global());

    System.out.println(System.getProperty("java.library.path"));

    String library = System.mapLibraryName("tree-sitter-java");
    SymbolLookup symbols = SymbolLookup.libraryLookup(library, Arena.global());
    Language language = Language.load(symbols, "tree_sitter_java");

    try (Parser parser = new Parser(language)) {
      String sourceCode = "public class Main { public static void main(String[] args) {} }";
      try (Tree tree = parser.parse(sourceCode, InputEncoding.UTF_8).orElseThrow()) {
        Node rootNode = tree.getRootNode();
        assert rootNode.getType().equals("program");
        assert rootNode.getStartPoint().column() == 0;
        assert rootNode.getEndPoint().column() == 14;
      }
    }
  }
}