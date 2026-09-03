package com.myengine.core;

import com.myengine.scene.Scene;

/**
 * Base class for a game built on the engine. Extend this and implement init()
 * to set up your scene — similar to how a Unity project doesn't touch the
 * engine's main loop directly, it just populates scenes with GameObjects.
 */
public abstract class Application {

    protected Window window;
    protected Scene scene;

    public final void run() {
        window = new Window(1280, 720, getTitle());
        window.init();

        scene = new Scene();
        init();

        loop();

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

    private void loop() {
        Time.init();

        while (!window.shouldClose()) {
            Time.update();
            float deltaTime = Time.getDeltaTime();

            Input.resetDeltas();
            window.pollEvents();

            scene.update(deltaTime);

            window.clear();
            scene.render(window.getAspectRatio());
            window.swapBuffers();
        }
    }
}
