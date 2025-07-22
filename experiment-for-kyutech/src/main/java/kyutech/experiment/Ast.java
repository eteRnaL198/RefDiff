package kyutech.experiment;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterRuby;

import refdiff.core.RefDiff;
import refdiff.core.cst.CstRoot;
import refdiff.core.diff.CstComparator;
import refdiff.core.diff.CstComparatorMonitor;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.Relationship;
import refdiff.core.diff.CstComparator.DiffBuilder;
import refdiff.core.diff.similarity.TfIdfSourceRepresentation;
import refdiff.core.diff.similarity.TfIdfSourceRepresentationBuilder;
import refdiff.core.io.SourceFolder;
import refdiff.core.io.SourceFile;
import refdiff.core.io.SourceFileSet;
import refdiff.parsers.java.JavaPlugin;
import refdiff.parsers.universal.UniversalPlugin;

public class Ast {
  public static void main(String[] args) throws Exception {
    new Ast().parse();
  }

  private void parse() throws Exception {
    TSParser parser = new TSParser();
    TSLanguage tsLang = new TreeSitterRuby();
    parser.setLanguage(tsLang);
    
		// Path basePath = Paths.get("repository/ollama/");
		// SourceFolder sources = SourceFolder.from(basePath, Paths.get("runner/ollamarunner/runner.go"));
    // SourceFolder sources = SourceFolder.from(basePath, Paths.get("fs/gguf/keyvalue.go"));
    // SourceFolder sources = SourceFolder.from(basePath, Paths.get("app/lifecycle/updater_windows.go"));
    
    // Path basePath = Paths.get("ast/js/");
    // SourceFolder sources = SourceFolder.from(basePath, Paths.get("file.js"));
    // SourceFolder sources = SourceFolder.from(basePath, Paths.get("class.js"));
    // SourceFolder sources = SourceFolder.from(basePath, Paths.get("function.js"));
    
    Path basePath = Paths.get("ast/ruby/");
    SourceFolder sources = SourceFolder.from(basePath, Paths.get("method.rb"));
    
    String sourceCode = sources.readContent(sources.getSourceFiles().get(0));
    TSTree tree = parser.parseString(null, sourceCode);
    String query = "_ @node";
    TSQuery tsQuery = new TSQuery(tsLang, query);
    TSNode rootNode = tree.getRootNode();
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(tsQuery, rootNode);
    TSQueryMatch match = new TSQueryMatch();
    while (cursor.nextMatch(match)) {
      TSQueryCapture[] captures = match.getCaptures();
      for (TSQueryCapture capture : captures) {
        TSNode node = capture.getNode();
        System.out.println(node.toString() + " L:" + node.getStartPoint().getRow());

      }
    }
  }
}
