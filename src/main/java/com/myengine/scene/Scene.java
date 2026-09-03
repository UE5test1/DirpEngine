package com.myengine.scene;

import com.myengine.components.Camera;
import com.myengine.components.MeshRenderer;
import com.myengine.ecs.GameObject;
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

    public GameObject createGameObject(String name) {
        GameObject gameObject = new GameObject(name);
        gameObjects.add(gameObject);
        return gameObject;
    }

    public void setActiveCamera(Camera camera) {
        this.activeCamera = camera;
    }

    public void update(float deltaTime) {
        for (GameObject gameObject : gameObjects) {
            gameObject.update(deltaTime);
        }
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
