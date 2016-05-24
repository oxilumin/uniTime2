package auxiliary;

public class Pair<K,V> {

	protected K key;
	protected V value;
	
	public Pair(K key, V value) {
		this.key = key;
		this.value = value;
	}
	
	public K getKey() {
		return key;
	}
	
	public V getValue() {
		return value;
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
        Pair e = (Pair) obj;
		if(e.key == this.key && e.value == this.value) { // || (e.to == this.from && e.from == this.to)) {
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return key.hashCode()* 300 + value.hashCode();
	}
	
}
