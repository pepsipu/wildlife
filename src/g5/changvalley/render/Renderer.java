package g5.changvalley.render;

import g5.changvalley.Window;
import g5.changvalley.engine.GameObject;
import g5.changvalley.render.mesh.Mesh;
import g5.changvalley.render.mesh.Uniform;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class Renderer {
    private static final float FOV = (float) Math.toRadians(60);
    private static final float Z_NEAR = 0.01f;
    private static final float Z_FAR = 1000f;
    private static final Matrix4f modelViewMatrix = new Matrix4f();
    private static final Matrix4f projectionMatrix = new Matrix4f();


    public static void construct() {
        // make projection matrix and add it to a uniform
        Uniform.makeUniform("projectionMatrix", Renderer.updateProjectionMatrix());
        // doesn't rlly matter what the world matrix starts off as so for now lets make it not change the object
        Uniform.makeUniform("modelViewMatrix", modelViewMatrix.identity());
        Uniform.makeUniform("textureSampler", 0);
        Uniform.makeUniform("color", new Vector4f(.5f, .5f, .5f, 1));
        Uniform.makeUniform("dither", false);

        glVertexAttrib2f(Mesh.TEXTURE_INDEX, -1, -1);
        glDisableVertexAttribArray(Mesh.TEXTURE_INDEX);
    }

    public static void render(GameObject gameObject) {
        Matrix4f modelMatrix = updateModelViewMatrix(gameObject.position, gameObject.orientation, gameObject.scale);
        Uniform.updateUniform("modelViewMatrix", modelMatrix);
        Uniform.updateUniform("color", gameObject.getColor());
        Uniform.makeUniform("dither", gameObject.getMesh().isDithered());

        gameObject.render();
    }

    public static Matrix4f updateModelViewMatrix(Vector3f translation, Quaternionf rotation, Vector3f scale) {
        return new Matrix4f(Camera.getViewMatrix())
                .mul(modelViewMatrix
                        .translation(translation)
                        .rotate(rotation)
                        .scale(scale));
    }

    public static Matrix4f updateProjectionMatrix() {
        return projectionMatrix.setPerspective(Renderer.FOV, Window.getWidth() / Window.getHeight(), Renderer.Z_NEAR, Renderer.Z_FAR);
    }

    public static void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
}
