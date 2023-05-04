#ifdef GL_ES
precision highp float;
#endif

uniform sampler2D u_texture0; // 0
uniform vec2 u_center; // Mouse position
uniform float u_time; // effect elapsed time
//uniform vec3 shockParams; // 10.0, 0.8, 0.1

varying vec2 v_texCoords;

void main()
{
    // get pixel coordinates
    vec2 l_texCoords = v_texCoords;
    //vec2 center = vec2(0.5, 0.5);
    vec3 shockParams = vec3(10, 0.8, 0.1);


    //get distance from center
    float distance = distance(v_texCoords, u_center);

    if ( (distance <= (u_time + shockParams.z)) && (distance >= (u_time - shockParams.z)) ) {
        float diff = (distance - u_time);
        float powDiff = 0.0;
        if(distance>0){
            powDiff = 1.0 - pow(abs(diff*shockParams.x), shockParams.y);
        }
        float diffTime = diff  * powDiff;
        vec2 diffUV = normalize(v_texCoords-u_center);
        //Perform the distortion and reduce the effect over time
        l_texCoords = v_texCoords + ((diffUV * diffTime)/(u_time * distance * 5.0));
    }
    gl_FragColor = texture2D(u_texture0, l_texCoords);

}