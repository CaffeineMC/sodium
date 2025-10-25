#version 330 core

#import <sodium:include/fog.glsl>

in vec4 v_Color; // The interpolated vertex color
in vec2 v_TexCoord; // The interpolated block texture coordinates
in vec2 v_FragDistance; // The fragment's distance from the camera (cylindrical and spherical)
in float fadeFactor; // The per-chunk fade

in float v_MaterialAlphaCutoff;

uniform sampler2D u_BlockTex; // The block texture

uniform vec4 u_FogColor; // The color of the shader fog
uniform vec2 u_EnvironmentFog; // The start and end position for environmental fog
uniform vec2 u_RenderFog; // The start and end position for border fog
uniform vec2 u_TexCoordSize;

out vec4 fragColor; // The output fragment for the color framebuffer
vec4 sampleNearest(sampler2D sampler, vec2 uv, vec2 pixelSize) {
    // Figure out how big a texel is on the screen, more or less
	vec2 du = dFdx(uv);
	vec2 dv = dFdy(uv);
	vec2 texelScreenSize = sqrt(du * du + dv * dv);

    // Convert our UV back up to texel coordinates and find out how far over we are from the center of each pixel
	vec2 uvTexelCoords = uv / pixelSize;
	vec2 texelCenter = round(uvTexelCoords) - 0.5f;
	vec2 texelOffset = uvTexelCoords - texelCenter;

    // Move our offset closer to the texel center based on texel size on screen
	texelOffset = (texelOffset - 0.5f) * pixelSize / texelScreenSize + 0.5f;
	texelOffset = clamp(texelOffset, 0.0f, 1.0f);

	uv = (texelCenter + texelOffset) * pixelSize;
	return textureGrad(sampler, uv, du, dv);
}
void main() {
    vec4 diffuseColor = sampleNearest(u_BlockTex, v_TexCoord, u_TexCoordSize);

    // Apply per-vertex color
    diffuseColor *= v_Color;

#ifdef USE_FRAGMENT_DISCARD
    if (diffuseColor.a < v_MaterialAlphaCutoff) {
        discard;
    }
#endif

    fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_EnvironmentFog, u_RenderFog, fadeFactor);
}