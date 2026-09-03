package com.myengine.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

/**
 * Loads, compiles and links a vertex + fragment shader pair from
 * src/main/resources, and provides typed helpers for setting uniforms.
 */
public class Shader {

    private final int programId;

    /**
     * @param vertexPath   classpath resource path, e.g. "/shaders/basic.vert"
     * @param fragmentPath classpath resource path, e.g. "/shaders/basic.frag"
     */
    public Shader(String vertexPath, String fragmentPath) {
        int vertexId = compile(loadResource(vertexPath), GL_VERTEX_SHADER);
        int fragmentId = compile(loadResource(fragmentPath), GL_FRAGMENT_SHADER);

        programId = glCreateProgram();
        glAttachShader(programId, vertexId);
        glAttachShader(programId, fragmentId);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader link error: " + glGetProgramInfoLog(programId));
        }

        glDeleteShader(vertexId);
        glDeleteShader(fragmentId);
    }

    private int compile(String source, int type) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);

        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader compile error: " + glGetShaderInfoLog(id));
        }
        return id;
    }

    private String loadResource(String path) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("Shader resource not found on classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader: " + path, e);
        }
    }

    public void use() {
        glUseProgram(programId);
    }

    public void stop() {
        glUseProgram(0);
    }

    public void setMat4(String name, Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            glUniformMatrix4fv(getLocation(name), false, buffer);
        }
    }

    public void setVec3(String name, Vector3f value) {
        glUniform3f(getLocation(name), value.x, value.y, value.z);
    }

    public void setFloat(String name, float value) {
        glUniform1f(getLocation(name), value);
    }

    private int getLocation(String name) {
        return glGetUniformLocation(programId, name);
    }

    public void cleanup() {
        glDeleteProgram(programId);
    }
}
