package edu.cornell.gdiac.game.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.crashinvaders.vfx.VfxRenderContext;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.effects.ShaderVfxEffect;
import com.crashinvaders.vfx.framebuffer.VfxFrameBuffer;
import com.crashinvaders.vfx.framebuffer.VfxPingPongWrapper;
import com.crashinvaders.vfx.gl.VfxGLUtils;
public class PortalEffect extends ShaderVfxEffect implements ChainVfxEffect {

    private static final String U_TEXTURE0 = "u_texture0";
    private static final String U_THICKNESS = "u_thickness";
    private static final String U_TIME = "u_time";
    private static final String U_RADIUS = "u_radius";
    private static final String U_EDGECOLOR = "u_edgeColor";
    private static final String U_BGCOLOR = "u_bgColor";
    private static final String U_GREYSCALE = "u_greyscale";
    private Color edgeColor = new Color();
    private Color bgColor = new Color();
    private float time;
    private float thickness;
    private float radius = 1f;
    private float alpha = 1f;
    private float greyscale;
    public boolean shouldBind = true;

    public PortalEffect() {
        super(VfxGLUtils.compileShader(Gdx.files.classpath("gdxvfx/shaders/screenspace.vert"), Gdx.files.internal("shaders/portal.frag")));
        rebind();
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        setTime(this.time + delta);
    }

    public float getTime() { return time; }

    public void setTime(float time) {
        this.time = time;
        if (shouldBind) {
            setUniform(U_TIME, time);
        } else {
            program.setUniformf(U_TIME, time);
        }
    }

    public float getRadius() { return radius; }

    public void setRadius(float radius) {
        this.radius = radius;
        if (shouldBind) {
            setUniform(U_RADIUS, radius);
        } else {
            program.setUniformf(U_RADIUS, radius);
        }
    }

    public float getGreyscale() { return greyscale; }

    public void setGreyscale(float greyscale) {
        this.greyscale = greyscale;
        if (shouldBind) {
            setUniform(U_GREYSCALE, greyscale);
        } else {
            program.setUniformf(U_GREYSCALE, greyscale);
        }
    }

    public float getThickness() { return thickness; }

    public void setThickness(float thickness) {
        this.thickness = thickness;
        if (shouldBind) {
            setUniform(U_THICKNESS, thickness);
        } else {
            program.setUniformf(U_THICKNESS, thickness);
        }
    }

    public Color getEdgeColor() { return edgeColor; }
    public void setEdgeColor(Color color){
        edgeColor.set(color);
        if (shouldBind) {
            setUniform(U_EDGECOLOR, color);
        } else {
            program.setUniformf(U_EDGECOLOR, color);
        }
    }

    public Color getBgColor() {return bgColor; }

    public void setBgColor(Color color){
        bgColor.set(color);
        if (shouldBind) {
            setUniform(U_BGCOLOR, color);
        } else {
            program.setUniformf(U_BGCOLOR, color);
        }
    }

    @Override
    public void rebind() {
        super.rebind();
        program.bind();
        program.setUniformi(U_TEXTURE0, TEXTURE_HANDLE0);
        program.setUniformf(U_THICKNESS, thickness);
        program.setUniformf(U_TIME, time);
        program.setUniformf(U_RADIUS, radius);
        program.setUniformf(U_EDGECOLOR, edgeColor);
        program.setUniformf(U_BGCOLOR, bgColor);
    }

    @Override
    public void render(VfxRenderContext context, VfxPingPongWrapper buffers) {
        render(context, buffers.getSrcBuffer(), buffers.getDstBuffer());
    }

    public void render(VfxRenderContext context, VfxFrameBuffer src, VfxFrameBuffer dst) {
        // Bind src buffer's texture as a primary one.
        src.getTexture().bind(TEXTURE_HANDLE0);
        // Apply shader effect and render result to dst buffer.
        renderShader(context, dst);
    }
}
