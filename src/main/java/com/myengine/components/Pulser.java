package com.myengine.components;

import com.myengine.ecs.Component;

/** Pulses the object's uniform scale up and down over time, around its starting scale. */
public class Pulser extends Component {

    public float amplitude = 0.2f;
    public float speed = 3f;

    private float baseScale;
    private float time = 0f;
    private boolean initialized = false;

    @Override
    public void update(float deltaTime) {
        if (!initialized) {
            baseScale = gameObject.transform.scale.x;
            initialized = true;
        }
        time += deltaTime;
        float s = baseScale + (float) Math.sin(time * speed) * amplitude;
        gameObject.transform.scale.set(s, s, s);
    }
}
