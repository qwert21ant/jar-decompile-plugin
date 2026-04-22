package ru.qwert21.gradle;

import java.io.File;

public class Utils {
	public static void deleteFile(File file) {
		if (file.isDirectory()) {
			for (File c : file.listFiles())
				deleteFile(c);
		}
		file.delete();
	}
}
