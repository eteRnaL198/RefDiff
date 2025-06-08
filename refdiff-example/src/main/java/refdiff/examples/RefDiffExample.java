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

public class RefDiffExample {

	public static void main(String[] args) throws Exception {
		runExamples();
		runUniversalForRepo();
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
