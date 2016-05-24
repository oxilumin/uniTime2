package ExamFeatureCalculation;

import graphModel.GraphModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.io.File;
import java.io.FileWriter;

import auxiliary.Edge;
import ExamModel.ExExam;
import ExamModel.ExModel;
import ExamModel.ExPeriod;
import ExamModel.ExRoom;
import ExamModel.ExStudent;
import GraphAlgorithms.ClusteringCoefficient;
import GraphAlgorithms.CommunityDetection;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

public class StaticFeatures {
	
	 static ExModel model; 
	 
	 private static String FeatureCalculationForSingleFile(String iPathname, double localSearchScore) throws Exception {
		
		String pathname = iPathname;//"D:/MasterThesis/Instances/art000.exam";
		StringBuffer  str = new StringBuffer();
		File file;
		file = new File(pathname);
		model =  new ExModel();
		model.load(file);
		
		// exams to students
	
		//str.append( + ",");
		
		//str.append( + ",");
		
		// students to exams
		
		// rooms constraints
		
	

		
		// info about periods
		List<ExPeriod> periods = model.getPeriods();
		ArrayList<Integer> periodsLength = new ArrayList<Integer>();
		ArrayList<Integer> periodsPenalty = new ArrayList<Integer>();
		str.append(periods.size() + ",");
		for(ExPeriod p : periods) {
			periodsLength.add(p.getLength());
			periodsPenalty.add(p.getWeight());
		}	
		str.append(StaticFeatures.numericFeaturesInt(periodsLength) + ",");
		str.append(model.getNrDays() + ",");
		str.append(StaticFeatures.numericFeaturesInt(periodsPenalty) + ",");
		
		// constraints info
		str.append(model.getNumberOfAfterConstraints() + ",");
		str.append(model.getNumberOfCoincidenceConstraints() + "," );
		str.append(model.getNumberOfExclusionConstraints() + ",");
		str.append(model.getNumberOfRoomExclusive() + ",");
		str.append(model.getTwoInARowWeight() + ",");
		str.append(model.getTwoInADayWeight() + ",");
		str.append(model.getPeriodSpreadLength() + ",");
		str.append(model.getMixedDurationWeight() + ",");
		str.append(model.getFronLoadThreshold() + ",");
		str.append(model.getFrontLoadWeight() + ",");
		// combine all info

        
		// graphBasedFeatures
		// community detection
		GraphModel graphModel = new GraphModel(model);
		CommunityDetection alg = new CommunityDetection(graphModel);
		str.append(alg.calculateBestSplit() + ",");
		str.append(alg.getNumberOfCommunities() + ",");
		
		// clustering coefficient
		ClusteringCoefficient cl = new ClusteringCoefficient(graphModel);
		cl.calculateClusteringCoefficient();
		ArrayList<Double> coef = StatisticalMeasures.convertDoubleArrayToArrayList(cl.getClusteringCoefficients()) ;

		ArrayList<Double> weighCoef = StatisticalMeasures.convertDoubleArrayToArrayList(cl.getWeightedClusteringCoefficients());
		
		str.append(StaticFeatures.numericFeaturesDouble(coef) + ",");
		str.append(StaticFeatures.numericFeaturesDouble(weighCoef) + ",");

		str.append(String.valueOf(localSearchScore));
		
		String resultString = str.toString();
		return resultString;
	}

    public static double GetLocalSearchFeatures(String input)
    {
		String output = input+"_output";

		long maxNumberOfIterations = 250000L;

		return ExItcTest.goSolve("exam", input, output, 300L, maxNumberOfIterations);
    }

	 public static void main(String[] args) throws Exception {
		 /*
		  * test
		  */
		 /*String str = "D:/MasterThesis/Instances/art001.exam";
		 System.out.println(FeatureCalculationForSingleFile(str));
*/

		 FileWriter fw = new FileWriter("C:/Data/OlyaMasterThesis/Instances/infoSetsNew1_LS.csv");

		 for(int i = 0; i <= 241; i ++) {
			 String str = String.format("C:/Olga/MasterThesis/Instances/art%03d.exam", i);

			 double localSearchScore = GetLocalSearchFeatures(str);

		     fw.write(FeatureCalculationForSingleFile(str, localSearchScore) + "\n");

		     System.out.println(i);
		 }

         fw.close();
	 }
	// statistical features
	private static String numericFeaturesInt(ArrayList<Integer> list) {

		StringBuffer  features = new StringBuffer();
        
		features.append(StatisticalMeasures.calculateMaxInt(list) + ",");
		features.append(StatisticalMeasures.calculateMinInt(list) + ",");
		features.append(StatisticalMeasures.calculateFirstQuantileInt(list) + ",");
		features.append(StatisticalMeasures.calculateMedianInt(list) +",");
		features.append(StatisticalMeasures.calculateThirdQuantileInt(list) +",");
		features.append(StatisticalMeasures.calculateMeanFromInteger(list) +",");
		features.append(StatisticalMeasures.calculateVariationCoefficientFromInteger(list));
		
		return features.toString();
		
	}
	
	private static String numericFeaturesDouble(ArrayList<Double> list) { 
		
		StringBuffer  features = new StringBuffer();			
        
		features.append(StatisticalMeasures.calculateMaxDouble(list) + ",");
		features.append(StatisticalMeasures.calculateMinDouble(list) + ",");
		features.append(StatisticalMeasures.calculateFirstQuantileDouble(list) + ",");
		features.append(StatisticalMeasures.calculateMedianDouble(list) +",");
		features.append(StatisticalMeasures.calculateThirdQuantileDouble(list) +",");
		features.append(StatisticalMeasures.calculateMeanFromDouble(list) +",");
		features.append(StatisticalMeasures.calculateVariationCoefficientFromDouble(list));
		
		return features.toString();
		
	}
	
	private static String numericFeaturesDoubleArray(Double[] arr) {
		ArrayList<Double> arrList= StatisticalMeasures.convertDoubleArrayToArrayList(arr);
		return StaticFeatures.numericFeaturesDouble(arrList);
	}
	
	private static String numericFeaturesIntArray(Integer[] arr) {
		ArrayList<Integer> arrList= StatisticalMeasures.convertIntegerArrayToArrayList(arr);
		return StaticFeatures.numericFeaturesInt(arrList);
	}
	
	
	private static String getExamInformation() {
		StringBuffer  str = new StringBuffer();
		List<ExExam> exams = model.variables(); 
		ArrayList<Integer> examInfo = new ArrayList<Integer>();
		for(ExExam exam : exams) {
			examInfo.add(exam.getStudents().size());
		}
		str.append(exams.size() + ",");
		str.append(StaticFeatures.numericFeaturesInt(examInfo));
		
		return str.toString(); 
	}
	
	private static String getStudentInformation() {
		StringBuffer  str = new StringBuffer();
		ArrayList<Integer> studentExams = new ArrayList<Integer>();
		List<ExStudent> students = model.getStudents();
		for(ExStudent student : students) 
			studentExams.add(student.countVariables());
		str.append(students.size() + ",");
		str.append(StaticFeatures.numericFeaturesInt(studentExams));
		
		return str.toString();
	}
	
	
	private static String getRoomInfo() {
		StringBuffer  str = new StringBuffer();
		List<ExRoom> rooms = model.getRooms();
		ArrayList<Integer> roomsSize = new ArrayList<Integer>();
		ArrayList<Integer> roomPenalty = new ArrayList<Integer>();
		for(ExRoom r : rooms) {
			roomsSize.add(r.getSize());
			roomPenalty.add(r.getPenalty());
		}
		str.append(StaticFeatures.numericFeaturesInt(roomsSize) + ",");
		str.append(StaticFeatures.numericFeaturesInt(roomPenalty));
		
		return str.toString();
	}
	
	// size + all statical information
	private static String getPeriodInfo() {
		StringBuffer  str = new StringBuffer();
		List<ExPeriod> periods = model.getPeriods();
		ArrayList<Integer> periodsLength = new ArrayList<Integer>();
		ArrayList<Integer> periodsPenalty = new ArrayList<Integer>();		
		for(ExPeriod p : periods) {
			periodsLength.add(p.getLength());
			periodsPenalty.add(p.getWeight());
		}
		str.append(periods.size() + ",");
		str.append(StaticFeatures.numericFeaturesInt(periodsLength) + ",");
		str.append(model.getNrDays() + ",");
		str.append(StaticFeatures.numericFeaturesInt(periodsPenalty));
		
		return str.toString();
	}
}
