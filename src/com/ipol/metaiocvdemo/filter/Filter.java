package com.ipol.metaiocvdemo.filter;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import android.graphics.Bitmap;

public class Filter {
	
	Size size;
	float displayFactorX;
	float displayFactorY;

	public Bitmap processFrame(Mat image) {
		return null;
	}
	
	public void setDetectionEnabled(boolean enabled) {
		
	}
	
	public int angle(Point origin, Point target) {
	    int n = 90 - (int) ((Math.atan2(origin.y - target.y, origin.x - target.x)) * 180 / Math.PI);
	    return n % 360;
	}
	
	public double angle(Point pt1, Point pt2, Point pt0) {
		double dx1 = pt1.x - pt0.x;
		double dy1 = pt1.y - pt0.y;
		double dx2 = pt2.x - pt0.x;
		double dy2 = pt2.y - pt0.y;
		return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
	}
}
