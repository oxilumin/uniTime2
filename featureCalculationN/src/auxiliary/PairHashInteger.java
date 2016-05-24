package auxiliary;

import java.util.Hashtable;

public class PairHashInteger<K , V> extends Pair<K, V>{

	private static Hashtable<Integer, Integer> ids = new Hashtable();
	
	public PairHashInteger(K key, V value) {
		super(key, value);
	}
	
	@Override
	public int hashCode(){
		return key.hashCode()* 300 + value.hashCode();
	}
}
