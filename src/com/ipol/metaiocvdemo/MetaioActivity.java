package com.ipol.metaiocvdemo;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;

public class MetaioActivity extends ARViewActivity {

	private static final String TAG = "MetaioActivity";

	private MetaioSDKCallback mMetaioSDKCallback;
	private TextView txtFramerate;
	private ImageView imgPreview;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			Log.e("","onManagerConnected");
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");

				mMetaioSDKCallback = new MetaioSDKCallback(MetaioActivity.this, metaioSDK);
				metaioSDK.registerCallback(mMetaioSDKCallback);

				// Vector2di imageResolution = new Vector2di();
				// Vector2d focalLengths = new Vector2d();
				// Vector2d principalPoint = new Vector2d();
				// Vector4d distortion = new Vector4d();
				// metaioSDK.getCameraParameters(imageResolution, focalLengths,
				// principalPoint,
				// distortion);
				// imageResolution.setX(640);
				// imageResolution.setY(480);
				// metaioSDK.setCameraParameters(imageResolution, focalLengths,
				// principalPoint, distortion);
				// Log.e("", "imageResolution: "+imageResolution);
			}
				break;
			case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION:
				System.out.println("INCOMPATIBLE_MANAGER_VERSION");
				break;
			case LoaderCallbackInterface.INIT_FAILED:
				System.out.println("INIT_FAILED");
				break;
			case LoaderCallbackInterface.INSTALL_CANCELED:
				System.out.println("INSTALL_CANCELED");
				break;
			case LoaderCallbackInterface.MARKET_ERROR:
				System.out.println("MARKET_ERROR");
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DisplayHelper.INSTANCE.init(this);

		// mMetaioSDKCallback = new MetaioSDKCallback(this, metaioSDK);
		// metaioSDK.registerCallback(mMetaioSDKCallback);

	}

	@Override
	protected int getGUILayout() {
		return R.layout.activity_metaio;
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
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_7, this, mLoaderCallback);
		metaioSDK.requestCameraImage();
	}

}
