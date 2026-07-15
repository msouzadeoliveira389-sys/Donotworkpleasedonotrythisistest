package com.example.driftsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DriftSimulationView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<CarSpec> cars = new ArrayList<>();
    private final ArrayDeque<Skid> skids = new ArrayDeque<>();
    private int selectedCar = 0;
    private boolean rcMode = false;
    private float x = 600, y = 360, speed = 0, heading = 0, bodyAngle = 0, steer = 0, throttle = 0;
    private long lastFrameNanos = System.nanoTime();

    public DriftSimulationView(Context context) {
        super(context);
        setFocusable(true);
        cars.add(new CarSpec("JDM Turbo", 0xffe53935, 520, 0.96f, 1.35f));
        cars.add(new CarSpec("Muscle V8", 0xff3949ab, 650, 0.91f, 1.55f));
        cars.add(new CarSpec("RC Drift Rua", 0xffffb300, 300, 0.83f, 2.15f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now = System.nanoTime();
        float dt = Math.min(0.033f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;
        updatePhysics(dt);
        drawStreet(canvas);
        drawSkids(canvas);
        drawCar(canvas);
        drawHud(canvas);
        invalidate();
    }

    private void updatePhysics(float dt) {
        CarSpec car = cars.get(selectedCar);
        float scale = rcMode ? 0.58f : 1f;
        float desiredGrip = rcMode ? 0.72f : car.grip;
        speed += throttle * car.power * scale * dt;
        speed *= (float) Math.pow(0.985f, dt * 60f);
        speed = clamp(speed, -180 * scale, 780 * scale);
        float driftSlip = Math.abs(steer) * Math.max(90f, Math.abs(speed)) * car.driftBias * (1f - desiredGrip);
        heading += steer * speed * 0.0017f * dt * 60f;
        float targetBody = heading - steer * driftSlip * 0.0028f;
        bodyAngle += (targetBody - bodyAngle) * (0.09f + (1f - desiredGrip) * 0.08f);
        x += Math.cos(bodyAngle) * speed * dt;
        y += Math.sin(bodyAngle) * speed * dt;
        wrapArena();
        if (Math.abs(steer) > 0.22f && Math.abs(speed) > 75f) {
            skids.add(new Skid(x, y, bodyAngle, rcMode));
            while (skids.size() > 180) skids.removeFirst();
        }
    }

    private void wrapArena() {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        if (x < -80) x = w + 80;
        if (x > w + 80) x = -80;
        if (y < -80) y = h + 80;
        if (y > h + 80) y = -80;
    }

    private void drawStreet(Canvas canvas) {
        canvas.drawColor(0xff263238);
        paint.setStrokeWidth(6);
        paint.setColor(0xff455a64);
        for (int i = -200; i < getWidth() + 200; i += 120) canvas.drawLine(i, 0, i + 260, getHeight(), paint);
        paint.setColor(0xfffdd835);
        paint.setStrokeWidth(5);
        for (int i = 0; i < getWidth(); i += 100) canvas.drawLine(i, getHeight() / 2f, i + 45, getHeight() / 2f, paint);
        paint.setColor(0xff1b5e20);
        canvas.drawCircle(getWidth() - 140, 120, 54, paint);
        canvas.drawCircle(getWidth() - 260, getHeight() - 100, 48, paint);
    }

    private void drawSkids(Canvas canvas) {
        int alpha = 25;
        for (Skid skid : skids) {
            paint.setColor(Color.argb(alpha, 8, 8, 8));
            paint.setStrokeWidth(skid.rc ? 3 : 7);
            float dx = (float) Math.cos(skid.angle + Math.PI / 2) * (skid.rc ? 8 : 16);
            float dy = (float) Math.sin(skid.angle + Math.PI / 2) * (skid.rc ? 8 : 16);
            canvas.drawLine(skid.x - dx, skid.y - dy, skid.x + dx, skid.y + dy, paint);
            alpha = Math.min(185, alpha + 1);
        }
    }

    private void drawCar(Canvas canvas) {
        CarSpec car = cars.get(selectedCar);
        float length = rcMode ? 58 : 118;
        float width = rcMode ? 30 : 58;
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate((float) Math.toDegrees(bodyAngle));
        paint.setColor(0xff111111);
        canvas.drawRoundRect(new RectF(-length / 2 - 4, -width / 2 - 4, length / 2 + 4, width / 2 + 4), 14, 14, paint);
        paint.setColor(car.color);
        canvas.drawRoundRect(new RectF(-length / 2, -width / 2, length / 2, width / 2), 12, 12, paint);
        paint.setColor(0xddbbdefb);
        canvas.drawRoundRect(new RectF(-length * 0.12f, -width * 0.38f, length * 0.30f, width * 0.38f), 8, 8, paint);
        paint.setColor(0xfffff59d);
        canvas.drawCircle(length / 2 - 8, -width / 4, 5, paint);
        canvas.drawCircle(length / 2 - 8, width / 4, 5, paint);
        canvas.restore();
    }

    private void drawHud(Canvas canvas) {
        paint.setTextSize(28);
        paint.setColor(Color.WHITE);
        CarSpec car = cars.get(selectedCar);
        canvas.drawText("Drift Realista - toque esquerda/direita para esterçar, topo acelera", 28, 42, paint);
        canvas.drawText(String.format(Locale.US, "Carro: %s | Modo: %s | Velocidade: %.0f km/h", car.name, rcMode ? "RC drift na rua" : "carro real", Math.abs(speed) * 0.18f), 28, 78, paint);
        drawButton(canvas, 28, getHeight() - 74, 230, getHeight() - 20, "Trocar carro");
        drawButton(canvas, 276, getHeight() - 74, 500, getHeight() - 20, rcMode ? "Modo real" : "Modo RC");
    }

    private void drawButton(Canvas canvas, float left, float top, float right, float bottom, String label) {
        paint.setColor(0xcc000000);
        canvas.drawRoundRect(new RectF(left, top, right, bottom), 12, 12, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(24);
        canvas.drawText(label, left + 22, bottom - 18, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float px = event.getX();
        float py = event.getY();
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (py > getHeight() - 90 && px < 250) selectedCar = (selectedCar + 1) % cars.size();
            if (py > getHeight() - 90 && px > 260 && px < 520) rcMode = !rcMode;
            steer = 0;
            throttle = 0;
            return true;
        }
        throttle = py < getHeight() * 0.55f ? 1f : -0.35f;
        steer = px < getWidth() * 0.45f ? -1f : (px > getWidth() * 0.55f ? 1f : 0f);
        return true;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class CarSpec {
        final String name;
        final int color;
        final float power;
        final float grip;
        final float driftBias;

        CarSpec(String name, int color, float power, float grip, float driftBias) {
            this.name = name;
            this.color = color;
            this.power = power;
            this.grip = grip;
            this.driftBias = driftBias;
        }
    }

    private static class Skid {
        final float x, y, angle;
        final boolean rc;

        Skid(float x, float y, float angle, boolean rc) {
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.rc = rc;
        }
    }
}
