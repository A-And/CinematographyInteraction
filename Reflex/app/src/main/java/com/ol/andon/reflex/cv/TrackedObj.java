package com.ol.andon.reflex.cv;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

/**
 * Created by andon on 15/03/17.
 */

public class TrackedObj {
    Rect roiPosition;
    Mat hsvMat;
    Mat hsvMask;
    Mat hsvHist;

    public TrackedObj(Rect roi){

    }

}
