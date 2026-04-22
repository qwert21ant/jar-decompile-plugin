package ru.qwert21.gradle;

import groovy.lang.Closure;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.DefaultTask;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class FilterTask extends DefaultTask implements PatternFilterable
{
	@InputFile
	private Object inJar;

	@Input
	private PatternSet pattern = new PatternSet();

	@OutputFile
	private Object outJar;

	@TaskAction
	public void doTask() throws IOException
	{
		// get the spec
		final Spec<FileTreeElement> spec = pattern.getAsSpec();

		File input = getInJar();

		File out = getOutJar();

		out.getParentFile().mkdirs();

		// begin reading jar
		final JarOutputStream zout = new JarOutputStream(new FileOutputStream(out));

		getProject().zipTree(input).visit(new FileVisitor() {

			@Override
			public void visitDir(FileVisitDetails details)
			{
				// ignore directories
			}

			@Override
			public void visitFile(FileVisitDetails details)
			{
				JarEntry entry = new JarEntry(details.getPath());
				entry.setSize(details.getSize());
				entry.setTime(details.getLastModified());

				try
				{
					if (spec.isSatisfiedBy(details))
					{
						zout.putNextEntry(entry);
						details.copyTo(zout);
						zout.closeEntry();
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});

		zout.close();
	}

	public File getInJar() {
		return getProject().file(inJar);
	}

	public void setInJar(Object inJar) {
		this.inJar = inJar;
	}

	public File getOutJar() {
		return getProject().file(outJar);
	}

	public void setOutJar(Object outJar) {
		this.outJar = outJar;
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
