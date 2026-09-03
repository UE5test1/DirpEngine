package com.myengine.ecs;

import java.util.ArrayList;
import java.util.List;

/**
 * A named "thing" in the scene, made of Components — same idea as a Unity
 * GameObject. It always has a Transform; everything else (meshes, cameras,
 * your own scripts) gets bolted on via addComponent().
 */
public class GameObject {

    public String name;
    public Transform transform;

    private final List<Component> components = new ArrayList<>();

    public GameObject(String name) {
        this.name = name;
        this.transform = new Transform();
        this.transform.gameObject = this;
        components.add(transform);
    }

    public <T extends Component> T addComponent(T component) {
        component.gameObject = this;
        components.add(component);
        component.start();
        return component;
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> type) {
        for (Component c : components) {
            if (type.isInstance(c)) {
                return (T) c;
            }
        }
        return null;
    }

    public void update(float deltaTime) {
        for (Component c : components) {
            c.update(deltaTime);
        }
    }
}
