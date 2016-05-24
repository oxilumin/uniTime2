package ExamFeatureCalculation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class StatisticalMeasures {

	// average calculation
	public static double calculateMeanFromInteger(ArrayList<Integer> list) {
		double average = 0;
		for(Integer element : list)
			average += element; 
		average /= list.size();
		return average;
	}
	
	public static double calculateMeanFromDouble(ArrayList<Double> list) {
		double average = 0;
		for(Double element : list)
			average += element; 
		average /= list.size();
		return average;
	}
	
	public static double calculateStandartDeviationFromInteger(ArrayList<Integer> list) {
		double mean = StatisticalMeasures.calculateMeanFromInteger(list);
		double deviation = 0;
		
		for(Integer element : list) {
			deviation += (element - mean)*(element - mean);
		}
		deviation = Math.sqrt(deviation/list.size());
		return deviation;
	}
	
	public static double calculateStandartDeviationFromDouble(ArrayList<Double> list) {
		double mean = StatisticalMeasures.calculateMeanFromDouble(list);
		double deviation = 0;
		
		for(Double element : list) {
			deviation += (element - mean)*(element - mean);
		}
		deviation = Math.sqrt(deviation/list.size());
		return deviation;
	}
	
	public static double calculateVariationCoefficientFromInteger(ArrayList<Integer> list) {
		double mean = StatisticalMeasures.calculateMeanFromInteger(list);
		double standartDeviation = StatisticalMeasures.calculateStandartDeviationFromInteger(list);
		if(mean == 0) return 0;
		return standartDeviation/mean;
	}
	
	public static double calculateVariationCoefficientFromDouble(ArrayList<Double> list) {
		double mean = StatisticalMeasures.calculateMeanFromDouble(list);
		double standartDeviation = StatisticalMeasures.calculateStandartDeviationFromDouble(list);
		return standartDeviation/mean;
	}
	
	public static int calculateMaxInt(ArrayList<Integer> list) {
		return Collections.max(list);
	}
	
	public static double calculateMaxDouble(ArrayList<Double> list) {
		Double max = Double.MIN_VALUE;
		for(Double d : list) {
			if(d != null && d.doubleValue() > max.doubleValue())
				max = d; 
		}
		return max.doubleValue();//Collections.max(list);
	}
	
	public static double calculateMinDouble(ArrayList<Double> list) {
		Double min = Double.MAX_VALUE;
		for(Double d : list) {
			if(d != null && d.doubleValue() < min.doubleValue())
				min = d; 
		}
		return min.doubleValue();
	}
	
	public static int calculateMinInt(ArrayList<Integer> list) {
		return Collections.min(list);
	}
	
	public static int calculateFirstQuantileInt(ArrayList<Integer> list) {	
	   Collections.sort(list);
	   return list.get(list.size()/4);
	}
	
	public static double calculateFirstQuantileDouble(ArrayList<Double> list) {
		   Collections.sort(list);
		   return list.get(list.size()/4);
	}
	
	public static int calculateThirdQuantileInt(ArrayList<Integer> list) {
		   Collections.sort(list);
		   return list.get((int)(list.size()*0.75));
	}
	
	public static double calculateThirdQuantileDouble(ArrayList<Double> list) {
		   Collections.sort(list);
		   return list.get((int)(list.size()*0.75));
	}
	
	public static int calculateMedianInt(ArrayList<Integer> list) {
		   Collections.sort(list);
		   return list.get(list.size()/2);
	}
	
	public static double calculateMedianDouble(ArrayList<Double> list) {
		   Collections.sort(list);
		   return list.get(list.size()/2);
	}
	
	public static ArrayList<Integer> convertIntegerArrayToArrayList(Integer[] array) {
		return new ArrayList<Integer>(Arrays.asList(array));
	}
	
	public static ArrayList<Double> convertDoubleArrayToArrayList(Double[] array) {
		ArrayList<Double> arr = new ArrayList<Double>(Arrays.asList(array)) ;
		arr.removeAll(Collections.singleton(null));;
		return arr;
	}
}
