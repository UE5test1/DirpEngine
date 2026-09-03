#version 330 core

in vec3 fragNormal;

uniform vec3 objectColor;
uniform vec3 lightDir;

out vec4 FragColor;

void main() {
    vec3 normal = normalize(fragNormal);
    float diffuse = max(dot(normal, -normalize(lightDir)), 0.0);
    float ambient = 0.25;
    vec3 result = objectColor * (ambient + diffuse * 0.75);
    FragColor = vec4(result, 1.0);
}
