package com.ipol.metaiocvdemo;

import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;

import com.ipol.metaiocvdemo.filter.FeatureDetection;
import com.ipol.metaiocvdemo.filter.GoalDetectionFilter;
import com.ipol.metaiocvdemo.filter.Marker;
import com.metaio.sdk.jni.IMetaioSDKAndroid;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.ImageStruct;

public class MetaioSDKCallback extends IMetaioSDKCallback {

	private MetaioActivity activity;
	private IMetaioSDKAndroid sdk;

	private GoalDetectionFilter goalDetectionFilter;
	private FeatureDetection featureDetection;

	private Paint markerPaint;

	private int frameCount = 0;

	public MetaioSDKCallback(MetaioActivity activity, IMetaioSDKAndroid sdk) {
		this.activity = activity;
		this.sdk = sdk;
		goalDetectionFilter = new GoalDetectionFilter();
		featureDetection = new FeatureDetection();
		markerPaint = new Paint();
		markerPaint.setColor(Color.parseColor("#ff0000"));
	}

	@Override
	public void onNewCameraFrame(ImageStruct cameraFrame) {
		super.onNewCameraFrame(cameraFrame);
		frameCount++;

		if (frameCount % 5 == 0) {
			activity.updateFramerate();
			new ConvertTask(cameraFrame).execute();

			// Mat mat = getMat(cameraFrame);
			// featureDetection.processFrame(mat);
		}
		sdk.requestCameraImage();
	}

	@Override
	public void onSDKReady() {
		super.onSDKReady();
	}

	private Bitmap bmp;

	public Mat getMat(ImageStruct src) {
		int width = src.getWidth();
		int height = src.getHeight();
		bmp = src.getBitmap();
		Mat mat = new Mat(new Size(width, height), CvType.CV_8UC3);
		if (bmp != null) {
			Utils.bitmapToMat(bmp, mat, false);
		}
		return mat;
	}

	private class ConvertTask extends AsyncTask<Void, Void, Void> {

		private ImageStruct cameraFrame;
		private Bitmap matBitmap;
		List<Marker> possibleMarkers;

		public ConvertTask(ImageStruct cameraFrame) {
			this.cameraFrame = new ImageStruct(cameraFrame);
		}

		@Override
		protected Void doInBackground(Void... params) {
			Mat mat = getMat(cameraFrame);
//			possibleMarkers = goalDetectionFilter.processFrame(mat);
			featureDetection.processFrame(mat);

//			matBitmap = Bitmap.createBitmap((int) mat.size().width, (int) mat.size().height, Bitmap.Config.ARGB_8888);
//			drawMarkers(possibleMarkers, matBitmap);

			return null;
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

		activity.updatePreview(bmp);
	}

}