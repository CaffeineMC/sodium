const uint MATERIAL_USE_MIP_OFFSET = 0u;
const uint MATERIAL_ALPHA_CUTOFF_OFFSET = 1u;

const float[4] ALPHA_CUTOFF = float[4](0.0001, 0.5, 0.5, 1.0);

float _material_alpha_cutoff(uint material) {
    return ALPHA_CUTOFF[(material >> MATERIAL_ALPHA_CUTOFF_OFFSET) & 3u];
}