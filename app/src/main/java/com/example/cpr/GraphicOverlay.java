package com.example.cpr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class GraphicOverlay extends View {
    private final Object lock = new Object();
    private final List<Graphic> graphics = new ArrayList<>();
    private final Matrix transformationMatrix = new Matrix();

    private int imageWidth;
    private int imageHeight;
    private float scaleFactor = 1.0f;

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public static abstract class Graphic {
        private GraphicOverlay overlay;

        public Graphic(GraphicOverlay overlay) {
            this.overlay = overlay;
        }

        public abstract void draw(Canvas canvas);

        public float scaleX(float horizontal) {
            return horizontal * overlay.scaleFactor;
        }

        public float scaleY(float vertical) {
            return vertical * overlay.scaleFactor;
        }

        public float translateX(float x) {
            return scaleX(x);
        }

        public float translateY(float y) {
            return scaleY(y);
        }

        public void postInvalidate() {
            overlay.postInvalidate();
        }
    }

    public void clear() {
        synchronized (lock) {
            graphics.clear();
        }
        postInvalidate();
    }

    public void add(Graphic graphic) {
        synchronized (lock) {
            graphics.add(graphic);
        }
    }

    public void remove(Graphic graphic) {
        synchronized (lock) {
            graphics.remove(graphic);
        }
        postInvalidate();
    }

    public void setImageSourceInfo(int imageWidth, int imageHeight) {
        synchronized (lock) {
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (lock) {
            if (imageWidth > 0 && imageHeight > 0) {
                scaleFactor = Math.min(
                        (float) getWidth() / imageWidth,
                        (float) getHeight() / imageHeight
                );
            }

            for (Graphic graphic : graphics) {
                graphic.draw(canvas);
            }
        }
    }
}