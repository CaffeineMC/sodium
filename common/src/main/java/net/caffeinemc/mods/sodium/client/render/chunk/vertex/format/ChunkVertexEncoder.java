package net.caffeinemc.mods.sodium.client.render.chunk.vertex.format;

public interface ChunkVertexEncoder {
    long write(long ptr, int materialBits, Vertex[] vertices, int sectionIndex);

    class Vertex {
        public float x;
        public float y;
        public float z;
        public int color;
        public float ao;
        public float u;
        public float v;
        public int light;

        public static Vertex[] uninitializedQuad() {
            Vertex[] vertices = new Vertex[4];

            for (int i = 0; i < 4; i++) {
                vertices[i] = new Vertex();
            }

            return vertices;
        }

        public static void copyVertexTo(Vertex from, Vertex to) {
            to.x = from.x;
            to.y = from.y;
            to.z = from.z;
            to.color = from.color;
            to.ao = from.ao;
            to.u = from.u;
            to.v = from.v;
            to.light = from.light;
        }

        public static void writeVertex(ChunkVertexEncoder.Vertex targetA, float newX, float newY, float newZ, int newColor, float newAo, float newU, float newV, int newLight) {
            targetA.x = newX;
            targetA.y = newY;
            targetA.z = newZ;
            targetA.color = newColor;
            targetA.ao = newAo;
            targetA.u = newU;
            targetA.v = newV;
            targetA.light = newLight;
        }
    }
}
