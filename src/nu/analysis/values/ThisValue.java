package nu.analysis.values;

import java.util.List;
import java.util.Objects;

import soot.SootClass;
import soot.Type;
import soot.UnitPrinter;
import soot.Value;
import soot.ValueBox;
import soot.util.Switch;

public class ThisValue extends AtomRightValue {
	SootClass cls;
	
	public ThisValue(SootClass sc){
		cls = sc;
	}
	
	public SootClass getThisClass(){
		return cls;
	}
	
	@Override
	public void apply(Switch sw) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean equivTo(Object o) {
		/*if(o instanceof ThisValue){
			ThisValue t = (ThisValue)o;
			if(t.getThisClass().equals(cls))
				return true;
		}
		return false;*/
		return hashCode() == o.hashCode();
	}
	
	@Override
	public int hashCode() {
	   return equivHashCode();
	}
	
	@Override
	public boolean equals(Object o){
		return this.equivTo(o);
	}

	@Override
	public int equivHashCode() {
		return Objects.hash("THIS", cls.getName());
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
	
	@Override
	public String toString(){
		return "THIS";
	}
	
	@Override
	public Object clone(){
		return new ThisValue(cls);
	}

}
