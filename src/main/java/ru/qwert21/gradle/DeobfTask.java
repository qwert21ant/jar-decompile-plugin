package ru.qwert21.gradle;

import net.md_5.specialsource.Jar;
import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.RemapperProcessor;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

public class DeobfTask extends AbstractTask {
	@InputFile
	private Object inJar;

	@InputFile
	private Object srg;

	@OutputFile
	private Object outJar;

	@TaskAction
	public void doTask() throws IOException {
		File inJar = getInJar();
		File srg = getSrg();
		File outJar = getOutJar();

		outJar.getParentFile().mkdirs();

		JarMapping mapping = new JarMapping();
		mapping.loadMappings(srg);

		JarRemapper remapper = new JarRemapper(mapping);

		Jar jar = Jar.init(inJar);

		remapper.remapJar(jar, outJar);
	}

	public File getInJar() {
		return getProject().file(inJar);
	}

	public void setInJar(Object inJar) {
		this.inJar = inJar;
	}

	public File getSrg() {
		return getProject().file(srg);
	}

	public void setSrg(Object srg) {
		this.srg = srg;
	}

	public File getOutJar() {
		return getProject().file(outJar);
	}

	public void setOutJar(Object outJar) {
		this.outJar = outJar;
	}
}
