package com.cic.robotics_lab.pedestrian_detector;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static int PICK_IMAGE_REQUEST = 1;
    private static final String MODEL_PATH = "model3.tflite";
    private static final String LABEL_PATH = "labels3.txt";
    private static final int INPUT_SIZE = 48;

    private Classifier classifier;
    private Bitmap bmpDetect;
    private Executor executor = Executors.newSingleThreadExecutor();
    private TextView textViewResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button choose_btn = findViewById(R.id.choose_image);
        Button detector_btn = findViewById(R.id.detect);
        textViewResult = findViewById(R.id.textView_pre);

        setBtnChoose(choose_btn);
        setBtnDetector(detector_btn);
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
                bmpDetect = Bitmap.createScaledBitmap(bmpDetect, INPUT_SIZE, INPUT_SIZE, false);
                bmpDetect = toGrayScale(bmpDetect);
                final List<Classifier.Recognition> results = classifier.recognizeImage(bmpDetect);

                textViewResult.setText(results.toString());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_IMAGE_REQUEST &&
                resultCode == RESULT_OK &&
                data.getData() != null){
            Uri uri = data.getData();

            try{
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                ImageView imageView = findViewById(R.id.imageView);
                imageView.setImageBitmap(bitmap);
                bmpDetect = bitmap;
            } catch (IOException e){
                e.printStackTrace();
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
    }

    public Bitmap toGrayScale(Bitmap bmpOriginal){
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
}
