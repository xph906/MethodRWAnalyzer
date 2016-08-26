package nu.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.analysis.values.RightValue;
import soot.Value;

public class DefAnalysisMap implements Map<Value, Set<RightValue>> {
	private HashMap<Value, Set<RightValue>> map;
	Comparator<RightValue> RightValueComparator = (RightValue a, RightValue b) -> {
	    return a.toString().compareTo(b.toString());
	};
	Comparator<Value> ValueComparator = (Value a, Value b) -> {
	    return a.toString().compareTo(b.toString());
	};
	
	public DefAnalysisMap(){
		map = new HashMap<Value, Set<RightValue>>();
	}
	
	public void addNewValue(Value key, RightValue value){
		if(map.containsKey(key)){
			map.get(key).add(value);
		}
		else{
			Set<RightValue> l = new HashSet<RightValue>();
			l.add(value);
			map.put(key, l);
		}
	}
	public void addNewValueSet(Value key, Set<RightValue> values){
		if(map.containsKey(key)){
			map.get(key).addAll(values);
		}
		else{
			Set<RightValue> l = new HashSet<RightValue>();
			l.addAll(values);
			map.put(key, l);
		}
	}
	//kill existing one, if any.
	public void setNewValue(Value key, RightValue value){
		Set<RightValue> l = new HashSet<RightValue>();
		l.add(value);
		map.put(key, l);
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
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public Set<RightValue> get(Object key) {
		return map.get(key);
	}

	@Override
	public Set<RightValue> put(Value key, Set<RightValue> value) {
		return map.put(key, value);
	}

	@Override
	public Set<RightValue> remove(Object key) {
		return map.remove(key);
	}

	@Override
	public void putAll(Map<? extends Value, ? extends Set<RightValue>> m) {
		map.putAll(m);
		
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public Set<Value> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<Set<RightValue>> values() {
		return map.values();
	}

	@Override
	public Set<java.util.Map.Entry<Value, Set<RightValue>>> entrySet() {
		
		return map.entrySet();
	}
	
	@Override
	public boolean equals(Object right){
		//System.out.println("compare");
		if(! (right instanceof DefAnalysisMap))
			return false;
		DefAnalysisMap rightMap = (DefAnalysisMap)right;
		if(map.keySet().size() != rightMap.keySet().size())
			return false;
		for(Value k : map.keySet()){
			if(!rightMap.containsKey(k))
				return false;
			if(map.get(k).size() != rightMap.get(k).size())
				return false;
			Set<RightValue> rightValues = rightMap.get(k); 
			for(Value v : map.get(k)){
				if(! rightValues.contains(v))
					return false;
			}
		}
		
		return true;
	}
	
	@Override 
	public Object clone(){
		HashMap<Value, List<Value>> newMap = new HashMap<Value, List<Value>>();
		for(Value k : map.keySet()){
			newMap.put(k, new LinkedList<Value>(map.get(k)));
		}
		return newMap;
	}
	
	@Override
	public String toString(){
		
		try{
			if(map==null)
				return "null";
			StringBuilder sb = new StringBuilder();
			List<Value> keys = new ArrayList<Value>(map.keySet());
			Collections.sort(keys, ValueComparator);
			
			for(Value k : keys){
				String kStr = String.format("%30s",k.toString());
				sb.append(kStr+":[");
				List<RightValue> tmp2 = new ArrayList<RightValue>(map.get(k));
				Collections.sort(tmp2, RightValueComparator);
				for(Value v : tmp2)
					sb.append(v.toString()+",");
				sb.append("]\n");
			}
			return sb.toString();
		}
		catch(Exception e){
			System.err.println("toString error: "+e.toString());
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String getValueString(Value key){
		if(map==null || !map.containsKey(key))
			return "null";
		StringBuilder sb = new StringBuilder();
		sb.append(key.toString()+":[");
		List<RightValue> tmp = new ArrayList<RightValue>(map.get(key));
		Collections.sort(tmp, RightValueComparator);
		for(Value v : tmp)
			sb.append(v.toString()+",");
		sb.append("],");
		return sb.toString();
	}
	
	
}
