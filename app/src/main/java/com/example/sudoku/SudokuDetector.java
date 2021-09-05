package com.example.sudoku;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SudokuDetector {
    private static final int RESIZED_IMAGE_H = 900;
    private static final int RESIZED_IMAGE_W = 900;

    private static Interpreter TfLite;
    private final ByteBuffer tfInput;
    private final float[][] tfOutput;
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_IMG_SIZE_X = 32;
    private static final int DIM_IMG_SIZE_Y = 32;
    private static final int DIM_PIXEL_SIZE = 3;
    private static final int NUMBER_LENGTH = 10;

    public SudokuDetector(Context ctx) throws IOException {
        TfLite = new Interpreter(loadModelFile(ctx));

        tfInput = ByteBuffer.allocateDirect(
                Float.BYTES * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        tfInput.order(ByteOrder.nativeOrder());
        tfOutput = new float[DIM_BATCH_SIZE][NUMBER_LENGTH];
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("best_cnn.tflite");

        FileInputStream fileInputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();

        return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.getStartOffset(),
                fileDescriptor.getDeclaredLength());
    }

    public Vector<Integer> detectNumber(String imagePath) {
        Mat imgOriginal = Imgcodecs.imread(imagePath);

        Mat imgSudokuMap = dropImageToGetMap(imgOriginal);
        if (imgSudokuMap == null)
            return null;

        Vector<Mat> boxes = splitImageIntoBoxes(imgSudokuMap);

        Vector<Integer> predictNumbers = new Vector<>();
        for (Mat mat : boxes) {
            Bitmap b = SudokuUtils.ToBitmap(mat);
            int num = predictNumber(mat);
            if (num == -1 | num == 9)
                num = 0;
            else
                num += 1;
            predictNumbers.add(num);
        }

        return predictNumbers;
    }

    private Mat getThresholdMap(Mat image) {
        Mat grayDestination = new Mat();
        Imgproc.cvtColor(image, grayDestination, Imgproc.COLOR_BGR2GRAY);

        Mat blurDestination = new Mat();
        Imgproc.GaussianBlur(
                grayDestination,
                blurDestination,
                new Size(5, 5),
                1);

        Mat threshHold = new Mat();
        Imgproc.adaptiveThreshold(
                blurDestination,
                threshHold,
                255,
                1,
                1,
                11,
                2);
        return threshHold;
    }

    private Mat dropImageToGetMap(Mat imgSource) {
        final Size IMG_SIZE = new Size(RESIZED_IMAGE_W, RESIZED_IMAGE_H);

        // Get Threshold
        Mat resizedImage = new Mat();
        Imgproc.resize(imgSource, resizedImage, IMG_SIZE);

        // find all contours
        Mat imgThreshHold = getThresholdMap(resizedImage);
        List<MatOfPoint> contourList = new ArrayList<>();
        Imgproc.findContours(
                imgThreshHold,
                contourList,
                new Mat(),
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);

        // find biggest
        MatOfPoint2f biggest = SudokuUtils.biggestContours(contourList);
        if (biggest.toArray().length == 0)
            return null;

        // drop image
        try {
            Point[] sortedPoints = SudokuUtils.reOrder(biggest);
            MatOfPoint2f src = new MatOfPoint2f(
                    sortedPoints[0],
                    sortedPoints[1],
                    sortedPoints[2],
                    sortedPoints[3]);
            MatOfPoint2f dst = new MatOfPoint2f(
                    new Point(0, 0),
                    new Point(RESIZED_IMAGE_W - 1, 0),
                    new Point(0, RESIZED_IMAGE_H - 1),
                    new Point(RESIZED_IMAGE_W - 1, RESIZED_IMAGE_H - 1)
            );
            Mat warpMat = Imgproc.getPerspectiveTransform(src, dst);

            Mat imgWarpColored = new Mat();
            Imgproc.warpPerspective(
                    resizedImage,
                    imgWarpColored,
                    warpMat,
                    IMG_SIZE);

            System.out.println("The image is successfully to find biggest contour");
            return imgWarpColored;
        } catch (Exception e) {
            System.out.println("No Sudoku Found");
            return null;
        }
    }

    private Vector<Mat> splitImageIntoBoxes(Mat imgSource) {
        Vector<Mat> boxes = new Vector<>();
        int blockSize = RESIZED_IMAGE_W / 9;
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                Range rowRange = new Range(i * blockSize, (i + 1) * blockSize - 1);
                Range colRange = new Range(j * blockSize, (j + 1) * blockSize - 1);
                Mat mat = new Mat(imgSource, rowRange, colRange);
                boxes.add(mat);
            }
        }
        System.out.println("Splitting image successfully");
        return boxes;
    }

    private int predictNumber(Mat mat) {
        Imgproc.resize(mat, mat, new Size(DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y));

        tfInput.rewind();
        for (int i = 0; i < DIM_IMG_SIZE_X; i++) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; j++) {
                for (int k = 0; k < DIM_PIXEL_SIZE; k++)
                    tfInput.putFloat((float) mat.get(i, j)[k] / 255.0f);
            }
        }
        TfLite.run(tfInput, tfOutput);

        // get predict num
        float predictValue = -1;
        int predictPos = -1;
        for (int i = 0; i < NUMBER_LENGTH; i++) {
            float f = tfOutput[0][i];
            if (f > 0.9f && f > predictValue) {
                predictValue = f;
                predictPos = i;
            }
        }
        return predictPos;
    }
}
