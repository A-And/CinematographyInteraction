package com.ol.andon.reflex.cv;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

import java.util.List;

/**
 * Created by andon on 07/03/17.
 */

public interface IDetector {

    void detect(Mat frame);
    Mat getMainDetected();
}
