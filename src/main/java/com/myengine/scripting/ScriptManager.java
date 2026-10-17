package com.myengine.scripting;

import com.myengine.components.ComponentRegistry;
import com.myengine.ecs.Component;

import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Lets you write real Java Component scripts from inside the editor: create a
 * .java file, edit it, and compile it on the fly using the JDK's own compiler
 * (no restart needed) — then attach it to any GameObject like a built-in
 * component. Scripts live as normal source files under
 * {@code src/main/java/com/mygame/scripts/}, so a real {@code mvn package}
 * build compiles them too; the built Player just needs to know their names at
 * startup to register them (see {@link #registerAllKnownScripts()}), which is
 * what {@code scripts-manifest.txt} is for.
 */
public final class ScriptManager {

    public static final String SCRIPTS_PACKAGE = "com.mygame.scripts";

    private static final String MANIFEST_CLASSPATH_RESOURCE = "/scripts-manifest.txt";
    private static final Set<String> knownScripts = new LinkedHashSet<>();

    private ScriptManager() {
    }

    // ---------------------------------------------------------------- Startup registration

    /**
     * Call once at startup, before loading/building the scene. Reads
     * scripts-manifest.txt and registers every script listed there:
     * <ul>
     *   <li>In the editor (dev), a script might not be compiled yet (freshly
     *       created since the last build) — that's fine, it just won't be
     *       available to attach until you hit "Compile &amp; Attach" on it.</li>
     *   <li>In a built Player, every listed script was already compiled as
     *       part of the normal "mvn package" build, so this just needs to
     *       find and register the already-compiled class — no compiler
     *       involved at all in a shipped game.</li>
     * </ul>
     */
    public static void registerAllKnownScripts() {
        knownScripts.clear();
        knownScripts.addAll(readManifestFromClasspath());

        for (String fqcn : knownScripts) {
            try {
                Class<?> loaded = Class.forName(fqcn, true, ScriptManager.class.getClassLoader());
                registerClass(loaded);
            } catch (ClassNotFoundException e) {
                // Not compiled yet on this machine — expected right after creating a new
                // script in the editor, before its first "Compile & Attach".
            } catch (ReflectiveOperationException | IllegalArgumentException e) {
                // No longer a valid Component (edited badly, or constructor removed) — skip.
            }
        }
    }

    // ---------------------------------------------------------------- File management

    private static File scriptsSourceDir() {
        return Path.of(System.getProperty("user.dir"), "src", "main", "java", "com", "mygame", "scripts").toFile();
    }

    private static File scriptSourceFile(String simpleClassName) {
        return new File(scriptsSourceDir(), simpleClassName + ".java");
    }

    /** Every script currently on disk, regardless of whether it's compiled/registered yet. */
    public static List<String> listScriptNames() {
        File[] files = scriptsSourceDir().listFiles((dir, name) -> name.endsWith(".java"));
        List<String> names = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                names.add(f.getName().substring(0, f.getName().length() - ".java".length()));
            }
        }
        Collections.sort(names);
        return names;
    }

    public static String readScript(String simpleClassName) throws IOException {
        return Files.readString(scriptSourceFile(simpleClassName).toPath(), StandardCharsets.UTF_8);
    }

    public static void writeScript(String simpleClassName, String content) throws IOException {
        Files.createDirectories(scriptsSourceDir().toPath());
        Files.writeString(scriptSourceFile(simpleClassName).toPath(), content, StandardCharsets.UTF_8);
    }

    /** Creates a new script file from a small Component template and returns its class name. */
    public static String createNewScript(String simpleClassName) throws IOException {
        if (!isValidClassName(simpleClassName)) {
            throw new IOException("\"" + simpleClassName + "\" isn't a valid Java class name "
                    + "(letters/numbers/underscore, can't start with a number).");
        }
        File file = scriptSourceFile(simpleClassName);
        if (file.exists()) {
            throw new IOException("A script named " + simpleClassName + " already exists.");
        }
        Files.createDirectories(scriptsSourceDir().toPath());
        Files.writeString(file.toPath(), template(simpleClassName), StandardCharsets.UTF_8);
        return simpleClassName;
    }

    private static boolean isValidClassName(String name) {
        return name != null && name.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    private static String template(String className) {
        return "package " + SCRIPTS_PACKAGE + ";\n\n"
                + "import com.myengine.ecs.Component;\n\n"
                + "public class " + className + " extends Component {\n\n"
                + "    public float speed = 1f;\n\n"
                + "    @Override\n"
                + "    public void update(float deltaTime) {\n"
                + "        // Write your behavior here. 'gameObject' and 'gameObject.transform'\n"
                + "        // are available, same as any built-in component.\n"
                + "    }\n"
                + "}\n";
    }

    // ---------------------------------------------------------------- Compilation (editor-only)

    /** Compiles one script file and registers it into ComponentRegistry if it succeeds. */
    public static CompileOutcome compileAndRegister(String simpleClassName) {
        File sourceFile = scriptSourceFile(simpleClassName);
        if (!sourceFile.isFile()) {
            return CompileOutcome.failure("Script file not found: " + sourceFile);
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return CompileOutcome.failure("No Java compiler available at runtime — this needs a full JDK "
                    + "(not just a JRE) as the project SDK. IntelliJ's bundled JDKs normally qualify.");
        }

        File outputDir = resolveClassesOutputDir();
        String classpath = System.getProperty("java.class.path");

        StringBuilder errors = new StringBuilder();
        boolean ok;
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units =
                    fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile));
            List<String> options = List.of("-classpath", classpath, "-d", outputDir.getAbsolutePath());

            List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, diagnostics::add, options, null, units);
            ok = task.call();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics) {
                errors.append(d).append('\n');
            }
        } catch (IOException e) {
            return CompileOutcome.failure("Compiler I/O error: " + e.getMessage());
        }

        if (!ok) {
            return CompileOutcome.failure(errors.toString());
        }

        String fqcn = SCRIPTS_PACKAGE + "." + simpleClassName;
        try {
            Class<?> loaded = Class.forName(fqcn, true, ScriptManager.class.getClassLoader());
            registerClass(loaded);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            return CompileOutcome.failure("Compiled, but couldn't attach it: " + e.getMessage()
                    + "\n(Does it 'extends Component' and have a no-argument constructor?)");
        }

        addToManifest(fqcn);
        return CompileOutcome.success();
    }

    /**
     * Picks where to compile scripts to, so the running JVM's own classloader can find the
     * result immediately. Heuristic: the first entry of the current classpath is, for a
     * normal IntelliJ/Maven run, always the project's own compiled-classes directory (its
     * dependency jars come after it) — this matches what this project's own Run output
     * has consistently shown. Falls back to target/classes if that assumption ever doesn't hold.
     */
    private static File resolveClassesOutputDir() {
        String classpath = System.getProperty("java.class.path", "");
        String[] entries = classpath.split(File.pathSeparator);
        if (entries.length > 0) {
            File first = new File(entries[0]);
            if (first.isDirectory()) {
                return first;
            }
        }
        File fallback = new File(System.getProperty("user.dir"), "target/classes");
        fallback.mkdirs();
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private static void registerClass(Class<?> clazz) throws ReflectiveOperationException {
        if (!Component.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(clazz.getSimpleName() + " does not extend Component");
        }
        Constructor<?> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        Constructor<? extends Component> typedCtor = (Constructor<? extends Component>) ctor;

        Supplier<Component> factory = () -> {
            try {
                return typedCtor.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to instantiate script " + clazz.getName(), e);
            }
        };
        ComponentRegistry.register(clazz.getSimpleName(), factory);
    }

    // ---------------------------------------------------------------- Manifest

    private static List<String> readManifestFromClasspath() {
        try (InputStream in = ScriptManager.class.getResourceAsStream(MANIFEST_CLASSPATH_RESOURCE)) {
            if (in == null) {
                return List.of();
            }
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            List<String> lines = new ArrayList<>();
            for (String line : content.split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    lines.add(trimmed);
                }
            }
            return lines;
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Adds a script to the persisted manifest (src/main/resources/scripts-manifest.txt) so future
     *  runs — including a built Player — know to register it at startup. */
    private static void addToManifest(String fqcn) {
        knownScripts.add(fqcn);
        try {
            Path resourcesDir = Path.of(System.getProperty("user.dir"), "src", "main", "resources");
            Files.createDirectories(resourcesDir);
            Path target = resourcesDir.resolve("scripts-manifest.txt");
            String content = String.join("\n", knownScripts);
            Files.writeString(target, knownScripts.isEmpty() ? "" : content + "\n", StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Not fatal: the script still works for the rest of this session (it's registered
            // in memory already) — it just won't be remembered next time until saved again.
        }
    }

    // ---------------------------------------------------------------- Result type

    public static final class CompileOutcome {
        public final boolean success;
        public final String message;

        private CompileOutcome(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static CompileOutcome success() {
            return new CompileOutcome(true, "Compiled and attached — it's now in the + Add Component menu.");
        }

        public static CompileOutcome failure(String message) {
            return new CompileOutcome(false, message.isBlank() ? "Compile failed." : message);
        }
    }
}
