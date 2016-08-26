package nu.analysis.values;

import java.util.Objects;

import soot.Value;

public class UndefinedValue extends AtomRightValue {
	Value var = null;
	public UndefinedValue(Value k){
		this.var = k;
	}
	
	@Override
	public boolean equivTo(Object o){
		if(o instanceof UndefinedValue){
			return true;
		}
		return false;
	}
	
	@Override
	public boolean equals(Object o){
		//return this.equivTo(o);
		return hashCode() == o.hashCode();
	}
	
	@Override
	public String toString(){
		return "UNDEF["+var.toString()+"]";
	}
	
	@Override
	public int equivHashCode() {
		return Objects.hash(toString());
	}
	
	@Override
	public int hashCode() {
	   return equivHashCode();
	}
	
	@Override
	public Object clone(){
		return new UndefinedValue(var);
	}
}
