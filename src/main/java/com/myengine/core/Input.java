package com.myengine.core;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Static, globally-accessible input state — the same convenience Unity gives you
 * with Input.GetKey(...) rather than having to wire up callbacks everywhere yourself.
 */
public class Input {

    private static final boolean[] keys = new boolean[GLFW_KEY_LAST + 1];
    private static final boolean[] mouseButtons = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    private static double lastMouseX, lastMouseY;
    private static double deltaX, deltaY;
    private static boolean firstMouseEvent = true;

    public static void init(long windowHandle) {
        glfwSetKeyCallback(windowHandle, (win, key, scancode, action, mods) -> {
            if (key >= 0 && key < keys.length) {
                keys[key] = action != GLFW_RELEASE;
            }
        });

        glfwSetMouseButtonCallback(windowHandle, (win, button, action, mods) -> {
            if (button >= 0 && button < mouseButtons.length) {
                mouseButtons[button] = action != GLFW_RELEASE;
            }
        });

        glfwSetCursorPosCallback(windowHandle, (win, xpos, ypos) -> {
            if (firstMouseEvent) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouseEvent = false;
            }
            deltaX += xpos - lastMouseX;
            deltaY += ypos - lastMouseY;
            lastMouseX = xpos;
            lastMouseY = ypos;
        });
    }

    /** Call once per frame, before polling events, so deltas don't carry over. */
    public static void resetDeltas() {
        deltaX = 0;
        deltaY = 0;
    }

    public static boolean isKeyDown(int glfwKeyCode) {
        return keys[glfwKeyCode];
    }

    public static boolean isMouseButtonDown(int glfwMouseButton) {
        return mouseButtons[glfwMouseButton];
    }

    public static double getMouseDeltaX() {
        return deltaX;
    }

    public static double getMouseDeltaY() {
        return deltaY;
    }
}
