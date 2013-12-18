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

import com.ipol.metaiocvdemo.filter.GoalDetectionFilter;
import com.ipol.metaiocvdemo.filter.Marker;
import com.metaio.sdk.jni.IMetaioSDKAndroid;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.ImageStruct;

public class MetaioSDKCallback extends IMetaioSDKCallback {

	private MetaioActivity activity;
	private IMetaioSDKAndroid sdk;

	private GoalDetectionFilter goalDetectionFilter;
	
	private Paint markerPaint;

	private int frameCount = 0;

	public MetaioSDKCallback(MetaioActivity activity, IMetaioSDKAndroid sdk) {
		this.activity = activity;
		this.sdk = sdk;
		goalDetectionFilter = new GoalDetectionFilter();
		markerPaint = new Paint();
		markerPaint.setColor(Color.parseColor("#ff0000"));
//		markerPaint.setStrokeWidth(8);
	}

	@Override
	public void onNewCameraFrame(ImageStruct cameraFrame) {
		super.onNewCameraFrame(cameraFrame);
		frameCount++;

		if (frameCount % 3 == 0) {
			activity.updateFramerate();

			Mat mat = getMat(cameraFrame);
			Bitmap matBitmap = Bitmap.createBitmap((int) mat.size().width,
					(int) mat.size().height, Bitmap.Config.ARGB_8888);
			List<Marker> possibleMarkers = goalDetectionFilter.processFrame(mat);
			
			drawMarkers(possibleMarkers, matBitmap);
			
//			Mat processedMat = goalDetectionFilter.processFrame(mat);

//			Utils.matToBitmap(processedMat, matBitmap, false);

//			activity.updatePreview(matBitmap);

//			new ConvertTask(cameraFrame).execute();
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

		public ConvertTask(ImageStruct cameraFrame) {
			this.cameraFrame = cameraFrame;
		}

		@Override
		protected Void doInBackground(Void... params) {
//			Mat mat = getMat(cameraFrame);
//			Mat processedMat = goalDetectionFilter.processFrame(mat);
//
//			matBitmap = Bitmap.createBitmap((int) processedMat.size().width,
//					(int) processedMat.size().height, Bitmap.Config.ARGB_8888);
//			Utils.matToBitmap(processedMat, matBitmap, false);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			activity.updatePreview(matBitmap);
		}

	}
	
	public void drawMarkers(List<Marker> markers, Bitmap bmp) {
		
		Canvas canvas = new Canvas(bmp);
		for (Marker m : markers) {
			System.out.println("marker: "+m);
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