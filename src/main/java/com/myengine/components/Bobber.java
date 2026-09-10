package com.myengine.components;

import com.myengine.ecs.Component;

/** Bobs the object up and down around whatever position it started at. */
public class Bobber extends Component {

    public float amplitude = 0.3f;
    public float speed = 2f;

    private float baseY;
    private float time = 0f;
    private boolean initialized = false;

    @Override
    public void update(float deltaTime) {
        if (!initialized) {
            baseY = gameObject.transform.position.y;
            initialized = true;
        }
        time += deltaTime;
        gameObject.transform.position.y = baseY + (float) Math.sin(time * speed) * amplitude;
    }
}
