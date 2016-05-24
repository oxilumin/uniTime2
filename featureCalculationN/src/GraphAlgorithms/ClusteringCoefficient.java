package GraphAlgorithms;

import graphModel.GraphModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;

import auxiliary.Edge;

public class ClusteringCoefficient {

	Hashtable<Integer, HashSet<Integer>> edges;
	Edge[] edgesArray;
	int numberOfNodes;
	ArrayList<Double> clusCoef;
	int[] nodeDegrees; 
	Double[] clusteringCoefficients;
	Double[] weightedClusteringCoefficients;
	
	public ClusteringCoefficient(GraphModel model)  { //{(Edge[] pEdges, int numberOfNodes) 
		
	    edgesArray = model.getEdges();
		numberOfNodes = model.getNumberOfNodes(); 
	    /*edgesArray = pEdges;
	    this.numberOfNodes = numberOfNodes; */
		nodeDegrees = new int[numberOfNodes];
		clusteringCoefficients = new Double[numberOfNodes];
		//Arrays.fill(clusteringCoefficients, 0);
		weightedClusteringCoefficients = new Double[numberOfNodes];
		//Arrays.fill(weightedClusteringCoefficients, 0);
		clusCoef = new ArrayList<Double>(numberOfNodes);
		edges = new Hashtable<Integer, HashSet<Integer>>();
		
		for(int i = 0; i < edgesArray.length; i ++) {
			Edge e = edgesArray[i];
			HashSet<Integer> connectedVertices;
			if(edges.containsKey(e.getFrom())) { 
				connectedVertices = edges.get(e.getFrom());
				connectedVertices.add(e.getTo());
				edges.put(e.getFrom(), connectedVertices);
			}
			else {
				connectedVertices = new HashSet<Integer>();
				connectedVertices.add(e.getTo());
				edges.put(e.getFrom(), connectedVertices);
			}	
		}
		
		for(Integer k : edges.keySet()) {
			nodeDegrees[k] = edges.get(k).size();
		}
	}
	
	public void calculateClusteringCoefficient() {
		
		HashSet<Integer> adjacentVertices;
		
		for(Integer node : edges.keySet()) {
		    adjacentVertices = edges.get(node);
		    int numberOfNeighbours = adjacentVertices.size();
		    int connectedNeighbours = 0;
		    int maxNumberOfEdges = numberOfNeighbours == 1 ? 1 : numberOfNeighbours*(numberOfNeighbours - 1);
		    for(Integer neighbour1 : adjacentVertices) {
		    	if(!edges.containsKey(neighbour1)) continue;
			    for(Integer neighbour2 : adjacentVertices)  {
				    if(neighbour1.equals(neighbour2)) continue;
			        	for(Integer n : edges.get(neighbour1))
			        		if(n.intValue() == neighbour2.intValue())
				                connectedNeighbours ++;
		         }
		     }
		    //clusCoef.add(node, (double)connectedNeighbours / maxNumberOfEdges); 
		    clusteringCoefficients[node] = ((double)connectedNeighbours / maxNumberOfEdges);
		    weightedClusteringCoefficients[node] = clusteringCoefficients[node]  * nodeDegrees[node];
	     }
     }
	
	public Double[] getClusteringCoefficients(){
		return clusteringCoefficients;
	}
	
	public Double[] getWeightedClusteringCoefficients(){
		return weightedClusteringCoefficients;
	}
	
	public ArrayList<Double> getArrayListOfClusteringCoefficients() {
		return clusCoef;
	}
}
