package com.ol.andon.reflex.cv;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;

/**
 * Created by andon on 15/03/17.
 */

public interface ITracker {

    void setReferenceFrame(Mat frame, Rect roi);
    void setROI(Rect roi);
    RotatedRect getROI(Mat newFrame);

}
