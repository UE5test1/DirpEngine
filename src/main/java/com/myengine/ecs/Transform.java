package com.myengine.ecs;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Every GameObject has exactly one of these, just like in Unity. Rotation is
 * stored as Euler angles in degrees (x = pitch, y = yaw, z = roll) since
 * that's the most intuitive form to read and tweak directly.
 */
public class Transform extends Component {

    public Vector3f position = new Vector3f(0, 0, 0);
    public Vector3f rotation = new Vector3f(0, 0, 0);
    public Vector3f scale = new Vector3f(1, 1, 1);

    /** The matrix that transforms this object's local vertices into world space. */
    public Matrix4f getModelMatrix() {
        return new Matrix4f()
                .translate(position)
                .rotateY((float) Math.toRadians(rotation.y))
                .rotateX((float) Math.toRadians(rotation.x))
                .rotateZ((float) Math.toRadians(rotation.z))
                .scale(scale);
    }

    /** The direction this transform is "facing", derived from its yaw/pitch. */
    public Vector3f getForward() {
        float yaw = (float) Math.toRadians(rotation.y);
        float pitch = (float) Math.toRadians(rotation.x);
        return new Vector3f(
                (float) (Math.cos(pitch) * Math.sin(yaw)),
                (float) Math.sin(pitch),
                (float) (-Math.cos(pitch) * Math.cos(yaw))
        ).normalize();
    }
}
