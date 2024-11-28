package com.example.cpr;


import android.util.Log;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.ArrayList;
import java.util.List;
public class CPRAnalyser {
    // Constants for compression detection
    private static final float MIN_COMPRESSION_DISTANCE = 0.15f;
    private static final float MAX_COMPRESSION_DISTANCE = 0.30f;
    private static final long MIN_COMPRESSION_INTERVAL = 300;
    private static final int RATE_WINDOW_SIZE = 10;

    // constant for hand position detection to check CPR is being performed
    private static final float MAX_HAND_SEPARATION = 0.1f; // Maximum allowed distance between hands

    // State tracking
    private final List<Long> compressionTimestamps = new ArrayList<>();
    private float lastWristPosition = 0f;
    private long lastCompressionTime = 0;
    private boolean isInCompressionDown = false;
    private float restingWristPosition = -1f;
    private boolean handsInPosition = false;

    public CPRMetrics analyzePose(Pose pose) {
        if (pose == null || pose.getAllPoseLandmarks().isEmpty()) {
            return new CPRMetrics(0, 0, false, false);
        }

        // Get relevant landmarks
        PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);

        if (leftWrist == null || rightWrist == null || leftShoulder == null || rightShoulder == null) {
            return new CPRMetrics(0, 0, false, false);
        }

        // Check if hands are in proper position
        float handSeparation = calculateHandSeparation(leftWrist, rightWrist);
        handsInPosition = handSeparation <= MAX_HAND_SEPARATION;

        Log.d("CPRDebug", String.format("Hand separation: %.3f, Hands in position: %b",
                handSeparation, handsInPosition));

        // If hands aren't in position, return early with no compression
        if (!handsInPosition) {
            resetMeasurements();
            return new CPRMetrics(0, 0, false, false);
        }

        // Calculate average positions
        float currentWristY = (leftWrist.getPosition().y + rightWrist.getPosition().y) / 2;
        float shoulderY = (leftShoulder.getPosition().y + rightShoulder.getPosition().y) / 2;

        // Initialise resting position if not set
        if (restingWristPosition < 0) {
            restingWristPosition = currentWristY;
            return new CPRMetrics(0, 0, false, true);
        }

        // Calculate vertical distance from resting position
        float compressionDepth = Math.abs(currentWristY - restingWristPosition);

        // Get current time
        long currentTime = System.currentTimeMillis();

        // Detect compression phases
        if (!isInCompressionDown &&
                compressionDepth > MIN_COMPRESSION_DISTANCE &&
                currentWristY > lastWristPosition &&
                (currentTime - lastCompressionTime) > MIN_COMPRESSION_INTERVAL) {

            isInCompressionDown = true;

        } else if (isInCompressionDown && currentWristY < lastWristPosition &&
                compressionDepth < MIN_COMPRESSION_DISTANCE) {

            isInCompressionDown = false;
            lastCompressionTime = currentTime;
            compressionTimestamps.add(currentTime);
            if (compressionTimestamps.size() > RATE_WINDOW_SIZE) {
                compressionTimestamps.remove(0);
            }
        }

        lastWristPosition = currentWristY;

        float rate = calculateCompressionRate();
        boolean isGoodTechnique = evaluateTechnique(compressionDepth, rate);

        return new CPRMetrics(compressionDepth, rate, isGoodTechnique, true);
    }

    private float calculateHandSeparation(PoseLandmark leftWrist, PoseLandmark rightWrist) {
        float dx = leftWrist.getPosition().x - rightWrist.getPosition().x;
        float dy = leftWrist.getPosition().y - rightWrist.getPosition().y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float calculateCompressionRate() {
        if (compressionTimestamps.size() < 2) {
            return 0f;
        }

        long timeWindow = compressionTimestamps.get(compressionTimestamps.size() - 1) -
                compressionTimestamps.get(0);
        return (compressionTimestamps.size() - 1) * 60000f / timeWindow;
    }

    private boolean evaluateTechnique(float depth, float rate) {
        boolean depthGood = depth >= MIN_COMPRESSION_DISTANCE && depth <= MAX_COMPRESSION_DISTANCE;
        boolean rateGood = rate >= 100 && rate <= 120;
        return depthGood && rateGood && handsInPosition;
    }

    private void resetMeasurements() {
        compressionTimestamps.clear();
        lastWristPosition = 0f;
        lastCompressionTime = 0;
        isInCompressionDown = false;
        restingWristPosition = -1f;
    }

    public void reset() {
        resetMeasurements();
        handsInPosition = false;
    }
}