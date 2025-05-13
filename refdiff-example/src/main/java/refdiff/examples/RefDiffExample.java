package refdiff.examples;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import refdiff.core.RefDiff;
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
import refdiff.parsers.c.CPlugin;
import refdiff.parsers.java.JavaPlugin;
import refdiff.parsers.universal.UniversalPlugin;
import refdiff.parsers.universal.UniversalParser;

public class RefDiffExample {

	public static void main(String[] args) throws Exception {
		runExamples();
	}

	private static void runExamples() throws Exception {
		// This is a temp folder to clone or checkout git repositories.
		File tempFolder = new File("temp");

		// In this example, we use the plugin for C.
		CPlugin cPlugin = new CPlugin();
		RefDiff refDiffC = new RefDiff(cPlugin);

		File gitRepo = refDiffC.cloneGitRepository(
				new File(tempFolder, "git"),
				"https://github.com/refdiff-study/git.git");

		printRefactorings(
				"Refactorings found in git ba97aea",
				refDiffC.computeDiffForCommit(gitRepo, "ba97aea1659e249a3a58ecc5f583ee2056a90ad8"));

		// Now, we use the plugin for Java.
		JavaPlugin javaPlugin = new JavaPlugin(tempFolder);
		RefDiff refDiffJava = new RefDiff(javaPlugin);

		File eclipseThemesRepo = refDiffJava.cloneGitRepository(
				new File(tempFolder, "eclipse-themes"),
				"https://github.com/icse18-refactorings/eclipse-themes.git");

		printRefactorings(
				"Refactorings found in eclipse-themes 72f61ec",
				refDiffJava.computeDiffForCommit(eclipseThemesRepo, "72f61ec"));

		// Now, we use the plugin for universal.
		System.out.println("\n\n----- Universal Plugin Java -----");
		UniversalPlugin universalPlugin = new UniversalPlugin();
		CstComparator comparator = new CstComparator(universalPlugin);
		String basePath = "example-for-universal/java";

		SourceFolder before = SourceFolder.from(Paths.get(basePath, "renameMethod/v0"), ".java");
		SourceFolder after = SourceFolder.from(Paths.get(basePath, "renameMethod/v1"), ".java");
		CstDiff diff = comparator.compare(before, after);
		printRefactorings("rename method:", diff);

		before = SourceFolder.from(Paths.get(basePath, "moveMethod/v0"), ".java");
		after = SourceFolder.from(Paths.get(basePath, "moveMethod/v1"), ".java");
		diff = comparator.compare(before, after);
		printRefactorings("\nmove method:", diff);

		before = SourceFolder.from(Paths.get(basePath, "moveClass/v0"), ".java");
		after = SourceFolder.from(Paths.get(basePath, "moveClass/v1"), ".java");
		diff = comparator.compare(before, after);
		printRefactorings("\nmove class:", diff);

		before = SourceFolder.from(Paths.get(basePath, "extractMethod/v0"), ".java");
		after = SourceFolder.from(Paths.get(basePath, "extractMethod/v1"), ".java");
		diff = comparator.compare(before, after);
		printRefactorings("\nextract method:", diff);

		before = SourceFolder.from(Paths.get(basePath, "extractAndMoveMethod/v0"), ".java");
		after = SourceFolder.from(Paths.get(basePath, "extractAndMoveMethod/v1"), ".java");
		diff = comparator.compare(before, after);
		printRefactorings("\nextract and move method:", diff);

		before = SourceFolder.from(Paths.get(basePath, "pullUpMethod/v0"), ".java");
		after = SourceFolder.from(Paths.get(basePath, "pullUpMethod/v1"), ".java");
		diff = comparator.compare(before, after);
		printRefactorings("\npull up method:", diff);

		System.out.println("\n\n----- Universal Plugin C -----");
		basePath = "example-for-universal/c";
		before = SourceFolder.from(Paths.get(basePath, "renameFunction/v0"), ".c");
		after = SourceFolder.from(Paths.get(basePath, "renameFunction/v1"), ".c");
		diff = comparator.compare(before, after);
		printRefactorings("rename function:", diff);
	}

	private static void printRefactorings(String headLine, CstDiff diff) {
		System.out.println(headLine);
		for (Relationship rel : diff.getRefactoringRelationships()) {
			System.out.println(rel.getStandardDescription());
		}
	}

}
