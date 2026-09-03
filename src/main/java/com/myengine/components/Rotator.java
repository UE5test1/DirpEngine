package com.myengine.components;

import com.myengine.ecs.Component;
import org.joml.Vector3f;

/**
 * A tiny example "script" component, the same idea as writing your own
 * MonoBehaviour in Unity — this is where your own gameplay logic goes.
 */
public class Rotator extends Component {

    public float degreesPerSecond = 45f;
    public Vector3f axis = new Vector3f(0, 1, 0);

    @Override
    public void update(float deltaTime) {
        Vector3f rotation = gameObject.transform.rotation;
        rotation.x += degreesPerSecond * deltaTime * axis.x;
        rotation.y += degreesPerSecond * deltaTime * axis.y;
        rotation.z += degreesPerSecond * deltaTime * axis.z;
    }
}
