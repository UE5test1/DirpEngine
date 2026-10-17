package com.myengine.ecs;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Every GameObject has exactly one of these, just like in Unity. Rotation is
 * stored as Euler angles in degrees (x = pitch, y = yaw, z = roll) since
 * that's the most intuitive form to read and tweak directly.
 *
 * <p>{@code position}/{@code rotation}/{@code scale} are LOCAL — relative to
 * the parent, if there is one (same as Unity's {@code localPosition} etc.).
 * Use {@link #getWorldMatrix()} to get the fully composed world transform.
 */
public class Transform extends Component {

    public Vector3f position = new Vector3f(0, 0, 0);
    public Vector3f rotation = new Vector3f(0, 0, 0);
    public Vector3f scale = new Vector3f(1, 1, 1);

    public Transform parent;
    private final List<Transform> children = new ArrayList<>();

    public List<Transform> getChildren() {
        return children;
    }

    /**
     * Makes this transform a child of {@code newParent} (or a root object, if null).
     * Refuses to create a cycle (parenting something to its own descendant) — that
     * call is silently ignored rather than corrupting the hierarchy.
     */
    public void setParent(Transform newParent) {
        if (newParent == this) {
            return;
        }
        if (newParent != null && isAncestorOf(this, newParent)) {
            return; // newParent is currently a descendant of 'this' — would create a cycle
        }
        if (parent != null) {
            parent.children.remove(this);
        }
        parent = newParent;
        if (newParent != null) {
            newParent.children.add(this);
        }
    }

    private static boolean isAncestorOf(Transform candidate, Transform node) {
        for (Transform p = node.parent; p != null; p = p.parent) {
            if (p == candidate) {
                return true;
            }
        }
        return false;
    }

    /** This transform's local translate/rotate/scale, not accounting for any parent. */
    public Matrix4f getLocalMatrix() {
        return new Matrix4f()
                .translate(position)
                .rotateY((float) Math.toRadians(rotation.y))
                .rotateX((float) Math.toRadians(rotation.x))
                .rotateZ((float) Math.toRadians(rotation.z))
                .scale(scale);
    }

    /** The matrix that transforms this object's local vertices into world space,
     *  composing with every ancestor's transform up the parent chain. */
    public Matrix4f getWorldMatrix() {
        Matrix4f local = getLocalMatrix();
        return parent != null ? parent.getWorldMatrix().mul(local) : local;
    }

    /** This transform's position in world space (accounting for any parent chain). */
    public Vector3f getWorldPosition() {
        return getWorldMatrix().getTranslation(new Vector3f());
    }

    /**
     * The direction this transform is "facing", derived from its LOCAL yaw/pitch only
     * — a parent's rotation isn't factored in. Fine for an unparented object (like the
     * default camera); if you parent the camera under a rotating object, its facing
     * direction won't follow that rotation. A more complete fix is a good follow-up.
     */
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
