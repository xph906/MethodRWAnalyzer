package nu.analysis.values;

import java.util.List;
import java.util.Objects;

import soot.SootClass;
import soot.SootField;
import soot.Type;
import soot.UnitPrinter;
import soot.Value;
import soot.ValueBox;
import soot.util.Switch;

public class StaticFieldValue extends AtomRightValue  {
	//ThisValue, ParamValue, NewValue
	SootClass base; 
	SootField field;
	
	public StaticFieldValue(SootClass b, SootField f){
		base = b;
		field = f;
	}
	public SootClass getBase(){
		return base;
	}
	public SootField getField(){
		return field;
	}
	
	@Override
	public void apply(Switch sw) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean equivTo(Object o) {
		/*if(o instanceof StaticFieldValue){
			StaticFieldValue r = (StaticFieldValue)o;
			if(base.getName().equals(r.getBase().getName()) && 
					field.getName().equals(r.getField().getName())){
				return true;
			}
		}
		return false;*/
		return hashCode() == o.hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		return equivTo(o);
	}

	@Override
	public int equivHashCode() {
		return Objects.hash("STATICFIELDREF-MAGICSTR", base.getName(), field.getName());
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
		return base.getName().toString()+"."+field.getName();
	}

	@Override
	public Object clone(){
		return new StaticFieldValue(base, field);
	}
	
	@Override
	public int hashCode() {
	   return equivHashCode();
	}
}
