package com.ipol.metaiocvdemo;

import org.opencv.android.OpenCVLoader;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKAndroid;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.Vector2d;
import com.metaio.sdk.jni.Vector2di;
import com.metaio.sdk.jni.Vector4d;

public class MetaioActivity extends ARViewActivity {

	private static final String TAG = "MetaioActivity";

	private MetaioSDKCallback mMetaioSDKCallback;
	private TextView txtFramerate;
	private ImageView imgPreview;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DisplayHelper.INSTANCE.init(this);
	}

	@Override
	protected int getGUILayout() {
		return R.layout.activity_metaio;
	}

	@Override
	public void onSurfaceCreated() {
		super.onSurfaceCreated();
		// Setup auto-focus
//        Camera camera = IMetaioSDKAndroid.getCamera(this);
//        Camera.Parameters params = camera.getParameters();
//        params.setFocusMode("continuous-picture");
//        camera.setParameters(params);
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler() {
		return mMetaioSDKCallback;
	}

	@Override
	protected void loadContents() {

	}

	@Override
	protected void onGeometryTouched(IGeometry geometry) {

	}

	public void updateFramerate() {
		if (txtFramerate != null) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					txtFramerate.setText(metaioSDK.getCameraFrameRate() + " fps");
				}
			});
		} else {
			txtFramerate = (TextView) findViewById(R.id.txt_framerate);
		}
	}

	public void updatePreview(final Bitmap preview) {
		if (imgPreview != null) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					imgPreview.setImageBitmap(preview);
				}
			});
		} else {
			imgPreview = (ImageView) findViewById(R.id.img_preview);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (OpenCVLoader.initDebug()) {
			mMetaioSDKCallback = new MetaioSDKCallback(MetaioActivity.this, metaioSDK);
			metaioSDK.registerCallback(mMetaioSDKCallback);
			metaioSDK.requestCameraImage();
			
			Vector2di imageResolution = new Vector2di();
			Vector2d focalLengths = new Vector2d();
			Vector2d principalPoint = new Vector2d();
			Vector4d distortion = new Vector4d();
			metaioSDK.getCameraParameters(imageResolution, focalLengths, principalPoint, distortion);
			Log.e("Metaio Camera", "resolution: "+imageResolution.toString());
		}
	}

}
