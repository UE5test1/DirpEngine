package com.myengine.core;

import com.myengine.editor.EditorUI;
import com.myengine.scene.Scene;

/**
 * Base class for a game built on the engine. Extend this and implement init()
 * to set up your scene — similar to how a Unity project doesn't touch the
 * engine's main loop directly, it just populates scenes with GameObjects.
 *
 * <p>Mirrors Unity's Editor-vs-Player split: with {@link #editorEnabled} true
 * (the default, used when you Run from IntelliJ) you get the Hierarchy/
 * Inspector/Build overlay. With it false — what the shipped/built game
 * actually runs as — there's no editor UI at all, just the game.
 */
public abstract class Application {

    protected Window window;
    protected Scene scene;
    protected EditorUI editor;
    protected boolean editorEnabled = true;

    public final void run() {
        window = new Window(1280, 720, getTitle());
        window.init();

        if (editorEnabled) {
            editor = new EditorUI(this);
            editor.init(window.getHandle());
        }

        scene = new Scene();
        init();

        loop();

        if (editorEnabled) {
            editor.dispose();
        }
        cleanup();
        window.destroy();
    }

    /** Override to set the window title. */
    protected String getTitle() {
        return "Java 3D Engine";
    }

    /** Build your scene here: create GameObjects, add Components, set the active camera. */
    protected abstract void init();

    /** Optional: release any extra resources (textures, sounds, etc.) you allocated yourself. */
    protected void cleanup() {
    }

    /** Lets the editor (and anything else) reach the live scene. */
    public Scene getScene() {
        return scene;
    }

    private void loop() {
        Time.init();

        while (!window.shouldClose()) {
            Time.update();
            float deltaTime = Time.getDeltaTime();

            Input.resetDeltas();
            window.pollEvents();

            if (editorEnabled) {
                editor.newFrame();
            }

            // A built Player (editorEnabled=false) always runs gameplay — there's no
            // Play/Stop toggle to check. With the editor, gameplay only runs in Play mode.
            boolean playing = !editorEnabled || editor.isPlaying();
            scene.update(deltaTime, playing);

            window.clear();
            scene.render(window.getAspectRatio());
            if (editorEnabled) {
                editor.render();
            }
            window.swapBuffers();
        }
    }
}
