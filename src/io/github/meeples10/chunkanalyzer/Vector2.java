package io.github.meeples10.chunkanalyzer;

public class Vector2 {
    public int x;
    public int z;

    public Vector2(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public boolean equals(Vector2 other) {
        return other.x == x && other.z == z;
    }

    public boolean equals(int x, int z) {
        return x == this.x && z == this.z;
    }
}