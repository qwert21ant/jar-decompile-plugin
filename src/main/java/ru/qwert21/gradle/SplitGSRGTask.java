package ru.qwert21.gradle;

import com.google.common.io.Files;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class SplitGSRGTask extends AbstractTask {
	private static final Pattern pattern = Pattern.compile("\\s+");
	@InputFile
	private Object inGsrg;

	@Input
	private boolean srgFirst = true;

	@OutputFile
	private Object outSrg;

	@OutputFile
	private Object outSig;

	@TaskAction
	public void doTask() throws IOException {
		File inGsrg = getInGsrg();
		File outSrg = getOutSrg();
		File outSig = getOutSig();

		outSrg.getParentFile().mkdirs();
		outSig.getParentFile().mkdirs();

		List<GsrgLine> gsrgContent = parseGsrg(inGsrg);

		PrintStream srgStream = new PrintStream(new FileOutputStream(outSrg));
		PrintStream sigStream = new PrintStream(new FileOutputStream(outSig));

		for (GsrgLine gsrg : gsrgContent) {
			switch (gsrg.type) {
				case "CL":
					srgStream.printf("CL: %s %s\n", gsrg.name, gsrg.newName);
					break;
				case "FD": {
					String newName = gsrg.name + "/" + gsrg.newName;

					srgStream.printf("FD: %s %s\n", gsrg.name, newName);

					if (gsrg.descriptor != null) {
						sigStream.printf("FD: %s %s\n", srgFirst ? newName : gsrg.name, gsrg.descriptor);
					}
					break;
				}
				case "MD": {
					String newName = gsrg.name + "/" + gsrg.newName;

					srgStream.printf("MD: %1$s %2$s %3$s %2$s\n", gsrg.name, gsrg.descriptor, newName);

					if (gsrg.signature != null) {
						sigStream.printf("MD: %s %s\n", srgFirst ? newName : gsrg.name, gsrg.signature);
					}
					break;
				}
			}
		}

		srgStream.close();
		sigStream.close();
	}

	private List<GsrgLine> parseGsrg(File gsrg) throws IOException {
		List<GsrgLine> result = new LinkedList<>();

		List<String> lines = Files.readLines(gsrg, StandardCharsets.UTF_8);

		for (String line : lines) {
			line = line.trim();
			if (line.isEmpty() || line.startsWith("#"))
				continue;

			String[] parts = pattern.split(line);

			parts[0] = parts[0].substring(0, 2);

			result.add(new GsrgLine(
				parts[0],
				parts[1],
				parts.length > 2 ? parts[2] : null,
				parts.length > 3 ? parts[3] : null,
				parts.length > 4 ? parts[4] : null
			));
		}

		return result;
	}

	public File getInGsrg() {
		return getProject().file(inGsrg);
	}

	public void setInGsrg(Object inGsrg) {
		this.inGsrg = inGsrg;
	}

	public boolean getIsSrgFirst() {
		return srgFirst;
	}

	public void setIsSrgFirst(boolean srgFirst) {
		this.srgFirst = srgFirst;
	}

	public File getOutSrg() {
		return getProject().file(outSrg);
	}

	public void setOutSrg(Object outSrg) {
		this.outSrg = outSrg;
	}

	public File getOutSig() {
		return getProject().file(outSig);
	}

	public void setOutSig(Object outSig) {
		this.outSig = outSig;
	}

	public static class GsrgLine {
		public String type;
		public String name;
		public String newName;
		public String descriptor;
		public String signature;

		public GsrgLine(String type, String name, String newName, String descriptor, String signature) {
			this.type = type;
			this.name = name;
			this.newName = newName;
			this.descriptor = descriptor;
			this.signature = signature;
		}
	}
}
