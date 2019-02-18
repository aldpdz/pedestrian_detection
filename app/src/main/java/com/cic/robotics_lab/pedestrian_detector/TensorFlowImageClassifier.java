package com.cic.robotics_lab.pedestrian_detector;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TensorFlowImageClassifier implements Classifier {

    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;
    private Interpreter.Options tfliteOptions;

    private Interpreter interpreter;
    private int inputSize;
    public String result_string = "";

    private TensorFlowImageClassifier() {

    }

    static Classifier create(AssetManager assetManager,
                             String modelPath,
                             String labelPath,
                             int inputSize) throws IOException {

        // Initialize interpreter with GPU delegate
//        GpuDelegate delegate = new GpuDelegate();

        TensorFlowImageClassifier classifier = new TensorFlowImageClassifier();
        MappedByteBuffer tfliteModel = classifier.loadModelFile(assetManager, modelPath);

//        classifier.tfliteOptions = new Interpreter.Options();
//        Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);
        classifier.interpreter = new Interpreter(tfliteModel, classifier.tfliteOptions);
//        classifier.interpreter = new Interpreter(tfliteModel, options);
        classifier.inputSize = inputSize;

        return classifier;
    }

    @Override
    public float[][][] recognizeImage(Bitmap bitmap, Context context) {

        // Ensure a valid EGL rendering context.
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(bitmap);
//        float[][][] result = new float[1][8732][33]; 20 classes
        float[][][] result = new float[1][8732][14]; // 2 classes with vgg13
        interpreter.run(byteBuffer, result);
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

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BATCH_SIZE * inputSize  * inputSize * PIXEL_SIZE * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                final int val = intValues[pixel++];
                // Set order as they were trained [2, 1, 0]
                // Subtract mean [123, 117, 104]
                byteBuffer.putFloat(((val) & 0xFF) - 104); // channel 2
                byteBuffer.putFloat(((val >> 8) & 0xFF) - 117); // channel 1
                byteBuffer.putFloat(((val >> 16) & 0xFF) - 123); // channel 0
            }
        }
        return byteBuffer;
    }

    public String getResult_string(){
        return result_string;
    }

}
