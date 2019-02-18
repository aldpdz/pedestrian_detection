package com.cic.robotics_lab.pedestrian_detector;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;

/**
 * Created by aldopedraza on 29/10/18.
 * Util class
 */

public class Utils {
    public static Bitmap drawBoundingBox(ArrayList<float[]> boxes, Bitmap bitmap){
        // Initialize a new Canvas instance
        Canvas canvas = new Canvas(bitmap);

        // Initialize a new Paint instance to draw the Rectangle
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float max = Math.max(width, height);

        paint.setStrokeWidth(max / 100);


        for (int i = 0; i < boxes.size(); i++){

            float cornerXMin = boxes.get(i)[1] * width;
            float cornerYMin = boxes.get(i)[2] * height;
            float cornerXMax = (boxes.get(i)[3] + boxes.get(i)[1]) * width;
            float cornerYMax = (boxes.get(i)[4] + boxes.get(i)[2]) * height;

            // Initialize a new Rect object
            // add values because they were removed in Decode function
            canvas.drawRect(cornerXMin,
                    cornerYMin,
                    cornerXMax,
                    cornerYMax,
                    paint);
        }
        return bitmap;
    }
}
