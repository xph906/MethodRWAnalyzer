package nu.analysis.values;

import java.util.List;
import java.util.Objects;

import soot.Type;
import soot.UnitPrinter;
import soot.Value;
import soot.ValueBox;
import soot.util.Switch;

public class ParamValue extends AtomRightValue {
	int index;
	
	public ParamValue(int idx){
		this.index = idx;
	}
	
	public int getIndex(){
		return index;
	}
	
	@Override
	public void apply(Switch sw) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean equivTo(Object o) {
		//TODO: add method name
		return hashCode() == o.hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		return equivTo(o);
	}

	@Override
	public int equivHashCode() {
		return Objects.hash("PARAM-MAGICSTRING", Integer.valueOf(index));
	}
	
	@Override
	public int hashCode() {
	   return equivHashCode();
	}

	@Override
	public List<ValueBox> getUseBoxes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void toString(UnitPrinter up) {
		// TODO Auto-generated method stub

	}
	public String toString(){
		return "Param@"+index;
	}
	
	@Override
	public Object clone(){
		return new ParamValue(index);
	}

}
