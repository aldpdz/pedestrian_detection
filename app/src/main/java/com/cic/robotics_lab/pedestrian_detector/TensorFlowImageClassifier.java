package com.cic.robotics_lab.pedestrian_detector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class TensorFlowImageClassifier implements Classifier {

    private static final int MAX_RESULTS = 3;
    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;
    private static final float THRESHOLD = 0.1f;
    private Interpreter.Options tfliteOptions;

    private Interpreter interpreter;
    private int inputSize;
//    private List<String> labelList;
    public String result_string = "";

    private TensorFlowImageClassifier() {

    }

    static Classifier create(AssetManager assetManager,
                             String modelPath,
                             String labelPath,
                             int inputSize) throws IOException {

        TensorFlowImageClassifier classifier = new TensorFlowImageClassifier();
        MappedByteBuffer tfliteModel = classifier.loadModelFile(assetManager, modelPath);
        classifier.tfliteOptions = new Interpreter.Options();
        classifier.interpreter = new Interpreter(tfliteModel, classifier.tfliteOptions);
//        classifier.labelList = classifier.loadLabelList(assetManager, labelPath);
        classifier.inputSize = inputSize;

        return classifier;
    }

    @Override
    public float[][][] recognizeImage(Bitmap bitmap, Context context) {
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(bitmap);
        float[][][] result = new float[1][8732][33];
        interpreter.run(byteBuffer, result);
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < result[0].length; i++){
            for(int j = 0; j < result[0][0].length; j++){
                builder.append( result[0][i][j]);
                if(j < result[0][0].length - 1){
                    builder.append(",");
                }
            }
            if(i < result[0].length - 1){
                builder.append(";");
            }
        }
        result_string = builder.toString();
        Log.d("finished", "yes");
        return result;
    }

    @Override
    public void close() {
        interpreter.close();
        interpreter = null;
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BATCH_SIZE * inputSize  * inputSize * PIXEL_SIZE * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat((((val >> 16) & 0xFF)/255.0f));
                byteBuffer.putFloat((((val >> 8) & 0xFF)/255.0f));
                byteBuffer.putFloat((((val) & 0xFF)/255.0f));
            }
        }
        return byteBuffer;
    }

    public String getResult_string(){
        return result_string;
    }

//    @SuppressLint("DefaultLocale")
//    private List<Recognition> getSortedResult(float[][] labelProbArray) {
//
//        PriorityQueue<Recognition> pq =
//                new PriorityQueue<>(
//                        MAX_RESULTS,
//                        new Comparator<Recognition>() {
//                            @Override
//                            public int compare(Recognition lhs, Recognition rhs) {
//                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
//                            }
//                        });
//
//        for (int i = 0; i < labelList.size(); ++i) {
//            float confidence = labelProbArray[0][i];
//            if (confidence > THRESHOLD) {
//                pq.add(new Recognition("" + i,
//                        labelList.size() > i ? labelList.get(i) : "unknown",
//                        confidence));
//            }
//        }
//
//        final ArrayList<Recognition> recognitions = new ArrayList<>();
//        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
//        for (int i = 0; i < recognitionsSize; ++i) {
//            recognitions.add(pq.poll());
//        }
//
//        return recognitions;
//    }

}
