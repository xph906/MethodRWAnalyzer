package nu.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import soot.Value;
import nu.analysis.values.RightValue;

public class ViewIdAnalysisMap implements Map<Integer, Set<Integer>>{
	private HashMap<Integer, Set<Integer>> map;
	private HashMap<Integer, Value> id2Value;
	
	public ViewIdAnalysisMap(){
		map = new HashMap<Integer, Set<Integer>>();
		id2Value = new HashMap<Integer, Value>();
	}
	
	public void addNewValue(Value key, Integer value){
		Integer id = key.equivHashCode();
		
		if(map.containsKey(id)){
			map.get(id).add(value);
		}
		else{
			Set<Integer> l = new HashSet<Integer>();
			l.add(value);
			map.put(id, l);
			id2Value.put(id, key);
		}
		assert(id2Value.size() == map.size());
	}
	
	public void addNewValueSet(Value key, Set<Integer> values){
		Integer id = key.equivHashCode();
		if(map.containsKey(id)){
			map.get(id).addAll(values);
		}
		else{
			Set<Integer> l = new HashSet<Integer>();
			l.addAll(values);
			map.put(id, l);
			id2Value.put(id, key);
		}
		assert(id2Value.size() == map.size());
	}
	
	//kill existing one, if any.
	public void setNewValue(Value key, Integer value){
		Integer id = key.equivHashCode();
		Set<Integer> l = new HashSet<Integer>();
		l.add(value);
		map.put(id, l);
		id2Value.put(id, key);
		assert(id2Value.size() == map.size());
	}
	
	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		assert(key instanceof Value);
		Integer id = ((Value)key).equivHashCode();
		if(map.containsKey(id))
			return true;
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		if(value instanceof Integer){
			for(Set<Integer> values : map.values()){
				if(values.contains(value))
					return true;
			}
		}
		return false;
	}

	@Override
	public Set<Integer> get(Object key) {
		assert(key instanceof Value);
		Integer id = ((Value)key).equivHashCode();
		return map.get(id);
	}

	@Override
	public Set<Integer> put(Integer key, Set<Integer> value) {
		//shouldn't use this method
		assert(1 == 2);
		return null;
	}
	
	public Set<Integer> put(Value key, Set<Integer> value) {
		Integer id = key.equivHashCode();
		id2Value.put(id, key);
		Set<Integer> rs = map.put(id, value);
		
		assert(id2Value.size() == map.size());
		return rs;
	}
	
	@Override
	public Set<Integer> remove(Object key) {
		assert(key instanceof Value);
		Integer id = ((Value)key).equivHashCode();
		Set<Integer> rs = map.remove(id);
		id2Value.remove(id);
		return rs;
	}

	@Override
	public void putAll(Map<? extends Integer, ? extends Set<Integer>> m) {
		//shouldn't use this method.
		assert(1 == 2);
	}

	@Override
	public void clear() {
		map.clear();
		id2Value.clear();
	}

	@Override
	public Set<Integer> keySet() {
		//Shouldn't call this method.
		//Use keyValueSet instead
		assert(1 == 2);
		return null;
	}
	
	public HashSet<Value> keyValueSet() {
		return new HashSet<Value>(id2Value.values());
	}

	@Override
	public Collection<Set<Integer>> values() {
		return map.values();
	}

	@Override
	public Set<java.util.Map.Entry<Integer, Set<Integer>>> entrySet() {
		return map.entrySet();
	}
	
	@Override
	public int hashCode() {
		return Objects.hash("NUAnalysis_"+toString());
	}
	
	@Override
	public boolean equals(Object right){
		//System.out.println("compare");
		if(! (right instanceof ViewIdAnalysisMap))
			return false;
		ViewIdAnalysisMap r = (ViewIdAnalysisMap)right;
		return hashCode() == r.hashCode();
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		List<Integer> ids = new ArrayList<Integer>(map.keySet());
		Collections.sort(ids);
		
		for(Integer id : ids){
			Value k = id2Value.get(id);
			String kStr = String.format("%30s",k.toString());
			sb.append(kStr+":[");
			List<Integer> tmp2 = new ArrayList<Integer>(map.get(id));
			Collections.sort(tmp2);
			for(Integer v : tmp2)
				sb.append(v.toString()+",");
			sb.append("]\n");
		}
		return sb.toString();
	}
	
	@Override
	public Object clone(){
		ViewIdAnalysisMap newObj = new ViewIdAnalysisMap();
		for(Value k : id2Value.values())
			newObj.put(k, get(k));
		assert(equals(newObj));
		return newObj;
	}
	


}
