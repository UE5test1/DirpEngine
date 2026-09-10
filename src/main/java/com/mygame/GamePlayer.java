package com.mygame;

/**
 * Entry point for the SHIPPED, BUILT game — this is the Unity "Player"
 * equivalent: same Game/scene as Main.java, but with the editor overlay
 * (Hierarchy/Inspector/Build panels) turned off entirely.
 *
 * <p>This is what pom.xml's maven-shade-plugin sets as the runnable jar's
 * Main-Class, so "mvn package" (and the editor's own Build button) produce a
 * jar that runs the game cleanly — no dev tools, just the game.
 *
 * <p>Use {@link Main} instead when working in IntelliJ; it keeps the editor on.
 */
public class GamePlayer {
    public static void main(String[] args) {
        new Game(false).run();
    }
}
