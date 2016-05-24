package auxiliary;

public class Edge {
	int from;
	int to;
	double weight; 
	
	public Edge(int from, int to) {
		this.to = to;
		this.from = from;
		this.weight = 1.0;
	}
	
	
	public Edge(int from, int to, double weight) {
		this.to = to;
		this.from = from;
		this.weight = weight;
	}
	
	public int getFrom() {
		return from;
	}
	
	public int getTo() {
		return to;
	}
	
	public double getWeight() {
		return weight;
	}
	
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		}
        if (!(obj instanceof Edge)) {
            return false;
        }
        if(obj == this) { 
        	return true;
        }
		Edge e = (Edge) obj;
		if((e.from == this.from && e.to == this.to)) { // || (e.to == this.from && e.from == this.to)) {
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		// old return this.from + this.to;
		return this.from*300 + this.to;//for simplicity reason
	}
	
}
