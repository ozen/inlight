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

void main()
{
    mat4 m_IrradianceMatrix[3];
    vec4 normal = vec4(normalize(texture2D(u_Bump, v_TexCoord).rgb*2.0-1.0), 1.0);

    float brdf[9];
    getBRDF(normal.xyz, brdf);

    // update irradiance matrix
    for(int band=0; band<3; band++)
    {
        float c5L20 = (u_IrradianceMatrix[band][2][2] / 0.743125) * 0.247708;
        float c4L00 = u_IrradianceMatrix[band][3][3] + c5L20;

        m_IrradianceMatrix[band][0][0] = u_IrradianceMatrix[band][0][0] * brdf[8];
        m_IrradianceMatrix[band][0][1] = u_IrradianceMatrix[band][0][1] * brdf[4];
        m_IrradianceMatrix[band][0][2] = u_IrradianceMatrix[band][0][2] * brdf[7];
        m_IrradianceMatrix[band][0][3] = u_IrradianceMatrix[band][0][3] * brdf[3];
        m_IrradianceMatrix[band][1][0] = u_IrradianceMatrix[band][1][0] * brdf[4];
        m_IrradianceMatrix[band][1][1] = u_IrradianceMatrix[band][1][1] * brdf[8];
        m_IrradianceMatrix[band][1][2] = u_IrradianceMatrix[band][1][2] * brdf[5];
        m_IrradianceMatrix[band][1][3] = u_IrradianceMatrix[band][1][3] * brdf[1];
        m_IrradianceMatrix[band][2][0] = u_IrradianceMatrix[band][2][0] * brdf[7];
        m_IrradianceMatrix[band][2][1] = u_IrradianceMatrix[band][2][1] * brdf[5];
        m_IrradianceMatrix[band][2][2] = u_IrradianceMatrix[band][2][2] * brdf[6];
        m_IrradianceMatrix[band][2][3] = u_IrradianceMatrix[band][2][3] * brdf[2];
        m_IrradianceMatrix[band][3][0] = u_IrradianceMatrix[band][3][0] * brdf[3];
        m_IrradianceMatrix[band][3][1] = u_IrradianceMatrix[band][3][1] * brdf[1];
        m_IrradianceMatrix[band][3][2] = u_IrradianceMatrix[band][3][2] * brdf[2];
        m_IrradianceMatrix[band][3][3] = c4L00 * brdf[0] - c5L20 * brdf[6];
    }

    vec3 specular = vec3(dot(normal, m_IrradianceMatrix[0] * normal),
                        dot(normal, m_IrradianceMatrix[1] * normal),
                        dot(normal, m_IrradianceMatrix[2] * normal));



    float irradiance_r = dot(normal, u_IrradianceMatrix[0] * normal);
    float irradiance_g = dot(normal, u_IrradianceMatrix[1] * normal);
    float irradiance_b = dot(normal, u_IrradianceMatrix[2] * normal);
    float mean = (irradiance_r + irradiance_g + irradiance_b) / 3.0;



    vec4 diffuse = 0.05 * mean  * texture2D(u_Texture, v_TexCoord);

    gl_FragColor = diffuse + vec4(specular*0.75, 1.0f);
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
    vec2 sph = vec2(acos(cart.z / length(cart)), atan2(cart.y , cart.x));
    return sph;
    /*
    vec2sph;
    sph.x = acos(cart.z / length(cart));
    sph.y = atan(cart.y / cart.x);
    return sph;
    */
}


vec2 sph2index(vec2 sph)
{
    vec2 index = (sph + vec2(0.0f, PI))  * vec2(10.0f/PI, 5.0f/(2.0f*PI));
    return index;

/*    vec2 index;
    index.x = (sph.x * (4.0 / pi)) + 2.0;
    index.y = (sph.y * (4.0 / pi)) + 2.0;
    return index;
    */
}