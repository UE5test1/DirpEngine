package com.myengine.editor;

import com.myengine.components.ComponentRegistry;
import com.myengine.components.MeshRenderer;
import com.myengine.core.Application;
import com.myengine.ecs.Component;
import com.myengine.ecs.GameObject;
import com.myengine.ecs.Transform;
import com.myengine.graphics.Mesh;
import com.myengine.graphics.MeshFactory;
import com.myengine.graphics.Shader;
import com.myengine.scene.Scene;
import com.myengine.scene.SceneIO;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * A lightweight, Unity-inspired editor overlay built on Dear ImGui: a
 * Hierarchy of GameObjects, an Inspector that live-edits the selected
 * object's Transform/Components, and a Build panel.
 *
 * <p>This runs on the SAME thread as the render loop — it's an immediate-mode
 * UI redrawn from scratch every frame from whatever the live Scene currently
 * looks like, so there's no separate UI state to keep in sync with the game.
 * The one exception is the Build button, which shells out to Maven on a
 * background thread so a multi-second build doesn't freeze the window; that
 * thread only touches a few {@code volatile} status fields, never the Scene.
 */
public class EditorUI {

    /**
     * True from init() until dispose(). Components (like FreeCameraController)
     * check this before calling any ImGui.* method, since a built/shipped
     * Player runs with editorEnabled=false — no ImGui context exists at all,
     * and calling into ImGui without one would crash rather than no-op.
     */
    public static volatile boolean active = false;

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    private final Application app;
    private GameObject selected;

    private volatile boolean building = false;
    private volatile String buildLog = "";
    private volatile String buildStatus = "";

    // Scratch buffers reused every frame for the Inspector's drag/color widgets.
    private final float[] posBuf = new float[3];
    private final float[] rotBuf = new float[3];
    private final float[] scaleBuf = new float[3];
    private final float[] colorBuf = new float[3];

    private String saveStatus = "";

    private boolean playing = false;
    private String editModeSnapshot = null;

    private final ScriptsPanel scriptsPanel = new ScriptsPanel();

    public EditorUI(Application app) {
        this.app = app;
    }

    public void init(long windowHandle) {
        ImGui.createContext();

        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null); // don't persist window layout between runs, for now
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);

        // installCallbacks=true: ImGui detects the GLFW callbacks Input.init() already
        // registered and chains onto them, so your own keyboard/mouse handling and the
        // editor's both keep working.
        imGuiGlfw.init(windowHandle, true);
        imGuiGl3.init("#version 330 core");

        active = true;
    }

    /** Call once per frame, before any game/editor logic that reads input state. */
    public void newFrame() {
        imGuiGl3.newFrame();   // builds the font atlas texture + shader on first call
        imGuiGlfw.newFrame();
        ImGui.newFrame();
    }

    /** Whether gameplay components should currently be updating — see Application.loop(). */
    public boolean isPlaying() {
        return playing;
    }

    /** Call once per frame, after the 3D scene has been drawn, so panels layer on top. */
    public void render() {
        drawPlayControls();
        drawHierarchy();
        drawInspector();
        drawBuildPanel();
        scriptsPanel.render();

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    public void dispose() {
        imGuiGl3.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();
        active = false;
    }

    // ---------------------------------------------------------------- Play Mode

    private void drawPlayControls() {
        ImGui.setNextWindowPos(20, 350, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(220, 50, ImGuiCond.FirstUseEver);
        ImGui.begin("Play Mode");

        if (!playing) {
            if (ImGui.button("Play")) {
                startPlaying();
            }
        } else {
            if (ImGui.button("Stop")) {
                stopPlaying();
            }
        }
        ImGui.sameLine();
        ImGui.textDisabled(playing ? "Playing" : "Editing");

        ImGui.end();
    }

    private void startPlaying() {
        // Remember the exact pre-Play state so Stop can put everything back,
        // no matter what the game (or you, via the Inspector) does while playing.
        editModeSnapshot = SceneIO.toJson(app.getScene());
        playing = true;
    }

    private void stopPlaying() {
        Scene scene = app.getScene();
        scene.clear(); // doesn't touch scene.defaultShader, so no need to re-save it
        selected = null;

        if (editModeSnapshot != null) {
            SceneIO.fromJson(scene, editModeSnapshot);
        }
        editModeSnapshot = null;
        playing = false;
    }

    // ---------------------------------------------------------------- Hierarchy

    private void drawHierarchy() {
        ImGui.setNextWindowPos(20, 20, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(220, 320, ImGuiCond.FirstUseEver);
        ImGui.begin("Hierarchy");

        Scene scene = app.getScene();
        for (GameObject go : scene.getGameObjects()) {
            if (go.transform.parent == null) { // only recurse from root objects
                drawHierarchyNode(go);
            }
        }

        ImGui.separator();
        if (ImGui.button("+ Add Cube")) {
            addCube();
        }

        ImGui.separator();
        ImGui.beginDisabled(playing);
        if (ImGui.button("Save Scene")) {
            saveScene();
        }
        ImGui.endDisabled();
        if (playing) {
            ImGui.sameLine();
            ImGui.textDisabled("(stop playing first)");
        } else if (!saveStatus.isEmpty()) {
            ImGui.sameLine();
            ImGui.textDisabled(saveStatus);
        }

        ImGui.end();
    }

    private void drawHierarchyNode(GameObject go) {
        List<Transform> children = go.transform.getChildren();

        int flags = ImGuiTreeNodeFlags.OpenOnArrow | ImGuiTreeNodeFlags.SpanAvailWidth
                | ImGuiTreeNodeFlags.DefaultOpen;
        if (go == selected) {
            flags |= ImGuiTreeNodeFlags.Selected;
        }
        if (children.isEmpty()) {
            flags |= ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen;
        }

        // OpenOnArrow means clicking the label itself (not the arrow) doesn't toggle
        // open/closed — we use that click for selection instead, via isItemClicked().
        boolean open = ImGui.treeNodeEx(go.name + "##" + System.identityHashCode(go), flags);
        if (ImGui.isItemClicked()) {
            selected = go;
        }

        if (open && !children.isEmpty()) {
            for (Transform child : children) {
                drawHierarchyNode(child.gameObject);
            }
            ImGui.treePop();
        }
    }

    private void saveScene() {
        try {
            SceneIO.save(app.getScene());
            saveStatus = "Saved.";
        } catch (IOException e) {
            saveStatus = "Save failed: " + e.getMessage();
        }
    }

    private void addCube() {
        // Safe to create GL objects here: the editor draws on the render thread,
        // so the OpenGL context is current, same as everywhere else in the loop.
        Scene scene = app.getScene();
        Shader shader = scene.defaultShader;
        if (shader == null) {
            shader = new Shader("/shaders/basic.vert", "/shaders/basic.frag");
            scene.defaultShader = shader;
        }

        Mesh mesh = MeshFactory.createCube();
        GameObject cube = scene.createGameObject("Cube " + (scene.getGameObjects().size() + 1));
        MeshRenderer renderer = cube.addComponent(new MeshRenderer(mesh, shader));
        renderer.color.set(0.8f, 0.4f, 0.2f);
        selected = cube;
    }

    private void drawParentPicker(GameObject go) {
        String currentLabel = go.transform.parent != null ? go.transform.parent.gameObject.name : "(none)";

        if (ImGui.beginCombo("Parent", currentLabel)) {
            if (ImGui.selectable("(none)", go.transform.parent == null)) {
                go.transform.setParent(null);
            }
            for (GameObject candidate : app.getScene().getGameObjects()) {
                if (candidate == go) {
                    continue; // can't parent an object to itself
                }
                boolean isCurrent = candidate.transform == go.transform.parent;
                if (ImGui.selectable(candidate.name, isCurrent)) {
                    // setParent() itself refuses anything that would create a cycle
                    // (e.g. picking one of this object's own descendants), so no
                    // extra validation is needed here.
                    go.transform.setParent(candidate.transform);
                }
            }
            ImGui.endCombo();
        }
    }

    // ---------------------------------------------------------------- Inspector

    private void drawInspector() {
        ImGui.setNextWindowPos(260, 20, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(300, 260, ImGuiCond.FirstUseEver);
        ImGui.begin("Inspector");

        if (selected == null) {
            ImGui.textDisabled("Select an object in the Hierarchy");
            ImGui.end();
            return;
        }

        ImGui.text(selected.name);
        drawParentPicker(selected);
        ImGui.separator();

        Transform t = selected.transform;

        posBuf[0] = t.position.x;
        posBuf[1] = t.position.y;
        posBuf[2] = t.position.z;
        if (ImGui.dragFloat3("Position (local)", posBuf, 0.05f)) {
            t.position.set(posBuf[0], posBuf[1], posBuf[2]);
        }

        rotBuf[0] = t.rotation.x;
        rotBuf[1] = t.rotation.y;
        rotBuf[2] = t.rotation.z;
        if (ImGui.dragFloat3("Rotation (local)", rotBuf, 0.5f)) {
            t.rotation.set(rotBuf[0], rotBuf[1], rotBuf[2]);
        }

        scaleBuf[0] = t.scale.x;
        scaleBuf[1] = t.scale.y;
        scaleBuf[2] = t.scale.z;
        if (ImGui.dragFloat3("Scale (local)", scaleBuf, 0.05f)) {
            t.scale.set(scaleBuf[0], scaleBuf[1], scaleBuf[2]);
        }

        MeshRenderer renderer = selected.getComponent(MeshRenderer.class);
        if (renderer != null) {
            ImGui.separator();
            ImGui.text("Mesh Renderer");
            colorBuf[0] = renderer.color.x;
            colorBuf[1] = renderer.color.y;
            colorBuf[2] = renderer.color.z;
            if (ImGui.colorEdit3("Color", colorBuf)) {
                renderer.color.set(colorBuf[0], colorBuf[1], colorBuf[2]);
            }
        }

        drawOtherComponents(selected);
        drawAddComponentMenu(selected);

        ImGui.end();
    }

    /**
     * Everything attached to this object besides Transform and MeshRenderer (which
     * already have dedicated sections above) — the "function" components like
     * Rotator or ColorCycle. Fields are shown via reflection, so any Component with
     * public float/Vector3f/boolean fields gets an editable Inspector row for free.
     */
    private void drawOtherComponents(GameObject go) {
        Component toRemove = null;

        for (Component component : go.getComponents()) {
            if (component instanceof Transform || component instanceof MeshRenderer) {
                continue;
            }

            ImGui.separator();
            ImGui.text(component.getClass().getSimpleName());
            ImGui.sameLine();
            if (ImGui.smallButton("Remove##" + System.identityHashCode(component))) {
                toRemove = component;
            }

            drawComponentFields(component);
        }

        if (toRemove != null) {
            go.removeComponent(toRemove);
        }
    }

    /** Renders an editable widget for each public field on a component, via reflection. */
    private void drawComponentFields(Component component) {
        for (Field field : component.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getDeclaringClass() == Component.class) {
                continue; // skip static fields and the inherited 'gameObject' back-reference
            }

            String label = field.getName() + "##" + System.identityHashCode(component);
            try {
                Object value = field.get(component);
                if (value instanceof Float) {
                    float[] buf = {(Float) value};
                    if (ImGui.dragFloat(label, buf, 0.02f)) {
                        field.set(component, buf[0]);
                    }
                } else if (value instanceof Vector3f v) {
                    float[] buf = {v.x, v.y, v.z};
                    if (ImGui.dragFloat3(label, buf, 0.05f)) {
                        v.set(buf[0], buf[1], buf[2]);
                    }
                } else if (value instanceof Boolean) {
                    ImBoolean buf = new ImBoolean((Boolean) value);
                    if (ImGui.checkbox(label, buf)) {
                        field.set(component, buf.get());
                    }
                } else if (value != null) {
                    ImGui.textDisabled(field.getName() + ": " + value);
                }
            } catch (IllegalAccessException ignored) {
                // field is public, so this shouldn't happen in practice
            }
        }
    }

    private void drawAddComponentMenu(GameObject go) {
        ImGui.separator();
        if (ImGui.button("+ Add Component")) {
            ImGui.openPopup("AddComponentPopup");
        }
        if (ImGui.beginPopup("AddComponentPopup")) {
            for (String typeName : ComponentRegistry.ADDABLE_TYPE_NAMES) {
                String label = ComponentRegistry.DISPLAY_NAMES.get(typeName);
                if (ImGui.menuItem(label)) {
                    Supplier<Component> factory = ComponentRegistry.BY_TYPE_NAME.get(typeName);
                    go.addComponent(factory.get());
                }
            }
            ImGui.endPopup();
        }
    }

    // ---------------------------------------------------------------- Build

    private void drawBuildPanel() {
        ImGui.setNextWindowPos(20, 410, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(540, 220, ImGuiCond.FirstUseEver);
        ImGui.begin("Build");

        ImGui.beginDisabled(building);
        if (ImGui.button("Build Game")) {
            startBuild();
        }
        ImGui.endDisabled();

        ImGui.sameLine();
        ImGui.text(building ? "Building..." : buildStatus);

        ImGui.separator();
        ImGui.beginChild("BuildLog", 0, 0, true);
        ImGui.textUnformatted(buildLog);
        if (ImGui.getScrollY() >= ImGui.getScrollMaxY()) {
            ImGui.setScrollHereY(1.0f);
        }
        ImGui.endChild();

        ImGui.end();
    }

    private void startBuild() {
        building = true;
        buildLog = "";
        buildStatus = "";

        Thread thread = new Thread(this::runMavenPackage, "build-thread");
        thread.start();
    }

    private void runMavenPackage() {
        StringBuilder log = new StringBuilder();
        try {
            String projectDir = System.getProperty("user.dir");
            boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

            List<String> command = buildMavenCommand(windows);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(projectDir));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.append(line).append('\n');
                    buildLog = log.toString();
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                buildStatus = "Build succeeded — see target/*.jar (not the '-original' one)";
            } else {
                buildStatus = "Build failed (exit code " + exitCode + ") — see log below";
                log.append("\nIf the log above says 'mvn' is not recognized, it isn't on your PATH.\n")
                        .append("Fastest workaround: IntelliJ's Maven tool window (right sidebar) ->\n")
                        .append("Lifecycle -> package — works right now, no PATH needed.\n")
                        .append("To fix permanently: download Maven from maven.apache.org/download.cgi,\n")
                        .append("extract it, add its bin folder to PATH, then open a NEW terminal.\n");
                buildLog = log.toString();
            }
        } catch (Exception e) {
            buildStatus = "Couldn't run Maven: " + e.getMessage();
            log.append("\n'mvn' isn't on your PATH and MAVEN_HOME/M2_HOME aren't set either.\n")
                    .append("Fastest workaround: IntelliJ's Maven tool window (right sidebar) -> \n")
                    .append("Lifecycle -> package. That always works, no PATH needed.\n")
                    .append("To fix it permanently: download Maven from maven.apache.org/download.cgi,\n")
                    .append("extract it, add its bin folder to PATH, open a NEW terminal, and\n")
                    .append("check 'mvn -version' works.\n");
            buildLog = log.toString();
        } finally {
            building = false;
        }
    }

    /**
     * Builds the Maven command to run, preferring a Maven install pointed to by
     * MAVEN_HOME/M2_HOME (common when Maven is installed but not added to PATH)
     * before falling back to a bare "mvn" that relies on PATH resolution.
     */
    private List<String> buildMavenCommand(boolean windows) {
        String mavenHome = System.getenv("MAVEN_HOME");
        if (mavenHome == null || mavenHome.isBlank()) {
            mavenHome = System.getenv("M2_HOME");
        }

        String mvnExecutable = "mvn";
        if (mavenHome != null && !mavenHome.isBlank()) {
            File candidate = new File(mavenHome, windows ? "bin/mvn.cmd" : "bin/mvn");
            if (candidate.isFile()) {
                mvnExecutable = candidate.getAbsolutePath();
            }
        }

        return windows
                ? List.of("cmd.exe", "/c", mvnExecutable, "-q", "-DskipTests", "package")
                : List.of(mvnExecutable, "-q", "-DskipTests", "package");
    }
}
