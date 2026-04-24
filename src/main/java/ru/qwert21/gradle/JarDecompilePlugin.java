package ru.qwert21.gradle;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;

public class JarDecompilePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        NamedDomainObjectContainer<DecompilePipeline> pipelines =
            project.getObjects().domainObjectContainer(DecompilePipeline.class, DecompilePipeline::new);
        project.getExtensions().add("decompilePipelines", pipelines);

        project.afterEvaluate(p -> pipelines.forEach(pipeline -> registerTasks(p, pipeline)));
    }

    private static String taskName(String prefix, String base) {
        if (prefix.isEmpty()) return base;
        return prefix + Character.toUpperCase(base.charAt(0)) + base.substring(1);
    }

    private static void registerTasks(Project project, DecompilePipeline pipeline) {
        final String n = pipeline.getName();
        final String group = pipeline.getGroup();
        final boolean deobf = pipeline.isDeobf();

        final File buildDir = new File(project.getBuildDir(), "decompile/" + n);
        final File srgFile = new File(buildDir, "mappings.srg");
        final File sigFile = new File(buildDir, "mappings.sig");
        final File classesJar = new File(buildDir, "classes.jar");
        final File resourcesJar = new File(buildDir, "resources.jar");
        final File sigedJar = new File(buildDir, "siged.jar");
        final File deobfJar = new File(buildDir, "deobf.jar");
        final File decompJar = new File(buildDir, "decomp.jar");

        final String splitName = taskName(n, "splitGsrg");
        final String filterClassesName = taskName(n, "filterClasses");
        final String filterResourcesName = taskName(n, "filterResources");
        final String restoreName = taskName(n, "restoreSignatures");
        final String deobfName = taskName(n, "deobf");
        final String decompileName = taskName(n, "decompile");
        final String extractSourcesName = taskName(n, "extractSources");
        final String extractResourcesName = taskName(n, "extractResources");
        final String setupName = taskName(n, "setup");

        project.getTasks().register(splitName, SplitGSRGTask.class, task -> {
            task.setGroup(group);
            task.setInGsrg(pipeline.getMappings());
            task.setOutSrg(srgFile);
            task.setOutSig(sigFile);
            task.setEnabled(deobf);
            task.getOutputs().upToDateWhen(t -> false);
        });

        project.getTasks().register(filterClassesName, FilterTask.class, task -> {
            task.setGroup(group);
            task.setInJar(pipeline.getInJar());
            task.setOutJar(classesJar);
            task.setIncludes(pipeline.getClassFilter().getIncludes());
            task.setExcludes(pipeline.getClassFilter().getExcludes());
            task.getOutputs().upToDateWhen(t -> false);
        });

        project.getTasks().register(filterResourcesName, FilterTask.class, task -> {
            task.setGroup(group);
            task.setInJar(pipeline.getInJar());
            task.setOutJar(resourcesJar);
            task.setIncludes(pipeline.getResourceFilter().getIncludes());
            task.setExcludes(pipeline.getResourceFilter().getExcludes());
            task.getOutputs().upToDateWhen(t -> false);
        });

        project.getTasks().register(restoreName, RestoreSigTask.class, task -> {
            task.setGroup(group);
            task.setSig(sigFile);
            task.setInJar(classesJar);
            task.setOutJar(sigedJar);
            task.setEnabled(deobf);
            task.dependsOn(filterClassesName, splitName);
            task.getOutputs().upToDateWhen(t -> false);
        });

        project.getTasks().register(deobfName, DeobfTask.class, task -> {
            task.setGroup(group);
            task.setSrg(srgFile);
            task.setInJar(sigedJar);
            task.setOutJar(deobfJar);
            task.setEnabled(deobf);
            task.dependsOn(restoreName, splitName);
            task.getOutputs().upToDateWhen(t -> false);
        });

        project.getTasks().register(decompileName, DecompileTask.class, task -> {
            task.setGroup(group);
            task.setInJar(deobf ? deobfJar : classesJar);
            task.setOutJar(decompJar);
            task.setClasspath(pipeline.getClasspath());
            task.dependsOn(deobf ? deobfName : filterClassesName);
            task.getOutputs().upToDateWhen(t -> false);
        });

        project.getTasks().register(extractSourcesName, ExtractTask.class, task -> {
            task.setGroup(group);
            task.from(decompJar);
            task.into(pipeline.getSourcesDir());
            task.dependsOn(decompileName);
            task.getOutputs().upToDateWhen(t -> false);
        });

        project.getTasks().register(extractResourcesName, ExtractTask.class, task -> {
            task.setGroup(group);
            task.from(resourcesJar);
            task.into(pipeline.getResourcesDir());
            task.dependsOn(filterResourcesName);
            task.getOutputs().upToDateWhen(t -> false);
        });

        project.getTasks().register(setupName, task -> {
            task.setGroup(group);
            task.dependsOn(extractSourcesName, extractResourcesName);
        });
    }
}
