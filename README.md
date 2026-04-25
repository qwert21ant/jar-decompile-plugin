# jar-decompile-plugin

Gradle plugin providing task types and a DSL for decompiling, deobfuscating, and extracting JAR files. Uses [Vineflower](https://vineflower.org/) as the decompiler.

**Requires:** Gradle 7+, JDK 17+ (runtime), Java 8+ bytecode target.

---

## Installation

Publish to mavenLocal from this project:

```bash
./gradlew publishToMavenLocal
```

Then in the consuming project's `buildSrc/build.gradle`:

```groovy
repositories {
    mavenLocal()
    maven { url = 'https://maven.minecraftforge.net/' }
    maven { name = 'sonatype'; url = 'https://oss.sonatype.org/content/repositories/snapshots/' }
}

dependencies {
    implementation 'ru.qwert21.gradle:jar-decompile-plugin:0.1-SNAPSHOT'
}
```

---

## Option A — Pipeline DSL (recommended)

Apply the plugin and configure named pipelines. Each pipeline auto-registers all required tasks.

```groovy
// build.gradle
apply plugin: 'ru.qwert21.jar-decompile'

decompilePipelines {
    launcher {                                          // pipeline name = task prefix
        inJar      = 'path/to/obfuscated.jar'
        sourcesDir = sourceSets.launcher.java.srcDirs[0]
        resourcesDir = sourceSets.launcher.resources.srcDirs[0]
        classpath  = configurations.launcher           // used by decompiler for type resolution

        // Optional — deobfuscation (default: false)
        deobf    = false
        mappings = 'resources/launcher.gsrg'           // GSRG file, only needed when deobf = true

        // Filter which entries become the "classes" JAR fed to the decompiler
        classFilter {
            include '**/*.class', '**/*.java'
            exclude 'javax/**', 'javafx/**'
            exclude 'com/**', 'de/**', 'net/**', 'org/**'
            exclude 'lombok/**', 'netscape/**', 'LZMA/**'
            exclude 'module-info.class'
        }

        // Filter which entries become the "resources" JAR (default: exclude *.class and *.java)
        // resourceFilter {
        //     exclude '**/*.class', '**/*.java'
        // }

        // Optional — change Gradle task group (default: "decompilation")
        // group = 'decompilation'
    }
}
```

### Generated tasks

For a pipeline named `launcher`, the following tasks are registered:

| Task | Description |
|---|---|
| `launcherSplitGsrg` | Parse GSRG mappings into `.srg` + `.sig` files. Enabled only when `deobf = true`. |
| `launcherFilterClasses` | Extract entries matching `classFilter` from `inJar` into a classes-only JAR. |
| `launcherFilterResources` | Extract entries matching `resourceFilter` from `inJar` into a resources JAR. |
| `launcherRestoreSignatures` | Apply generic signatures from `.sig` file back onto classes. Enabled only when `deobf = true`. |
| `launcherDeobf` | Remap obfuscated names using `.srg` mappings (SpecialSource). Enabled only when `deobf = true`. |
| `launcherDecompile` | Decompile classes JAR to Java sources using Vineflower. |
| `launcherExtractSources` | Extract decompiled `.java` files into `sourcesDir`. |
| `launcherExtractResources` | Extract resources into `resourcesDir`. |
| `launcherSetup` | Aggregate task — runs `extractSources` + `extractResources`. |

Intermediate JARs are written to `build/decompile/launcher/`.

Multiple pipelines are supported — each gets its own prefixed task set and build directory.

### GSRG format

GSRG is a superset of SRG that carries generic signatures. `SplitGSRGTask` splits it:

```
CL oldClass newClass
FD oldClass/oldField newField [descriptor]
MD oldClass/oldMethod oldDescriptor newMethod newDescriptor [signature]
```

---

## Option B — Manual task wiring

Import task types directly in any `.gradle` script (no plugin application needed):

```groovy
import ru.qwert21.gradle.DecompileTask
import ru.qwert21.gradle.DeobfTask
import ru.qwert21.gradle.ExtractTask
import ru.qwert21.gradle.FilterTask
import ru.qwert21.gradle.RestoreSigTask
import ru.qwert21.gradle.SplitGSRGTask
```

### Task type reference

#### `FilterTask`
Writes a new JAR containing only entries matching the pattern filter.

```groovy
tasks.register('filterClasses', FilterTask) {
    inJar  = file('path/to/input.jar')
    outJar = file("$buildDir/classes.jar")
    include '**/*.class'
    exclude 'javax/**'
}
```

#### `SplitGSRGTask`
Splits a GSRG mappings file into a `.srg` remapping file and a `.sig` signatures file.

```groovy
tasks.register('splitMappings', SplitGSRGTask) {
    inGsrg = file('resources/mappings.gsrg')
    outSrg = file("$buildDir/mappings.srg")
    outSig = file("$buildDir/mappings.sig")
}
```

#### `RestoreSigTask`
Applies generic signatures from a `.sig` file onto classes in a JAR (ASMTools).

```groovy
tasks.register('restoreSigs', RestoreSigTask) {
    inJar  = file("$buildDir/classes.jar")
    sig    = file("$buildDir/mappings.sig")
    outJar = file("$buildDir/siged.jar")
}
```

#### `DeobfTask`
Remaps obfuscated names in a JAR using a `.srg` mappings file (SpecialSource).

```groovy
tasks.register('deobf', DeobfTask) {
    inJar  = file("$buildDir/siged.jar")
    srg    = file("$buildDir/mappings.srg")
    outJar = file("$buildDir/deobf.jar")
}
```

#### `DecompileTask`
Decompiles a JAR to Java sources using Vineflower. Output is a JAR of `.java` files.

```groovy
tasks.register('decompile', DecompileTask) {
    inJar    = file("$buildDir/classes.jar")
    outJar   = file("$buildDir/decomp.jar")
    setClasspath configurations.compileClasspath   // optional, improves decompilation quality
}
```

#### `ExtractTask`
Extracts a ZIP/JAR to a directory. Supports include/exclude patterns.

```groovy
tasks.register('extractSources', ExtractTask) {
    from file("$buildDir/decomp.jar")
    into file('src/main/java')
    include '**/*.java'
}
```

---

## Pipeline flow (with deobfuscation)

```
inJar
  ├─ filterClasses  ──► splitGsrg
  │        │                 │
  │    restoreSignatures ◄───┤
  │        │                 │
  │      deobf ◄─────────────┘
  │        │
  │    decompile
  │        │
  │    extractSources ──► sourcesDir
  │
  └─ filterResources
         │
     extractResources ──► resourcesDir
```

Without deobfuscation (`deobf = false`), `splitGsrg`, `restoreSignatures`, and `deobf` tasks are disabled and `decompile` runs directly on the output of `filterClasses`.

---

## Dependencies

| Library | Purpose |
|---|---|
| `org.vineflower:vineflower:1.11.2` | Java decompiler |
| `ru.qwert21.tools:ASMTools:0.1-SNAPSHOT` | Generic signature restoration |
| `net.md-5:SpecialSource:1.7.4` | JAR remapping / deobfuscation |
| `com.google.guava:guava:21.0` | Used internally by `SplitGSRGTask` |
