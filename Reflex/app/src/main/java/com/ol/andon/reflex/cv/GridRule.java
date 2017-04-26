package com.ol.andon.reflex.cv;

import android.util.Log;

import org.opencv.core.Point;

/**
 * Created by andon on 05/04/17.
 */

public class GridRule {
    public final static String TAG = "GridRule";
    public static Point getClosestGridIntersectionPoint(Point roiPoint, int width, int height, int divNum){
        return getClosestPoint(roiPoint, width, height, divNum, false);
    }

    public static Point getClosestGridCenterPoint(Point roiPoint, int width, int height, int divNum){
        return getClosestPoint(roiPoint, width, height, divNum, true);
    }
    private static Point getClosestPoint(Point roiPoint, int width, int height, int divNum, boolean trackCenter){
        Point closest = new Point(0, 0);
        double minDistance = Integer.MAX_VALUE;
        Log.d(TAG, roiPoint.toString());
        int segPointNum = trackCenter ? divNum * divNum :  (divNum - 1) * (divNum - 1);
        int divisor = trackCenter ? 2 : 1;
        Log.d(TAG, "Divisions " + segPointNum);
        for(int i = 0; i < segPointNum/2 ; i++){
            for(int j = 0; j < segPointNum/2; j++){
                Point currPoint = new Point(((i + 1) * width/divNum) / divisor, ((j+1) * height/divNum) / divisor);
                Log.d(TAG, "Width: " + width + "DivNum: " + 3 + "Divisor: " + divisor);
                Log.d(TAG, "Div Point " + i + " " + currPoint.toString());
                double eD = euclidianDistance(roiPoint, currPoint);
                Log.d(TAG, "Div Point " + i + " Distance: " + eD);
                if(eD < minDistance){
                    minDistance = eD;
                    closest = currPoint;
                }
            }
        }
        Log.d(TAG, "Closest point " + closest.toString());
        return closest;
    }

    private static double euclidianDistance(Point p1, Point p2){
        // For readability
        double x = Math.abs(p1.x - p2.x);
        double y = Math.abs(p1.y - p2.y);
        return Math.sqrt(x*x + y*y);
    }

}
