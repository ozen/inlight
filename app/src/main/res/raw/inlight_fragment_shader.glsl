precision mediump float;

uniform sampler2D u_Texture;
uniform sampler2D u_Bump;
uniform mat4 u_CoeffMatrixRed;
uniform mat4 u_CoeffMatrixGreen;
uniform mat4 u_CoeffMatrixBlue;
uniform vec3 u_LightDirection;


varying vec2 v_TexCoord;

void main()
{
    vec4 normal = vec4(normalize(texture2D(u_Bump, v_TexCoord).rgb*2.0-1.0), 1.0);
    //vec4 normal = vec4(0.0, 0.0, 1.0, 1.0);
    vec4 irradiance = vec4(0.0, 0.0, 0.0, 1.0);
    irradiance.r = dot(normal, u_CoeffMatrixRed * normal);
    irradiance.g = dot(normal, u_CoeffMatrixGreen * normal);
    irradiance.b = dot(normal, u_CoeffMatrixBlue *normal);

    vec4 materialDiffuse = vec4(0.3);
    vec4 materialEmissive=vec4(0.001);
    vec4 ambient = vec4(0.03);

    vec4 diffuse =  irradiance * materialDiffuse;
    vec4 emissive = materialEmissive;

    gl_FragColor = (emissive + ambient + diffuse) *texture2D(u_Texture, v_TexCoord);
}

