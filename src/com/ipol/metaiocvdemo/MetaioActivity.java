package com.ipol.metaiocvdemo;

import org.opencv.android.OpenCVLoader;

import android.graphics.Bitmap;
import android.graphics.Color;
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
	private TextView txtDistance;
	private TextView txtAngle;
	private TextView txtDistanceToTarget;
	private TextView txtPoints;
	private TextView txtDistanceMiss;
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
		// Camera camera = IMetaioSDKAndroid.getCamera(this);
		// Camera.Parameters params = camera.getParameters();
		// params.setFocusMode("continuous-picture");
		// camera.setParameters(params);
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

	public void updateLabel(final String label, final String value, final boolean valid) {
		if (label.equals("angle")) {
			if (txtAngle != null) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						txtAngle.setText(label + ": " + value);
						if (valid)
							txtAngle.setTextColor(Color.GREEN);
						else
							txtAngle.setTextColor(Color.RED);
					}
				});
			} else {
				txtAngle = (TextView) findViewById(R.id.txt_angle);
			}
		} else if (label.equals("distance")) {
			if (txtDistance != null) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						txtDistance.setText(label + ": " + value);
						if (valid)
							txtDistance.setTextColor(Color.GREEN);
						else
							txtDistance.setTextColor(Color.RED);
					}
				});
			} else {
				txtDistance = (TextView) findViewById(R.id.txt_distance);
			}
		} else if (label.equals("distanceToTarget")) {
			if (txtDistanceToTarget != null) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						txtDistanceToTarget.setText(label + ": " + value);
						if (valid)
							txtDistanceToTarget.setTextColor(Color.GREEN);
						else
							txtDistanceToTarget.setTextColor(Color.RED);
					}
				});
			} else {
				txtDistanceToTarget = (TextView) findViewById(R.id.txt_distance_to_target);
			}
		} else if (label.equals("hitPoints")) {
			if (txtPoints != null) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						txtPoints.setText(label + ": " + value);
						if (valid)
							txtPoints.setTextColor(Color.GREEN);
						else
							txtPoints.setTextColor(Color.RED);
					}
				});
			} else {
				txtPoints = (TextView) findViewById(R.id.txt_points);
			}
		} else if (label.equals("distanceMiss")) {
			if (txtDistanceMiss != null) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						txtDistanceMiss.setText(label + ": " + value);
						if (valid)
							txtDistanceMiss.setTextColor(Color.GREEN);
						else
							txtDistanceMiss.setTextColor(Color.RED);
					}
				});
			} else {
				txtDistanceMiss = (TextView) findViewById(R.id.txt_distance_miss);
			}
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
			Log.e("Metaio Camera", "resolution: " + imageResolution.toString());
		}
	}

}
