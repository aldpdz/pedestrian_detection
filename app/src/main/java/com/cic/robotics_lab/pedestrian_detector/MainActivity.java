package com.cic.robotics_lab.pedestrian_detector;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static int PICK_IMAGE_REQUEST = 1;
    private static final String MODEL_PATH = "tflite_vgg13_bn.tflite";
    private static final String LABEL_PATH = "labels3.txt";
    private static final int INPUT_SIZE = 300;

    private int SAVE = 12;

    private Classifier classifier;
    private Bitmap bmpOriginal;
    private Executor executor = Executors.newSingleThreadExecutor();
    private TextView textViewResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button choose_btn = findViewById(R.id.choose_image);
        Button detector_btn = findViewById(R.id.detect);
        Button save_btn = findViewById(R.id.save);
        textViewResult = findViewById(R.id.textView_pre);

        setBtnChoose(choose_btn);
        setBtnDetector(detector_btn);
        setBtnSave(save_btn);
        initTensorFlowAndLoadModel();
    }

    private void setBtnChoose(Button button){
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();

                // Show images
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);

                // Always show the chooser
                startActivityForResult(intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
            }
        });
    }

    private void setBtnDetector(Button button){
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long startTime;
                long endTime;
                String rescaleTime;
                String netTime;
                String posProcessTime;

                startTime = System.currentTimeMillis();
                Bitmap bmpDetect = Bitmap.createScaledBitmap(bmpOriginal, INPUT_SIZE, INPUT_SIZE, false);
                endTime = System.currentTimeMillis();
                rescaleTime = String.format("%.4f", (float)(endTime - startTime)/1000);

                startTime = System.currentTimeMillis();
                final float[][][] results = classifier.recognizeImage(bmpDetect, getApplicationContext());
                endTime = System.currentTimeMillis();
                netTime = String.format("%.4f", (float)(endTime - startTime)/1000);

                startTime = System.currentTimeMillis();
                Decode_Detections decode_detections = new Decode_Detections(results, 0.50f, bmpOriginal.getWidth(), bmpOriginal.getHeight());
                ArrayList<float[]> decodeDetections = decode_detections.getDecode_detections();
                endTime = System.currentTimeMillis();
                posProcessTime = String.format( "%.4f", (float)(endTime - startTime)/1000);

                Bitmap new_bitmap = Utils.drawBoundingBox(decodeDetections, bmpOriginal.copy(Bitmap.Config.ARGB_8888, true));
                ImageView imageView = findViewById(R.id.imageView);
                imageView.setImageBitmap(new_bitmap);

                String time = "Rescale time: " + rescaleTime + "sec. \nNetwork time: " + netTime + " sec. \nPosprocessing time: " + posProcessTime + " sec.";
                textViewResult.setText(time);
            }
        });
    }

    private void setBtnSave(Button button){
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveResult();
            }
        });
    }

    public void saveResult(){
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT); // Activity that creates a document to save the words
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType( "text/plain" );
        startActivityForResult( intent, SAVE );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK){
            if(requestCode == PICK_IMAGE_REQUEST && data.getData() != null){
                Uri uri = data.getData();

                try{
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    ImageView imageView = findViewById(R.id.imageView);
                    imageView.setImageBitmap(bitmap);
                    bmpOriginal = bitmap;
                } catch (IOException e){
                    e.printStackTrace();
                }
            }else if(requestCode == SAVE){
                if( data != null ){
                    Uri uri = data.getData(); // We get the file uri

                    try {
                        ParcelFileDescriptor file = getApplicationContext().getContentResolver().openFileDescriptor( uri, "w" );

                        FileOutputStream outputStream = new FileOutputStream(file.getFileDescriptor());
                        outputStream.write( classifier.getResult_string().getBytes() ); // The object is written

                        outputStream.close(); // Stream object is closed
                        file.close();
                    }catch ( IOException ex ){
                    }

                }
            }
        }


    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_PATH,
                            LABEL_PATH,
                            INPUT_SIZE);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
//        try {
//            classifier = TensorFlowImageClassifier.create(
//                    getAssets(),
//                    MODEL_PATH,
//                    LABEL_PATH,
//                    INPUT_SIZE);
//        } catch (final Exception e) {
//            throw new RuntimeException("Error initializing TensorFlow!", e);
//        }
    }
}
