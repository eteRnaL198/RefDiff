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
		runUniversalForRepo();
		runUniversalJavaOnLocal();
		runUniversalCOnLocal();
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

		// Now, we use the plugin for Universal.
		UniversalPlugin universalPlugin = new UniversalPlugin();
		RefDiff refDiffUniversal = new RefDiff(universalPlugin);
		printRefactorings(
				"Refactorings found in universal 72f61ec",
				refDiffUniversal.computeDiffForCommit(eclipseThemesRepo, "72f61ec"));
	}

	private static void printRefactorings(String headLine, CstDiff diff) {
		System.out.println(headLine);
		for (Relationship rel : diff.getRefactoringRelationships()) {
			System.out.println(rel.getStandardDescription());
		}
	}

	private static void runUniversalJavaOnLocal() throws Exception {
		System.out.println("\n\n----- Universal Plugin Java -----");
		UniversalPlugin universalPlugin = new UniversalPlugin();
		CstComparator comparator = new CstComparator(universalPlugin);
		String basePath = "example-for-universal/java";
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "moveClass/v0"), ".java");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "moveClass/v1"), ".java");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("Move Class:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "moveMethod/v0"), ".java");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "moveMethod/v1"), ".java");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nMove Method:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "moveAndRenameClass/v0"), ".java");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "moveAndRenameClass/v1"), ".java");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nMove and Rename Class:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "renameClass/v0"), ".java");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "renameClass/v1"), ".java");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nRename Class:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "renameMethod/v0"), ".java");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "renameMethod/v1"), ".java");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nRename Method:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "extractInterface/v0"), ".java");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "extractInterface/v1"), ".java");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nExtract Interface:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "extractSuperclass/v0"), ".java");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "extractSuperclass/v1"), ".java");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nExtract Superclass:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "pullUpMethod/v0"), ".java");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "pullUpMethod/v1"), ".java");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nPull Up Method:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "pushDownMethod/v0"), ".java");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "pushDownMethod/v1"), ".java");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nPush Down Method:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "extractMethod/v0"), ".java");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "extractMethod/v1"), ".java");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nExtract Method:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "extractAndMoveMethod/v0"), ".java");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "extractAndMoveMethod/v1"), ".java");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nExtract and Move Method:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "InlineMethod/v0"), ".java");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "InlineMethod/v1"), ".java");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nInline Method:", diff);
		}
	}

	private static void runUniversalCOnLocal() throws Exception {
		System.out.println("\n\n----- Universal Plugin C -----");
		UniversalPlugin universalPlugin = new UniversalPlugin();
		CstComparator comparator = new CstComparator(universalPlugin);
		String basePath = "example-for-universal/c";
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "changeSignature/v0"), ".c");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "changeSignature/v1"), ".c");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("Change Signature:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "moveFile/v0"), ".c");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "moveFile/v1"), ".c");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nMove File:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "moveFunction/v0"), ".c");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "moveFunction/v1"), ".c");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nMove Function:", diff);
		}		
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "renameFile/v0"), ".c");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "renameFile/v1"), ".c");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nRename File:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "renameFunction/v0"), ".c");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "renameFunction/v1"), ".c");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nRename Function:", diff);
		}		
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "moveAndRenameFile/v0"), ".c");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "moveAndRenameFile/v1"), ".c");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nMove and Rename File:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "moveAndRenameFunction/v0"), ".c");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "moveAndRenameFunction/v1"), ".c");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nMove and Rename Function:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "extractFunction/v0"), ".c");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "extractFunction/v1"), ".c");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nExtract Function:", diff);
		}
		{
			SourceFolder before = SourceFolder.from(Paths.get(basePath, "inlineFunction/v0"), ".c");
			SourceFolder after = SourceFolder.from(Paths.get(basePath, "inlineFunction/v1"), ".c");
			CstDiff diff = comparator.compare(before, after);
			printRefactorings("\nInline Function:", diff);
		}
	}

	private static void runUniversalForRepo() throws Exception {
		System.out.println("\n\n----- Universal Plugin Java on Repository -----");

		File tempFolder = new File("temp");

		UniversalPlugin universalPlugin = new UniversalPlugin();
		RefDiff refDiffUniversal = new RefDiff(universalPlugin);

		File seyrenRepo = refDiffUniversal.cloneGitRepository(
				new File(tempFolder, "seyren"),
				"https://github.com/icse18-refactorings/seyren.git");

		printRefactorings(
				"Refactorings found in seyren 5fb36a321af7df470d4c845cb18da8f85be31c38",
				refDiffUniversal.computeDiffForCommit(seyrenRepo, "5fb36a321af7df470d4c845cb18da8f85be31c38"));

		File clojureRepo = refDiffUniversal.cloneGitRepository(
				new File(tempFolder, "clojure"),
				"https://github.com/icse18-refactorings/clojure.git");

		printRefactorings(
				"\nRefactorings found in clojure 309c03055b06525c275b278542c881019424760e",
				refDiffUniversal.computeDiffForCommit(clojureRepo, "309c03055b06525c275b278542c881019424760e"));

		File crashRepo = refDiffUniversal.cloneGitRepository(
				new File(tempFolder, "crash"),
				"https://github.com/icse18-refactorings/crash.git");

		printRefactorings(
				"\nRefactorings found in crash 2801269c7e47bd6e243612654a74cee809d20959",
				refDiffUniversal.computeDiffForCommit(crashRepo, "2801269c7e47bd6e243612654a74cee809d20959"));

		File eurekaRepo = refDiffUniversal.cloneGitRepository(
				new File(tempFolder, "eureka"),
				"https://github.com/icse18-refactorings/eureka.git");

		printRefactorings(
				"\nRefactorings found in eureka 5103ace802b2819438318dd53b5b07512aae0d25",
				refDiffUniversal.computeDiffForCommit(eurekaRepo, "5103ace802b2819438318dd53b5b07512aae0d25"));

		File springDataRestRepo = refDiffUniversal.cloneGitRepository(
				new File(tempFolder, "spring-data-rest"),
				"https://github.com/icse18-refactorings/spring-data-rest.git");

		printRefactorings(
				"\nRefactorings found in spring-data-rest b7cba6a700d8c5e456cdeffe9c5bf54563eab7d3",
				refDiffUniversal.computeDiffForCommit(springDataRestRepo, "b7cba6a700d8c5e456cdeffe9c5bf54563eab7d3"));
	
	}
}
