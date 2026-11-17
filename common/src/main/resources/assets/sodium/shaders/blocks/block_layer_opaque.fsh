#version 460 core

#include <sodium:globals.glsl>
#include <sodium:fog.glsl>
#include <sodium:chunk_material.glsl>
#include <minecraft:oit.glsl>

layout(location = 0) in vec4 v_Color; // The interpolated vertex color
layout(location = 1) in vec2 v_TexCoord; // The interpolated block texture coordinates

#ifdef USE_FOG
layout(location = 2) in vec2 v_FragDistance; // The fragment's distance from the camera (cylindrical and spherical)
layout(location = 3) in float v_fadeFactor;
#endif

#ifdef IMPROVED_FOG
uniform sampler2D SkySampler;
#endif

#ifdef CLOUDS_IN_FOG
uniform sampler2D CloudsSampler;
uniform sampler2D CloudsDepthSampler;
#endif

#ifdef CHECK_WATER_MASK
uniform sampler2D WaterMaskSampler;
#endif

const float ENVIRONMENTAL_BACKGROUND_BLEND_START = 0.9;

#ifdef IMPROVED_FOG
vec3 sampleBackgroundColor(ivec2 pixelCoords) {
    vec3 skyColor = texelFetch(SkySampler, pixelCoords, 0).rgb;
    #ifdef CLOUDS_IN_FOG
    vec4 cloudColor = texelFetch(CloudsSampler, pixelCoords, 0);
    float cloudDepth = texelFetch(CloudsDepthSampler, pixelCoords, 0).r;
    float cloudsBehind = step(cloudDepth, gl_FragCoord.z);
    return mix(skyColor, cloudColor.rgb / (cloudColor.a > 0.0 ? cloudColor.a : 1.0), cloudColor.a * cloudsBehind);
    #else
    return skyColor;
    #endif
}
#endif

uniform sampler2D u_BlockTex; // The block texture

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor; // The output fragment for the color framebuffer
#endif

#ifndef OIT_ALPHA_ONLY
vec4 calculateFinalColor(vec4 color) {
    float environmentalValue = linear_fog_value(v_FragDistance.y, u_EnvironmentFog.x, u_EnvironmentFog.y);
    float renderDistanceValue = linear_fog_value(v_FragDistance.x, u_RenderFog.x, u_RenderFog.y);

    float environmentalBackgroundValue = smoothstep(ENVIRONMENTAL_BACKGROUND_BLEND_START, 1.0, environmentalValue);

    float fogValue = max(environmentalValue, renderDistanceValue);

    float backgroundFogValue = max(environmentalBackgroundValue, renderDistanceValue);

    if (backgroundFogValue > 0.0 || v_fadeFactor < 1.0) {
        #ifdef IMPROVED_FOG
        vec3 backgroundColor = sampleBackgroundColor(ivec2(gl_FragCoord.xy));
        vec3 targetColor = mix(u_FogColor.rgb, backgroundColor, backgroundFogValue);
        #else
        vec3 backgroundColor = u_FogColor.rgb;
        vec3 targetColor = backgroundColor;
        #endif
        vec3 foggedColor = mix(color.rgb, targetColor, fogValue);
        vec3 foggedFadedColor = mix(backgroundColor, foggedColor, v_fadeFactor);
        color = vec4(foggedFadedColor, color.a);
    } else {
        color.rgb = mix(color.rgb, u_FogColor.rgb, fogValue);
    }
    #ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
    #endif

    return color;
}
#endif

vec4 sampleNearest(sampler2D source, vec2 uv, vec2 pixelSize, vec2 du, vec2 dv, vec2 texelScreenSize) {
    // Convert our UV back up to texel coordinates and find out how far over we are from the center of each pixel
    vec2 uvTexelCoords = uv / pixelSize;
    vec2 texelCenter = round(uvTexelCoords) - 0.5f;
    vec2 texelOffset = uvTexelCoords - texelCenter;

    // Move our offset closer to the texel center based on texel size on screen
    texelOffset = (texelOffset - 0.5f) * pixelSize / texelScreenSize + 0.5f;
    texelOffset = clamp(texelOffset, 0.0f, 1.0f);

    uv = (texelCenter + texelOffset) * pixelSize;
    return textureGrad(source, uv, du, dv);
}

vec4 sampleNearest(sampler2D source, vec2 uv, vec2 pixelSize) {
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);
    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    return sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);
}

// Rotated Grid Super-Sampling
vec4 sampleRGSS(sampler2D source, vec2 uv, vec2 pixelSize) {
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);

    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    float maxTexelSize = max(texelScreenSize.x, texelScreenSize.y);

    float minPixelSize = min(pixelSize.x, pixelSize.y);

    float transitionStart = minPixelSize * 1.0;
    float transitionEnd = minPixelSize * 2.0;
    float blendFactor = smoothstep(transitionStart, transitionEnd, maxTexelSize);

    float duLength = length(du);
    float dvLength = length(dv);
    float minDerivative = min(duLength, dvLength);
    float maxDerivative = max(duLength, dvLength);

    float effectiveDerivative = sqrt(minDerivative * maxDerivative);

    float mipLevelExact = max(0.0, log2(effectiveDerivative / minPixelSize));

    const vec2 offsets[4] = vec2[](
    vec2(0.125, 0.375),
    vec2(-0.125, -0.375),
    vec2(0.375, -0.125),
    vec2(-0.375, 0.125)
    );

    vec4 rgssColor = vec4(0.0);
    for (int i = 0; i < 4; ++i) {
        vec2 sampleUV = uv + offsets[i] * pixelSize;
        rgssColor += textureLod(source, sampleUV, mipLevelExact);
    }
    rgssColor *= 0.25;

    vec4 nearestColor = sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);

    return mix(nearestColor, rgssColor, blendFactor);
}

void main() {
    vec4 color = u_UseRGSS ? sampleRGSS(u_BlockTex, v_TexCoord, u_TexelSize) : sampleNearest(u_BlockTex, v_TexCoord, u_TexelSize);
    color *= v_Color; // Apply per-vertex color modulator

#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
#endif

    #ifdef CHECK_WATER_MASK
    float waterMaskDepth = texelFetch(WaterMaskSampler, ivec2(gl_FragCoord.xy), 0).r;
    if (gl_FragCoord.z <= waterMaskDepth) {
        discard;
    }
    #endif

    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    fragColor = calculateFinalColor(color);
    #endif
}
