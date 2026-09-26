package fr.minlego.redstonevisualizer.render;

import net.minecraft.client.render.VertexConsumer;

/** Multiplies model vertex alpha while preserving tint, light and texture data. */
public final class AlphaVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int opacity;
    private final boolean colorOnVertex;

    public AlphaVertexConsumer(VertexConsumer delegate, int opacity) {
        this(delegate, opacity, false);
    }

    public AlphaVertexConsumer(VertexConsumer delegate, int opacity, boolean colorOnVertex) {
        this.delegate = delegate;
        this.opacity = opacity;
        this.colorOnVertex = colorOnVertex;
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        delegate.vertex(x, y, z);
        if (colorOnVertex) {
            delegate.color(255, 255, 255, opacity);
        }
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color(red, green, blue, (alpha * opacity + 127) / 255);
        return this;
    }

    @Override
    public VertexConsumer color(int color) {
        return color((color >>> 16) & 255, (color >>> 8) & 255,
                color & 255, (color >>> 24) & 255);
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        delegate.texture(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        delegate.overlay(u, v);
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        delegate.light(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer lineWidth(float width) {
        delegate.lineWidth(width);
        return this;
    }
}
