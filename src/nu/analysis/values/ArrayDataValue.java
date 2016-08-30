package nu.analysis.values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import soot.SootField;

public class ArrayDataValue extends RightValue {
	Set<RightValue> data;
	RightValue base; //if data==null, this is the key.
	Comparator<RightValue> RightValueComparator = (RightValue a, RightValue b) -> {
	    return a.toString().compareTo(b.toString());
	};
	
	public ArrayDataValue(RightValue base){
		data = new HashSet<RightValue>();
		this.base = base;
		data.add(base);
	}
	
	public void addData(RightValue d){
		data.add(d);
	}
	public RightValue getBase(){
		return base;
	}
	public Set<RightValue> getData(){
		return data;
	}
	public void setData(Set<RightValue> d){
		this.data = d;
	}
	
	public int getSize(){
		return data.size();
	}
	
	@Override
	public boolean equivTo(Object o) {
		return equals(o);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(base==null)
			sb.append("ARRAY-DATA-KEY");
		else{
			sb.append("ARR:"+base.toString()+"[");
			List<RightValue> tmp = new ArrayList<RightValue>(data);
			Collections.sort(tmp, RightValueComparator);
			for(RightValue rv : tmp)
				sb.append(rv.toString()+",");
			sb.append("]");
		}
		return sb.toString();
	}

	@Override
	public Object clone() {
		ArrayDataValue adv = new ArrayDataValue(base);
		for(RightValue rv : data)
			adv.addData(rv);
		return adv;
	}

	@Override
	public boolean equals(Object o) {
		return hashCode() == o.hashCode();
	}

	@Override
	public int hashCode() {
		return equivHashCode();
	}

	@Override
	public int equivHashCode() {
		return Objects.hash("ARRAYDATAVALUE-MAGICSTRING", toString());
	}
}
