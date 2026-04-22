package ru.qwert21.gradle;

import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import ru.qwert21.tools.asm.JarTransformer;
import ru.qwert21.tools.asm.SigContainer;
import ru.qwert21.tools.asm.transformers.SigApplier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class RestoreSigTask extends AbstractTask {
	@InputFile
	private Object inJar;

	@InputFile
	private Object sig;

	@OutputFile
	private Object outJar;

	@TaskAction
	public void doTask() throws IOException {
		File inJar = getInJar();
		File sig = getSig();
		File outJar = getOutJar();

		outJar.getParentFile().mkdirs();

		SigContainer sigs = new SigContainer();
		sigs.loadFromFile(sig);

		JarTransformer transformer = new JarTransformer(new SigApplier(sigs));

		try (ZipInputStream in = new ZipInputStream(Files.newInputStream(inJar.toPath()));
			 	ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(outJar.toPath()))) {
			transformer.transform(in, out);
		}
	}

	public File getInJar() {
		return getProject().file(inJar);
	}

	public void setInJar(Object inJar) {
		this.inJar = inJar;
	}

	public File getSig() {
		return getProject().file(sig);
	}

	public void setSig(Object sig) {
		this.sig = sig;
	}

	public File getOutJar() {
		return getProject().file(outJar);
	}

	public void setOutJar(Object outJar) {
		this.outJar = outJar;
	}
}
