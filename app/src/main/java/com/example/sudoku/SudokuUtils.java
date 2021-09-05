package com.example.sudoku;

import static org.opencv.android.Utils.matToBitmap;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.tensorflow.lite.Interpreter;

import java.util.List;

public class SudokuUtils {

    static Bitmap ToBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.RGB_565);
        matToBitmap(mat, bitmap);
        return bitmap;
    }


    private static final int NUMBER_LENGTH = 10;
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_IMG_SIZE_X = 28;
    private static final int DIM_IMG_SIZE_Y = 28;
    private static final int DIM_PIXEL_SIZE = 1;
    private static final int BYTE_SIZE_OF_FLOAT = 4;
    private Interpreter tflite;


    public static Point[] reOrder(MatOfPoint2f approx) {
        Moments moment = Imgproc.moments(approx);
        int x = (int) (moment.get_m10() / moment.get_m00());
        int y = (int) (moment.get_m01() / moment.get_m00());
        Point[] sortedPoints = new Point[4];
        double[] data;
        int count = 0;
        for (int i = 0; i < approx.rows(); i++) {
            data = approx.get(i, 0);
            double datax = data[0];
            double datay = data[1];
            if (datax < x && datay < y) {
                sortedPoints[0] = new Point(datax, datay);
                count++;
            } else if (datax > x && datay < y) {
                sortedPoints[1] = new Point(datax, datay);
                count++;
            } else if (datax < x && datay > y) {
                sortedPoints[2] = new Point(datax, datay);
                count++;
            } else if (datax > x && datay > y) {
                sortedPoints[3] = new Point(datax, datay);
                count++;
            }
        }
        return sortedPoints;
    }

    public static MatOfPoint2f biggestContours(List<MatOfPoint> contoursList) {
        MatOfPoint2f biggest = new MatOfPoint2f();
        double max_area = 0;
        for (MatOfPoint c : contoursList) {
            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            double area = Imgproc.contourArea(c2f);
            if (area > 50) {
                double peri = Imgproc.arcLength(c2f, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);
                Point[] points = approx.toArray();
                if (area > max_area && points.length == 4) {
                    biggest = approx;
                    max_area = area;
                }
            }
        }
        return biggest;
    }
}