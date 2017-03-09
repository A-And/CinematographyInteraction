package com.ol.andon.reflex.cv;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.objdetect.HOGDescriptor.DEFAULT_NLEVELS;

/**
 * Created by andon on 07/03/17.
 */

public class FigureDetector implements IDetector{

    MatOfRect mAllDetected;
    MatOfPoint mainFigure;
    HOGDescriptor mHogDescriptor;

    public    FigureDetector(){
        //mHogDescriptor = new HOGDescriptor(new Size(new Point(64, 128)), new Size(new Point(16, 16)), new Size(new Point(8, 8)),new Size(new Point(8, 8)), 9);
        //public   HOGDescriptor(Size _winSize, Size _blockSize, Size _blockStride, Size _cellSize, int _nbins, int _derivAperture, double _winSigma, int _histogramNormType, double _L2HysThreshold, boolean _gammaCorrection, int _nlevels, boolean _signedGradient)

        mHogDescriptor = new HOGDescriptor(new Size(new Point(64, 128)), new Size(new Point(16, 16)), new Size(new Point(8, 8)),new Size(new Point(8, 8)), 9,1, -1.0,1,0.2,true, DEFAULT_NLEVELS, false );
        // TODO benchmark both
        //mHogDescriptor.setSVMDetector(HOGDescriptor.getDaimlerPeopleDetector());
        mHogDescriptor.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());
    }

    @Override
    public void detect(Mat frame) {
        Mat frameUC3 = new Mat();
        frame.convertTo(frameUC3, CvType.CV_8U);
        Mat frameUC3Color = new Mat();
        Imgproc.cvtColor(frameUC3, frameUC3Color, Imgproc.COLOR_RGBA2GRAY);

        Mat pyrDownMat = new Mat();

        Imgproc.pyrDown(frameUC3Color, pyrDownMat);
        Imgproc.pyrDown(pyrDownMat, pyrDownMat);

        MatOfRect detectedFigures = new MatOfRect();
        MatOfDouble detectedWeights = new MatOfDouble();
        mHogDescriptor.detectMultiScale(pyrDownMat, detectedFigures, detectedWeights,0, new Size(8,8), new Size(32,32), 1.05, 2.0, false);
        List<Rect> figureL = detectedFigures.toList();

        Core.multiply(detectedFigures, new Scalar(2,2), detectedFigures);
        for(Rect rect : figureL){
            Log.v("FD", "Fig " + rect.toString());

        }
        mAllDetected = detectedFigures;

        frameUC3.release();
        frameUC3Color.release();
        pyrDownMat.release();
    }

    public MatOfRect getAllDetected() {
        return mAllDetected;
    }

    @Override
    public Mat getMainDetected() {
        return null;
    }
}
