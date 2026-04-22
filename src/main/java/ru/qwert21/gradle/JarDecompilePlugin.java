package ru.qwert21.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class JarDecompilePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        // No tasks registered — plugin exists to put task types on the classpath.
        // Consumers wire tasks themselves in their own build scripts.
    }
}
