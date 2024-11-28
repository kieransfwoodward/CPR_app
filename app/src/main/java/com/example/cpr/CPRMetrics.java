package com.example.cpr;

public class CPRMetrics {
    private final float depth;
    private final float rate;
    private final boolean goodTechnique;
    private final boolean handsInPosition;

    public CPRMetrics(float depth, float rate, boolean goodTechnique, boolean handsInPosition) {
        this.depth = depth;
        this.rate = rate;
        this.goodTechnique = goodTechnique;
        this.handsInPosition = handsInPosition;
    }

    public float getDepth() { return depth; }
    public float getRate() { return rate; }
    public boolean isGoodTechnique() { return goodTechnique; }
    public boolean areHandsInPosition() { return handsInPosition; }
}