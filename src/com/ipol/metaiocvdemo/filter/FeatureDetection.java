package com.ipol.metaiocvdemo.filter;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;

import com.ipol.metaiocvdemo.DisplayHelper;

public class FeatureDetection extends Filter {

	private static final int factor = 6;
	private static final boolean DEBUG = true;

	private Bitmap bmp;
	private Scalar pinkScalar = new Scalar(255, 0, 255);
	private Scalar yellowScalar = new Scalar(255, 255, 0);

	private Point targetPoint;

	private int blurrFactor = 9;
	private MatOfPoint initial;
	private MatOfPoint features;
	private MatOfPoint initialFound;
	private MatOfPoint toFound;
	private MatOfPoint2f points;
	private MatOfPoint2f previousPoints;
	private Point target;
	private Point hitPointFrom;
	private Point hitPointTo;

	private List<Float> history;
	private List<Float> historyEnd;

	private FeatureTracker tracker;
	private int counter;

	public FeatureDetection() {
		tracker = new FeatureTracker();
		targetPoint = new Point();

		hitPointTo = new Point();
		hitPointFrom = new Point();
	}

	@Override
	public Bitmap processFrame(Mat image) {

		counter++;

		Size filterSize = image.size();
		Size newSize = new Size(filterSize.width / factor, filterSize.height / factor);

		int ballRadius = Math.round((150 * 1.2f) / factor);

		targetPoint.x = DisplayHelper.INSTANCE.getDisplayHeight() / 2;
		targetPoint.y = DisplayHelper.INSTANCE.getDisplayWidth() / 2;

		double factorX = DisplayHelper.INSTANCE.getDisplayHeight() / newSize.width;
		double factorY = DisplayHelper.INSTANCE.getDisplayWidth() / newSize.height;

		double dfactorX = DisplayHelper.INSTANCE.getDisplayHeight() / filterSize.width;
		double dfactorY = DisplayHelper.INSTANCE.getDisplayWidth() / filterSize.height;

		Mat frame = new Mat();
		Imgproc.resize(image, frame, newSize);

		tracker.blurrFactor = blurrFactor;
		tracker.qualityLevel = 0.01f;
		tracker.process(frame, frame);

		initial = tracker.getInitials();
		points = tracker.getCurrentPoints();
		previousPoints = tracker.getPreviousPoints();

		history = tracker.history;
		historyEnd = tracker.historyEnd;

		if (DEBUG) {
			displayPoints(image, initial, points);
		}

		Core.circle(image, new Point(targetPoint.x / dfactorX, targetPoint.y / dfactorY), ballRadius, yellowScalar);

		target = new Point(targetPoint.x / factorX, targetPoint.y / factorY);

		int numberOfPoints = points.cols() * points.rows();
		int numberOfInitials = initial.cols() * initial.rows();

		if (DEBUG) {
			Point to = hitPointTo;
			Point from = hitPointFrom;

			Core.line(image, from, to, pinkScalar, 10);
		}

		ArrayList<ShootingPoint> nearPointsTo = new ArrayList<ShootingPoint>();
		ArrayList<ShootingPoint> hitPointsTo = new ArrayList<ShootingPoint>();
		ArrayList<ShootingPoint> missPoints = new ArrayList<ShootingPoint>();

		float frameAverage = 0;

		int numberOfFoundPoints = 0;
		int numberOfHitPoints = 0;
		int numberOfMissPoints = 0;
		double averageRight = 0;

		// get all points inside the ball radius
		for (int i = 0; i < numberOfPoints; i++) {

			Point p = points.toArray()[i];
			Point initialPoint = initial.toArray()[i];
			Point prevoisPpint = previousPoints.toArray()[i];

			float frameStart = history.get(i);
			float frameEnd = historyEnd.get(i);

			if (numberOfInitials > i) {

				if (p.x > averageRight)
					averageRight = p.x;

				float shootingDistance = (float) Math.sqrt(Math.pow(p.x * factorX - initialPoint.x * factorX, 2)
						+ Math.pow(p.y * factorX - initialPoint.y * factorX, 2));
				float speed = (float) Math.sqrt(Math.pow(p.x * factorX - prevoisPpint.x * factorX, 2)
						+ Math.pow(p.y * factorY - prevoisPpint.y * factorY, 2));

				boolean outsideBall = Math.pow((p.x - target.x), 2) + Math.pow((p.y - target.y), 2) < Math.pow(
						ballRadius * 2, 2);
				boolean insideBall = Math.pow((p.x - target.x), 2) + Math.pow((p.y - target.y), 2) < Math.pow(
						ballRadius, 2);

				ShootingPoint shootingPoint = new ShootingPoint();

				shootingPoint.to = new Point(p.x * factorX, p.y * factorY);
				shootingPoint.from = new Point(initialPoint.x * factorX, initialPoint.y * factorY);
				shootingPoint.speed = speed;
				shootingPoint.distance = shootingDistance;

				float sinus = angle(initialPoint, p);
				shootingPoint.angle = sinus;

				if (outsideBall) {

					nearPointsTo.add(shootingPoint);

					if (insideBall) {
						hitPointsTo.add(shootingPoint);
						frameAverage += (frameEnd - frameStart);
						numberOfHitPoints++;
					}

					numberOfFoundPoints++;

				} else {
					missPoints.add(shootingPoint);
					numberOfMissPoints++;
				}
			}
		}

		float averageDistanceMiss = 0;

		for (int i = 0; i < numberOfMissPoints; i++) {

			ShootingPoint shootingPoint = missPoints.get(i);
			averageDistanceMiss += shootingPoint.distance;
		}

		averageDistanceMiss /= numberOfMissPoints;

		if (numberOfHitPoints == 0) {
			for (int i = 0; i < numberOfPoints; i++) {

				Point p = points.toArray()[i];
				Point initialPoint = initial.toArray()[i];
				Point prevoisPpint = previousPoints.toArray()[i];

				boolean intersects = circleLineIntersect(initialPoint.x, initialPoint.y, p.x, p.y, target.x, target.y,
						ballRadius);

				boolean fullStike = (initialPoint.y > target.y && p.y < target.y);

				if (intersects && fullStike) {

					float shootingDistance = (float) Math.sqrt(Math.pow(p.x * factorX - initialPoint.x * factorX, 2)
							+ Math.pow(p.y * factorY - initialPoint.y * factorY, 2));
					float speed = (float) Math.sqrt(Math.pow(p.x * factorX - prevoisPpint.x * factorX, 2)
							+ Math.pow(p.y * factorY - prevoisPpint.y * factorY, 2));

					Core.line(image, drawablePoint(p), drawablePoint(p), pinkScalar, 10);

					ShootingPoint shootingPoint = new ShootingPoint();

					shootingPoint.to = new Point(p.x * factorX, p.y * factorY);
					shootingPoint.from = new Point(initialPoint.x * factorX, initialPoint.y * factorY);
					shootingPoint.speed = speed;
					shootingPoint.distance = shootingDistance;

					float sinus = angle(initialPoint, p);
					shootingPoint.angle = sinus;

					nearPointsTo.add(shootingPoint);
					hitPointsTo.add(shootingPoint);

					numberOfHitPoints++;
					numberOfFoundPoints++;
				}

			}
		}

		frameAverage /= numberOfHitPoints;

		float averageAngle = 0;
		float averageDistance = 0;
		float averageToX = 0;
		float averageToY = 0;
		float averageFromX = 0;
		float averageFromY = 0;

		float averageSpeed = 0;

		for (int i = 0; i < numberOfFoundPoints; i++) {

			ShootingPoint shootingPoint = nearPointsTo.get(i);

			averageDistance += shootingPoint.distance;
			averageAngle += shootingPoint.angle;

			averageToX += shootingPoint.to.x;
			averageToY += shootingPoint.to.y;

			averageFromX += shootingPoint.from.x;
			averageFromY += shootingPoint.from.y;

			averageSpeed += shootingPoint.speed;
		}

		averageAngle /= numberOfFoundPoints;
		averageDistance /= numberOfFoundPoints;

		averageToX /= numberOfFoundPoints;
		averageToY /= numberOfFoundPoints;

		averageFromX /= numberOfFoundPoints;
		averageFromY /= numberOfFoundPoints;

		averageSpeed /= numberOfFoundPoints;

		float dviationFactor = 1.5f;
		ArrayList<ShootingPoint> noDeviation = new ArrayList<ShootingPoint>();
		int noDeviationPoints = 0;

		for (int i = 0; i < numberOfFoundPoints; i++) {

			// calculate the medium
			ShootingPoint shootingPoint = nearPointsTo.get(i);

			if (Math.abs(shootingPoint.angle) < (Math.abs(averageAngle) * dviationFactor)
					&& Math.abs(shootingPoint.angle) > (Math.abs(averageAngle) / dviationFactor)) {

				noDeviation.add(shootingPoint);
				noDeviationPoints++;
			}
		}

		averageToX = 0;
		averageToY = 0;
		averageFromX = 0;
		averageFromY = 0;

		for (int i = 0; i < noDeviationPoints; i++) {

			// calculate the medium
			ShootingPoint shootingPoint = noDeviation.get(i);

			// averageDistance += shootingPoint.distance;
			averageAngle += shootingPoint.angle;

			averageToX += shootingPoint.to.x;
			averageToY += shootingPoint.to.y;

			averageFromX += shootingPoint.from.x;
			averageFromY += shootingPoint.from.y;
		}

		averageAngle /= noDeviationPoints;
		// averageDistance /= noDeviationPoints;

		averageToX /= noDeviationPoints;
		averageToY /= noDeviationPoints;

		averageFromX /= noDeviationPoints;
		averageFromY /= noDeviationPoints;

		Point averageFrom = new Point(averageFromX * factorX, averageFromY * factorY);
		Point averageTo = new Point(averageToX * factorX, averageToY * factorY);

		float newAngle = angle(averageFrom, averageTo);

		if (numberOfHitPoints > 0) {

			hitPointFrom = averageFrom;
			hitPointTo = averageTo;
		}

		if (DEBUG) {

		}

		boolean angleValid = Math.abs(newAngle) <= 55;
		float distanceToBallX = averageFromX - averageToX;
		float distanceToBallY = averageFromY - averageToY;

		// if ([[VFSettings sharedSettings] enableBackwardsShot]) {
		// if (!angleValid) {
		// angleValid = (180 - fabs(newAngle) <= [[VFSettings sharedSettings]
		// maximumShootingAngle]);
		// distanceToBallX = averageToX - averageFromX;
		// distanceToBallY = averageToY - averageFromY;
		// }
		// }

		boolean distanceValid = averageDistance >= 42;
		boolean minumumPointCountValid = numberOfHitPoints >= 1;
		boolean distanceToTargetValid = Math.sqrt(Math.pow(averageToX - target.x * factorX, 2)
				+ Math.pow(averageToY - target.y * factorY, 2)) < 1999;

		if (DEBUG) {

		}
		// CGSize moveVector = CGSizeMake(-distanceToBallX / 20 ,
		// distanceToBallY / 20 );

		boolean numberOfFramesReached = counter > 0;

		// dispatch_sync(dispatch_get_main_queue(), ^{

		boolean numberOfHitPointsBiggerThenNumberOfMissPoints = (numberOfMissPoints / 1.5f) < numberOfFoundPoints;
		boolean missedPointsLongEnough = averageDistanceMiss >= 42;

//		System.out.println(angleValid + " angleValid");
//		System.out.println(distanceValid + " distanceValid");
//		System.out.println(minumumPointCountValid + " minumumPointCountValid");
//		System.out.println("---------------");
		// System.out.println("numberOfFramesReached: "+numberOfFramesReached);
		// System.out.println("distanceToTargetValid: "+distanceToTargetValid);
		// System.out.println("angleValid: "+angleValid);
		// System.out.println("angleValid: "+angleValid);

		if (angleValid && distanceValid && minumumPointCountValid && numberOfFramesReached && distanceToTargetValid
				&& numberOfHitPoints > 0 && numberOfHitPointsBiggerThenNumberOfMissPoints && missedPointsLongEnough) {

			counter = 0;

			tracker.restart();

			// if ([self.delegate
			// respondsToSelector:@selector(filter:didHitTargetWithSpeed:atPoint:withMoveVector:)])
			// {
			// [self.delegate filter:self didHitTargetWithSpeed: averageSpeed
			// atPoint:CGPointMake(0, 0) withMoveVector:moveVector];
			// }
		}

		// });

		tracker.swapPoints();

		bmp = Bitmap.createBitmap((int) filterSize.width, (int) filterSize.height, Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(image, bmp);

		return bmp;
	}

	private boolean circleLineIntersect(double x1, double y1, double x2, double y2, double cx, double cy, double cr) {
		double dx = x2 - x1;
		double dy = y2 - y1;
		double a = dx * dx + dy * dy;
		double b = 2 * (dx * (x1 - cx) + dy * (y1 - cy));
		double c = cx * cx + cy * cy;
		c += x1 * x1 + y1 * y1;
		c -= 2 * (cx * x1 + cy * y1);
		c -= cr * cr;
		double bb4ac = b * b - 4 * a * c;

		if (bb4ac < 0) {
			return false; // No collision
		} else {
			return true; // Collision
		}
	}

	private Point drawablePoint(Point p) {
		return new Point(p.x * factor, p.y * factor);
	}

	private void displayPoints(Mat image, MatOfPoint fromPoints, MatOfPoint2f toPoints) {

		int fromPointsSize = fromPoints.cols() * fromPoints.rows();
		System.out.println("fromPointsSize: "+fromPointsSize);
		for (int i = 0; i < fromPointsSize; i++) {

			Point from = fromPoints.toArray()[i];
			Point to = toPoints.toArray()[i];

			from.x *= factor;
			from.y *= factor;

			to.x *= factor;
			to.y *= factor;

			Core.line(image, from, to, yellowScalar, 10);
		}
	}

	public class ShootingPoint {
		Point from;
		Point to;
		float angle;
		float distance;
		float speed;
	}
}
