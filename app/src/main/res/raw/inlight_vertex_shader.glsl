uniform mat4 u_MVPMatrix;

attribute vec2 a_Position;

varying vec2 v_TexCoord;


void main()
{
    gl_Position = u_MVPMatrix * vec4(a_Position, 1.0, 1.0);
    v_TexCoord = a_Position * vec2(0.5, -0.5) + vec2(0.5);
}


