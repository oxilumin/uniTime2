package GraphAlgorithms;

import graphModel.GraphModel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;

import auxiliary.Edge;
import auxiliary.Pair;
import auxiliary.PairHashInteger;

/*
 * Implementation of fast Neumann algorithm for community detection
 */

public class CommunityDetection {

	// edges of a graph
	Edge[] edges;
	// community coloring
	int[] color, bestColor;
	//Exy[x,y] - edges from x to y
	Hashtable<Pair<Integer, Integer>, Double> Exy;
	//Exx[x] - edges where both vertices are in X 	
	double[] Exx;
	//A[x] - edges where the begin of it in component X 
	double[] A;
    
	int numberOfNodes;
	int numberOfEdges;
	
	double best = 0.0;
	int numberOfCommunities;
	
	
	public CommunityDetection(GraphModel model) {
		numberOfNodes = model.getNumberOfNodes();
		edges = model.getEdges();		
		numberOfEdges = edges.length;
		
	}
	
	private double CalculateModularity() {
		
		Exy = new Hashtable<Pair<Integer, Integer>, Double>();
		for (int i = 0; i <= numberOfNodes; i++) {
			Exx[i] = 0.0;
			A[i] = 0.0;
		}

		for (int i = 0; i < numberOfEdges; i++) {
			int colorFrom = color[edges[i].getFrom()];
			int colorTo = color[edges[i].getTo()];
			A[colorFrom] += edges[i].getWeight();
			if (colorFrom == colorTo) {
				Exx[colorFrom] += edges[i].getWeight();
			}
			else {
				Pair<Integer, Integer> kvp = new Pair(colorFrom, colorTo);
				if (Exy.containsKey(kvp))
					Exy.put(kvp, Exy.get(kvp) + edges[i].getWeight());
				else
					Exy.put(kvp, edges[i].getWeight());
			}
		}

		double Q = 0.0;
		for (int x = 1; x <= numberOfNodes; x++) {
			Q += Exx[x] - A[x] * A[x];
			
		}
		return Q;
	}
	
	public double calculateBestSplit() {
		/*
		double totalWeight = 0.0;
		for (int i = 0;i < numberOfEdges; i++) { 
			totalWeight += edges[i].getWeight();
		}
         */
		// working just with weights equals 1, in another case uncomment the upper code
		for (int i = 0; i <numberOfEdges; i++) { 
			edges[i].setWeight(edges[i].getWeight()/edges.length);
		}
		
		color = new int[numberOfNodes + 1];
		for (int i = 0; i <= numberOfNodes; i++) color[i] = i;

		A = new double[numberOfNodes + 1];
		Exx = new double[numberOfNodes + 1];
		double bestQ = Double.MIN_VALUE;

		for (int I = 1; I <= numberOfNodes; I++) {
			double Q = CalculateModularity();
		
			if (bestColor == null || bestQ < Q) {
				bestColor = new int[numberOfNodes + 1];
				for (int j = 0; j <= numberOfNodes; j++) bestColor[j] = color[j];
				bestQ = Q;
			}
			//find next optimal coloring, what will we try to merge
			int x = -1, y = -1;

			for (int i = 0; i < numberOfEdges; i++) {
				Integer X = color[edges[i].getFrom()];
				Integer Y = color[edges[i].getTo()];
				if (X == Y) continue;
				double EXY = 0;
				
				//EXY = Exy.get(new Pair<Integer, Integer>(X,Y)) + Exy.get(new Pair<Integer, Integer>(Y,X));
			    
				if(Exy.get(new Pair<Integer, Integer>(X,Y)) != null)
			    	EXY += Exy.get(new Pair<Integer, Integer>(X,Y));
			    if(Exy.get(new Pair<Integer, Integer>(Y,X)) != null)
			    	EXY += Exy.get(new Pair<Integer, Integer>(Y,X));
			    
				double deltaQ = EXY - 2.0 * A[X] * A[Y];
				if (x == -1 || best < deltaQ) {
					x = X;
					y = Y;
					best = deltaQ;
				}
			}
			if (x == -1) break;
			//merge
			for (int i = 1; i <= numberOfNodes; i++) if (color[i] == y) {
				color[i] = x;
			}
		 }
		
		HashSet<Integer> diffColors = new HashSet<Integer>();
		
		for(int i = 0; i < numberOfNodes; i++) {
			diffColors.add(color[i]);
		}
		numberOfCommunities = diffColors.size();
		return best;
	 } 
	
	public int getNumberOfCommunities() {
		return numberOfCommunities;
	}
}

