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
    Mat mFrameHSVMat = new Mat();
    Mat mRoiHist = new Mat();

    // HSV Range values
    int SMin = 30;
    int VMin = 10;
    int VMax = 256;

    String TAG = "CamShift";

    MatOfFloat histRanges = new MatOfFloat(0f, 180f);
    MatOfInt histSize = new MatOfInt(16);
    MatOfFloat pHistRanges = histRanges;
    MatOfInt channels = new MatOfInt(0, 0);
    Mat mMaskRoi;


    Mat hist = new Mat();

    public Camshift(Mat initialFrame, Rect roi){
        Log.i(TAG, "Constructed CamShift with frame: " + initialFrame.toString() + " ROI: " + roi.toString());
        setReferenceFrame(initialFrame,roi);
    }

    @Override
    public void setReferenceFrame(Mat frame, Rect roi) {
        setup(frame,roi);
//        mCurrentFrame = frame;
//        mRoi = roi;
//
//        // set up the ROI for tracking
//        mFrameHSVMat= rgbaToHsv(frame);
//
//        // Matrix to mix channels
//
//        List<Mat> images = new ArrayList<>(Arrays.asList(mFrameHSVMat));
//
//        Core.mixChannels(images,images, channels);
//        mRoiMat = mFrameHSVMat.submat(roi);
//
//        Log.i("CamShift", "ROIHSV channels " + mFrameHSVMat.channels());
//        Mat hsvMask = new Mat();
//        Core.inRange(mRoiMat, new Scalar(0, 30, 32), new Scalar(180,255,255), hsvMask);
//
//        images = new ArrayList<>(Arrays.asList(mRoiMat));
//
//        Imgproc.calcHist(images, new MatOfInt(0), hsvMask, mRoiHist, histSize, histRanges);
//        Core.normalize(mRoiHist, mRoiHist, 0, 255, Core.NORM_MINMAX);
//
//        mMaskRoi = hsvMask.submat(roi);

    }

    @Override
    public void setROI(Rect roi) {
        this.mRoi = roi;
    }

    @Override
    public RotatedRect getROI(Mat newFrame) {
           Log.v(TAG, "Frame: " + newFrame.toString());
        return findInFrame(newFrame);
//        // TODO Continue tracking even through an empty frame
//        if(newFrame.height() == 0 || newFrame.width() == 0) return new RotatedRect(new Point(0,0), new Size(0,0), 0);
//        if(mRoi.height   == 0 || mRoi.width == 0) return new RotatedRect(new Point(0,0), new Size(0,0), 0);
//
//        Mat newFrameHSV = new Mat(newFrame.size(), Imgproc.COLORMAP_HSV);
//
//
//        Imgproc.cvtColor(newFrame, newFrameHSV, Imgproc.COLOR_RGB2HSV);
//
//        Mat hsvMask = new Mat();
//        Core.inRange(newFrameHSV, new Scalar(0, 30, 32), new Scalar(180,255,255), hsvMask);
//
//        Mat dest = new Mat();
//        List<Mat> images = new ArrayList<Mat>(Arrays.asList(newFrameHSV));
//        Core.mixChannels(images,images, channels);
//
//        Imgproc.calcBackProject(images,channels, mRoiHist, dest, histRanges,1);
//        Log.i(TAG,"ROI" + mRoi.toString());
//
//        Mat backProj = new Mat();
//        dest.copyTo(backProj, hsvMask);
//        RotatedRect shiftWindow = Video.CamShift(backProj, mRoi,new TermCriteria(TermCriteria.MAX_ITER, 80, 1));
//
//        dest.release();
//        return shiftWindow;
    }

    private Mat rgbaToHsv(Mat rgbaFrame){
        Mat rgb = new Mat();
        Imgproc.cvtColor(rgbaFrame, rgb, Imgproc.COLOR_RGBA2RGB);
        Mat hsv = new Mat();
        Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV);
        rgb.release();
        return hsv;

    }

    public void setup(Mat frame, Rect roi){
        mRoi = roi;
        Mat image = new Mat();
        frame.copyTo(image);

        Mat hsvMat = rgbaToHsv(image);
        Mat mask = new Mat();
        Core.inRange(hsvMat, new Scalar(0, SMin, Math.min(VMin, VMax)), new Scalar(180, 256, Math.max(VMin, VMax)), mask);

        Mat hue = new Mat(hsvMat.size(), hsvMat.depth());

        List<Mat> hsvList = new ArrayList<Mat>(Arrays.asList(hsvMat));
        List<Mat> hueList = new ArrayList<Mat>(Arrays.asList(hue));

        Core.mixChannels(hsvList, hueList,channels);

        Mat roiMat = hue.submat(roi);
        Mat maskRoi = mask.submat(roi);

        List<Mat> rois = new ArrayList<>(Arrays.asList(roiMat));

        Imgproc.calcHist(rois, new MatOfInt(0), maskRoi, hist, histSize, pHistRanges);

        Core.normalize(hist, hist, 0, 255, Core.NORM_MINMAX);

        Log.i(TAG, hist.toString());
    }

    public RotatedRect findInFrame(Mat newFrame){

        Mat image = new Mat();
        newFrame.copyTo(image);

        Mat hsvMat = rgbaToHsv(image);
        Mat mask = new Mat();
        Core.inRange(hsvMat, new Scalar(0, SMin, Math.min(VMin, VMax)), new Scalar(180, 256, Math.max(VMin, VMax)), mask);

        Mat hue = new Mat(hsvMat.size(), hsvMat.depth());

        List<Mat> hsvList = new ArrayList<Mat>(Arrays.asList(hsvMat));
        List<Mat> hueList = new ArrayList<Mat>(Arrays.asList(hue));

        Core.mixChannels(hsvList, hueList,channels);

        Mat backProj = new Mat();

        Imgproc.calcBackProject(hueList, new MatOfInt(0), hist, backProj, pHistRanges, 1);

        Mat maskedBackProj = new Mat();

        backProj.copyTo(maskedBackProj, mask);
        Log.i(TAG, "backProj: " + maskedBackProj.toString());
        Log.i(TAG, "mRoi: " + mRoi.toString());

        RotatedRect window = Video.CamShift(maskedBackProj, mRoi, new TermCriteria(TermCriteria.MAX_ITER, 10, 1));

        image.release();
        hsvMat.release();
        hue.release();
        backProj.release();
        maskedBackProj.release();
        return window;
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
