precision mediump float;

uniform sampler2D u_Texture;
uniform sampler2D u_Bump;
uniform mat4 u_IrradianceMatrix[3];
uniform vec3 u_BRDFCoeffs[225];

varying vec2 v_TexCoord;

int brdfIndex(int i1, int i2, int i3);
vec3 getBRDF(vec3 normal, int part);
vec2 cart2sph(vec3 cart);
vec2 sph2index(vec2 sph);


void main()
{
    mat4 m_IrradianceMatrix[3];
    vec4 normal = vec4(normalize(texture2D(u_Bump, v_TexCoord).rgb*2.0-1.0), 1.0);

    // interpolate BRDF
    vec3 brdf1 = getBRDF(normal.xyz, 0);
    vec3 brdf2 = getBRDF(normal.xyz, 1);
    vec3 brdf3 = getBRDF(normal.xyz, 2);

    // update irradiance matrix
    for(int band=0; band<3; band++)
    {
        float c5L20 = (u_IrradianceMatrix[band][2][2] / 0.743125) * 0.247708;
        float c4L00 = u_IrradianceMatrix[band][3][3] + c5L20;

        m_IrradianceMatrix[band][0][0] = u_IrradianceMatrix[band][0][0] * brdf3.z;
        m_IrradianceMatrix[band][0][1] = u_IrradianceMatrix[band][0][1] * brdf2.y;
        m_IrradianceMatrix[band][0][2] = u_IrradianceMatrix[band][0][2] * brdf3.y;
        m_IrradianceMatrix[band][0][3] = u_IrradianceMatrix[band][0][3] * brdf2.x;
        m_IrradianceMatrix[band][1][0] = u_IrradianceMatrix[band][1][0] * brdf2.y;
        m_IrradianceMatrix[band][1][1] = u_IrradianceMatrix[band][1][1] * brdf3.z;
        m_IrradianceMatrix[band][1][2] = u_IrradianceMatrix[band][1][2] * brdf2.z;
        m_IrradianceMatrix[band][1][3] = u_IrradianceMatrix[band][1][3] * brdf1.y;
        m_IrradianceMatrix[band][2][0] = u_IrradianceMatrix[band][2][0] * brdf3.y;
        m_IrradianceMatrix[band][2][1] = u_IrradianceMatrix[band][2][1] * brdf2.z;
        m_IrradianceMatrix[band][2][2] = u_IrradianceMatrix[band][2][2] * brdf3.x;
        m_IrradianceMatrix[band][2][3] = u_IrradianceMatrix[band][2][3] * brdf1.z;
        m_IrradianceMatrix[band][3][0] = u_IrradianceMatrix[band][3][0] * brdf2.x;
        m_IrradianceMatrix[band][3][1] = u_IrradianceMatrix[band][3][1] * brdf1.y;
        m_IrradianceMatrix[band][3][2] = u_IrradianceMatrix[band][3][2] * brdf1.z;
        m_IrradianceMatrix[band][3][3] = c4L00 * brdf1.x - c5L20 * brdf3.x;
    }

    vec4 irradiance = vec4(0.0, 0.0, 0.0, 1.0);
    irradiance.r = dot(normal, m_IrradianceMatrix[0] * normal);
    irradiance.g = dot(normal, m_IrradianceMatrix[1] * normal);
    irradiance.b = dot(normal, m_IrradianceMatrix[2] * normal);

    vec4 materialDiffuse = vec4(0.3);
    vec4 materialEmissive=vec4(0.001);
    vec4 ambient = vec4(0.03);

    vec4 diffuse =  irradiance * materialDiffuse;
    vec4 emissive = materialEmissive;

    gl_FragColor = (emissive + ambient + diffuse) *texture2D(u_Texture, v_TexCoord);
}


vec3 getBRDF(vec3 normal, int part)
{
    vec2 index = sph2index(cart2sph(normal));
    ivec2 ul = ivec2(int(floor(index.x)), int(ceil(index.y)));
    ivec2 ll = ivec2(int(floor(index.x)), int(floor(index.y)));
    ivec2 ur = ivec2(int(ceil(index.x)), int(ceil(index.y)));
    ivec2 lr = ivec2(int(ceil(index.x)), int(floor(index.y)));

    float hratio = index.x - floor(index.x);
    float vratio = index.y - floor(index.y);

    return mix(
        mix(u_BRDFCoeffs[brdfIndex(ll.x, ll.y, part)], u_BRDFCoeffs[brdfIndex(lr.x, lr.y, part)], hratio),
        mix(u_BRDFCoeffs[brdfIndex(ul.x, ul.y, part)], u_BRDFCoeffs[brdfIndex(ur.x, ur.y, part)], hratio), vratio);
}

int brdfIndex(int i1, int i2, int i3)
{
    return 99 * i1 + 3 * i2 + i3;
}


vec2 cart2sph(vec3 cart)
{
    vec2 sph;
    sph.x = acos(cart.z / length(cart));
    sph.y = atan(cart.y / cart.x);
    return sph;
}


vec2 sph2index(vec2 sph)
{
    float pi = 3.14159265358;
    vec2 index;
    index.x = (sph.x * (4.0 / pi)) + 2.0;
    index.y = (sph.y * (4.0 / pi)) + 2.0;
    return index;
}