package com.myengine.components;

import com.myengine.ecs.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Every no-argument-constructor Component type the engine knows about, keyed
 * by its simple class name. This is the single source of truth used by:
 * <ul>
 *   <li>the editor's Inspector "+ Add Component" menu ({@link #ADDABLE_TYPE_NAMES})</li>
 *   <li>scene saving/loading (SceneIO), which needs to reconstruct a component
 *       by name when reading a saved scene file back in</li>
 *   <li>{@code ScriptManager}, which registers your own scripts (written and
 *       compiled from the editor's Scripts panel) here at runtime, the same
 *       way the built-in components below register themselves at class-load time</li>
 * </ul>
 * To make your own built-in component both save/load-able AND addable from
 * the editor, add one {@code register(...)} line below. User scripts written
 * via the Scripts panel register themselves automatically — no edit needed here.
 */
public final class ComponentRegistry {

    public static final Map<String, Supplier<Component>> BY_TYPE_NAME = new LinkedHashMap<>();
    public static final Map<String, String> DISPLAY_NAMES = new LinkedHashMap<>();
    public static final List<String> ADDABLE_TYPE_NAMES = new ArrayList<>();

    static {
        // Components that show up in the editor's "+ Add Component" menu:
        register("Rotator", "Rotate", Rotator::new, true);
        register("ColorCycle", "Change Color", ColorCycle::new, true);
        register("Bobber", "Bob Up/Down", Bobber::new, true);
        register("Pulser", "Pulse Scale", Pulser::new, true);

        // Not addable via that menu (a GameObject doesn't need a second Camera),
        // but still need to be reconstructable when a saved scene file is loaded.
        register("Camera", "Camera", Camera::new, false);
        register("FreeCameraController", "Free Camera Controller", FreeCameraController::new, false);
    }

    private ComponentRegistry() {
    }

    /** Full form: used for built-in components, where the menu label can differ from the class name. */
    public static void register(String typeName, String displayName, Supplier<Component> factory, boolean addable) {
        BY_TYPE_NAME.put(typeName, factory);
        DISPLAY_NAMES.put(typeName, displayName);
        if (addable && !ADDABLE_TYPE_NAMES.contains(typeName)) {
            ADDABLE_TYPE_NAMES.add(typeName);
        }
    }

    /** Convenience form for user scripts (see ScriptManager): always addable, display name = class name. */
    public static void register(String typeName, Supplier<Component> factory) {
        register(typeName, typeName, factory, true);
    }
}
