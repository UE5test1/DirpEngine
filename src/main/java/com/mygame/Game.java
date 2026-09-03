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

/**
 * This is YOUR game code — everything above (in com.myengine) is the reusable
 * engine. Building a level is just: create GameObjects, attach Components.
 */
public class Game extends Application {

    @Override
    protected String getTitle() {
        return "My 3D Game";
    }

    @Override
    protected void init() {
        Shader basicShader = new Shader("/shaders/basic.vert", "/shaders/basic.frag");
        Mesh cubeMesh = MeshFactory.createCube();

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
