package com.ipol.metaiocvdemo.filter;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.ipol.metaiocvdemo.DisplayHelper;


public class GoalDetectionFilter extends Filter {

	private static final int factor = 4;
	private static final boolean DEBUG = true;

	private int counter;
	private Bitmap bmp;
	private Paint markerPaint;

	private Mat mGrayscaleImage = new Mat();
	private Mat mThresholdImage = new Mat();

	Mat mHierarchy = new Mat();

	private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
	private List<MatOfPoint> hull = new ArrayList<MatOfPoint>();
	private List<Marker> possibleMarkers = new ArrayList<Marker>();

	private int m_minContourLengthAllowed;
	
	public GoalDetectionFilter() {
		super();
		markerPaint = new Paint();
		markerPaint.setColor(Color.parseColor("#ff0000"));
	}
	
	private Size filterSize;

	@Override
	public Bitmap processFrame(Mat bigImage) {
		Mat image = null;
		possibleMarkers.clear();

		// if (counter % 5 == 0) {

		filterSize = bigImage.size();
		Size newSize = new Size(filterSize.width / factor, filterSize.height / factor);

		this.size = new Size(newSize.width, newSize.height);
		this.displayFactorX = (float) (DisplayHelper.INSTANCE.getDisplayHeight() / filterSize.width);
		this.displayFactorY = (float) (DisplayHelper.INSTANCE.getDisplayWidth() / filterSize.height);

		image = new Mat();
		Imgproc.resize(bigImage, image, newSize);

		detectWithWhiteMethod(image);
		
		// }
		
		if (DEBUG) {
			bmp = Bitmap.createBitmap((int) filterSize.width, (int) filterSize.height, Bitmap.Config.ARGB_8888);
			drawMarkers(possibleMarkers, bmp);
		} else {
			bmp = null;
		}

		counter++;
		return bmp;
	}

	public void detectWithWhiteMethod(Mat image) {

		hull.clear();

		Imgproc.cvtColor(image, mGrayscaleImage, Imgproc.COLOR_BGRA2GRAY);
		Imgproc.medianBlur(mGrayscaleImage, mGrayscaleImage, 0);
		Imgproc.adaptiveThreshold(mGrayscaleImage, mThresholdImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
				Imgproc.THRESH_BINARY_INV, 7, 7);

		findContoursInImage(mThresholdImage, mContours, mGrayscaleImage.cols() / 5);

		if (mContours.size() > 0) {
			
			MatOfInt mOi = null;
			int[] intlist;
			Point[] l;
			for (int i = 0; i < mContours.size(); i++) {

				mOi = new MatOfInt();
				Imgproc.convexHull(mContours.get(i), mOi);
				intlist = mOi.toArray();
				l = new Point[intlist.length];
				for (int j = 0; j < intlist.length; j++)
					l[j] = (mContours.get(i).toList().get(mOi.toList().get(j)));

				hull.add(new MatOfPoint(l));
			}
		}

		 findCandidates(hull, possibleMarkers);
	}

	public void performThreshold(Mat grayscale, Mat thresholdImg) {
		Imgproc.adaptiveThreshold(grayscale, thresholdImg, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
				Imgproc.THRESH_BINARY_INV, 7, 7);
	}

	public void findContoursInImage(Mat thresholdImg, List<MatOfPoint> contours, int minContourPointsAllowed) {

		m_minContourLengthAllowed = minContourPointsAllowed;

		ArrayList<MatOfPoint> allContours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(thresholdImg, allContours, mHierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

		contours.clear();

		int allContoursSize = allContours.size();
		for (int i = 0; i < allContoursSize; i++) {
			int contourSize = allContours.get(i).rows() * allContours.get(i).cols();
			if (contourSize > minContourPointsAllowed) {
				contours.add(allContours.get(i));
			}
		}
	}

	public void findCandidates(List<MatOfPoint> contours, List<Marker> detectedMarkers) {

//		float xFactor = (float) (referenceFrame.size().width / size.width);
//		float yFactor = (float) (referenceFrame.size().height / size.height);
		float xFactor = factor;
		float yFactor = factor;

		MatOfPoint2f approxCurve = new MatOfPoint2f();
		ArrayList<Marker> _possibleMarkers = new ArrayList<Marker>();

		ArrayList<MatOfPoint2f> approxPoly = new ArrayList<MatOfPoint2f>();

		// For each contour, analyze if it is a parallelepiped likely to be the
		// marker
		for (int i = 0; i < contours.size(); i++) {
			// Approximate to a polygon
			double eps = (contours.get(i).cols() * contours.get(i).rows()) * 1;
			Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), approxCurve, eps, true);

			approxPoly.add(approxCurve);

			// We interested only in polygons that contains only four points
			if ((approxCurve.cols() * approxCurve.rows()) != 4)
				continue;

			// And they have to be convex
			if (!Imgproc.isContourConvex(new MatOfPoint(approxCurve.toArray())))
				continue;

			// Ensure that the distance between consecutive points is large
			// enough
			float minDist = Float.MAX_VALUE;

			for (int p = 0; p < 4; p++) {
				Point side = new Point(approxCurve.toArray()[p].x - approxCurve.toArray()[(p + 1) % 4].x,
						approxCurve.toArray()[p].y - approxCurve.toArray()[(p + 1) % 4].y);
				float squaredSideLength = (float) side.dot(side);
				minDist = Math.min(minDist, squaredSideLength);
			}

			// Check that distance is not very small
			if (minDist < m_minContourLengthAllowed)
				continue;

			if (minDist > 100000)
				continue;

			// Mat approxMat = new Mat(approxCurve);

			float area = (float) Math.abs(Imgproc.contourArea(approxCurve));

			if (approxCurve.rows() * approxCurve.cols() == 4 && area > (400 * 20) / factor
					&& area < (4000 * 20) / factor && Imgproc.isContourConvex(new MatOfPoint(approxCurve.toArray()))) {

				double maxCosine = 0;

				for (int j = 2; j < 5; j++) {
					double cosine = Math.abs(angle(approxCurve.toArray()[j % 4], approxCurve.toArray()[j - 2],
							approxCurve.toArray()[j - 1]));
					maxCosine = Math.max(maxCosine, cosine);
				}

				if (maxCosine > 0.3) {
					continue;
				}
			}

			RotatedRect minRect = Imgproc.minAreaRect(approxCurve);

			Point center = minRect.center;

			center.x *= xFactor;
			center.y *= yFactor;

			Point lowerLeft = center;
			Point lowerRight = center;
			Point upperLeft = center;
			Point upperRight = center;

			for (int j = 0; j < 4; j++) {

				Point controlPoint = approxCurve.toArray()[j];

				controlPoint.x *= (xFactor);
				controlPoint.y *= (yFactor);

				if (controlPoint.x > center.x) {
					// Right
					if (controlPoint.y > center.y) {
						lowerRight = controlPoint;
					} else {
						upperRight = controlPoint;
					}

				} else {
					// Left
					if (controlPoint.y > center.y) {
						lowerLeft = controlPoint;
					} else {
						upperLeft = controlPoint;
					}
				}

			}

			float differenceOfUpperY = (float) Math.abs(upperLeft.y - upperRight.y);
			float differenceOfLowerY = (float) Math.abs(lowerLeft.y - lowerRight.y);

			float differenceOfRightX = (float) Math.abs(upperRight.x - lowerRight.x);
			float differenceOfLeftX = (float) Math.abs(lowerLeft.x - upperLeft.x);

			float tolerance = 15;

			if (differenceOfUpperY > tolerance || differenceOfLowerY > tolerance || differenceOfRightX > tolerance
					|| differenceOfLeftX > tolerance) {
				continue;
			}

			float width = (float) Math.sqrt(Math.pow(lowerLeft.x - lowerRight.x, 2)
					+ Math.pow(lowerLeft.y - lowerRight.y, 2));
			float height = (float) Math.sqrt(Math.pow(upperLeft.x - lowerLeft.x, 2)
					+ Math.pow(upperRight.y - lowerRight.y, 2));

			float ratio = width / height;
			if (height > width) {
				ratio = height / width;
			}

//			 if (ratio >= [[VFSettings sharedSettings] goalRatio]) {
//			 continue;
//			 }

//			 exclude big rectangles
//			 if (width > [[VFSettings sharedSettings] maximumGoalWidth]) {
//			 continue;
//			 }

			// All tests are passed. Save marker candidate:
			Marker m = new Marker();

			m.upperLeft = upperLeft;
			m.lowerRight = lowerRight;
			m.upperRight = upperRight;
			m.lowerLeft = lowerLeft;

			for (int p = 0; p < 4; p++)
				m.points.add(new Point(approxCurve.toArray()[p].x * xFactor, approxCurve.toArray()[p].y * yFactor));

			// Sort the points in anti-clockwise order
			// Trace a line between the first and second point.
			// If the third point is at the right side, then the points are
			// anti-clockwise
			Point v1 = new Point(m.points.get(1).x - m.points.get(0).x, m.points.get(1).y - m.points.get(0).y);
			Point v2 = new Point(m.points.get(2).x - m.points.get(0).x, m.points.get(2).y - m.points.get(0).y);

			double o = (v1.x * v2.y) - (v1.y * v2.x);

			if (o < 0.0) // if the third point is in the left side, then sort in
							// anti-clockwise order
				Collections.swap(m.points, 1, 3);

			_possibleMarkers.add(m);
		}
		
		// if (approxPoly.size() > 0)
		// cv::drawContours(m_grayscaleImage, approxPoly, -1, cv::Scalar(255,
		// 255, 9));

		// Remove these elements which corners are too close to each other.
		// First detect candidates for removal:
		ArrayList<SimpleEntry<Integer, Integer>> tooNearCandidates = new ArrayList<SimpleEntry<Integer, Integer>>();
		for (int i = 0; i < _possibleMarkers.size(); i++) {
			Marker m1 = _possibleMarkers.get(i);

			// calculate the average distance of each corner to the nearest
			// corner of the other marker candidate
			for (int j = i + 1; j < _possibleMarkers.size(); j++) {
				Marker m2 = _possibleMarkers.get(j);

				float distSquared = 0;

				for (int c = 0; c < 4; c++) {
					Point v = new Point(m1.points.get(c).x - m2.points.get(c).x, m1.points.get(c).y
							- m2.points.get(c).y);
					distSquared += v.dot(v);
				}

				distSquared /= 4;

				if (distSquared < 100 * xFactor * yFactor) {
					tooNearCandidates.add(new SimpleEntry<Integer, Integer>(i, j));
				}
			}
		}

		// Mark for removal the element of the pair with smaller perimeter
		boolean[] removalMask = new boolean[_possibleMarkers.size()];

		for (int i = 0; i < tooNearCandidates.size(); i++) {
			float p1 = TinyLA.perimeter(_possibleMarkers.get(tooNearCandidates.get(i).getKey()).points);
			float p2 = TinyLA.perimeter(_possibleMarkers.get(tooNearCandidates.get(i).getValue()).points);

			int removalIndex;
			if (p1 > p2)
				removalIndex = tooNearCandidates.get(i).getValue();
			else
				removalIndex = tooNearCandidates.get(i).getKey();

			removalMask[removalIndex] = true;
		}

		// Return candidates
		// detectedMarkers.clear();
		for (int i = 0; i < _possibleMarkers.size(); i++) {
			if (!removalMask[i])
				possibleMarkers.add(_possibleMarkers.get(i));
		}
		
	}
	
	public void drawMarkers(List<Marker> markers, Bitmap bmp) {

		Canvas canvas = new Canvas(bmp);
		for (Marker m : markers) {
			List<Point> points = m.getPoints();
			Path rect = new Path();
			rect.moveTo((float) points.get(0).x, (float) points.get(0).y);
			rect.lineTo((float) points.get(1).x, (float) points.get(1).y);
			rect.lineTo((float) points.get(2).x, (float) points.get(2).y);
			rect.lineTo((float) points.get(3).x, (float) points.get(3).y);
			rect.lineTo((float) points.get(0).x, (float) points.get(0).y);
			canvas.drawPath(rect, markerPaint);
		}
	}
}
