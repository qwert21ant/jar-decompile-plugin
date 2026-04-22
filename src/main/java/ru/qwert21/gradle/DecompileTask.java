package ru.qwert21.gradle;

import org.gradle.api.file.FileCollection;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.*;
import org.jetbrains.java.decompiler.util.InterpreterUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class DecompileTask extends DefaultTask {
	@InputFile
	Object inJar;

	@OutputFile
	Object outJar;

	private FileCollection classpath;

	@TaskAction
	public void doTask() throws IOException {
		final File in = getInJar();
		final File out = getOutJar();
		final File tempDir = this.getTemporaryDir();
		final File tempJar = new File(tempDir, in.getName());

		Map<String, Object> options = new HashMap<>();
		options.put(IFernflowerPreferences.DECOMPILE_INNER, "1");
		options.put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1");
		options.put(IFernflowerPreferences.ASCII_STRING_CHARACTERS, "0");
		options.put(IFernflowerPreferences.REMOVE_SYNTHETIC, "0");
		options.put(IFernflowerPreferences.REMOVE_BRIDGE, "1");
		options.put(IFernflowerPreferences.LITERALS_AS_IS, "0");
		options.put(IFernflowerPreferences.UNIT_TEST_MODE, "0");
		options.put(IFernflowerPreferences.MAX_PROCESSING_METHOD, "0");

		PrintStreamLogger logger = new PrintStreamLogger(System.out);
		BaseDecompiler decompiler = new BaseDecompiler(new ArtifactSaver(tempDir), options, logger);

		decompiler.addSource(in);
		if (classpath != null) {
			for (File library : classpath) {
				decompiler.addLibrary(library);
			}
		}

		decompiler.decompileContext();
		Files.copy(tempJar.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	static class ArtifactSaver implements IResultSaver {
		private final Map<String, ZipOutputStream> mapArchiveStreams = new HashMap<String, ZipOutputStream>();
		private final Map<String, Set<String>> mapArchiveEntries = new HashMap<String, Set<String>>();
		private final File root;

		public ArtifactSaver(File tempDir) {
			this.root = tempDir;
		}

		private String getAbsolutePath(String path) {
			return new File(root, path).getAbsolutePath();
		}

		@Override
		public void saveFolder(String path) {
			File dir = new File(getAbsolutePath(path));
			if (!(dir.mkdirs() || dir.isDirectory())) {
				throw new RuntimeException("Cannot create directory " + dir);
			}
		}

		@Override
		public void copyFile(String source, String path, String entryName) {
			try {
				InterpreterUtil.copyFile(new File(source), new File(getAbsolutePath(path), entryName));
			} catch (IOException ex) {
				DecompilerContext.getLogger().writeMessage("Cannot copy " + source + " to " + entryName, ex);
			}
		}

		@Override
		public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
			File file = new File(getAbsolutePath(path), entryName);
			try {
				try (Writer out = new OutputStreamWriter(new FileOutputStream(file), "UTF8")) {
					out.write(content);
				}
			} catch (IOException ex) {
				DecompilerContext.getLogger().writeMessage("Cannot write class file " + file, ex);
			}
		}

		@Override
		public void createArchive(String path, String archiveName, Manifest manifest) {
			File file = new File(getAbsolutePath(path), archiveName);
			try {
				if (!(file.createNewFile() || file.isFile())) {
					throw new IOException("Cannot create file " + file);
				}

				FileOutputStream fileStream = new FileOutputStream(file);
				ZipOutputStream zipStream = manifest != null ? new JarOutputStream(fileStream, manifest) : new ZipOutputStream(fileStream);
				mapArchiveStreams.put(file.getPath(), zipStream);
			} catch (IOException ex) {
				DecompilerContext.getLogger().writeMessage("Cannot create archive " + file, ex);
			}
		}

		@Override
		public void saveDirEntry(String path, String archiveName, String entryName) {
			saveClassEntry(path, archiveName, null, entryName, null);
		}

		@Override
		public void copyEntry(String source, String path, String archiveName, String entryName) {
			String file = new File(getAbsolutePath(path), archiveName).getPath();

			if (!checkEntry(entryName, file)) {
				return;
			}

			try {
				try (ZipFile srcArchive = new ZipFile(new File(source))) {
					ZipEntry entry = srcArchive.getEntry(entryName);
					if (entry != null) {
						InputStream in = srcArchive.getInputStream(entry);
						ZipOutputStream out = mapArchiveStreams.get(file);
						out.putNextEntry(new ZipEntry(entryName));
						InterpreterUtil.copyStream(in, out);
						in.close();
					}
				}
			} catch (IOException ex) {
				String message = "Cannot copy entry " + entryName + " from " + source + " to " + file;
				DecompilerContext.getLogger().writeMessage(message, ex);
			}
		}

		@Override
		public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
			String file = new File(getAbsolutePath(path), archiveName).getPath();

			if (!checkEntry(entryName, file)) {
				return;
			}

			try {
				ZipOutputStream out = mapArchiveStreams.get(file);
				out.putNextEntry(new ZipEntry(entryName));
				if (content != null) {
					out.write(content.getBytes("UTF-8"));
				}
			} catch (IOException ex) {
				String message = "Cannot write entry " + entryName + " to " + file;
				DecompilerContext.getLogger().writeMessage(message, ex);
			}
		}

		private boolean checkEntry(String entryName, String file) {
			Set<String> set = mapArchiveEntries.get(file);
			if (set == null) {
				mapArchiveEntries.put(file, set = new HashSet<String>());
			}

			boolean added = set.add(entryName);
			if (!added) {
				String message = "Zip entry " + entryName + " already exists in " + file;
				DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.WARN);
			}
			return added;
		}

		@Override
		public void closeArchive(String path, String archiveName) {
			String file = new File(getAbsolutePath(path), archiveName).getPath();
			try {
				mapArchiveEntries.remove(file);
				mapArchiveStreams.remove(file).close();
			} catch (IOException ex) {
				DecompilerContext.getLogger().writeMessage("Cannot close " + file, IFernflowerLogger.Severity.WARN);
			}
		}
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

	public FileCollection getClasspath() {
		return classpath;
	}

	public void setClasspath(FileCollection classpath) {
		this.classpath = classpath;
	}
}
