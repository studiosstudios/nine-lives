#ifdef GL_ES
#define PRECISION mediump
precision PRECISION float;
precision PRECISION int;
#else
#define PRECISION
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;
uniform float u_blurSize;
uniform float u_intensity;


void main( )
{
    vec4 sum = vec4(0);
    vec2 texcoord = v_texCoords;
    int j;
    int i;

    //thank you! http://www.gamerendering.com/2008/10/11/gaussian-blur-filter-shader/ for the
    //blur tutorial
    // blur in y (vertical)
    // take nine samples, with the distance u_blurSize between them
    sum += texture2D(u_texture0, vec2(texcoord.x - 4.0*u_blurSize, texcoord.y)) * 0.05;
    sum += texture2D(u_texture0, vec2(texcoord.x - 3.0*u_blurSize, texcoord.y)) * 0.09;
    sum += texture2D(u_texture0, vec2(texcoord.x - 2.0*u_blurSize, texcoord.y)) * 0.12;
    sum += texture2D(u_texture0, vec2(texcoord.x - u_blurSize, texcoord.y)) * 0.15;
    sum += texture2D(u_texture0, vec2(texcoord.x, texcoord.y)) * 0.16;
    sum += texture2D(u_texture0, vec2(texcoord.x + u_blurSize, texcoord.y)) * 0.15;
    sum += texture2D(u_texture0, vec2(texcoord.x + 2.0*u_blurSize, texcoord.y)) * 0.12;
    sum += texture2D(u_texture0, vec2(texcoord.x + 3.0*u_blurSize, texcoord.y)) * 0.09;
    sum += texture2D(u_texture0, vec2(texcoord.x + 4.0*u_blurSize, texcoord.y)) * 0.05;

    // blur in y (vertical)
    // take nine samples, with the distance u_blurSize between them
    sum += texture2D(u_texture0, vec2(texcoord.x, texcoord.y - 4.0*u_blurSize)) * 0.05;
    sum += texture2D(u_texture0, vec2(texcoord.x, texcoord.y - 3.0*u_blurSize)) * 0.09;
    sum += texture2D(u_texture0, vec2(texcoord.x, texcoord.y - 2.0*u_blurSize)) * 0.12;
    sum += texture2D(u_texture0, vec2(texcoord.x, texcoord.y - u_blurSize)) * 0.15;
    sum += texture2D(u_texture0, vec2(texcoord.x, texcoord.y)) * 0.16;
    sum += texture2D(u_texture0, vec2(texcoord.x, texcoord.y + u_blurSize)) * 0.15;
    sum += texture2D(u_texture0, vec2(texcoord.x, texcoord.y + 2.0*u_blurSize)) * 0.12;
    sum += texture2D(u_texture0, vec2(texcoord.x, texcoord.y + 3.0*u_blurSize)) * 0.09;
    sum += texture2D(u_texture0, vec2(texcoord.x, texcoord.y + 4.0*u_blurSize)) * 0.05;

    //increase blur with u_intensity!
    gl_FragColor = sum*u_intensity + texture2D(u_texture0, texcoord);
    //    gl_FragColor = sum + texture2D(u_texture0, texcoord);
}