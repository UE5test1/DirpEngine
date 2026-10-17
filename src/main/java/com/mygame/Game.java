package com.mygame;

import com.myengine.components.Camera;
import com.myengine.components.FreeCameraController;
import com.myengine.components.MeshRenderer;
import com.myengine.components.Rotator;
import com.myengine.core.Application;
import com.myengine.ecs.GameObject;
import com.myengine.graphics.Mesh;
import com.myengine.graphics.MeshFactory;
import com.myengine.graphics.Shader;
import com.myengine.scene.SceneIO;

import java.io.IOException;

/**
 * This is YOUR game code — everything above (in com.myengine) is the reusable
 * engine. Building a level is just: create GameObjects, attach Components.
 *
 * <p>On startup this first tries to load src/main/resources/scene.json — the
 * file the editor's "Save Scene" button writes to — so edits made in the
 * editor (moving objects, adding cubes, attaching components) actually stick
 * around next time you run, and get bundled into a built game too. Only if
 * that file doesn't exist yet does it fall back to building the demo scene
 * below, which it then immediately saves as the starting scene.json.
 */
public class Game extends Application {

    /** Dev mode (Main.java uses this): the editor overlay is shown. */
    public Game() {
        this(true);
    }

    /** editorEnabled=false is what GamePlayer.java (the built/shipped game) uses. */
    public Game(boolean editorEnabled) {
        this.editorEnabled = editorEnabled;
    }

    @Override
    protected String getTitle() {
        return "My 3D Game";
    }

    @Override
    protected void init() {
        if (SceneIO.loadIfPresent(scene)) {
            return; // loaded a previously-saved scene — nothing more to do
        }

        buildDefaultScene();

        // First run ever: persist this as scene.json so it's the baseline the
        // editor's Save Scene button and future runs build on top of.
        try {
            SceneIO.save(scene);
        } catch (IOException ignored) {
            // Not fatal — the demo scene still runs fine in-memory this session,
            // it just won't have been written to disk yet.
        }
    }

    private void buildDefaultScene() {
        Shader basicShader = new Shader("/shaders/basic.vert", "/shaders/basic.frag");
        Mesh cubeMesh = MeshFactory.createCube();
        scene.defaultShader = basicShader; // lets the editor's "+ Add Cube" reuse it

        // A spinning cube
        GameObject cube = scene.createGameObject("Cube");
        MeshRenderer cubeRenderer = cube.addComponent(new MeshRenderer(cubeMesh, basicShader));
        cubeRenderer.color.set(0.3f, 0.6f, 1.0f);
        cube.addComponent(new Rotator());

        // A second cube, just to show the scene can hold more than one object
        GameObject groundCube = scene.createGameObject("Ground");
        groundCube.transform.position.set(0, -1.5f, 0);
        groundCube.transform.scale.set(6f, 0.2f, 6f);
        MeshRenderer groundRenderer = groundCube.addComponent(new MeshRenderer(cubeMesh, basicShader));
        groundRenderer.color.set(0.35f, 0.35f, 0.4f);

        // A fly-around camera
        GameObject cameraObject = scene.createGameObject("Main Camera");
        cameraObject.transform.position.set(0, 1.2f, 4f);
        Camera camera = cameraObject.addComponent(new Camera());
        cameraObject.addComponent(new FreeCameraController());
        scene.setActiveCamera(camera);
    }
}
