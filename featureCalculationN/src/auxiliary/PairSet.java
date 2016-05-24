package auxiliary;

public class PairSet <K,V> extends Pair<K,V>{
	
	public PairSet(K key, V value) {
		super(key, value);
	}

	@Override
	public int hashCode(){
		return key.hashCode() + value.hashCode();//for simplicity reason
	}
}
