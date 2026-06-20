package acore.aurora.utility.render;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import acore.aurora.features.modules.Module;
import acore.aurora.features.modules.client.HudEditor;

public class GhostRenderer3D {
    private static final float TRAIL_LENGTH = 20.5F;
    private static final double TRAIL_POINT_SPACING = 0.045;
    private static final int MAX_POINTS_PER_TICK = 10;
    public static final double TRAIL_VERTICAL_OFFSET = 0.7;
    private static final float INNER_GLOW_SCALE = 1.8F;
    private static final float OUTER_GLOW_SCALE = 2.6F;
    private static final float INNER_GLOW_ALPHA = 0.42F;
    private static final float OUTER_GLOW_ALPHA = 0.2F;
    private Vec3d prevPosition = Vec3d.ZERO;
    private Vec3d position;
    private Vec3d motion;
    private final List<Vector4f> tail = new ArrayList<>();
    private final float size;
    private final float trailLength;

    public GhostRenderer3D(Vec3d position, Vec3d motion, float size) {
        this(position, motion, size, TRAIL_LENGTH);
    }

    public GhostRenderer3D(Vec3d position, Vec3d motion, float size, float trailLength) {
        this.prevPosition = position;
        this.position = position;
        this.motion = motion;
        this.size = size;
        this.trailLength = trailLength <= 0.0F ? TRAIL_LENGTH : trailLength;
    }

    public void tick() {
        this.prevPosition = this.position;
        this.position = this.position.add(this.motion);
        this.addTrailPoints(this.prevPosition, this.position);
        float fps = Math.max(Module.mc.getCurrentFps(), 5);
        float deltaY = 0.004F / fps * 300.0F;
        float deltaLife = 0.3F / fps * 300.0F;
        Iterator<Vector4f> iterator = this.tail.iterator();
        while (iterator.hasNext()) {
            Vector4f vec = iterator.next();
            vec.set(vec.x(), vec.y() + deltaY, vec.z(), vec.w() - deltaLife);
            if (vec.w() <= 0.0F) {
                iterator.remove();
            }
        }
        double damp = 0.95 / fps * 300.0;
        this.motion = this.motion.multiply(damp, damp, damp);
    }

    private void addTrailPoints(Vec3d from, Vec3d to) {
        double distance = from.distanceTo(to);
        if (distance <= 1.0E-4) {
            this.tail.add(new Vector4f((float)to.x, (float)(to.y + 0.7), (float)to.z, this.trailLength));
        } else {
            int interpolatedSteps = (int)Math.ceil(distance / 0.045) + 1;
            int steps = Math.max(2, Math.min(10, interpolatedSteps));
            for (int step = 1; step <= steps; step++) {
                double delta = (double)step / steps;
                Vec3d point = from.lerp(to, delta);
                this.tail.add(new Vector4f((float)point.x, (float)(point.y + 0.7), (float)point.z, this.trailLength));
            }
        }
    }

    public void render(BufferBuilder buffer, Camera camera) {
        if (this.tail.size() < 2) return;

        java.util.List<Vector4f> pts = this.tail;
        for (int i = 0; i < pts.size() - 1; i++) {
            Vector4f a = pts.get(i);
            Vector4f b = pts.get(i + 1);
            if (a.w() <= 0.0F || b.w() <= 0.0F) continue;

            float progressA = a.w() / this.trailLength;
            float progressB = b.w() / this.trailLength;

            int colorA = Render2DEngine.injectAlpha(HudEditor.getColor(0), MathHelper.clamp((int)(progressA * 255.0F), 0, 255)).getRGB();
            int colorB = Render2DEngine.injectAlpha(HudEditor.getColor(0), MathHelper.clamp((int)(progressB * 255.0F), 0, 255)).getRGB();

            float widthA = this.size * (0.55F + progressA * 0.45F);
            float widthB = this.size * (0.55F + progressB * 0.45F);

            renderRibbonSegment(buffer, camera, a, b, widthA * OUTER_GLOW_SCALE, widthB * OUTER_GLOW_SCALE,
                    colorA, colorB, scaleAlpha((int)(progressA * 255.0F), OUTER_GLOW_ALPHA), scaleAlpha((int)(progressB * 255.0F), OUTER_GLOW_ALPHA));
            renderRibbonSegment(buffer, camera, a, b, widthA * INNER_GLOW_SCALE, widthB * INNER_GLOW_SCALE,
                    colorA, colorB, scaleAlpha((int)(progressA * 255.0F), INNER_GLOW_ALPHA), scaleAlpha((int)(progressB * 255.0F), INNER_GLOW_ALPHA));
            renderRibbonSegment(buffer, camera, a, b, widthA, widthB,
                    colorA, colorB, MathHelper.clamp((int)(progressA * 255.0F), 0, 255), MathHelper.clamp((int)(progressB * 255.0F), 0, 255));
        }
    }

    private static final Matrix4f IDENTITY = new Matrix4f();

    private static void renderRibbonSegment(BufferBuilder buffer, Camera camera, Vector4f a, Vector4f b,
                                              float widthA, float widthB, int colorA, int colorB, int alphaA, int alphaB) {
        if (alphaA <= 0 && alphaB <= 0) return;

        double ax = a.x() - camera.getPos().x;
        double ay = a.y() - camera.getPos().y;
        double az = a.z() - camera.getPos().z;
        double bx = b.x() - camera.getPos().x;
        double by = b.y() - camera.getPos().y;
        double bz = b.z() - camera.getPos().z;

        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double midX = (ax + bx) / 2.0;
        double midY = (ay + by) / 2.0;
        double midZ = (az + bz) / 2.0;

        double nx = midY * dz - midZ * dy;
        double ny = midZ * dx - midX * dz;
        double nz = midX * dy - midY * dx;
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-6) return;
        nx /= len; ny /= len; nz /= len;

        float hwA = widthA / 2.0F;
        float hwB = widthB / 2.0F;

        int rA = colorA >> 16 & 0xFF, gA = colorA >> 8 & 0xFF, bA = colorA & 0xFF;
        int rB = colorB >> 16 & 0xFF, gB = colorB >> 8 & 0xFF, bB = colorB & 0xFF;

        buffer.vertex(IDENTITY, (float)(ax - nx * hwA), (float)(ay - ny * hwA), (float)(az - nz * hwA)).texture(0.0F, 1.0F).color(rA, gA, bA, alphaA);
        buffer.vertex(IDENTITY, (float)(bx - nx * hwB), (float)(by - ny * hwB), (float)(bz - nz * hwB)).texture(0.0F, 0.0F).color(rB, gB, bB, alphaB);
        buffer.vertex(IDENTITY, (float)(bx + nx * hwB), (float)(by + ny * hwB), (float)(bz + nz * hwB)).texture(1.0F, 0.0F).color(rB, gB, bB, alphaB);
        buffer.vertex(IDENTITY, (float)(ax + nx * hwA), (float)(ay + ny * hwA), (float)(az + nz * hwA)).texture(1.0F, 1.0F).color(rA, gA, bA, alphaA);
    }

    private static int scaleAlpha(int alpha, float factor) {
        return MathHelper.clamp((int)(alpha * factor), 0, 255);
    }

    public boolean hasTrail() {
        return !this.tail.isEmpty();
    }

    public Vec3d getPosition() {
        return this.position;
    }

    public void setPosition(Vec3d position) {
        this.prevPosition = position;
        this.position = position;
    }

    public Vec3d getMotion() {
        return this.motion;
    }

    public void setMotion(Vec3d motion) {
        this.motion = motion;
    }
}

    
