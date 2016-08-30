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

import nu.analysis.values.ArrayDataValue;
import nu.analysis.values.RightValue;
import soot.Value;

public class DefAnalysisMap implements Map<Integer, Set<RightValue>> {
	private HashMap<Integer, Set<RightValue>> map;
	private HashMap<Integer, Value> id2Value;
	Comparator<RightValue> RightValueComparator = (RightValue a, RightValue b) -> {
	    return a.toString().compareTo(b.toString());
	};
	Comparator<Value> ValueComparator = (Value a, Value b) -> {
	    return a.toString().compareTo(b.toString());
	};
	ArrayDataValue arrDataKey = new ArrayDataValue(null);
	
	public DefAnalysisMap(){
		map = new HashMap<Integer, Set<RightValue>>();
		id2Value = new HashMap<Integer, Value>();
	}
	
	public ArrayDataValue getArrDataKey(){
		assert(arrDataKey.equals(new ArrayDataValue(null)) );
		return arrDataKey;
	}
	
	public void addNewValue(Value key, RightValue value){
		Integer id = key.equivHashCode();
		
		if(map.containsKey(id)){
			map.get(id).add(value);
		}
		else{
			Set<RightValue> l = new HashSet<RightValue>();
			l.add(value);
			map.put(id, l);
			id2Value.put(id, key);
		}
		assert(id2Value.size() == map.size());
	}
	public void addNewValueSet(Value key, Set<RightValue> values){
		Integer id = key.equivHashCode();
		if(map.containsKey(id)){
			map.get(id).addAll(values);
		}
		else{
			Set<RightValue> l = new HashSet<RightValue>();
			l.addAll(values);
			map.put(id, l);
			id2Value.put(id, key);
		}
		assert(id2Value.size() == map.size());
	}
	//kill existing one, if any.
	public void setNewValue(Value key, RightValue value){
		Integer id = key.equivHashCode();
		Set<RightValue> l = new HashSet<RightValue>();
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
		if(value instanceof RightValue){
			for(Set<RightValue> values : map.values()){
				if(values.contains(value))
					return true;
			}
		}
		else{
			System.out.println("error: containsValue object is not RightValue: "+value.getClass());
		}
		return false;
	}

	@Override
	public Set<RightValue> get(Object key) {
		assert(key instanceof Value);
		Integer id = ((Value)key).equivHashCode();
		return map.get(id);
	}

	public Set<RightValue> put(Value key, Set<RightValue> value) {
		Integer id = key.equivHashCode();
		id2Value.put(id, key);
		Set<RightValue> rs = map.put(id, value);
		
		assert(id2Value.size() == map.size());
		return rs;
		
	}
	
	@Override
	public Set<RightValue> put(Integer key, Set<RightValue> value) {
		//shouldn't use this method
		assert(id2Value.containsKey(key));
		return map.put(key, value);
	}

	@Override
	public Set<RightValue> remove(Object key) {
		assert(key instanceof Value);
		Integer id = ((Value)key).equivHashCode();
		Set<RightValue> rs = map.remove(id);
		id2Value.remove(id);
		return rs;
	}

	@Override
	public void putAll(Map<? extends Integer, ? extends Set<RightValue>> m) {
		//shouldn't use this method.
		//TODO: figure out a way to conceal this method.
		assert(1 == 2);
	}

	@Override
	public void clear() {
		map.clear();
		id2Value.clear();
	}

	@Override
	public Set<Integer> keySet() {
		//THIS FUNCTION IS DISABLED because it's easy to get confused with keyValueSet()
		//TODO: figure out a way to conceal this method.
		assert(1 == 2);
		return null;
	}
	
	public HashSet<Value> keyValueSet() {
		return new HashSet<Value>(id2Value.values());
	}
	
	public ArrayDataValue findArrayDataValueFromBase(RightValue base){
		Set<RightValue> rs = get(arrDataKey);
		if(rs == null)
			return null;
		else{
			for(RightValue v : rs){
				ArrayDataValue av = (ArrayDataValue)v;
				if(av.getBase().equals(base))
					return av;
			}
		}
		return null;
	}
	
	public Set<RightValue> getAllArrayDataValue(){
		return get(arrDataKey);
	}
	
	public void addNewArrayDataValue(ArrayDataValue adv){
		Set<RightValue> advs = get(arrDataKey);
		if(advs == null)
			setNewValue(arrDataKey, adv);
		else{
			for(RightValue rv : advs){
				ArrayDataValue a = (ArrayDataValue)rv;
				if(a.getBase().equals(adv.getBase())){
					assert(adv.getData().size() >= a.getData().size());
					//System.out.println("BASEVS1: "+a.getBase()+" VS "+adv.getBase());
					a.setData(adv.getData());
					return;
				}
				else{
					//System.out.println("BASEVS2: "+a.getBase()+" VS "+adv.getBase());
				}
			}
			addNewValue(arrDataKey, adv);
		}
		
	}

	@Override
	public Collection<Set<RightValue>> values() {
		return map.values();
	}

	@Override
	public Set<java.util.Map.Entry<Integer, Set<RightValue>>> entrySet() {
		
		return map.entrySet();
	}
	
	@Override
	public boolean equals(Object right){
		//System.out.println("compare");
		if(! (right instanceof DefAnalysisMap))
			return false;
		DefAnalysisMap rightMap = (DefAnalysisMap)right;
		if(keyValueSet().size() != rightMap.keyValueSet().size())
			return false;
		for(Value key : keyValueSet()){
			if(!rightMap.containsKey(key))
				return false;
			if(get(key).size() != rightMap.get(key).size())
				return false;
			Set<RightValue> rightValues = rightMap.get(key); 
			for(RightValue v : get(key)){
				if(! rightValues.contains(v))
					return false;
			}
		}
		
		return true;
	}
	
	@Override 
	public Object clone(){
		DefAnalysisMap dam = new DefAnalysisMap();
		for(Value k : id2Value.values()){
			dam.addNewValueSet(k, get(k));
		}
		return dam;
	}
	
	public HashMap<Integer, Set<RightValue>> getMap() {
		return map;
	}

	public HashMap<Integer, Value> getId2Value() {
		return id2Value;
	}

	@Override
	public String toString(){
		
		try{
			if(map==null)
				return "null";
			StringBuilder sb = new StringBuilder();
			List<Integer> ids = new ArrayList<Integer>(map.keySet());
			Collections.sort(ids);
			
			for(Integer id : ids){
				Value k = id2Value.get(id);
				String kStr = String.format("%30s",k.toString());
				sb.append(kStr+":[");
				List<RightValue> tmp2 = new ArrayList<RightValue>(map.get(id));
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
