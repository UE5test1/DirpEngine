package com.myengine.scene;

import com.myengine.components.Camera;
import com.myengine.components.MeshRenderer;
import com.myengine.ecs.GameObject;
import com.myengine.graphics.Shader;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Holds every GameObject in the current level and drives their update/render each frame. */
public class Scene {

    private final List<GameObject> gameObjects = new ArrayList<>();
    private Camera activeCamera;

    /** A single directional light (like the sun), used by the basic shader. */
    public Vector3f lightDirection = new Vector3f(-0.5f, -1f, -0.3f).normalize();

    /** Set by your Game.init(); lets the editor spawn new objects without recompiling a shader. */
    public Shader defaultShader;

    public GameObject createGameObject(String name) {
        GameObject gameObject = new GameObject(name);
        gameObjects.add(gameObject);
        return gameObject;
    }

    public List<GameObject> getGameObjects() {
        return gameObjects;
    }

    public void setActiveCamera(Camera camera) {
        this.activeCamera = camera;
    }

    public void update(float deltaTime, boolean playing) {
        for (GameObject gameObject : gameObjects) {
            gameObject.update(deltaTime, playing);
        }
    }

    /** Removes every GameObject (used by Stop to wipe the scene before restoring a snapshot). */
    public void clear() {
        gameObjects.clear();
        activeCamera = null;
    }

    public void render(float aspectRatio) {
        if (activeCamera == null) {
            return;
        }

        Matrix4f view = activeCamera.getViewMatrix();
        Matrix4f projection = activeCamera.getProjectionMatrix(aspectRatio);

        for (GameObject gameObject : gameObjects) {
            MeshRenderer renderer = gameObject.getComponent(MeshRenderer.class);
            if (renderer != null) {
                renderer.render(view, projection, lightDirection);
            }
        }
    }
}
