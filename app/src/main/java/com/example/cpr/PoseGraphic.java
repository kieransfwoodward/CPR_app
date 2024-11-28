package com.example.cpr;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;
import java.util.List;

class PoseGraphic extends GraphicOverlay.Graphic {
    private static final float DOT_RADIUS = 8.0f;
    private static final float IN_FRAME_LIKELIHOOD_TEXT_SIZE = 30.0f;
    private static final float STROKE_WIDTH = 10.0f;

    private final Pose pose;
    private final Paint leftPaint;
    private final Paint rightPaint;
    private final Paint whitePaint;

    PoseGraphic(GraphicOverlay overlay, Pose pose) {
        super(overlay);
        this.pose = pose;

        whitePaint = new Paint();
        whitePaint.setStrokeWidth(STROKE_WIDTH);
        whitePaint.setColor(Color.WHITE);
        whitePaint.setTextSize(IN_FRAME_LIKELIHOOD_TEXT_SIZE);

        leftPaint = new Paint();
        leftPaint.setStrokeWidth(STROKE_WIDTH);
        leftPaint.setColor(Color.GREEN);

        rightPaint = new Paint();
        rightPaint.setStrokeWidth(STROKE_WIDTH);
        rightPaint.setColor(Color.YELLOW);
    }

    @Override
    public void draw(Canvas canvas) {
        List<PoseLandmark> landmarks = pose.getAllPoseLandmarks();
        if (landmarks.isEmpty()) {
            return;
        }

        // Draw all the points
        for (PoseLandmark landmark : landmarks) {
            float x = translateX(landmark.getPosition().x);
            float y = translateY(landmark.getPosition().y);
            canvas.drawCircle(x, y, DOT_RADIUS, whitePaint);
        }

        // Draw lines between landmarks
        // Left body
        drawLine(canvas, landmarks, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, leftPaint);
        drawLine(canvas, landmarks, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, leftPaint);

        // Right body
        drawLine(canvas, landmarks, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, rightPaint);
        drawLine(canvas, landmarks, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, rightPaint);

        // Body center
        drawLine(canvas, landmarks, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER, whitePaint);
    }

    private void drawLine(Canvas canvas, List<PoseLandmark> landmarks, int start, int end, Paint paint) {
        PoseLandmark startLandmark = pose.getPoseLandmark(start);
        PoseLandmark endLandmark = pose.getPoseLandmark(end);
        if (startLandmark != null && endLandmark != null) {
            canvas.drawLine(
                    translateX(startLandmark.getPosition().x),
                    translateY(startLandmark.getPosition().y),
                    translateX(endLandmark.getPosition().x),
                    translateY(endLandmark.getPosition().y),
                    paint
            );
        }
    }
}