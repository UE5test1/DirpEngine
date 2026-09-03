package com.myengine.components;

import com.myengine.ecs.Component;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Attach to a GameObject to turn it into a viewpoint the Scene can render from. */
public class Camera extends Component {

    public float fieldOfViewDegrees = 70f;
    public float nearPlane = 0.1f;
    public float farPlane = 1000f;

    public Matrix4f getViewMatrix() {
        Vector3f position = gameObject.transform.position;
        Vector3f forward = gameObject.transform.getForward();
        Vector3f target = new Vector3f(position).add(forward);
        return new Matrix4f().lookAt(position, target, new Vector3f(0, 1, 0));
    }

    public Matrix4f getProjectionMatrix(float aspectRatio) {
        return new Matrix4f().perspective(
                (float) Math.toRadians(fieldOfViewDegrees), aspectRatio, nearPlane, farPlane);
    }
}
