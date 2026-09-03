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

## Controls

- **W A S D** — move the camera
- **Space / Left Shift** — up / down
- **Hold right mouse button + move mouse** — look around

## How it's organized

```
com.myengine.core        Window, Input, Time, Application (the engine's main loop)
com.myengine.ecs         GameObject, Component, Transform (the object model)
com.myengine.graphics    Shader, Mesh, MeshFactory (low-level GPU wrappers)
com.myengine.components  Camera, MeshRenderer, Rotator, FreeCameraController
com.myengine.scene       Scene (holds and updates/renders all GameObjects)

com.mygame                Your actual game — Game.java builds the demo scene
```

The idea, same as Unity: **everything under `com.myengine` is reusable engine
code you shouldn't need to touch often.** Your game lives in `com.mygame` and
is just a script that creates GameObjects and attaches Components to them.

## Extending it

**Add a new primitive shape** — add a method to `MeshFactory` that returns a
`new Mesh(vertices, indices)` (same interleaved `x,y,z,nx,ny,nz` format the cube uses).

**Write your own script/behaviour** — create a class that extends `Component`,
override `update(float deltaTime)`, and attach it with `gameObject.addComponent(...)`.
`Rotator` is a minimal example to copy from.

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

- **"UnsatisfiedLinkError" / native library errors** — make sure Maven actually
  finished downloading dependencies (check the Maven tool window for errors),
  and that you're running on a 64-bit JVM.
- **Black window / nothing renders** — check the "Run" console in IntelliJ for
  a shader compile/link error message; the `Shader` class prints GLSL errors verbatim.
- **macOS: window doesn't appear** — if you ever add `-XstartOnFirstThread` requirements
  from other LWJGL tutorials, note this project doesn't need it for GLFW+OpenGL alone.
