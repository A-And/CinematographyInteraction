package com.ol.andon.reflex.cv;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

/**
 * Created by andon on 23/03/17.
 */

public class LKOpticalFlow implements ITracker {

    Rect mRoi;
    MatOfPoint2f mNextRoiPoints;
    MatOfPoint2f mRoiPoints;
    Mat prevGrayScale;
    Size subPixWinSize = new Size(10,10);
    Size winSize = new Size(31,31);
    TermCriteria terminationCriteria = new TermCriteria(TermCriteria.MAX_ITER,20,0.03);

    static String TAG = "LKOpticalFlow";

    int MAX_COUNT = 500;

    public LKOpticalFlow(Mat frame, Rect roi){
        setReferenceFrame(frame, roi);
    }

    @Override
    public void setReferenceFrame(Mat frame, Rect roi) {
        setROI(roi);

        Mat image = new Mat();
        frame.copyTo(image);

        MatOfPoint roiPoints = new MatOfPoint(new Point(roi.x, roi.y),new Point(roi.x + roi.width, roi.y),
                new Point(roi.x, roi.y + roi.height), new Point(roi.x + roi.width, roi.y + roi.height), new Point(roi.x + roi.width/2, roi.y + roi.height/2));

        Mat grayscale = new Mat();
        Imgproc.cvtColor(image, grayscale, Imgproc.COLOR_RGBA2GRAY);
        //Imgproc.goodFeaturesToTrack(grayscale, roiPoints, MAX_COUNT, 0.01, 10, new Mat(), 3, false, 0.04);

        mRoiPoints = new MatOfPoint2f(roiPoints.toArray());
        //Imgproc.cornerSubPix(grayscale, mRoiPoints, subPixWinSize, new Size(-1, -1), terminationCriteria);

        prevGrayScale = grayscale;

        image.release();
        roiPoints.release();

    }

    @Override
    public void setROI(Rect roi) {
        mRoi = roi;
    }

    @Override
    public RotatedRect getROI(Mat newFrame) {

        if(mRoiPoints.empty()) return new RotatedRect();

        MatOfByte status = new MatOfByte();
        MatOfFloat err = new MatOfFloat();
        Mat image = new Mat();
        newFrame.copyTo(image);


        Mat grayscale = new Mat();
        Imgproc.cvtColor(image, grayscale, Imgproc.COLOR_RGBA2GRAY);

        MatOfPoint2f nextRoiPoints = new MatOfPoint2f();
        Video.calcOpticalFlowPyrLK(prevGrayScale, grayscale, mRoiPoints, nextRoiPoints, status, err);
        RotatedRect res = new RotatedRect();

        Log.i(TAG, "Prev ROI points: " + mRoiPoints.toString());
        Log.i(TAG, "Prev ROI points: " + nextRoiPoints.toString());

        Log.i(TAG, "Status " + status.toString());
        Log.i(TAG, "Error " + err.toString());

        if(nextRoiPoints.empty()){
            Log.i(TAG, "Null roi points");

        }
        else{

            double width = Math.abs(nextRoiPoints.get(3,0)[0] - nextRoiPoints.get(0,0)[0]);
            double height = Math.abs(nextRoiPoints.get(3,0)[1] - nextRoiPoints.get(0,0)[1]);

              res = new RotatedRect(new Point(nextRoiPoints.get(0, 4)), new Size(width, height), 0);
        }

        return res;
    }
}
