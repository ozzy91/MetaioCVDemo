package com.ipol.metaiocvdemo.filter;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import android.util.Log;

public class FeatureTracker {
	
	private static final String TAG = "FeatureTracker";

	private Mat gray; // current gray-level image
	private Mat grayPrev; // previous gray-level image
	private MatOfPoint2f[] points = new MatOfPoint2f[2]; // tracked features
															// from
															// 0->1
	private MatOfPoint initial; // initial position of tracked points
	private MatOfPoint features; // detected features
	private int maxCount; // maximum number of features to detect
	private double qlevel; // quality level for feature detection
	private double minDist; // min distance between two points
	private MatOfByte status; // status of tracked features
	private MatOfFloat err; // error in tracking

	public boolean useFarnback;
	public List<Float> history;
	public List<Float> historyEnd;

	public Point targetPoint;

	public float qualityLevel;
	public int blurrFactor;
	public int counter; // frame counter

	public FeatureTracker() {
		maxCount = 500;
		qlevel = 0.09;
		minDist = 30;

		gray = new Mat();
		grayPrev = new Mat();
		status = new MatOfByte();
		err = new MatOfFloat();
		points[0] = new MatOfPoint2f();
		points[1] = new MatOfPoint2f();
		initial = new MatOfPoint();
		features = new MatOfPoint();
		history = new ArrayList<Float>();
		historyEnd = new ArrayList<Float>();
	}

	public void resetCounter() {
		this.counter = 0;
		this.history.clear();
	}

	public MatOfPoint getInitials() {
		return initial;
	}

	public MatOfPoint2f getPreviousPoints() {
		return points[0];
	}

	public MatOfPoint2f getCurrentPoints() {
		return points[1];
	}

	public void restart() {
		resetCounter();
		points[0] = new MatOfPoint2f();
		points[1] = new MatOfPoint2f();
		initial = new MatOfPoint();
		features = new MatOfPoint();
		historyEnd.clear();
		grayPrev.release();
	}

	public void process(Mat frame, Mat output) {
		minDist = 10;
		maxCount = 500;

		Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
		if (blurrFactor != 0)
			Imgproc.medianBlur(gray, gray, blurrFactor);

		if (addNewPoints()) {
			// detect feature points
			detectFeaturePoints();

			// add the detected features to the currently tracked features
			ArrayList<Point> tmpList = new ArrayList<Point>();
			tmpList.addAll(points[0].toList());
			tmpList.addAll(features.toList());
			// points[0].addAll(features.toList());
			Point[] array = new Point[tmpList.size()];
			for (int i = 0; i < tmpList.size(); i++)
				array[i] = tmpList.get(i);
			points[0] = new MatOfPoint2f(array);

			tmpList.clear();
			Log.v(TAG, "initial size: "+initial.toList().size());
			Log.v(TAG, "features size: "+features.toList().size());
			tmpList.addAll(initial.toList());
			tmpList.addAll(features.toList());
			// initial.insert(initial.end(),features.begin(),features.end());
			array = new Point[tmpList.size()];
			for (int i = 0; i < tmpList.size(); i++)
				array[i] = tmpList.get(i);
			initial = new MatOfPoint(array);
			Log.v(TAG, "array size: "+array.length);

			ArrayList<Float> frames = new ArrayList<Float>();
			int pointsSize = points[0].cols() * points[0].rows();
			for (int i = 0; i < pointsSize; i++) {
				frames.add(counter / 1f);
			}
			history.addAll(frames);
		}

		// for first image of the sequence
		if (grayPrev.empty())
			gray.copyTo(grayPrev);

		if ((points[0].cols() * points[0].rows()) == 0)
			return;

		Video.calcOpticalFlowPyrLK(grayPrev, gray, // 2 consecutive images
				points[0], // input point position in first image
				points[1], // output point postion in the second image
				status, // tracking success
				err, new Size(40, 40), 5); // tracking error

		ArrayList<Float> frames2 = new ArrayList<Float>();
		int historySize = history.size();
		for (int i = 0; i < historySize; i++) {
			frames2.add(counter / 1f);
		}
		historyEnd = frames2;

		// 2. loop over the tracked points to reject the undesirables
		int k = 0;
		int numberOfPoints = points[1].cols() * points[1].rows();
		for (int i = 0; i < numberOfPoints; i++) {

			// do we keep this point?
			if (acceptTrackedPoint(i)) {

				// keep this point in vector
				// initial[k] = initial[i];
				// points[1][k++] = points[1][i];
				// history[k - 1] = history[i];
				Point[] array = initial.toArray();
				Log.v(TAG, "initial array length: "+array.length);
				array[k] = array[i];
				initial = new MatOfPoint(array);
				array = points[1].toArray();
				array[k++] = array[i];
				points[1] = new MatOfPoint2f(array);
				history.set(k - 1, history.get(i));
			}
		}

		// eliminate unsuccesful points

		// points[1] = new MatOfPoint2f((Point[])
		// points[1].toList().subList(0, k).toArray());
		List<Point> tmpList = points[1].toList().subList(0, k);
		Point[] array = new Point[tmpList.size()];
		for (int i = 0; i < tmpList.size(); i++)
			array[i] = tmpList.get(i);
		points[1] = new MatOfPoint2f(array);

		// initial = new MatOfPoint((Point[]) initial.toList().subList(0,
		// k).toArray());
		tmpList = initial.toList().subList(0, k);
		array = new Point[tmpList.size()];
		for (int i = 0; i < tmpList.size(); i++)
			array[i] = tmpList.get(i);
		initial = new MatOfPoint(array);

		history = history.subList(0, k);

		counter++;
		
		if (counter > 50) {
			Log.e(TAG, "restart");
			restart();
		}
	}

	public void detectFeaturePoints() {
		Imgproc.goodFeaturesToTrack(gray, features, maxCount, qualityLevel, minDist);
	}

	public boolean addNewPoints() {
		return (points[0].cols() * points[0].rows()) <= 10;
	}
	
	public void swapPoints() {
		// 4. current points and image become previous ones
		MatOfPoint2f tmp = points[0];
		points[0] = points[1];
		points[1] = tmp;
		Mat tmpMat = grayPrev;
		grayPrev = gray;
		gray = tmpMat;
	}

	public boolean acceptTrackedPoint(int i) {
		return ((Byte) status.toArray()[i]).toString().equals("0") &&
		// if point has moved
				(Math.abs(points[0].toArray()[i].x - points[1].toArray()[i].x)
						+ (Math.abs(points[0].toArray()[i].y - points[1].toArray()[i].y)) > 1);
	}

}