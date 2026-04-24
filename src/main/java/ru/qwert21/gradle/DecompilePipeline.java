package ru.qwert21.gradle;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;

public class DecompilePipeline implements Named {
    private final String name;

    private Object inJar;
    private String group = "decompilation";
    private boolean deobf = false;
    private Object mappings;
    private Object sourcesDir;
    private Object resourcesDir;
    private FileCollection classpath;

    private final PatternSet classFilter = new PatternSet();
    private final PatternSet resourceFilter = new PatternSet();

    public DecompilePipeline(String name) {
        this.name = name;
        resourceFilter.exclude("**/*.class", "**/*.java");
    }

    @Override
    public String getName() { return name; }

    public Object getInJar() { return inJar; }
    public void setInJar(Object inJar) { this.inJar = inJar; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public boolean isDeobf() { return deobf; }
    public void setDeobf(boolean deobf) { this.deobf = deobf; }

    public Object getMappings() { return mappings; }
    public void setMappings(Object mappings) { this.mappings = mappings; }

    public Object getSourcesDir() { return sourcesDir; }
    public void setSourcesDir(Object sourcesDir) { this.sourcesDir = sourcesDir; }

    public Object getResourcesDir() { return resourcesDir; }
    public void setResourcesDir(Object resourcesDir) { this.resourcesDir = resourcesDir; }

    public FileCollection getClasspath() { return classpath; }
    public void setClasspath(FileCollection classpath) { this.classpath = classpath; }

    public PatternSet getClassFilter() { return classFilter; }

    public void classFilter(Action<PatternFilterable> action) {
        action.execute(classFilter);
    }

    public void classFilter(Closure<?> action) {
        action.setDelegate(classFilter);
        action.setResolveStrategy(Closure.DELEGATE_FIRST);
        action.call();
    }

    public PatternSet getResourceFilter() { return resourceFilter; }

    public void resourceFilter(Action<PatternFilterable> action) {
        action.execute(resourceFilter);
    }

    public void resourceFilter(Closure<?> action) {
        action.setDelegate(resourceFilter);
        action.setResolveStrategy(Closure.DELEGATE_FIRST);
        action.call();
    }
}
