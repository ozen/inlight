precision mediump float;


#define PI 3.141592653589793238462643383279


uniform sampler2D u_Texture;
uniform sampler2D u_Bump;
uniform mat4 u_IrradianceMatrix[3];
uniform float u_BRDFCoeffs[225];
uniform float u_TextureSize[2];

varying vec2 v_TexCoord;

int brdfIndex(int i1, int i2, int i3);
void getBRDF(vec3 normal, inout float brdf[9]);
vec2 cart2sph(vec3 cart);
vec2 sph2index(vec2 sph);
vec4 getTexture(sampler2D texture, vec2 coord);

float c1 = 0.429043;
float c2 = 0.511664;
float c3 = 0.743125;
float c4 = 0.886227;
float c5 = 0.247708;

void main()
{
    mat4 m_IrradianceMatrix[3];
    vec4 normal = vec4(normalize(texture2D(u_Bump, v_TexCoord).rgb*2.0-1.0), 1.0);

    float brdf[9];
    getBRDF(normal.xyz, brdf);

    // update irradiance matrix
    for(int band=0; band<3; band++)
    {
        m_IrradianceMatrix[band][0][0] = c1 * brdf[8];
        m_IrradianceMatrix[band][0][1] = c1 * brdf[4];
        m_IrradianceMatrix[band][0][2] = c1 * brdf[7];
        m_IrradianceMatrix[band][0][3] = c2 * brdf[3];
        m_IrradianceMatrix[band][1][0] = c1 * brdf[4];
        m_IrradianceMatrix[band][1][1] = -c1 * brdf[8];
        m_IrradianceMatrix[band][1][2] = c1 * brdf[5];
        m_IrradianceMatrix[band][1][3] = c2 * brdf[1];
        m_IrradianceMatrix[band][2][0] = c1 * brdf[7];
        m_IrradianceMatrix[band][2][1] = c1 * brdf[5];
        m_IrradianceMatrix[band][2][2] = c3 * brdf[6];
        m_IrradianceMatrix[band][2][3] = c2 * brdf[2];
        m_IrradianceMatrix[band][3][0] = c2 * brdf[3];
        m_IrradianceMatrix[band][3][1] = c2 * brdf[1];
        m_IrradianceMatrix[band][3][2] = c2 * brdf[2];
        m_IrradianceMatrix[band][3][3] = c4 * brdf[0] - c5 * brdf[6];
    }

    mat4 mean_irradiance = (u_IrradianceMatrix[0] + u_IrradianceMatrix[1] + u_IrradianceMatrix[2]) / 3.0;
    float mean = dot(normal, mean_irradiance * normal);

    vec3 specular = vec3(
        dot(normal, m_IrradianceMatrix[0] * normal) * dot(normal, u_IrradianceMatrix[0] * normal),
        dot(normal, m_IrradianceMatrix[1] * normal) * dot(normal, u_IrradianceMatrix[1] * normal),
        dot(normal, m_IrradianceMatrix[2] * normal) * dot(normal, u_IrradianceMatrix[2] * normal));

    vec4 diffuse = 0.03 * mean * texture2D(u_Texture, v_TexCoord);

    gl_FragColor = vec4(vec3(0.10) * specular + diffuse.xyz, 1.0);
}


vec4 getTexture(sampler2D texture, vec2 coord)
{
    coord.x = mod(floor(coord.x/10.0), u_TextureSize[0]);
    coord.y = mod(floor(coord.y/10.0), u_TextureSize[1]);
    return texture2D(texture, coord);
}


void getBRDF(vec3 normal, inout float brdf[9])
{
    vec2 index = sph2index(cart2sph(normal));
    ivec2 ul = ivec2(int(floor(index.x)), int(ceil(index.y)));
    ivec2 ll = ivec2(int(floor(index.x)), int(floor(index.y)));
    ivec2 ur = ivec2(int(ceil(index.x)), int(ceil(index.y)));
    ivec2 lr = ivec2(int(ceil(index.x)), int(floor(index.y)));

    float hratio = index.x - floor(index.x);
    float vratio = index.y - floor(index.y);

    for(int k=0; k<9; k++)
    {
        brdf[k] = mix(
            mix(u_BRDFCoeffs[brdfIndex(ll.x, ll.y, k)], u_BRDFCoeffs[brdfIndex(lr.x, lr.y, k)], hratio),
            mix(u_BRDFCoeffs[brdfIndex(ul.x, ul.y, k)], u_BRDFCoeffs[brdfIndex(ur.x, ur.y, k)], hratio),
             vratio);
    }
}

int brdfIndex(int i1, int i2, int i3)
{
    return 45 * i1 + 9 * i2 + i3;
}


vec2 cart2sph(vec3 cart)
{
    return vec2(acos(cart.z / length(cart)), atan(cart.y, cart.x));
}


vec2 sph2index(vec2 sph)
{
    return (sph + vec2(0.0, PI)) * vec2(8.0/PI, 4.0/(2.0*PI));
}

