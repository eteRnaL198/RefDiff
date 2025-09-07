package pluginmaker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.lib.Repository;
import org.treesitter.*;
import java.util.function.Supplier;

import refdiff.core.io.GitHelper;


public class PluginMaker {
  private enum Language {
    PYTHON("python", ".py", "--kinds-Python=cfm", () -> new TreeSitterPython()),
    GO("go", ".go", "--kinds-Go=f", () -> new TreeSitterGo()),
    PHP("php", ".php", "--php-kinds=f", () -> new TreeSitterPhp()),
    JAVA("java", ".java", "--kinds-Java=pigacm", () -> new TreeSitterJava());

    private final String name;
    private final String extension;
    private final String ctagsOption;
    private final Supplier<TSLanguage> tsSupplier;

    Language(String name, String extension, String ctagsOption, Supplier<TSLanguage> tsSupplier) {
      this.name = name;
      this.extension = extension;
      this.ctagsOption = ctagsOption;
      this.tsSupplier = tsSupplier;
    }

    String getName() { return name; }
    String getExtension() { return extension; }
    String getCtagsOption() { return ctagsOption; }
    TSLanguage getTSLanguage() { return tsSupplier.get(); }

    static Language fromName(String n) {
      for (Language l : values()) {
        if (l.name.equalsIgnoreCase(n)) return l;
      }
      throw new IllegalArgumentException("Unknown language: " + n);
    }
  }

  public static void main(String[] args) throws Exception {
    PluginMaker pluginMaker = new PluginMaker();
    System.out.println(Arrays.toString(args));
    String langArg = null;
    if (args != null && args.length > 0 && args[0] != null && !args[0].isEmpty()) {
      langArg = args[0];
    }

    if (langArg == null) {
      throw new IllegalArgumentException("Language argument is required");
    }
    Language language = Language.fromName(langArg);

    Path srcDir = Paths.get("context/" + language.getName() + "/src/");
    List<Path> sourceFiles = Files.list(srcDir)
        .filter(Files::isRegularFile)
        .filter(p -> p.toString().endsWith(language.getExtension()))
        .collect(Collectors.toList());
    
    // List<Path> sourceFiles;
    // if (Files.exists(srcDir)) {
    //   try (Stream<Path> stream = Files.list(srcDir)) {
    //     sourceFiles = stream
    //     .filter(Files::isRegularFile)
    //     .filter(p -> p.toString().endsWith(LANGUAGE_EXTENSION))
    //     .collect(Collectors.toList());
    //   }
    // } else {
    //   sourceFiles = pluginMaker.getRandomSourceFiles(repoDir.toPath(), 5, LANGUAGE_EXTENSION);
    //   Files.createDirectories(srcDir);
    //   for (Path sourceFile : sourceFiles) {
    //     Path destinationFile = srcDir.resolve(sourceFile.getFileName());
    //     Files.copy(sourceFile, destinationFile);
    //   }
    // }
    
    Path tagsDir = Paths.get("context/" + language.getName() + "/tags/");
    if (!Files.exists(tagsDir)) {
      Files.createDirectories(tagsDir);
    }
    for (Path sourceFile : sourceFiles) {
      String sourceFileName = sourceFile.getFileName().toString();
      String baseName = sourceFileName.substring(0, sourceFileName.lastIndexOf('.'));
      Path ctagsOutputFile = tagsDir.resolve("tags-" + baseName + ".txt");

    pluginMaker.executeCommand(
      srcDir.toFile(),
      "ctags",
      "--pseudo-tags",
      "--sort=no",
      "-o",
      ctagsOutputFile.toAbsolutePath().toString(),
      "--fields=+n",
      language.getCtagsOption(),
      sourceFileName);
    }

    Path astDir = Paths.get("context/" + language.getName() + "/ast/");
    if (!Files.exists(astDir)) {
      Files.createDirectories(astDir);
    }

    for (Path sourceFile : sourceFiles) {
      String astContent = pluginMaker.parse(sourceFile, language.getTSLanguage());
      String sourceFileName = sourceFile.getFileName().toString();
      String baseName = sourceFileName.substring(0, sourceFileName.lastIndexOf('.'));
      Path astOutputFile = astDir.resolve("ast-" + baseName + ".txt");
      Files.writeString(astOutputFile, astContent);
    }

  }

  private String parse(Path sourceFile, TSLanguage tsLang) throws Exception {
    TSParser parser = new TSParser();
    parser.setLanguage(tsLang);

    String sourceCode = Files.readString(sourceFile);
    TSTree tree = parser.parseString(null, sourceCode);
    String query = "_ @node";
    TSQuery tsQuery = new TSQuery(tsLang, query);
    TSNode rootNode = tree.getRootNode();
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(tsQuery, rootNode);
    TSQueryMatch match = new TSQueryMatch();
    StringBuilder astContent = new StringBuilder();
    while (cursor.nextMatch(match)) {
      TSQueryCapture[] captures = match.getCaptures();
      for (TSQueryCapture capture : captures) {
        TSNode node = capture.getNode();
        astContent.append(node.toString()).append(" L:").append(node.getStartPoint().getRow() + 1).append("\n");
      }
    }
    return astContent.toString();
  }

  /**
   * 指定されたディレクトリパスから、特定の拡張子を持つソースファイルをランダムに指定された数だけ抽出します。
   *
   * @param directoryPath 検索対象のディレクトリのパス
   * @param count 抽出するファイルの数
   * @param fileExtension 検索対象のファイルの拡張子 (例: ".java", ".py")
   * @return ランダムに選択されたソースファイルのパスのリスト
   * @throws IOException ファイルの探索中にI/Oエラーが発生した場合
   */
  private List<Path> getRandomSourceFiles(Path directoryPath, int count, String fileExtension) throws IOException {
    List<Path> sourceFiles;
    try (Stream<Path> walk = Files.walk(directoryPath)) {
      sourceFiles = walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(fileExtension))
          .collect(Collectors.toList());
    }

    if (sourceFiles.isEmpty()) {
      System.out.println("No '" + fileExtension + "' files found in " + directoryPath);
      return Collections.emptyList();
    }

    Collections.shuffle(sourceFiles);

    int limit = Math.min(count, sourceFiles.size());
    return sourceFiles.subList(0, limit);
  }

  /**
   * 指定されたURLからGitリポジトリをクローンし、指定されたパスに保存します。<br>
   * <b>注意:</b> このメソッドは通常の（ベアではない）リポジトリをクローンします。<br>
   * 既にディレクトリが存在する場合は、クローンせずに既存のリポジトリを開こうとします。
   *
   * @param url クローンするGitリポジトリのURL
   * @param destinationPath リポジトリをクローンするローカルディレクトリのパス
   * @return クローンされた、または既存のリポジトリの作業ディレクトリを表すFileオブジェクト
   * @throws Exception クローン処理またはリポジトリを開く際にエラーが発生した場合
   */
  private File cloneRepository(String url, String destinationPath) throws Exception {
    System.out.println("Cloning " + url + " into " + destinationPath + " if it does not exist...");
    Repository repository = GitHelper.cloneIfNotExists(destinationPath, url);
    return repository.getWorkTree();
  }

  /**
   * コマンドラインでコマンドを実行します。
   *
   * @param command 実行するコマンドと引数.
   * @throws IOException プロセスの開始に失敗した場合
   * @throws InterruptedException プロセスが中断された場合
   * @throws RuntimeException コマンドが0以外の終了コードで終了した場合
   */
  private void executeCommand(File workingDir, String... command) throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.directory(workingDir);
    processBuilder.inheritIO(); // 標準入出力をこのプロセスにリダイレクト
    Process process = processBuilder.start();
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException("Command execution failed with exit code " + exitCode);
    }
  }
}
