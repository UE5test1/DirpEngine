package com.myengine.components;

import com.myengine.core.Input;
import com.myengine.ecs.Component;
import com.myengine.ecs.Transform;
import com.myengine.editor.EditorUI;
import imgui.ImGui;
import imgui.ImGuiIO;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A simple fly-around camera: WASD to move, Space/Left-Shift for up/down,
 * hold the right mouse button and move the mouse to look around.
 */
public class FreeCameraController extends Component {

    public float moveSpeed = 5f;
    public float mouseSensitivity = 0.15f;

    @Override
    public boolean runsInEditMode() {
        return true; // flying the camera around is navigation, not gameplay
    }

    @Override
    public void update(float deltaTime) {
        // Let the editor UI have the mouse/keyboard when you're interacting with it
        // (e.g. dragging an Inspector field) instead of also flying the camera.
        // Guarded by EditorUI.active: a built Player has no ImGui context at all.
        if (EditorUI.active) {
            ImGuiIO io = ImGui.getIO();
            if (io.getWantCaptureMouse() || io.getWantCaptureKeyboard()) {
                return;
            }
        }

        Transform transform = gameObject.transform;

        if (Input.isMouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT)) {
            transform.rotation.y += Input.getMouseDeltaX() * mouseSensitivity;
            transform.rotation.x -= Input.getMouseDeltaY() * mouseSensitivity;
            transform.rotation.x = Math.max(-89f, Math.min(89f, transform.rotation.x));
        }

        Vector3f forward = transform.getForward();
        Vector3f right = new Vector3f(forward).cross(new Vector3f(0, 1, 0)).normalize();

        Vector3f move = new Vector3f();
        if (Input.isKeyDown(GLFW_KEY_W)) move.add(forward);
        if (Input.isKeyDown(GLFW_KEY_S)) move.sub(forward);
        if (Input.isKeyDown(GLFW_KEY_D)) move.add(right);
        if (Input.isKeyDown(GLFW_KEY_A)) move.sub(right);
        if (Input.isKeyDown(GLFW_KEY_SPACE)) move.add(0, 1, 0);
        if (Input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) move.sub(0, 1, 0);

        if (move.lengthSquared() > 0f) {
            move.normalize().mul(moveSpeed * deltaTime);
            transform.position.add(move);
        }
    }
}
