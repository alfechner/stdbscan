/**
 * Spatio-Temporal DBScan Algorithm
 * @author Alex Fechner
 * @email fechner.alex@gmail.com
 */

package com.rapidminer.operator.clustering.clusterer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.AttributeSetPrecondition;
import com.rapidminer.operator.ports.metadata.DistanceMeasurePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasureHelper;
import com.rapidminer.tools.math.similarity.DistanceMeasures;
import com.rapidminer.tools.LogService;

public class STDBScan extends
		com.rapidminer.operator.clustering.clusterer.RMAbstractClusterer
		implements CapabilityProvider {

	private static final String PARAMETER_EPSILON_SPACE = "epsilon_spatial";
	private static final String PARAMETER_SPACE_UNIT = "spatial unit";
	public static final String[] SPATIAL_UNITS = { "millimeters", "centimeter",
			"decimeter", "meters", "kilometers" };
	public static final int SPATIAL_UNITS_PREDEFINED = 4;

	private static final String PARAMETER_EPSILON_TIME = "epsilon_temporal";
	private static final String PARAMETER_TEMPORAL_UNIT = "temporal unit";
	public static final String[] TEMPORAL_UNITS = { "milliseconds", "seconds",
			"minutes", "hours", "days", "weeks" };
	public static final int TEMPORAL_UNITS_PREDEFINED = 4;

	private static final String PARAMETER_MIN_POINTS = "min_points";

	public static final String PARAMETER_ATTRIBUTE_LONGITUDE = "longitude attribute";
	public static final String PARAMETER_ATTRIBUTE_LATITUDE = "latitude attribute";
	public static final String PARAMETER_ATTRIBUTE_TIME = "temporal attribute";

	public static final String PARAMETER_ADD_CLUSTER_ATTRIBUTE = "add_cluster_attribute";

	public STDBScan(OperatorDescription description) {

		super(description);

		getExampleSetInputPort()
				.addPrecondition(
						new DistanceMeasurePrecondition(
								getExampleSetInputPort(), this));

		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(),
						AttributeSetPrecondition.getAttributesByParameter(this,
								PARAMETER_ATTRIBUTE_LONGITUDE)));

		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(),
						AttributeSetPrecondition.getAttributesByParameter(this,
								PARAMETER_ATTRIBUTE_LATITUDE)));

		getExampleSetInputPort().addPrecondition(
				new AttributeSetPrecondition(getExampleSetInputPort(),
						AttributeSetPrecondition.getAttributesByParameter(this,
								PARAMETER_ATTRIBUTE_TIME)));

	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		return true;
	}

	private double getKms(double space, String unit) {

		if (unit.equals("millimeters")) {
			space *= 0.000001;
		} else if (unit.equals("centimeter")) {
			space *= 0.00001;
		} else if (unit.equals("decimeter")) {
			space *= 0.0001;
		} else if (unit.equals("meters")) {
			space *= 0.001;
		} else if (unit.equals("kilometers")) {
			//
		}

		return space;
	}

	private double getDays(double time, String unit) {

		if (unit.equals("milliseconds")) {
			time /= (1000 * 24 * 60 * 60);
		} else if (unit.equals("seconds")) {
			time /= (24 * 60 * 60);
		} else if (unit.equals("minutes")) {
			time /= (24 * 60);
		} else if (unit.equals("hours")) {
			time /= 24;
		} else if (unit.equals("days")) {
			//
		} else if (unit.equals("weeks")) {
			time *= 7;
		}

		return time;
	}

	@Override
	public ClusterModel generateClusterModel(ExampleSet exampleSet)
			throws OperatorException {

		double epsilonSpace = getKms(
				getParameterAsDouble(PARAMETER_EPSILON_SPACE),
				getParameterAsString(PARAMETER_SPACE_UNIT));
		double epsilonTime = getDays(
				getParameterAsDouble(PARAMETER_EPSILON_TIME),
				getParameterAsString(PARAMETER_TEMPORAL_UNIT));
		int minPoints = getParameterAsInt(PARAMETER_MIN_POINTS);

		// checking and creating ids if necessary
		Tools.checkAndCreateIds(exampleSet);

		// additional checks
		Tools.onlyNonMissingValues(exampleSet, "STDBScan");

		// extracting attribute names
		Attributes attributes = exampleSet.getAttributes();
		ArrayList<String> attributeNames = new ArrayList<String>(
				attributes.size());
		for (Attribute attribute : attributes)
			attributeNames.add(attribute.getName());

		boolean[] visited = new boolean[exampleSet.size()];
		boolean[] noised = new boolean[exampleSet.size()];
		int[] clusterAssignments = new int[exampleSet.size()];

		int i = 0;
		int clusterIndex = 1;
		for (Example example : exampleSet) {
			checkForStop();
			if (!visited[i]) {
				Queue<Integer> centerNeighbourhood = getNeighbourhood(example,
						exampleSet, epsilonSpace, epsilonTime);
				if (centerNeighbourhood.size() < minPoints) {
					noised[i] = true;
				} else {
					// then its center point of a cluster. Assign example to new
					// cluster
					clusterAssignments[i] = clusterIndex;
					// expanding cluster within density borders
					while (centerNeighbourhood.size() > 0) {
						int currentIndex = centerNeighbourhood.poll()
								.intValue();
						Example currentExample = exampleSet
								.getExample(currentIndex);
						// assigning example to current cluster
						clusterAssignments[currentIndex] = clusterIndex;
						visited[currentIndex] = true;

						// appending own neighbourhood to queue
						Queue<Integer> neighbourhood = getNeighbourhood(
								currentExample, exampleSet, epsilonSpace,
								epsilonTime);
						if (neighbourhood.size() >= minPoints) {
							// then this neighbor of center is also a center of
							// the cluster
							while (neighbourhood.size() > 0) {
								int neighbourIndex = neighbourhood.poll()
										.intValue();
								if (!visited[neighbourIndex]) {
									if (!noised[neighbourIndex]) {
										// if its not noised, then it might be
										// center of cluster! So append to queue
										centerNeighbourhood.add(neighbourIndex);
									}
									clusterAssignments[neighbourIndex] = clusterIndex;
									visited[neighbourIndex] = true;
								}
							}
						}
					}
					// step to next cluster
					clusterIndex++;
				}
			}
			i++;
		}

		ClusterModel model = new ClusterModel(
				exampleSet,
				Math.max(clusterIndex, 1),
				getParameterAsBoolean(RMAbstractClusterer.PARAMETER_ADD_AS_LABEL),
				getParameterAsBoolean(RMAbstractClusterer.PARAMETER_REMOVE_UNLABELED));
		
		model.setClusterAssignments(clusterAssignments, exampleSet);

		if (addsClusterAttribute()) {
			Attribute cluster = AttributeFactory.createAttribute(
					Attributes.CLUSTER_NAME, Ontology.NOMINAL);
			exampleSet.getExampleTable().addAttribute(cluster);
			exampleSet.getAttributes().setCluster(cluster);
			i = 0;
			for (Example example : exampleSet) {
				example.setValue(cluster, "cluster_" + clusterAssignments[i]);
				i++;
			}
		}
		return model;
	}

	private LinkedList<Integer> getNeighbourhood(Example centerExample,
			ExampleSet exampleSet, double epsilonSpace, double epsilonTime) {

		LinkedList<Integer> neighbourhood = new LinkedList<Integer>();
		int i = 0;
		for (Example example : exampleSet) {

			if (distanceTime(centerExample, example) < epsilonTime
					&& distanceSpace(centerExample, example) < epsilonSpace)
				neighbourhood.add(i);
			i++;

		}
		return neighbourhood;
	}

	private double distanceSpace(Example centerExample, Example example) {

		double lng1 = 0;
		double lng2 = 0;
		double lat1 = 0;
		double lat2 = 0;

		try {
			
			lng1 = centerExample.getValue(centerExample.getAttributes().get(
					getParameterAsString(PARAMETER_ATTRIBUTE_LONGITUDE)));
			lat1 = centerExample.getValue(centerExample.getAttributes().get(
					getParameterAsString(PARAMETER_ATTRIBUTE_LATITUDE)));

			lng2 = example.getValue(example.getAttributes().get(
					getParameterAsString(PARAMETER_ATTRIBUTE_LONGITUDE)));
			lat2 = example.getValue(example.getAttributes().get(
					getParameterAsString(PARAMETER_ATTRIBUTE_LATITUDE)));
			
		} catch (Exception e) {
			return 0;
		}

		double earthRadius = 6371;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double sindLat = Math.sin(dLat / 2);
		double sindLng = Math.sin(dLng / 2);
		double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
				* Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2));
		
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double dist = earthRadius * c;

		return dist;
	}

	private double distanceTime(Example centerExample, Example example) {

		double time1 = 0;
		double time2 = 0;

		try {
			time1 = centerExample.getValue(centerExample.getAttributes().get(
					getParameterAsString(PARAMETER_ATTRIBUTE_TIME)));
			time2 = example.getValue(example.getAttributes().get(
					getParameterAsString(PARAMETER_ATTRIBUTE_TIME)));
		} catch (Exception e) {
			return 0;
		}

		double days = Math.abs(time1 - time2) / (60 * 60 * 24);

		return days;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();

		types.add(new ParameterTypeDouble(PARAMETER_EPSILON_SPACE,
				"Specifies the spacial size of neighbourhood.", 0,
				Double.POSITIVE_INFINITY, 10, false));
		types.add(new ParameterTypeCategory(PARAMETER_SPACE_UNIT,
				"The unit of the spatial size of neighbourhood.",
				SPATIAL_UNITS, SPATIAL_UNITS_PREDEFINED, false));

		types.add(new ParameterTypeDouble(PARAMETER_EPSILON_TIME,
				"Specifies the temporal size of neighbourhood.", 0,
				Double.POSITIVE_INFINITY, 10, false));
		types.add(new ParameterTypeCategory(PARAMETER_TEMPORAL_UNIT,
				"The unit of the temporal size of neighbourhood.",
				TEMPORAL_UNITS, TEMPORAL_UNITS_PREDEFINED, false));

		types.add(new ParameterTypeInt(PARAMETER_MIN_POINTS,
				"The minimal number of points forming a cluster.", 1,
				Integer.MAX_VALUE, 5, false));

		ParameterTypeAttribute attributeLong = new ParameterTypeAttribute(
				PARAMETER_ATTRIBUTE_LONGITUDE,
				"The attribute which contains the longitude values.",
				getExampleSetInputPort(), false, false, Ontology.REAL);
		types.add(attributeLong);

		ParameterTypeAttribute attributeLat = new ParameterTypeAttribute(
				PARAMETER_ATTRIBUTE_LATITUDE,
				"The attribute which contains the latitude values.",
				getExampleSetInputPort(), false, false, Ontology.REAL);
		types.add(attributeLat);

		ParameterTypeAttribute attributeTime = new ParameterTypeAttribute(
				PARAMETER_ATTRIBUTE_TIME,
				"The attribute which contains the temporal values. Expects a timestamp measured from certain point in time in mliseconds.",
				getExampleSetInputPort(), false, false, Ontology.INTEGER);
		types.add(attributeTime);

		types.add(new ParameterTypeBoolean(
				PARAMETER_ADD_CLUSTER_ATTRIBUTE,
				"If enabled, a cluster id is generated as new special attribute directly in this operator, otherwise this operator does not add an id attribute. In the latter case you have to use the Apply Model operator to generate the cluster attribute.",
				true, false));

		return types;
	}

}
