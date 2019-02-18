package com.cic.robotics_lab.pedestrian_detector;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by aldopedraza on 29/10/18.
 * Decode detections
 * Perform non-maxima-supression
 * Filter just the person class
 */

public class Decode_Detections {
    private float [][][] rawDetections;
    private int width;
    private int height;
    private float threshold;

    public Decode_Detections(float [][][] rawDetections, float threshold, int width, int height){
        this.rawDetections = rawDetections;
        this.threshold = threshold;
        this.width = width;
        this.height = height;
    }


    /***
     * Perform threshold over the person confidence
     * @return ArrayList of float arrays (confidence + 12)
     */
    public ArrayList<float[]> getDecode_detections(){
        ArrayList<float[]> thresholdDetections = new ArrayList<>();

        // loop over the prediction
        for(int i = 0; i < rawDetections[0].length; i++){
            // perform threshold over the person item
            // int id_class = 15
            int id_class = 1;
            if (rawDetections[0][i][id_class] > threshold){
                float [] values = new float[5];
                float [] detection = rawDetections[0][i];
                values[0] = detection[id_class]; // confidence value

                float cx = .0f;
                float cy = .0f;
                float w = .0f;
                float h = .0f;

                // convert anchor box offsets to image offsets
                // configuration 20 classes
                if(id_class == 15){
                    cx = detection[21] * detection[29] * detection[27] + detection[25];
                    cy = detection[22] * detection[30] * detection[28] + detection[26];
                    w = (float) Math.exp((double)(detection[23] * detection[31])) * detection[27];
                    h = (float) Math.exp((double)(detection[24] * detection[32])) * detection[28];
                }else if(id_class == 1){
                    cx = detection[2] * detection[10] * detection[8] + detection[6];
                    cy = detection[3] * detection[11] * detection[9] + detection[7];
                    w = (float) Math.exp((double)(detection[4] * detection[12])) * detection[8];
                    h = (float) Math.exp((double)(detection[5] * detection[13])) * detection[9];
                }

                // convert centroids to corners
                float xmin = cx - 0.5f * w;
                float ymin = cy - 0.5f * h;
                float xmax = cx + 0.5f * w;
                float ymax = cy + 0.5f * h;

                // convert from relative coordinates to absolute coordinates
//                xmin = xmin * width;
//                ymin = ymin * height;
//                xmax = xmax * width;
//                ymax = ymax * height;

                // convert right, down coordinates to width and height
                // iou function needs (x, y, width, height)
                values[1] = xmin;
                values[2] = ymin;
                values[3] = xmax - xmin;
                values[4] = ymax - ymin;

                thresholdDetections.add(values);
            }
        }

        return nonMaxSuppression(thresholdDetections, 0.45f);
    }

    /***
     * Apply non-maximum-suppression
     * @param boxes - List of boxes
     * @param overlapThresh - Threshold
     * @return List of boxes after applying nms.
     */
    public ArrayList<float[]> nonMaxSuppression(ArrayList<float[]> boxes, float overlapThresh){
        // If there are no boxes, return an empty arraylist
        if (boxes.isEmpty())
            return new ArrayList<>();

        // Initialize the list of picked indexes
        ArrayList<float[]> pick = new ArrayList<>();

        // Confidence array
        ArrayList<Float> confidence = new ArrayList<>();

        // Determine the area of each bounding box
        for(float[] box: boxes){
            confidence.add(box[0]);
        }

        // Index array
        ArrayList<Integer> idxs = new ArrayList<>();

        ArrayList<Float> aux = new ArrayList<>(confidence);

        // Sorted
        Collections.sort(aux);

        // Get all index values
        for(int i = 0; i < aux.size(); i++){
            idxs.add(confidence.indexOf(aux.get(i)));
        }

        // Keep looping while some indexes still remain in the indexes list
        while (!idxs.isEmpty()){

            // grab the last index in the indexes list
            int last = idxs.size() - 1;
            int i = idxs.get(last);
            // add box to the picked boxes
            pick.add(boxes.get(i));
            ArrayList<Integer> supress = new ArrayList<>();

            // Loop over all indexes in the indexes list
            for( int pos = 0; pos < last; pos++){
                // grab the current index
                int j = idxs.get(pos);

                float iou = iou(boxes.get(i), boxes.get(j));

                // if there is sufficient overlap, suppress the current bounding box
                if( iou > overlapThresh){
                    supress.add(pos);
                }
            }
            // delete all indexes from the index list that are in the suppression list
            supress.add(last);
            for(int ind_s = supress.size()-1; ind_s >= 0; ind_s--){
                int remove_v = supress.get(ind_s);
                idxs.remove(remove_v);
            }
        }
        return pick;
    }

    /***
     * Get the intersection over union from two boxes
     * @param box1 - Box one
     * @param box2 - Box two
     * @return float - Intersection over union
     */
    private float iou(float[] box1, float[] box2){
        // Determine the (x, y)-coordinates of the intersection rectangle
        Float xA = Math.max(box1[1], box2[1]);
        Float yA = Math.max(box1[2], box2[2]);
        Float xB = Math.min(box1[3] + box1[1], box2[3] + box2[1]);
        Float yB = Math.min(box1[4] + box1[2], box2[4] + box2[2]);

        // Compute the area of intersection rectangle
        Float interArea = Math.max(0, xB - xA) * Math.max(0, yB - yA);

        // Compute the area of each rectangle
        Float boxAarea = box1[3] * box1[4];
        Float boxBarea = box2[3] * box2[4];

        // compute the intersection over union by taking the intersection
        // area and dividing it by the sum of prediction + ground truth areas - the intersection area
        return interArea / (boxAarea + boxBarea - interArea);
    }
}
