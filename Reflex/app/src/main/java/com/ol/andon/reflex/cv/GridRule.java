package com.ol.andon.reflex.cv;

import android.util.Log;

import org.opencv.core.Point;
import org.opencv.core.Rect;

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
    public static boolean conforms(Rect object, int width, int height, int divNum){
        int segPointNum = divNum - 1;
//
//        // Save lines of a rectangle as segments
//        Point[][] objectLines = new Point[4][2];
//        objectLines[0] = new Point[] {new Point(object.x, object.y), new Point(object.x, object.y + object.height)};
//        objectLines[1] = new Point[] {new Point(object.x, object.y), new Point(object.x + object.width, object.y)};
//        objectLines[2] = new Point[] {new Point(object.x + object.width, object.y), new Point(object.x + object.width, object.y + object.height)};
//        objectLines[3] = new Point[] {new Point(object.x, object.y + object.height), new Point(object.x + object.width, object.y + object.height)};
//
//        // Start and end of lines
//        // Would be more efficient to use a single data structure, but preserving for readability
//        Point[][] horizontalLines = new Point[lineSegments][2];
//        for (int i = 0; i < lineSegments; i++){
//            horizontalLines[i][0] = new Point(i * (height / lineSegments), 0);
//            horizontalLines[i][1] = new Point(i * (height / lineSegments), width);
//        }
//
//        Point[][] verticalLines = new Point[lineSegments][2];
//        for(int i = 0; i < lineSegments; i ++){
//            verticalLines[i][0] = new Point(i * (width / lineSegments), 0);
//            verticalLines[i][1] = new Point(i * (width / lineSegments), height);
//        }
//
//        boolean intersectionFound = false;
//        boolean horizontalIntersectionFound = false;
//        boolean verticalIntersectionFound = false;
//        // Check for intersections
//        for(int i = 0; i < lineSegments && !intersectionFound; i++){
//            for(int j = 0; j < 4 && !intersectionFound; j++){
//                verticalIntersectionFound = verticalIntersectionFound? true :intersect(objectLines[j][0], objectLines[j][1], verticalLines[i][0], verticalLines[i][1])
//                horizontalIntersectionFound = horizontalIntersectionFound? true :intersect(objectLines[j][0], objectLines[j][1], horizontalLines[i][0], horizontalLines[i][1]);
//                intersectionFound = verticalIntersectionFound && horizontalIntersectionFound;
//            }
//        }
        boolean intersectionFound = false;
        for(int i = 0; i < segPointNum/2 && !intersectionFound; i++){
            for(int j = 0; j < segPointNum/2 && !intersectionFound; j++){
                Point currPoint = new Point(((i + 1) * width/divNum), ((j+1) * height/divNum) );
                intersectionFound = object.contains(currPoint);
            }
        }
        return intersectionFound;
    }
    private static boolean intersect(Point l1start, Point l1end, Point l2start, Point l2end){

        double q = (l1start.y - l2start.y) * (l2end.x - l2start.x) - (l1start.x - l2start.x) * (l2end.y - l2start.y);
        double d = (l1end.x - l1start.x) * (l2end.y - l2start.y) - (l1end.y - l1start.y) * (l2end.x - l2start.x);

        if( d == 0 )
        {
            return false;
        }

        double r = q / d;

        q = (l1start.y - l2start.y) * (l1end.x - l1start.x) - (l1start.x - l2start.x) * (l1end.y - l1start.y);
        double s = q / d;

        if( r < 0 || r > 1 || s < 0 || s > 1 )
        {
            return false;
        }

        return true;
    }
    private static double euclidianDistance(Point p1, Point p2){
        // For readability
        double x = Math.abs(p1.x - p2.x);
        double y = Math.abs(p1.y - p2.y);
        return Math.sqrt(x*x + y*y);
    }

}
