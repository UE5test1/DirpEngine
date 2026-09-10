package com.myengine.ecs;

/**
 * Base class for anything you attach to a GameObject — the equivalent of
 * Unity's MonoBehaviour. Override start() for one-time setup and update()
 * for per-frame logic.
 */
public abstract class Component {

    /** Set automatically when this component is added to a GameObject. Excluded
     *  from scene saving/Inspector field listing (see SceneIO / EditorUI) since
     *  it's a back-reference, not tunable data. */
    public transient GameObject gameObject;

    /** Called once, right after the component is added to a GameObject. */
    public void start() {
    }

    /** Called every frame with the time (in seconds) since the last frame. */
    public void update(float deltaTime) {
    }

    /**
     * Override to return true if this component should keep updating even
     * while the editor is NOT in Play mode — for editor-only helpers like a
     * fly-around camera. Everything else defaults to false, matching Unity:
     * gameplay logic only runs during Play, not while editing.
     */
    public boolean runsInEditMode() {
        return false;
    }
}
