package nu.analysis.values;

import java.util.Objects;

import soot.Type;
import soot.Value;
import soot.jimple.Constant;
import soot.util.Switch;

public class ConstantValue extends AtomRightValue {
	Value originalValue;
	public ConstantValue(Value v){
		originalValue = v;
	}
	
	public Value getOriginalValue(){
		return this.originalValue;
	}
	
	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void apply(Switch sw) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public boolean equivTo(Object o){
		/*if(o instanceof ConstantValue){
			return true;
		}
		return false;*/
		return hashCode() == o.hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		return this.equivTo(o);
	}
	
	@Override
	public String toString(){
		return "CONST";
	}
	
	@Override
	public int equivHashCode() {
		return Objects.hash("CONSTANTVALUE-MAGICSTRING");
	}

	@Override
	public int hashCode() {
	   return equivHashCode();
	}

	@Override
	public Object clone() {
		return new ConstantValue(originalValue);
	}

}
