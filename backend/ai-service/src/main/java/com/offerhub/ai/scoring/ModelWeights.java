package com.offerhub.ai.scoring;

import java.util.List;

public class ModelWeights {

    public RecommendationModel recommendationModel;
    public SegmentModel segmentModel;
    public List<String> tariffs;
    public List<String> campaignTypes;
    public List<String> segments;

    public static class RecommendationModel {
        public String modelType;
        public List<String> featureOrder;
        public List<Double> mean;
        public List<Double> std;
        public List<Double> weights;
        public double bias;
        public double trainAccuracy;
        public double testAccuracy;
    }

    public static class SegmentModel {
        public String modelType;
        public List<String> featureOrder;
        public List<TreeNode> nodes;
        public List<String> classes;
        public double trainAccuracy;
        public double testAccuracy;
    }

    public static class TreeNode {
        public boolean isLeaf;
        public Integer featureIndex;
        public Double threshold;
        public Integer left;
        public Integer right;
        public String predictedClass;
    }
}