package nu.analysis.values;

import java.util.List;

import soot.Type;
import soot.UnitPrinter;
import soot.Value;
import soot.ValueBox;
import soot.util.Switch;

public abstract class RightValue implements Value {

	@Override
	public void apply(Switch sw) {
		// TODO Auto-generated method stub

	}
	
	@Override
	abstract public boolean equivTo(Object o);

	@Override
	public int equivHashCode() {
		// TODO Auto-generated method stub
		return 0;
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
	abstract public String toString();
	
	@Override
	abstract public Object clone();
	
	@Override
	abstract public boolean equals(Object o);
	
	@Override
	abstract public int hashCode();

}
