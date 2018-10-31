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
    private ArrayList<float[]> thresholding_det;
    private int img_size = 300;

    public Decode_Detections(float [][][] rawDetections){
        this.rawDetections = rawDetections;
        thresholding_det = getPersonThreshold(this.rawDetections, 0.5f);
    }


    /***
     * Perform threshold over the person confidence
     * @param rawDetections - raw detections from the model
     * @param threshold - threshold applied
     * @return ArrayList of float arrays (confidence + 12)
     */
    public ArrayList<float[]> getPersonThreshold(float[][][] rawDetections, float threshold){
        ArrayList<float[]> thresholdDetections = new ArrayList<>();

        // loop over the prediction
        for(int i = 0; i < rawDetections[0].length; i++){
            // perform threshold over the person item
            if (rawDetections[0][i][15] > threshold){
                float [] values = new float[5];
                float [] detection = rawDetections[0][i];
                values[0] = detection[15]; // confidence value

                // convert anchor box offsets to image offsets
                float cx = detection[21] * detection[29] * detection[27] + detection[25];
                float cy = detection[22] * detection[30] * detection[28] + detection[26];
                float w = (float) Math.exp((double)(detection[23] * detection[31])) * detection[27];
                float h = (float) Math.exp((double)(detection[24] * detection[32])) * detection[28];

                // convert centroids to corners
                float xmin = cx - 0.5f * w;
                float ymin = cy - 0.5f * h;
                float xmax = cx + 0.5f * w;
                float ymax = cy + 0.5f * h;

                // convert from relative coordinates to absolute coordinates
                xmin = xmin * img_size;
                ymin = ymin * img_size;
                xmax = xmax * img_size;
                ymax = ymax * img_size;

                values[1] = xmin;
                values[2] = ymin;
                values[3] = xmax;
                values[4] = ymax;

                thresholdDetections.add(values);
            }
        }

        return nonMaxSuppression(thresholdDetections, 0.45f);
    }

    public ArrayList<float[]> nonMaxSuppression(ArrayList<float[]> boxes, float overlapThresh){
        // If there are no boxes, return an empty arraylist
        if (boxes.isEmpty())
            return new ArrayList<>();

        // Initialize the list of picked indexes
        ArrayList<float[]> pick = new ArrayList<>();

        // Confidence array
        ArrayList<Float> confidence = new ArrayList<>();

        // Area arrayList
        ArrayList<Float> area = new ArrayList<>();

        // Determine the area of each bounding box
        for(float[] box: boxes){
            confidence.add(box[0]);
            area.add((box[3] - box[1] + 1) * (box[4] - box[2] + 1));
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
            pick.add(boxes.get(idxs.get(last)));
            ArrayList<Integer> supress = new ArrayList<>();
            supress.add(last);

            // Loop over all indexes in the indexes list
            for( int pos = 0; pos < last; pos++){
                // grab the current index
                int j = idxs.get(pos);

                // find the largest (x, y) coordinates for the start of the bounding box and the
                // smallest (x, y) coordinates for the end of the bounding box
                float xx1 = Math.max(boxes.get(i)[1], boxes.get(j)[1]);
                float yy1 = Math.max(boxes.get(i)[2], boxes.get(j)[2]);
                float xx2 = Math.max(boxes.get(i)[3], boxes.get(j)[3]);
                float yy2 = Math.max(boxes.get(i)[4], boxes.get(j)[4]);

                // compute the width an height of the bounding box
                float w = Math.max(0.0f, xx2 - xx1 + 1);
                float h = Math.max(0.0f, yy2 - yy1 + 1);

                // compute the ratio of overlap between the computed bounding box and the
                // bounding box ain the area list
                float overlap = (w * h) / area.get(j);

                // if there is sufficient overlap, suppress the current bounding box
                if( overlap > overlapThresh){
                    supress.add(pos);
                }
            }
            // delete all indexes from the index list that are in the suppression list
//            for(int ind_s = 0; ind_s < supress.size(); ind_s++){
//                idxs.remove(supress.get(ind_s));
//            }
            idxs.removeAll(supress);
        }
        return pick;
    }
}
