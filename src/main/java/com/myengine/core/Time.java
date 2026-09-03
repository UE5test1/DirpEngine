package com.myengine.core;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

/** Frame timing, so movement/rotation/etc. run at the same speed regardless of framerate. */
public class Time {

    private static double lastFrameTime;
    private static float deltaTime;

    public static void init() {
        lastFrameTime = glfwGetTime();
    }

    public static void update() {
        double now = glfwGetTime();
        deltaTime = (float) (now - lastFrameTime);
        lastFrameTime = now;
    }

    public static float getDeltaTime() {
        return deltaTime;
    }
}
