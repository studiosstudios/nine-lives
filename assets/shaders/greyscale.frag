#ifdef GL_ES
#define PRECISION mediump
precision PRECISION float;
precision PRECISION int;
#else
#define PRECISION
#endif

uniform sampler2D u_texture; // 0
varying vec2 v_texCoords;

uniform float u_greyscale;

void main() {

    vec4 color = texture2D(u_texture,v_texCoords);
    float grey = dot(color.xyz, vec3(0.2126, 0.7152, 0.0722));



    gl_FragColor = vec4(mix(color.xyz, vec3(grey), u_greyscale), color.a);
}
