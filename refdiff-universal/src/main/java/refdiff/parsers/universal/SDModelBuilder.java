package refdiff.parsers.universal;

import java.io.BufferedReader;
import java.util.HashMap;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AST;

import refdiff.core.cst.CstNode;

public class SDModelBuilder {
  	private static final String systemFileSeparator = Matcher.quoteReplacement(File.separator);

  	private Map<CstNode, List<String>> postProcessReferences;
    private Map<CstNode, List<String>> postProcessSupertypes;

  public void analyze(File rootDir, List<String> javaFiles) {
    postProcessReferences = new HashMap<CstNode, List<String>>();
		postProcessSupertypes = new HashMap<CstNode, List<String>>();
    final String projectRoot = rootDir.getPath();
    final String[] emptyArray = new String[0];

    String encoding = StandardCharsets.UTF_8.name();
    String[] filesArray = new String[javaFiles.size()];
    String[] encodings = new String[javaFiles.size()];
    for (int i = 0; i < javaFiles.size(); i++) {
      filesArray[i] = rootDir + File.separator + javaFiles.get(i).replaceAll("/", systemFileSeparator);
      encodings[i] = encoding;
    }
    final String[] sourceFolders = this.inferSourceFolders(filesArray);
    // final ASTParser parser = buildAstParser(sourceFolders);
  }

  // private static ASTParser buildAstParser(String[] sourceFolders) { // TODO implement buildAstParser
  //   @SuppressWarnings("deprecation")
  //   ASTParser parser = ASTParser.newParser(AST.JLS8);
  //   parser.setKind(0);
  // }

  private String[] inferSourceFolders(String[] filesArray) {
    Set<String> sourceFolders = new TreeSet<String>();
    nextFile:
    for (String file: filesArray)  {
      for (String sourceFolder : sourceFolders) {
        if (file.startsWith(sourceFolder)) {
          continue nextFile;
        }
      }
      String otherSourceFolder = extractSourceFolderFromPath(file);
      if (otherSourceFolder != null) {
        sourceFolders.add(otherSourceFolder);
      }
    }
    return sourceFolders.toArray(new String[sourceFolders.size()]);
  }

  private String extractSourceFolderFromPath(String sourceFilePath) {
    try (BufferedReader scanner = new BufferedReader(new FileReader(sourceFilePath))) {
      String line;
      while ((line = scanner.readLine()) != null) {
        if (!line.startsWith("package ")){
          continue;
        }
        String packageName = line.substring(8, line.indexOf(';'));
        String packagePath = packageName.replace('.', File.separator.charAt(0));
        int indexOfPackagePath = sourceFilePath.lastIndexOf(packagePath + File.separator);
        if (indexOfPackagePath >= 0) {
          return sourceFilePath.substring(0, indexOfPackagePath -1);
        }
        return null;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
