package ru.qwert21.gradle;

import groovy.lang.Closure;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;

import java.io.File;
import java.util.Set;

public class ExtractTask extends AbstractTask implements PatternFilterable {
	@Input
	private Object inJar;

	@Input
	private PatternSet pattern = new PatternSet();

	@OutputDirectory
	private Object outDir;

	@TaskAction
	public void doTask() {
		Utils.deleteFile(getOutDir());

		getProject().zipTree(getInJar()).visit(new ExtractionVisitor());
	}

	public File getInJar() {
		return getProject().file(inJar);
	}

	public void from(Object inJar) {
		this.inJar = inJar;
	}

	public File getOutDir() {
		return getProject().file(outDir);
	}

	public void into(Object outDir) {
		this.outDir = outDir;
	}

	class ExtractionVisitor implements FileVisitor {
		private Spec<FileTreeElement> spec;
		private File outDir;

		ExtractionVisitor() {
			this.spec = pattern.getAsSpec();
			this.outDir = getOutDir();
		}

		@Override
		public void visitDir(FileVisitDetails details) {
			if (!spec.isSatisfiedBy(details)) return;

			File dir = new File(outDir, details.getPath());
			dir.mkdirs();
		}

		@Override
		public void visitFile(FileVisitDetails details) {
			if (!spec.isSatisfiedBy(details)) return;

			File out = new File(outDir, details.getPath());
			out.getParentFile().mkdirs();
			details.copyTo(out);
		}
	}

	@Override
	public PatternFilterable exclude(String... arg0) {
		return pattern.exclude(arg0);
	}

	@Override
	public PatternFilterable exclude(Iterable<String> arg0) {
		return pattern.exclude(arg0);
	}

	@Override
	public PatternFilterable exclude(Spec<FileTreeElement> arg0) {
		return pattern.exclude(arg0);
	}

	@Override
	public PatternFilterable exclude(Closure arg0) {
		return pattern.exclude(arg0);
	}

	@Override
	public Set<String> getExcludes() {
		return pattern.getExcludes();
	}

	@Override
	public Set<String> getIncludes() {
		return pattern.getIncludes();
	}

	@Override
	public PatternFilterable include(String... arg0) {
		return pattern.include(arg0);
	}

	@Override
	public PatternFilterable include(Iterable<String> arg0) {
		return pattern.include(arg0);
	}

	@Override
	public PatternFilterable include(Spec<FileTreeElement> arg0) {
		return pattern.include(arg0);
	}

	@Override
	public PatternFilterable include(Closure arg0) {
		return pattern.include(arg0);
	}

	@Override
	public PatternFilterable setExcludes(Iterable<String> arg0) {
		return pattern.setExcludes(arg0);
	}

	@Override
	public PatternFilterable setIncludes(Iterable<String> arg0) {
		return pattern.setIncludes(arg0);
	}
}
