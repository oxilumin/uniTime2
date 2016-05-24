package graphModel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import auxiliary.Edge;
import auxiliary.Pair;
import auxiliary.PairSet;
import ExamModel.ExExam;
import ExamModel.ExModel;
import ExamModel.ExStudent;

public class GraphModel {
	
	private HashSet<Edge> edges = new HashSet<Edge>();
	private int numberOfNodes;
	private int numberOfEdges;

	public GraphModel(ExModel model) {
		List<ExStudent> students = model.getStudents();
		for(ExStudent s : students) {
			List<ExExam> exams = (List<ExExam>) s.variables();
			for(int i = 0; i < exams.size(); i ++) {
				for(int j = i + 1; j  < exams.size(); j ++) {
					int e1 = (int) exams.get(i).getId();
					int e2 = (int) exams.get(j).getId();
					addUndirectedEdge(e1, e2);
			    }
		    }
	     }
		 this.setNumberOfEdges(edges.size());
		 this.setNumberOfNodes(model.getNumberOfExams());
	}
	
	public void addConstraitsAsEdges(List<PairSet<Integer, Integer>> constraints) {
		for(PairSet p : constraints) {
			addUndirectedEdge((int)p.getKey(), (int)p.getValue());
		}
	}
	
	public void addUndirectedEdge(int from, int to) {
		edges.add(new Edge(from, to));
		edges.add(new Edge(to, from));
	}

	public int getNumberOfNodes() {
		return numberOfNodes;
	}
	
	public int getNumberOfEdges() {
		return numberOfEdges;
	}

    public Edge[] getEdges() {
    	return   Arrays.copyOf(edges.toArray(), edges.size(), Edge[].class);
    }
	
	private  void setNumberOfNodes(int numberOfNodes) {
		this.numberOfNodes = numberOfNodes;
	}
	
	private void setNumberOfEdges(int numberOfEdges) {
		this.numberOfEdges = numberOfEdges;
	}
}
