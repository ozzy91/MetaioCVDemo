package com.ipol.metaiocvdemo.filter;

import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class FeatureDetection extends Filter {
	
	private static final int factor = 4;
	
	private int blurrFactor = 1;
	
	private FeatureTracker tracker;
	
	public FeatureDetection() {
		tracker = new FeatureTracker();
	}

	@Override
	public List<Marker> processFrame(Mat image) {

		Size filterSize = image.size();
		Size newSize = new Size(filterSize.width / factor, filterSize.height / factor);
		
		Mat frame = new Mat();
		Imgproc.resize(image, frame, newSize);
		
		tracker.blurrFactor = blurrFactor;
		tracker.qualityLevel = 0.01f;
		tracker.process(frame, frame);
		
		return null;
	}
}
