package com.rapidminer;

import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.example.Attribute;

import java.util.logging.Level;
import com.rapidminer.tools.LogService;

public class Haversine extends DistanceMeasure {
	
	@Override
	public double calculateDistance(double[] value1, double[] value2) {
		
			double lng1 = value1[0];
			double lng2 = value2[0];
			double lat1 = value1[1];
			double lat2 = value2[1];
			
		    double earthRadius = 6371;
		    double dLat = Math.toRadians(lat2-lat1);
		    double dLng = Math.toRadians(lng2-lng1);
		    double sindLat = Math.sin(dLat / 2);
		    double sindLng = Math.sin(dLng / 2);
		    double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
		            * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
		    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		    double dist = earthRadius * c;

//		    String ausgabe = lng1 + "/" + lat1 + " - " + lng2 + "/" + lat2 + " = " + dist;
//		    LogService.getRoot().log(Level.WARNING, ausgabe);
		    return dist;
		
	}
	
	@Override
	public double calculateSimilarity(double[] value1, double[] value2) {
		
	    	LogService.getRoot().log(Level.WARNING, "Similarity");
		    return (Double) null;
		
	}
	
	@Override
	public double calculateDistance(Example firstExample, Example secondExample) {

            double[] firstValues = new double[2];
            double[] secondValues = new double[2];

            firstValues[0] = firstExample.getValue(firstExample.getAttributes().get("longitude"));
            firstValues[1] = firstExample.getValue(firstExample.getAttributes().get("latitude"));
            
            secondValues[0] = secondExample.getValue(firstExample.getAttributes().get("longitude"));
            secondValues[1] = secondExample.getValue(firstExample.getAttributes().get("latitude"));

            return calculateDistance(firstValues, secondValues);

    }
	
}
