package com.myengine.scene;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.myengine.components.Camera;
import com.myengine.components.ComponentRegistry;
import com.myengine.components.MeshRenderer;
import com.myengine.ecs.Component;
import com.myengine.ecs.GameObject;
import com.myengine.ecs.Transform;
import com.myengine.graphics.Mesh;
import com.myengine.graphics.MeshFactory;
import com.myengine.graphics.Shader;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Saves/loads a Scene's GameObjects (Transform, MeshRenderer color, and any
 * other attached components) as JSON — this is what makes changes made in the
 * editor (moving objects, adding cubes, attaching Rotate/Change Color/etc.)
 * persist across restarts and into a built game, instead of vanishing the
 * moment you close the window.
 *
 * <p>The saved file lives at {@code src/main/resources/scene.json} so Maven
 * bundles it into the built jar automatically, and both {@code Main} (dev) and
 * {@code GamePlayer} (built) load it the same way, via the classpath.
 */
public final class SceneIO {

    /** Where Game.init() looks for a saved scene, and where the editor's Save button writes to. */
    public static final String CLASSPATH_RESOURCE = "/scene.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SceneIO() {
    }

    // ---------------------------------------------------------------- Save

    /** Serializes the scene to a JSON string, without touching disk. */
    public static String toJson(Scene scene) {
        SceneData data = new SceneData();
        for (GameObject go : scene.getGameObjects()) {
            data.gameObjects.add(toData(go));
        }
        return GSON.toJson(data);
    }

    /** Writes the scene to src/main/resources/scene.json (the actual source file, not target/classes). */
    public static void save(Scene scene) throws IOException {
        Path resourcesDir = Path.of(System.getProperty("user.dir"), "src", "main", "resources");
        Files.createDirectories(resourcesDir);
        Path target = resourcesDir.resolve("scene.json");
        Files.writeString(target, toJson(scene), StandardCharsets.UTF_8);
    }

    private static GameObjectData toData(GameObject go) {
        GameObjectData data = new GameObjectData();
        data.name = go.name;
        data.position = toArray(go.transform.position);
        data.rotation = toArray(go.transform.rotation);
        data.scale = toArray(go.transform.scale);

        MeshRenderer renderer = go.getComponent(MeshRenderer.class);
        if (renderer != null) {
            data.hasMeshRenderer = true;
            data.color = toArray(renderer.color);
        }

        for (Component component : go.getComponents()) {
            if (component instanceof Transform || component instanceof MeshRenderer) {
                continue; // handled above, not through the generic path
            }
            data.components.add(toComponentData(component));
        }

        return data;
    }

    private static ComponentData toComponentData(Component component) {
        ComponentData data = new ComponentData();
        data.type = component.getClass().getSimpleName();

        for (Field field : component.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getDeclaringClass() == Component.class) {
                continue;
            }
            try {
                Object value = field.get(component);
                if (value instanceof Float f) {
                    data.floats.put(field.getName(), f);
                } else if (value instanceof Boolean b) {
                    data.booleans.put(field.getName(), b);
                } else if (value instanceof Vector3f v) {
                    data.vectors.put(field.getName(), toArray(v));
                }
                // Other field types aren't handled by the Inspector either, so skip them here too.
            } catch (IllegalAccessException ignored) {
                // field is public, so this shouldn't happen in practice
            }
        }
        return data;
    }

    private static float[] toArray(Vector3f v) {
        return new float[]{v.x, v.y, v.z};
    }

    // ---------------------------------------------------------------- Load

    /** Populates the given (freshly-created, empty) scene from a JSON string, without touching disk. */
    public static void fromJson(Scene scene, String json) {
        SceneData data = GSON.fromJson(json, SceneData.class);
        if (data == null || data.gameObjects == null) {
            return;
        }
        for (GameObjectData god : data.gameObjects) {
            fromData(scene, god);
        }
    }

    /**
     * Loads {@link #CLASSPATH_RESOURCE} into the given (freshly-created, empty) scene.
     * Returns false if no saved scene file exists yet, so callers can fall back to
     * building a default scene in code.
     */
    public static boolean loadIfPresent(Scene scene) {
        try (InputStream in = SceneIO.class.getResourceAsStream(CLASSPATH_RESOURCE)) {
            if (in == null) {
                return false;
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            fromJson(scene, json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void fromData(Scene scene, GameObjectData data) {
        GameObject go = scene.createGameObject(data.name);
        applyVector(go.transform.position, data.position);
        applyVector(go.transform.rotation, data.rotation);
        applyVector(go.transform.scale, data.scale);

        if (Boolean.TRUE.equals(data.hasMeshRenderer)) {
            Shader shader = scene.defaultShader;
            if (shader == null) {
                shader = new Shader("/shaders/basic.vert", "/shaders/basic.frag");
                scene.defaultShader = shader;
            }
            Mesh mesh = MeshFactory.createCube();
            MeshRenderer renderer = go.addComponent(new MeshRenderer(mesh, shader));
            if (data.color != null) {
                renderer.color.set(data.color[0], data.color[1], data.color[2]);
            }
        }

        if (data.components != null) {
            for (ComponentData componentData : data.components) {
                applyComponentData(go, componentData);
            }
        }

        // Every scene needs an active camera; if the saved data included one, use it.
        Camera camera = go.getComponent(Camera.class);
        if (camera != null) {
            scene.setActiveCamera(camera);
        }
    }

    private static void applyComponentData(GameObject go, ComponentData data) {
        Supplier<Component> factory = ComponentRegistry.BY_TYPE_NAME.get(data.type);
        if (factory == null) {
            return; // unknown/renamed component type — skip rather than fail the whole load
        }

        Component component = go.addComponent(factory.get());
        for (Field field : component.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getDeclaringClass() == Component.class) {
                continue;
            }
            try {
                if (data.floats.containsKey(field.getName())) {
                    field.set(component, data.floats.get(field.getName()));
                } else if (data.booleans.containsKey(field.getName())) {
                    field.set(component, data.booleans.get(field.getName()));
                } else if (data.vectors.containsKey(field.getName())) {
                    applyVector((Vector3f) field.get(component), data.vectors.get(field.getName()));
                }
            } catch (IllegalAccessException ignored) {
                // field is public, so this shouldn't happen in practice
            }
        }
    }

    private static void applyVector(Vector3f target, float[] values) {
        if (values != null && values.length == 3) {
            target.set(values[0], values[1], values[2]);
        }
    }

    // ---------------------------------------------------------------- Data model

    private static class SceneData {
        List<GameObjectData> gameObjects = new ArrayList<>();
    }

    private static class GameObjectData {
        String name;
        float[] position;
        float[] rotation;
        float[] scale;
        Boolean hasMeshRenderer;
        float[] color;
        List<ComponentData> components = new ArrayList<>();
    }

    private static class ComponentData {
        String type;
        Map<String, Float> floats = new LinkedHashMap<>();
        Map<String, Boolean> booleans = new LinkedHashMap<>();
        Map<String, float[]> vectors = new LinkedHashMap<>();
    }
}
