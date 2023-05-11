#ifdef GL_ES
#define PRECISION mediump
precision PRECISION float;
precision PRECISION int;
#else
#define PRECISION
#endif

uniform sampler2D u_texture; // 0
varying vec2 v_texCoords;
varying PRECISION vec4 v_color;
uniform float u_greyscale;

void main() {

    vec4 color = v_color * texture2D(u_texture,v_texCoords);
    float grey = dot(color.xyz, vec3(0.299, 0.587, 0.114));
    vec3 out_col = mix(color.xyz, vec3(grey), u_greyscale);

    gl_FragColor = vec4(out_col * color.a, color.a);

}
