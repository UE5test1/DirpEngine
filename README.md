# Java 3D Engine

A tiny, from-scratch 3D game engine in Java, built on LWJGL (OpenGL + GLFW)
with a Unity-style **GameObject → Component** architecture. Renders on the GPU,
runs on Windows/Linux/macOS.

## Opening it in IntelliJ IDEA

1. **File → Open...** and select the `java3d-engine` folder (the one containing `pom.xml`).
2. IntelliJ will detect it as a Maven project — click **"Load Maven Project"** if prompted,
   or use the Maven tool window (right sidebar) → the elephant/refresh icon.
3. Wait for dependencies to download (needs internet the first time — it's pulling LWJGL and JOML).
4. Make sure **Project SDK is Java 17+**: `File → Project Structure → Project → SDK`.
5. Open `src/main/java/com/mygame/Main.java` and click the green ▶ next to `main()`.

A window should open with a spinning blue cube.

## Editor vs. Player (Main.java vs. GamePlayer.java)

Same split Unity has:

- **`Main.java`** — what you Run from IntelliJ while developing. Launches the
  game **with** the editor overlay (Hierarchy/Inspector/Build).
- **`GamePlayer.java`** — what a *built* game actually runs as. Same game,
  same Scene, but the editor overlay is completely off — just the game, like
  a Unity Player build.

`mvn package` (and the editor's own Build button) always target `GamePlayer`,
so the jar it produces in `target/` is the real, shippable game — not the dev
environment with your scene loaded into it.

## Edit mode vs. Play mode

Also matching Unity: while running `Main.java`, there's a **Play Mode** panel
with a Play/Stop button.

- **Editing** (the default when you launch): the scene is static. You can
  freely reposition objects, add cubes, attach/remove components — but
  gameplay components (Rotate, Change Color, Bob Up/Down, Pulse Scale) do
  **not** run, so nothing moves on its own while you're setting things up.
  The fly-camera still always works, in both modes — that's navigation, not
  gameplay.
- **Playing** (click Play): gameplay components start running, exactly like
  hitting Play in Unity.
- **Click Stop**: the scene snaps back to exactly how it was the moment you
  clicked Play — any movement, or edits you made while playing, are discarded.
  This is a separate, temporary in-memory snapshot, **not** the same thing as
  Save Scene (see below); Play/Stop never touches `scene.json` on disk.

If your own component should keep running even outside Play mode (like the
camera does), override `runsInEditMode()` to return `true` — see
`FreeCameraController` for the one-line example.

## Saving your scene

**Nothing you do in the editor is saved automatically.** Adding cubes,
dragging positions, attaching Rotate/Change Color/etc. — all of that only
exists in memory for that one run, exactly like Unity discards changes made
in Play Mode. To keep it, click **Save Scene** in the Hierarchy panel.

That writes the whole current scene to `src/main/resources/scene.json`. On
startup, `Game.init()` checks for that file first: if it's there, it loads
it (in whatever state you last saved) instead of the hardcoded demo scene.
The very first time you ever run the game, there's no file yet, so it builds
the demo scene from code and immediately saves that as your starting point.

**Important: this doesn't hot-reload.** Saving writes to the source file, but
the game already running still has its old in-memory state — you won't see
anything change in that window. The updated scene shows up the *next* time
you launch:
- **In IntelliJ:** stop the running game, click Run again. IntelliJ rebuilds
  automatically before running, which copies the new `scene.json` where the
  game can find it.
- **In a built jar:** run **Build Game** (or `mvn package`) again after
  saving — Maven bundles whatever `scene.json` currently says into the new jar.

This is also why the built game now actually reflects your edits (the
original problem this section exists to solve): before, `Game.init()` only
ever ran the same hardcoded Java code, so nothing you changed in the editor
could possibly end up in a build. Now it loads your saved scene instead.

If you want to intentionally go back to the original coded demo scene, just
delete `src/main/resources/scene.json` and run again.

## Controls

- **W A S D** — move the camera
- **Space / Left Shift** — up / down
- **Hold right mouse button + move mouse** — look around

## The editor overlay

On top of the 3D view you'll see several floating panels (drag their title bars
to move or dock them together against the window edges — docking is enabled).
**These only appear when running via `Main.java`** — the built/shipped game
(`GamePlayer.java`) never shows them:

- **Play Mode** — Play/Stop toggle. See "Edit mode vs. Play mode" above.

- **Hierarchy** — every GameObject in the scene, shown as a **tree**: parent
  objects have an arrow to expand/collapse their children. Click a name to
  select it (clicking the arrow only expands/collapses — it won't change
  selection). "+ Add Cube" spawns a new cube into the running scene, live.
  **"Save Scene"** writes the current scene to disk — see "Saving your scene"
  below; without clicking this, anything you do in the editor is lost when you
  close the window.
- **Inspector** — the selected object's **Parent** (a dropdown — pick any
  other object to make this one its child, or "(none)" to make it a root
  object again), then Position/Rotation/Scale, labeled **(local)** since
  they're relative to the parent if it has one (matching Unity's
  `localPosition`/etc. — exactly like the parent, drag the numbers or
  double-click to type). It also shows the object's color if it has a
  MeshRenderer. Edits apply immediately — you'll see the spinning cube react
  in real time. Below that, **+ Add Component** lets you attach a ready-made
  "function" to the object without writing code:
  - **Rotate** — spins the object (same as the demo cube already has)
  - **Change Color** — cycles its color through the rainbow
  - **Bob Up/Down** — floats it up and down
  - **Pulse Scale** — pulses its size in and out

  Each attached component shows its tunable numbers right there in the
  Inspector (speed, amplitude, axis, etc.) and can be removed with its
  **Remove** button. This works for any component with public fields — if you
  write your own (see "Extending it" below) and add one line to
  `ComponentRegistry`, it shows up in this menu automatically with editable
  fields, no extra UI code needed.
- **Build** — click "Build Game" to run `mvn package`, producing the actual
  shippable game jar (see "Editor vs. Player" above — it runs `GamePlayer`,
  not `Main`). **This requires Maven's `mvn` command to be on your system
  PATH** — many IntelliJ setups only have Maven bundled inside the IDE, not on
  PATH. The button also checks `MAVEN_HOME`/`M2_HOME` if `mvn` alone isn't
  found, in case Maven's installed but PATH just isn't set up. If it still fails:
  - **Guaranteed workaround (no setup):** IntelliJ's Maven tool window (small
    tab on the right edge, or `View → Tool Windows → Maven`) → expand your
    project → **Lifecycle** → double-click **package**. Same result either way.
  - **Permanent fix:** install Maven and put it on PATH.
    - **Windows:** there's no official winget package for Maven, so install
      manually: download the binary zip from
      [maven.apache.org/download.cgi](https://maven.apache.org/download.cgi),
      extract it somewhere permanent with no spaces in the path (e.g.
      `C:\tools\apache-maven-3.9.9`), then add its `bin` folder to your PATH
      environment variable (Windows key → "Environment Variables" → Edit the
      system environment variables → Environment Variables → under User
      variables, edit Path → New → paste the `bin` folder path). Open a
      **brand-new** terminal afterward and confirm with `mvn -version`. You'll
      also need `JAVA_HOME` set to your JDK folder (e.g. the one under
      `C:\Users\<you>\.jdks\...` if IntelliJ installed it) the same way.
    - **macOS:** `brew install maven`
    - **Linux:** your package manager (`apt install maven`, etc.)

  Either way, the runnable jar is `target/java3d-engine-1.0-SNAPSHOT.jar`
  (ignore the one with `-original` in the name; that's the slim pre-bundle jar
  the shade plugin leaves behind). Run it with `java -jar <name>.jar` — this
  launches `GamePlayer`, so you'll see just the game, no editor panels.

The editor is built on [Dear ImGui](https://github.com/ocornut/imgui) via the
`imgui-java` bindings, drawn on top of the 3D scene every frame on the same
thread as the game loop — so there's no separate editor process or state to
keep in sync.

**Roadmap for extending the editor further** (each is a separate chunk of work):
transform gizmos in the viewport for move/rotate/scale + mouse-picking to
select objects, and a general file/asset browser panel. (Edit/Play mode and
a script editor are both done — see above and below.)

## Scripts (writing your own components from inside the editor)

The **Scripts** panel lets you write real Java Component scripts without
leaving the editor or restarting the game — no new language, just the JDK's
own compiler, called at runtime.

**Workflow:**
1. Click **+ New Script**, give it a name (e.g. `Bounce`) — this creates
   `src/main/java/com/mygame/scripts/Bounce.java` from a small template and
   opens it in the panel's text editor.
2. Write your logic — you have the exact same `Component` API as any
   built-in one (`gameObject`, `gameObject.transform`, `update(deltaTime)`, etc.).
3. Click **Compile && Attach**. This compiles the file using the JDK's own
   compiler (`javax.tools` — the same one `javac`/IntelliJ use) and registers
   it into `ComponentRegistry` — the exact same registry `Rotator`,
   `ColorCycle`, etc. live in. It immediately shows up in the Inspector's
   **+ Add Component** menu for any GameObject, and (since it's now a normal
   registered component) also saves/loads with the scene like anything else.
4. Select a GameObject, open **+ Add Component**, pick your script's name.

**Why this needs no extra runtime dependency for the built game:** scripts
are real `.java` files under `src/main/java/com/mygame/scripts/`, so a normal
`mvn package` build compiles them exactly like `Game.java` — the built
`GamePlayer` jar just loads the already-compiled class at startup (via
`src/main/resources/scripts-manifest.txt`, written automatically the first
time you compile a script), no JDK or compiler needed at runtime for the
shipped game. The JDK-at-runtime requirement only applies to the **editor's**
Compile button, and IntelliJ's bundled JDK already satisfies it.

**One real limitation, worth knowing:** recompiling a script that's already
been compiled *and loaded* earlier in the same run won't take effect until
you restart the game. The JVM caches a loaded class by name and won't swap in
new bytecode for it mid-session. First-time compiling a new script, or
compiling right after editing it — *before* ever running with it attached —
works fine every time; it's specifically "edit → recompile → see the change
without restarting" for an already-running script that doesn't work yet. If
that turns out to matter a lot in practice, a follow-up fix (loading scripts
through their own dedicated classloader instead of the shared one) would
solve it — flagging it now rather than silently getting it wrong.

## Parenting

GameObjects can now be nested (Hierarchy tree + the Inspector's Parent
dropdown). A few things worth knowing:

- `transform.position`/`rotation`/`scale` are **local** — relative to the
  parent, same as Unity's `localPosition` etc. Use `transform.getWorldMatrix()`
  or `transform.getWorldPosition()` when you need the fully-composed world-space
  result (e.g. writing your own component that needs to know where something
  really is on screen).
- Parenting refuses to create a cycle (e.g. trying to parent an object to its
  own child) — it just silently does nothing rather than corrupting the scene.
- **Don't parent the camera (yet).** `Camera`'s facing direction is computed
  from its own local rotation only, not the full parent chain — a camera
  parented under a rotating object won't turn with it. Position still works
  correctly either way (`getWorldPosition()` does account for the parent
  chain); it's specifically the look direction that's simplified for now.

## How it's organized

```
com.myengine.core        Window, Input, Time, Application (the engine's main loop)
com.myengine.ecs         GameObject, Component, Transform (the object model)
com.myengine.graphics    Shader, Mesh, MeshFactory (low-level GPU wrappers)
com.myengine.components  Camera, MeshRenderer, Rotator, FreeCameraController,
                           ColorCycle, Bobber, Pulser, ComponentRegistry (the
                           shared list of save/load-able + addable component types)
com.myengine.scene       Scene (holds/updates/renders GameObjects), SceneIO
                           (saves/loads scene.json)
com.myengine.scripting   ScriptManager (compiles/registers your .java scripts
                           from com.mygame.scripts, at runtime for the editor)
com.myengine.editor      EditorUI (Hierarchy/Inspector/Build/Play overlay),
                           ScriptsPanel (the Scripts panel), both ImGui-based

com.mygame                Your actual game — Game.java builds the demo scene.
                           Main.java = dev entry point (editor on).
                           GamePlayer.java = built/shipped entry point (editor off).
com.mygame.scripts        Your own Component scripts, written from the editor's
                           Scripts panel (starts empty — nothing here yet)
```

The idea, same as Unity: **everything under `com.myengine` is reusable engine
code you shouldn't need to touch often.** Your game lives in `com.mygame` and
is just a script that creates GameObjects and attaches Components to them.

## Extending it

**Add a new primitive shape** — add a method to `MeshFactory` that returns a
`new Mesh(vertices, indices)` (same interleaved `x,y,z,nx,ny,nz` format the cube uses).

**Write your own script/behaviour** — create a class that extends `Component`,
override `update(float deltaTime)`, and attach it with `gameObject.addComponent(...)`.
`Rotator`, `ColorCycle`, `Bobber`, and `Pulser` are all minimal examples to copy
from. Keep its tunable numbers as **public fields** (not private+getters) and
they'll automatically get an editable row in the Inspector, and automatically
save/load correctly too (see "Saving your scene" above). To make it selectable
from the editor's **+ Add Component** menu as well, add one line in
`ComponentRegistry`'s static block:
```java
register("MyBehaviour", "My Behaviour", MyBehaviour::new, true);
```
(the last `true` means "show it in the Add Component menu" — components like
`Camera` that shouldn't be added twice use `false` there, but still save/load
correctly since they're still in the registry.)

**Add another object to the scene** — in `Game.init()`:
```java
GameObject sphere = scene.createGameObject("Sphere");
sphere.transform.position.set(2, 0, 0);
sphere.addComponent(new MeshRenderer(someMesh, basicShader));
```

**Load real models / textures** — this basic version only draws solid-colored
primitives. The natural next steps (roughly in order) are: texture mapping
(sample a bound texture in the fragment shader using UV coordinates), an
`.obj` model loader, and multiple lights instead of one directional light.

## Troubleshooting

- **`WARNING: A restricted method in java.lang.System has been called` /
  `sun.misc.Unsafe` deprecation warnings in the Run console** — harmless.
  Newer JDKs (24+) print these whenever a native library like LWJGL does
  low-level system calls; they're not errors, and `exit code 0` at the end
  means the program ran and closed normally. If they bother you, add
  `--enable-native-access=ALL-UNNAMED` to the VM options in your Run
  Configuration to silence them — purely cosmetic either way.
- **"UnsatisfiedLinkError" / native library errors** — make sure Maven actually
  finished downloading dependencies (check the Maven tool window for errors),
  and that you're running on a 64-bit JVM.
- **Black window / nothing renders** — check the "Run" console in IntelliJ for
  a shader compile/link error message; the `Shader` class prints GLSL errors verbatim.
- **A method call on `ImGui.*` doesn't match / red squiggly in `EditorUI.java`** —
  the Dear ImGui API is large and imgui-java's exact overloads shift slightly
  between versions; if IntelliJ flags one, hit Ctrl+Space on it to see the
  available overloads (usually an extra/missing argument), or tell me the
  exact error and I'll fix it.
- **macOS: window doesn't appear** — if you ever add `-XstartOnFirstThread`
  requirements from other LWJGL tutorials, note this project doesn't need it
  for GLFW+OpenGL+ImGui alone.
