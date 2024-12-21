// package refdiff.parsers.universal;

// import java.io.File;
// import java.nio.file.Path;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.List;
// import java.util.Optional;

// import refdiff.core.io.FilePathFilter;
// import refdiff.core.io.SourceFile;
// import refdiff.core.io.SourceFileSet;

// public class UniversalPlugin {
// 	private File tempDir = null;

//   public UniversalPlugin(File tempDir) {
//     this.tempDir = tempDir;
//   }

//   public void parse(SourceFileSet sources) throws Exception {
//     List<String> javaFiles = new ArrayList<>();
//     Optional<Path> optBasePath = sources.getBasePath();

//     // TODO optBasePathの存在確認

//     for (SourceFile sourceFile : sources.getSourceFiles()) {
//       javaFiles.add(sourceFile.getPath());
//     }
//     File rootFolder = optBasePath.get().toFile();

//     SDModelBuilder mb = new SDModelBuilder();
//     mb.analyze(rootFolder, javaFiles);

//   }


//   public FilePathFilter getAllowedFilesFilter() {
//     return new FilePathFilter(Arrays.asList(".java"));
//   }
// }
