#ifdef GL_ES
#define PRECISION mediump
precision PRECISION float;
precision PRECISION int;
#else
#define PRECISION
#endif

float PI = 3.1415;


uniform sampler2D u_texture0; // 0
uniform float u_time; // effect elapsed time
uniform float u_thickness;
uniform float u_radius;
uniform vec4 u_edgeColor;
uniform vec4 u_bgColor;

varying vec2 v_texCoords;

//adapted from https://www.shadertoy.com/view/fddSWj#
void main( )
{
    float t = u_time;
    // Normalized pixel coordinates (from -1 to 1)
    vec2 uvOrig = v_texCoords;
    vec2 uv = (v_texCoords - 0.5)*2.0;


    // polar
    float d = length(uv);
    //float alpha = atan(uv.y, uv.x) / (2.*PI) + 0.5; // normalize -pi,pi to 0, 1 for display
    float alpha = atan(uv.y, uv.x); //-pi to pi
    vec2 pc = vec2(d, alpha); // polar coords

    //fancy calc or irregular shape
    float sinVal = sin(0.5+pc.y*3.+t*7.)*sin(pc.y*18.+t*2.)*0.02 - cos(0.3-pc.y*8.+t*5.)*0.015 + sin(pc.y*8.+t*8.)*0.03 * sin(-pc.y*2.+t*2.);
    float targetVal = u_radius + sinVal;

    float res = 1. - smoothstep(targetVal-u_thickness, targetVal+u_thickness, d);

    vec4 col;
    vec4 portalColor = texture2D(u_texture0,uvOrig);

    float edgeDist = smoothstep(targetVal-u_thickness,targetVal+u_thickness, d);
    if(d < targetVal+u_thickness && d > targetVal - u_thickness){
        col = mix(portalColor, u_edgeColor, u_edgeColor.a);
    } else if (d < targetVal - u_thickness) {
        col = portalColor;
    } else {
        col = mix(portalColor, u_bgColor, u_bgColor.a);
    }
    // Output to screen
    gl_FragColor = col;
}