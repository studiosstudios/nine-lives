package edu.cornell.gdiac.game.shaders;

import com.badlogic.gdx.Gdx;
import com.crashinvaders.vfx.VfxRenderContext;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.effects.ShaderVfxEffect;
import com.crashinvaders.vfx.framebuffer.VfxFrameBuffer;
import com.crashinvaders.vfx.framebuffer.VfxPingPongWrapper;
import com.crashinvaders.vfx.gl.VfxGLUtils;
public class BloomEffect extends ShaderVfxEffect implements ChainVfxEffect {

    private static final String U_TEXTURE0 = "u_texture0";
    private static final String U_BLURSIZE = "u_blurSize";
    private static final String U_INTENSITY = "u_intensity";
    private float blurSize = 0.005f;
    private float intensity = 0.35f;

    public BloomEffect() {
        super(VfxGLUtils.compileShader(Gdx.files.classpath("gdxvfx/shaders/screenspace.vert"), Gdx.files.internal("shaders/bloom.frag")));
        rebind();
    }

    @Override
    public void update(float delta) {
        super.update(delta);
    }

    public float getBlurSize() {
        return blurSize;
    }

    public void setBlursize(float blurSize) {
        this.blurSize = blurSize;
        setUniform(U_BLURSIZE, blurSize);
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
        setUniform(U_INTENSITY, intensity);
    }

    @Override
    public void rebind() {
        super.rebind();
        program.bind();
        program.setUniformi(U_TEXTURE0, TEXTURE_HANDLE0);
        program.setUniformf(U_BLURSIZE, blurSize);
        program.setUniformf(U_INTENSITY, intensity);
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
