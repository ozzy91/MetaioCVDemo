package com.ipol.metaiocvdemo.filter;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Point;

public class Marker {

	Point lowerLeft;
	Point lowerRight;
	Point upperLeft;
	Point upperRight;

	List<Point> points;
	
	public Marker() {
		points = new ArrayList<Point>();
		lowerLeft = new Point();
		lowerRight = new Point();
		upperLeft = new Point();
		upperRight = new Point();
	}
	
	public List<Point> getPoints() {
		return points;
	}

	@Override
	public String toString() {
		return "["+upperLeft+"|"+upperRight+"|"+lowerRight+"|"+lowerLeft+"]";
	}
}
