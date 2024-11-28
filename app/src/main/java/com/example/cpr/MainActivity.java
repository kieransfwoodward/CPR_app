package com.example.cpr;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import android.widget.TextView;

import java.util.concurrent.ExecutionException;

@ExperimentalGetImage public class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private TextView metricsTextView;
    private PoseDetector poseDetector;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private CPRAnalyser cprAnalyzer;

    private GraphicOverlay graphicOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.preview_view);
        metricsTextView = findViewById(R.id.metrics_text_view);
        graphicOverlay = findViewById(R.id.graphic_overlay);


        // Initialize pose detector
        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
//                .setPerformanceMode(PoseDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();
        poseDetector = PoseDetection.getClient(options);

        cprAnalyzer = new CPRAnalyser();

        // Check and request camera permissions
        if (!checkPermissions()) {
            requestPermissions();
        } else {
            startCamera();
        }
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start camera
                startCamera();
            } else {
                // Permission denied
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Camera Permission Required")
                .setMessage("This app needs camera access to detect CPR movements. ")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    // Open app settings
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Close the app or handle accordingly
                    finish();
                })
                .setCancelable(false)
                .show();
    }


    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetRotation(previewView.getDisplay().getRotation())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
            int rotationDegrees = image.getImageInfo().getRotationDegrees();

            // Update overlay dimensions
            if (graphicOverlay != null) {
                graphicOverlay.setImageSourceInfo(
                        image.getWidth(), image.getHeight());
            }

            InputImage inputImage = InputImage.fromMediaImage(
                    image.getImage(), rotationDegrees);

            poseDetector.process(inputImage)
                    .addOnSuccessListener(pose -> {
                        // Process metrics
                        CPRMetrics metrics = cprAnalyzer.analyzePose(pose);
                        updateMetricsDisplay(metrics);

                        // Update graphics
                        graphicOverlay.clear();
                        graphicOverlay.add(new PoseGraphic(graphicOverlay, pose));
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CPRActivity", "Pose detection failed", e);
                    })
                    .addOnCompleteListener(result -> {
                        image.close();
                    });
        });

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private void updateMetricsDisplay(CPRMetrics metrics) {
        runOnUiThread(() -> {
            String displayText = String.format(
                    "Compression Depth: %.2f\n" +
                            "Rate: %.1f compressions/min\n" +
                            "Status: %s",
                    metrics.getDepth(),
                    metrics.getRate(),
                    metrics.isGoodTechnique() ? "Good" : "Adjust Technique"
            );
            metricsTextView.setText(displayText);
        });
    }
}
