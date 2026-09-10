package com.myengine.components;

import com.myengine.ecs.Component;

/** Continuously cycles the attached MeshRenderer's color through the rainbow. */
public class ColorCycle extends Component {

    public float speed = 0.5f; // roughly full cycles per second

    private float hue = 0f;
    private MeshRenderer renderer;

    @Override
    public void start() {
        renderer = gameObject.getComponent(MeshRenderer.class);
    }

    @Override
    public void update(float deltaTime) {
        if (renderer == null) {
            renderer = gameObject.getComponent(MeshRenderer.class);
            if (renderer == null) {
                return; // no MeshRenderer on this object (yet) — nothing to color
            }
        }

        hue = (hue + speed * deltaTime) % 1f;
        float[] rgb = hsvToRgb(hue, 0.7f, 1.0f);
        renderer.color.set(rgb[0], rgb[1], rgb[2]);
    }

    private static float[] hsvToRgb(float h, float s, float v) {
        int i = (int) (h * 6f);
        float f = h * 6f - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        switch (i % 6) {
            case 0: return new float[]{v, t, p};
            case 1: return new float[]{q, v, p};
            case 2: return new float[]{p, v, t};
            case 3: return new float[]{p, q, v};
            case 4: return new float[]{t, p, v};
            default: return new float[]{v, p, q};
        }
    }
}
