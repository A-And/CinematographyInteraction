package com.ol.andon.reflex.cv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import android.util.Log;
/**
 * Created by andon on 15/03/17.
 *
 */

public class Camshift implements ITracker {

    Mat mCurrentFrame;
    Rect mRoi;

    Mat mRoiMat = new Mat();
    Mat mRoiHSVMat = new Mat();
    Mat mRoiHist = new Mat();

    String TAG = "CamShift";

    MatOfFloat ranges = new MatOfFloat(0f, 256f, 0f, 256f);
    MatOfInt histSize = new MatOfInt(256,256);

    public Camshift(Mat initialFrame, Rect roi){
        this.mRoi = roi;
        Log.i(TAG, "Constructed CamShift with frame: " + initialFrame.toString() + " ROI: " + roi.toString());
        setReferenceFrame(initialFrame,roi);
    }

    @Override
    public void setReferenceFrame(Mat frame, Rect roi) {
        mCurrentFrame = frame;


        // set up the ROI for tracking
        mRoiMat = mCurrentFrame.submat(roi);
        Imgproc.cvtColor(mRoiMat, mRoiHSVMat, Imgproc.COLOR_BGR2HSV);
        Log.v("CamShift", "ROIHSV channels " + mRoiHSVMat.channels());
        Mat hsvMask = new Mat();
        Core.inRange(mRoiMat, new Scalar(0, 60, 32), new Scalar(180,255,255), hsvMask);
        List<Mat> images = new ArrayList<>(Arrays.asList(mRoiHSVMat));

        Imgproc.calcHist(images, new MatOfInt(1,0), hsvMask, mRoiHist, histSize, ranges);
        Core.normalize(mRoiHist, mRoiHist, 0, 255, Core.NORM_MINMAX);

    }

    @Override
    public void setROI(Rect roi) {
        this.mRoi = roi;
    }

    @Override
    public RotatedRect getROI(Mat newFrame) {
        // TODO Continue tracking even through an empty frame
        if(newFrame.height() == 0 || newFrame.width() == 0) return new RotatedRect(new Point(0,0), new Size(0,0), 0);
        if(mRoi.height == 0 || mRoi.width == 0) return new RotatedRect(new Point(0,0), new Size(0,0), 0);
        Mat newFrameHSV = new Mat(newFrame.size(), Imgproc.COLORMAP_HSV);

        Imgproc.cvtColor(newFrame, newFrameHSV, Imgproc.COLOR_BGR2HSV);
        Mat dest = new Mat();
        List<Mat> images = new ArrayList<Mat>(Arrays.asList(newFrameHSV));
        Imgproc.calcBackProject(images,new MatOfInt(1,0), mRoiHist, dest, ranges,1);
        Log.i(TAG,"ROI" + mRoi.toString());
        RotatedRect shiftWindow = Video.CamShift(dest, mRoi,new TermCriteria(TermCriteria.EPS, 10, 1));
        dest.release();
        return shiftWindow;
    }


//http://stackoverflow.com/questions/9804254/image-comparison-of-logos
//http://opencv.willowgarage.com/documentation/histograms.html
    //http://code.opencv.org/issues/1447
//http://answers.opencv.org/question/3650/trying-to-calculate-histogram-on-android-and-find/
//http://answers.opencv.org/question/664/camshift-in-android/
//https://groups.google.com/forum/?fromgroups=#!topicsearchin/android-opencv/calcHist/android-opencv/v_TNQea3xxM
//http://grokbase.com/t/gg/android-opencv/122g44w1vp/histogram-calculation
//http://android-spikes.googlecode.com/svn/HelloImageDetection/src/gestoreImmagini/HistogramCompareUtil.java
//https://projects.developer.nokia.com/opencv/browser/opencv/opencv-2.3.1/modules/java/android_test/src/org/opencv/test/imgproc/ImgprocTest.java?rev=ffd62ba23055b3d4b8ba068d5554e2760f0f0eea
//https://code.ros.org/trac/opencv/changeset/6111/trunk/opencv/modules/java/android_test/src/org/opencv/test/imgproc/imgprocTest.java


}
