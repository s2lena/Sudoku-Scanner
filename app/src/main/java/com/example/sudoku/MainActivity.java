package com.example.sudoku;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final String TAG = "SUDOKU - ";
    private static final int TAKE_PHOTO_REQUEST = 1;
    String currentPhotoPath, filename;
    private TextView tvPlaceHolder;
    private ImageView imageView;

    private static Vector<Integer> numbers = null;
    SudokuDetector detector;
    SudokuSolver solver;

    static {
        if (OpenCVLoader.initDebug())
            Log.d(TAG, "opencv installed successfully");
        else
            Log.d(TAG, "opencv isn't installed");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        tvPlaceHolder = findViewById(R.id.tvPlaceholder);

        Button btnTakePicture = findViewById(R.id.btnTakePhoto);
        Button btnShowSolution = findViewById(R.id.btnShowSolution);
        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    dispatchTakePictureIntent();
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });

        btnShowSolution.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (numbers == null)
                    Toast.makeText(view.getContext(), "No Sudoku Found", Toast.LENGTH_SHORT).show();
                else {
                    Vector<Integer> results = solver.applyAlgorithm(numbers);
                    onDraw(numbers, results);
                    if (!solver.checkLegalSudoku(results))
                        Toast.makeText(view.getContext(), "Illegal Sudoku", Toast.LENGTH_SHORT).show();
                }
            }
        });

        try {
            detector = new SudokuDetector(this);
            solver = new SudokuSolver();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                showPreviewImage(currentPhotoPath);
                numbers = detector.detectNumber(currentPhotoPath);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void onDraw(Vector<Integer> numbers, Vector<Integer> results) {
       Bitmap bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888);
       imageView.setImageBitmap(bitmap);

       Paint paint = new Paint();
       paint.setTextSize(50);
       paint.setColor(Color.RED);
       paint.setStyle(Paint.Style.STROKE);

       Canvas canvas = new Canvas(bitmap);
       canvas.drawColor(Color.WHITE);
       for (int i = 0; i < 4; i++){
           canvas.drawLine(i * 333, 0 , i * 333, 1000, paint);
           canvas.drawLine(0, i * 333, 1000, i * 333 , paint);
       }

       int count = 0;
       int size = 900 / 9;
       int tmpY = 0;
        for(int i = 0; i < 9; i++) {
           int tmpX = 0;
           if (i % 3 ==0 && i != 0)
               tmpY = tmpY + 30;
           for (int j = 0; j < 9; j++) {
               size = 100;
               if (numbers.get(count) == results.get(count))
                   paint.setColor(Color.BLACK);
               else
                   paint.setColor(Color.MAGENTA);

               if(j % 3 == 0 && j != 0)
                   tmpX = tmpX + 20;
               canvas.drawText(results.get(count).toString(),
                       j * size + 60 + tmpX, i * size + 100 + tmpY, paint);
               count++;
           }
       }
       imageView.invalidate();
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(
                source,
                0, 0,
                source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private void showPreviewImage(String photoPath) throws IOException {
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoPath, bmOptions);

        // Determine how much to scale down the image
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor = Math.max(1, Math.min(photoW / targetW, photoH / targetH));

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inMutable = true;
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath, bmOptions);

        // Detect and rotate
        ExifInterface ei = new ExifInterface(photoPath);
        int orientation = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
        );
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                bitmap = rotateImage(bitmap, 90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                bitmap = rotateImage(bitmap, 180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                bitmap = rotateImage(bitmap, 270);
                break;
            case ExifInterface.ORIENTATION_NORMAL:
                break;
            default:
                break;
        }

        // Display
        imageView.setImageBitmap(bitmap);
        tvPlaceHolder.setVisibility(View.INVISIBLE);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) == null)
            return;

        File photoFile = null;
        try {
            photoFile = createImageTempFile();
            if (photoFile == null)
                return;
        } catch (IOException ex) {
            Log.d(TAG, ex.getMessage());
            return;
        }

        Uri photoURI = FileProvider.getUriForFile(
                this,
                "com.example.sudoku.fileprovider",
                photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    private File createImageTempFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        filename = "JPEG_" + timeStamp + ".jpg";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
}