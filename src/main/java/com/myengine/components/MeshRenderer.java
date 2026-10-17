package com.myengine.components;

import com.myengine.ecs.Component;
import com.myengine.graphics.Mesh;
import com.myengine.graphics.Shader;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Attach to a GameObject to make it visible — the equivalent of Unity's MeshRenderer. */
public class MeshRenderer extends Component {

    private final Mesh mesh;
    private final Shader shader;
    public Vector3f color = new Vector3f(1f, 1f, 1f);

    public MeshRenderer(Mesh mesh, Shader shader) {
        this.mesh = mesh;
        this.shader = shader;
    }

    public void render(Matrix4f view, Matrix4f projection, Vector3f lightDirection) {
        shader.use();
        shader.setMat4("model", gameObject.transform.getModelMatrix());
        shader.setMat4("view", view);
        shader.setMat4("projection", projection);
        shader.setVec3("objectColor", color);
        shader.setVec3("lightDir", lightDirection);
        mesh.render();
        shader.stop();
    }
}
