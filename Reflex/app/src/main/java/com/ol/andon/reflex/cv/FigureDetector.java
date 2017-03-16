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
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.HOGDescriptor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.objdetect.HOGDescriptor.DEFAULT_NLEVELS;

/**
 * Created by andon on 07/03/17.
 */

public class FigureDetector implements IDetector{

    MatOfRect mFacesDetected;
    MatOfRect mBodiesDetected;
    MatOfRect mBodyROIs;
    MatOfPoint mainFigure;

    // Define size of frame to look for a body as compared to face frame
    int bodyFrameMultiplier = 3;

    static String TAG = "FigureDetector";
    CascadeClassifier faceDetector;
    CascadeClassifier bodyDetector;

    HOGDescriptor mHogDescriptor;

    Size faceSize;


    public FigureDetector(){
        mHogDescriptor = new HOGDescriptor(new Size(new Point(64, 128)), new Size(new Point(16, 16)), new Size(new Point(8, 8)), new Size(new Point(8, 8)), 9, 1, -1.0, 1, 0.2, true, DEFAULT_NLEVELS, false);
        // TODO benchmark both
        //mHogDescriptor.setSVMDetector(HOGDescriptor.getDaimlerPeopleDetector());
        mHogDescriptor.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());
    }

    public FigureDetector(String faceCCLocation, String bodyCCLocation){
        try {
            //mHogDescriptor = new HOGDescriptor(new Size(new Point(64, 128)), new Size(new Point(16, 16)), new Size(new Point(8, 8)),new Size(new Point(8, 8)), 9);
            //public   HOGDescriptor(Size _winSize, Size _blockSize, Size _blockStride, Size _cellSize, int _nbins, int _derivAperture, double _winSigma, int _histogramNormType, double _L2HysThreshold, boolean _gammaCorrection, int _nlevels, boolean _signedGradient)

            //mHogDescriptor = new HOGDescriptor(new Size(new Point(64, 128)), new Size(new Point(16, 16)), new Size(new Point(8, 8)), new Size(new Point(8, 8)), 9, 1, -1.0, 1, 0.2, true, DEFAULT_NLEVELS, false);
            // TODO benchmark both
            //mHogDescriptor.setSVMDetector(HOGDescriptor.getDaimlerPeopleDetector());
            //mHogDescriptor.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());
            Log.v(TAG, faceCCLocation);


            faceDetector = new CascadeClassifier(faceCCLocation);
            faceDetector.load(faceCCLocation);
            if (faceDetector.empty()) {
                Log.e(TAG, "Failed to load face cascade classifier");
            } else
                Log.i(TAG, "Loaded face cascade classifier from " + faceCCLocation);

            faceSize = new Size(90 ,90);

            bodyDetector = new CascadeClassifier(bodyCCLocation);
            bodyDetector.load(bodyCCLocation);
            if (bodyDetector.empty()) {
                Log.e(TAG, "Failed to load body cascade classifier");
            } else
                Log.i(TAG, "Loaded body cascade classifier from " + faceCCLocation);

        }
        catch (Exception e) {
            Log.e(TAG, "Error creating FigureDetector", e);
        }
    }

    @Override
    public void detect(Mat frame) {
        Mat frameUC3 = new Mat();
        frame.convertTo(frameUC3, CvType.CV_8U);

        Mat frameUC3Grayscale = new Mat();
        Imgproc.cvtColor(frameUC3, frameUC3Grayscale, Imgproc.COLOR_RGBA2GRAY);

        Mat pyrDownMat = new Mat();
        MatOfRect faces = new MatOfRect();

        //Imgproc.resize(frameUC3Grayscale, resize, new Size(160,120));

        Imgproc.pyrDown(frameUC3Grayscale, pyrDownMat);
        Imgproc.pyrDown(pyrDownMat, pyrDownMat);


        if (faceDetector != null) {
            faceDetector.detectMultiScale(pyrDownMat, faces);
        }

        Core.multiply(faces, new Scalar(4,4,4,4), faces);
        mFacesDetected = faces;

        MatOfRect bodies = new MatOfRect();
        List<Rect> detectedFaces = faces.toList();
        Rect[] bodyROIs = new Rect[detectedFaces.size()];

        for(int i = 0; i < detectedFaces.size(); i ++){
            Rect rect = detectedFaces.get(i);
            Log.i(TAG, "Face detected" + rect.toString());

            // Outline region of interest for body recognition

            double multiWidth = rect.width*(((double)bodyFrameMultiplier - 1 )/2.0);

            int bodyRectX = Math.min(Math.max((int)(rect.x - multiWidth), 0), frameUC3Grayscale.width());
            int bodyRectWidth = Math.min(rect.width * bodyFrameMultiplier, frameUC3Grayscale.width() - bodyRectX);

            Rect bodyRect =  new Rect(bodyRectX, rect.y,bodyRectWidth , Math.max(frameUC3Grayscale.height() - rect.y, 0));
            bodyROIs[i] = bodyRect;

            Log.i(TAG, "ROI " + bodyRect.size() + " " + bodyRect.x + " " + bodyRect.y);
            Log.i(TAG, "frame " + frameUC3Grayscale.size());

            Mat bodyFrameROI = frameUC3Grayscale.submat(bodyRect);
            //frameUC3Grayscale.submat(bodyRect).copyTo(bodyFrameROI);
            //bodyDetector.detectMultiScale(bodyFrameROI, bodies);
            //Core.add(bodies, new Scalar(bodyFrameROI.width(), bodyFrameROI.height(), 0, 0), bodies);

            bodyFrameROI.release();
        }
        for(Rect body: bodies.toList()){
            Log.i(TAG, "body detected" + body.toString());

        }
        // Create rectangle matrix to be available for easier drawing
        MatOfRect bodyROIsMat = new MatOfRect();
        bodyROIsMat.fromArray(bodyROIs);
        mBodyROIs = bodyROIsMat;

        mBodiesDetected = bodies;

        frameUC3.release();
        frameUC3Grayscale.release();
        pyrDownMat.release();
    }

    public MatOfRect getAllDetected() {
        return mFacesDetected;
    }
    public MatOfRect getmFacesDetected(){return mFacesDetected;}
    public MatOfRect getBodiesDetected(){return mBodiesDetected;}
    public MatOfRect getBodyROIs(){return mBodyROIs;};

    @Override
    public Mat getMainDetected() {
        return null;
    }
}
