package com.ipol.metaiocvdemo;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.Log;

import com.ipol.metaiocvdemo.filter.FeatureDetection;
import com.ipol.metaiocvdemo.filter.GoalDetectionFilter;
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
		featureDetection = new FeatureDetection(activity);
		markerPaint = new Paint();
		markerPaint.setColor(Color.parseColor("#ff0000"));
	}

	@Override
	public void onNewCameraFrame(ImageStruct cameraFrame) {
		super.onNewCameraFrame(cameraFrame);
		frameCount++;

		if (frameCount % 2 == 0) {
			activity.updateFramerate();
			new ConvertTask(cameraFrame).execute();
			
//			Mat mat = getMat(cameraFrame);
//			bitmap = goalDetectionFilter.processFrame(mat);
//			bitmap = featureDetection.processFrame(mat);
//			if (bitmap != null)
//				activity.updatePreview(bitmap);
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
		private Bitmap bitmap;

		public ConvertTask(ImageStruct cameraFrame) {
			this.cameraFrame = new ImageStruct(cameraFrame);
		}

		@Override
		protected Void doInBackground(Void... params) {
			Mat mat = getMat(cameraFrame);
//			bitmap = goalDetectionFilter.processFrame(mat);
			long starttime = System.currentTimeMillis();
			bitmap = featureDetection.processFrame(mat);
			Log.e("timer", "processFrame finished after " + (System.currentTimeMillis() - starttime));

			if (bitmap != null)
				activity.updatePreview(bitmap);

			return null;
		}
	}

}