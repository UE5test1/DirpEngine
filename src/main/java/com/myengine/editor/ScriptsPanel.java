package com.myengine.editor;

import com.myengine.scripting.ScriptManager;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;

import java.io.IOException;
import java.util.List;

/**
 * The Scripts panel: create, edit, and hot-compile real Java Component
 * scripts without leaving the editor or restarting the game. Compiling
 * registers the script into {@code ComponentRegistry}, so it immediately
 * shows up in the Inspector's "+ Add Component" menu.
 */
public class ScriptsPanel {

    private String openScript;
    private final ImString buffer = new ImString(1024 * 64); // 64 KB — plenty for a simple script
    private boolean dirty = false;

    private String status = "";

    private final ImString newScriptName = new ImString(64);

    public void render() {
        ImGui.setNextWindowPos(580, 20, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(480, 480, ImGuiCond.FirstUseEver);
        ImGui.begin("Scripts");

        drawFileList();
        ImGui.separator();
        drawEditor();
        ImGui.separator();
        drawActions();
        drawNewScriptPopup();

        ImGui.end();
    }

    private void drawFileList() {
        ImGui.text("com.mygame.scripts:");
        ImGui.sameLine();
        if (ImGui.smallButton("+ New Script")) {
            newScriptName.set("");
            ImGui.openPopup("NewScriptPopup");
        }

        ImGui.beginChild("ScriptList", 0, 70, true);
        List<String> names = ScriptManager.listScriptNames();
        if (names.isEmpty()) {
            ImGui.textDisabled("No scripts yet — click + New Script to create one.");
        }
        for (String name : names) {
            if (ImGui.selectable(name, name.equals(openScript))) {
                openScript(name);
            }
        }
        ImGui.endChild();
    }

    private void openScript(String name) {
        try {
            buffer.set(ScriptManager.readScript(name));
            openScript = name;
            dirty = false;
            status = "";
        } catch (IOException e) {
            status = "Couldn't open " + name + ": " + e.getMessage();
        }
    }

    private void drawEditor() {
        if (openScript == null) {
            ImGui.textDisabled("Select a script above, or create a new one.");
            return;
        }

        ImGui.text(openScript + ".java" + (dirty ? "  (unsaved changes)" : ""));
        if (ImGui.inputTextMultiline("##ScriptEditor", buffer, 460, 280, ImGuiInputTextFlags.AllowTabInput)) {
            dirty = true;
        }
    }

    private void drawActions() {
        if (openScript == null) {
            return;
        }

        if (ImGui.button("Save")) {
            save();
        }
        ImGui.sameLine();
        if (ImGui.button("Compile && Attach")) {
            save();
            ScriptManager.CompileOutcome outcome = ScriptManager.compileAndRegister(openScript);
            status = outcome.message;
        }

        if (!status.isEmpty()) {
            ImGui.separator();
            ImGui.beginChild("ScriptStatus", 0, 80, true);
            ImGui.textWrapped(status);
            ImGui.endChild();
        }
    }

    private void save() {
        try {
            ScriptManager.writeScript(openScript, buffer.get());
            dirty = false;
        } catch (IOException e) {
            status = "Save failed: " + e.getMessage();
        }
    }

    private void drawNewScriptPopup() {
        if (!ImGui.beginPopup("NewScriptPopup")) {
            return;
        }

        ImGui.text("Class name (e.g. Bounce):");
        ImGui.inputText("##NewScriptName", newScriptName);

        if (ImGui.button("Create")) {
            try {
                String name = ScriptManager.createNewScript(newScriptName.get().trim());
                openScript(name);
                ImGui.closeCurrentPopup();
            } catch (IOException e) {
                status = "Couldn't create script: " + e.getMessage();
                ImGui.closeCurrentPopup();
            }
        }
        ImGui.sameLine();
        if (ImGui.button("Cancel")) {
            ImGui.closeCurrentPopup();
        }

        ImGui.endPopup();
    }
}
