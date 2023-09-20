package com.example.braillereader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;


import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.braillereader.ml.BrailleDetect;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

//import com.google.android.material.snackbar.Snackbar;
//import androidx.core.view.WindowCompat;
//import androidx.navigation.NavController;
//import androidx.navigation.Navigation;
//import androidx.navigation.ui.AppBarConfiguration;
//import androidx.navigation.ui.NavigationUI;
//
//import com.example.braillereader.databinding.ActivityMainBinding;
//
//import android.view.Menu;
//import android.view.MenuItem;
//import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    Button camera, gallery;
    ImageView display_image;
    TextView results;

    int imageSize = 32;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // define ID
        display_image = findViewById(R.id.display_image);
        camera = findViewById(R.id.camera);
        results = findViewById(R.id.results);

        Log.v("com.example.braillereader", "1");
        // open camera
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("com.example.braillereader", "permission ok");
                // have permission
                if(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 3);
                }
                // no permission
                else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });

        // open gallery (no intention for gallery yet)
//        gallery.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                startActivityForResult(cameraIntent, 1);
//            }
//        });

        // source 1
//        // Camera_open button is for open the camera and add the setOnClickListener in this button
//        camera.setOnClickListener(v -> {
//            // Create the camera_intent ACTION_IMAGE_CAPTURE it will open the camera for capture the image
//            Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//            // Start the activity with camera_intent, and request pic id
//            startActivityForResult(camera_intent, pic_id);
//        });


    }

    public void detect_braille(Bitmap image) {
        BrailleDetect model = null;
        int iwidth = image.getWidth();
        int ilength = image.getHeight();
        try {
            model = BrailleDetect.newInstance(getApplicationContext());
            Log.v("com.example.braillereader", "model loaded");

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, imageSize, imageSize, 27}, DataType.FLOAT32);
//            TensorBuffer inputFeature0 = TensorBuffer.createDynamic(DataType.FLOAT32);
            Log.v("com.example.braillereader", "input feature");

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * iwidth * ilength * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            Log.v("com.example.braillereader", String.valueOf(byteBuffer));

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0 ,image.getWidth(), image.getHeight());

            Log.v("com.example.braillereader", "iterate image");

//             iterate over each pixel (CNN only)
            int pixel = 0;
            for (int i=0; i<iwidth; i++){
                for (int j=0; j<ilength; j++){
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 1));
                }
            }

            Log.v("com.example.braillereader", String.valueOf(pixel));
            inputFeature0.loadBuffer(byteBuffer);

            Log.v("com.example.braillereader", "load buffer");
            // Runs model inference and gets result.
            BrailleDetect.Outputs outputs = model.process(inputFeature0);
            Log.v("com.example.braillereader", "output");

            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            Log.v("com.example.braillereader", "outputFeature0");

            Log.v("com.example.braillereader", String.valueOf(outputFeature0.getDataType()));

//            display_image.setImageBitmap(TensorBuffer.getbuffer(outputFeature0));
//            results.setText((CharSequence) outputFeature0);

//            float[] confidence = outputFeature0.getFloatArray();
//            // find most confidence
//            int maxPos = 0;
//            float maxConfidence = 0;
//            for (int i = 0; i < confidence.length; i++){
//                if (confidence[i] > maxConfidence) {
//                    maxConfidence = confidence[i];
//                    maxPos = i;
//                }
//            }
//
//            String[] classes = {"#", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
//            results.setText(classes[maxPos]);

        } catch (IOException e) {
            // TODO Handle the exception
        } finally {
            // Releases model resources if no longer used.
            model.close();
        }

    }

    // resize picture (maybe want)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        if(resultCode ==RESULT_OK){
            Log.v("com.example.braillereader", "image ok");
            // camera
            if(requestCode == 3){
                Log.v("com.example.braillereader", "camera");
                Bitmap image = (Bitmap) data.getExtras().get("data");
                Log.v("com.example.braillereader", String.valueOf(image.getWidth()));
                Log.v("com.example.braillereader", String.valueOf(image.getHeight()));
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                display_image.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                Log.v("com.example.braillereader", "call model");
//                detect_braille(image);
            }

            // gallery
            else {
                Log.v("com.example.braillereader", "gallery");
                Uri dat= data.getData();
                Bitmap image = null;
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                } catch (IOException e){
                    e.printStackTrace();
                }
                display_image.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                detect_braille(image);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    };

    // source 1
//    // This method will help to retrieve the image
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        // Match the request 'pic id with requestCode
//        if (requestCode == pic_id) {
//            // BitMap is data structure of image file which store the image in memory
//            Bitmap photo = (Bitmap) data.getExtras().get("data");
//            // Set the image in imageview for display
//            display_image.setImageBitmap(photo);
//        }
//    }
}