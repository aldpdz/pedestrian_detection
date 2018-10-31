package com.cic.robotics_lab.pedestrian_detector;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.ArrayList;

/**
 * Created by aldopedraza on 29/10/18.
 */

public class Utils {
    public static Bitmap drawBoundingBox(ArrayList<float[]> boxes, Bitmap bitmap){
        // Initialize a new Canvas instance
        Canvas canvas = new Canvas(bitmap);

        // Initialize a new Paint instance to draw the Rectangle
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);

        for (int i = 0; i < boxes.size(); i++){
            // Initialize a new Rect object
            Rect rectangle = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());

            canvas.drawRect(boxes.get(i)[1], boxes.get(i)[2], boxes.get(i)[3], boxes.get(i)[4], paint);
        }
        return bitmap;
    }
}
