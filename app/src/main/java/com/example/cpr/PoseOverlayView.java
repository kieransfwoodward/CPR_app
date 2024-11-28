package com.example.cpr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

public class PoseOverlayView extends View {
    private Pose currentPose;
    private final Paint landmarkPaint;
    private final Paint linePaint;
    private int viewWidth;
    private int viewHeight;
    private float scaleFactor = 1.0f;

    public PoseOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Setup paint for landmarks
        landmarkPaint = new Paint();
        landmarkPaint.setColor(Color.RED);
        landmarkPaint.setStrokeWidth(10f);
        landmarkPaint.setStyle(Paint.Style.FILL);

        // Setup paint for connecting lines
        linePaint = new Paint();
        linePaint.setColor(Color.GREEN);
        linePaint.setStrokeWidth(5f);
        linePaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
    }

    public void updatePose(Pose pose) {
        currentPose = pose;
        invalidate(); // Trigger redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (currentPose == null) return;

        // Draw relevant landmarks and connections for CPR
        // Left shoulder to right shoulder
        PoseLandmark leftShoulder = currentPose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark rightShoulder = currentPose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark leftWrist = currentPose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        PoseLandmark rightWrist = currentPose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark leftElbow = currentPose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
        PoseLandmark rightElbow = currentPose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);

        if (leftShoulder != null && rightShoulder != null) {
            drawLine(canvas, leftShoulder, rightShoulder);
        }

        // Draw arms
        if (leftShoulder != null && leftElbow != null) {
            drawLine(canvas, leftShoulder, leftElbow);
        }
        if (leftElbow != null && leftWrist != null) {
            drawLine(canvas, leftElbow, leftWrist);
        }
        if (rightShoulder != null && rightElbow != null) {
            drawLine(canvas, rightShoulder, rightElbow);
        }
        if (rightElbow != null && rightWrist != null) {
            drawLine(canvas, rightElbow, rightWrist);
        }

        // Draw all landmarks
        drawLandmark(canvas, leftShoulder);
        drawLandmark(canvas, rightShoulder);
        drawLandmark(canvas, leftElbow);
        drawLandmark(canvas, rightElbow);
        drawLandmark(canvas, leftWrist);
        drawLandmark(canvas, rightWrist);

        // Draw compression reference line
        if (leftWrist != null && rightWrist != null) {
            float avgWristY = (leftWrist.getPosition().y + rightWrist.getPosition().y) / 2;
            float avgShoulderY = (leftShoulder.getPosition().y + rightShoulder.getPosition().y) / 2;

            // Draw horizontal lines at wrist and shoulder level
            Paint referencePaint = new Paint(linePaint);
            referencePaint.setColor(Color.YELLOW);
            referencePaint.setStrokeWidth(2f);
            referencePaint.setStyle(Paint.Style.STROKE);

            canvas.drawLine(0, translateY(avgWristY), viewWidth, translateY(avgWristY), referencePaint);
            canvas.drawLine(0, translateY(avgShoulderY), viewWidth, translateY(avgShoulderY), referencePaint);
        }
    }

    private void drawLandmark(Canvas canvas, PoseLandmark landmark) {
        if (landmark == null) return;
        PointF point = landmark.getPosition();
        canvas.drawCircle(
                translateX(point.x),
                translateY(point.y),
                8f,
                landmarkPaint
        );
    }

    private void drawLine(Canvas canvas, PoseLandmark start, PoseLandmark end) {
        if (start == null || end == null) return;
        canvas.drawLine(
                translateX(start.getPosition().x),
                translateY(start.getPosition().y),
                translateX(end.getPosition().x),
                translateY(end.getPosition().y),
                linePaint
        );
    }

    private float translateX(float x) {
        // Convert ML Kit coordinate to view coordinate
        return x * viewWidth;
    }

    private float translateY(float y) {
        // Convert ML Kit coordinate to view coordinate
        return y * viewHeight;
    }
}