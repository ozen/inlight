precision mediump float;

uniform sampler2D u_Texture;
uniform sampler2D u_Bump;
uniform mat4 u_IrradianceMatrix[3];
uniform vec3 u_BRDFCoeffs[33][33][3];

varying vec2 v_TexCoord;

void main()
{
    vec4 normal = vec4(normalize(texture2D(u_Bump, v_TexCoord).rgb*2.0-1.0), 1.0);

    // interpolate BRDF
    vec3 brdf1 = getBRDF(normal.xyz, 0);
    vec3 brdf2 = getBRDF(normal.xyz, 1);
    vec3 brdf3 = getBRDF(normal.xyz, 2);

    // update irradiance matrix
    for(int band=0; band<3; band++)
    {
        c5L20 = (u_IrradianceMatrix[band][2][2] / 0.743125) * 0.247708;
        c4L00 = u_IrradianceMatrix[band][2][2] + c5L20;

        u_IrradianceMatrix[band][0][0] *= brdf3.z;
        u_IrradianceMatrix[band][0][1] *= brdf2.y;
        u_IrradianceMatrix[band][0][2] *= brdf3.y;
        u_IrradianceMatrix[band][0][3] *= brdf2.x;
        u_IrradianceMatrix[band][1][0] *= brdf2.y;
        u_IrradianceMatrix[band][1][1] *= brdf3.z;
        u_IrradianceMatrix[band][1][2] *= brdf2.z;
        u_IrradianceMatrix[band][1][3] *= brdf1.y;
        u_IrradianceMatrix[band][2][0] *= brdf3.y;
        u_IrradianceMatrix[band][2][1] *= brdf2.z;
        u_IrradianceMatrix[band][2][2] *= brdf3.x;
        u_IrradianceMatrix[band][2][3] *= brdf1.z;
        u_IrradianceMatrix[band][3][0] *= brdf2.x;
        u_IrradianceMatrix[band][3][1] *= brdf1.y;
        u_IrradianceMatrix[band][3][2] *= brdf1.z;
        u_IrradianceMatrix[band][3][3] = c4L00 * brdf1.x - c5L20 * brdf3.x;
    }

    vec4 irradiance = vec4(0.0, 0.0, 0.0, 1.0);
    irradiance.r = dot(normal, u_IrradianceMatrix[0] * normal);
    irradiance.g = dot(normal, u_IrradianceMatrix[1] * normal);
    irradiance.b = dot(normal, u_IrradianceMatrix[2] * normal);

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
    uvec2 ul = (floor(index.x), ceil(index.y))
    uvec2 ll = (floor(index.x), floor(index.y))
    uvec2 ur = (ceil(index.x), ceil(index.y))
    uvec2 lr = (ceil(index.x), floor(index.y))

    float hratio = index.x - floor(index.x)
    float vratio = index.y - floor(index.y)

    return mix(
        mix(brdfCoeff[ll.x][ll.y][part], brdfCoeff[lr.x][lr.y][part], hratio),
        mix(brdfCoeff[ul.x][ul.y][part], brdfCoeff[ur.x][ur.y][part], hratio), vratio);
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
    index.x = (sph.x * (32.0 / pi)) + 16;
    index.y = (sph.y * (32.0 / pi)) + 16;
    return index;
}