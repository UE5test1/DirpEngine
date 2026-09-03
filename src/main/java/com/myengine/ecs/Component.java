package com.myengine.ecs;

/**
 * Base class for anything you attach to a GameObject — the equivalent of
 * Unity's MonoBehaviour. Override start() for one-time setup and update()
 * for per-frame logic.
 */
public abstract class Component {

    /** Set automatically when this component is added to a GameObject. */
    public GameObject gameObject;

    /** Called once, right after the component is added to a GameObject. */
    public void start() {
    }

    /** Called every frame with the time (in seconds) since the last frame. */
    public void update(float deltaTime) {
    }
}
