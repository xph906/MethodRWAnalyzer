package nu.analysis.values;

import java.util.Comparator;
import java.util.Set;

import soot.SootMethod;

public class NativeCallRetValue extends AtomRightValue{
	Comparator<RightValue> RightValueComparator = (RightValue a, RightValue b) -> {
	    return a.toString().compareTo(b.toString());
	};
	SootMethod method;
	Set[] args;
	Set<AtomRightValue> thisArg;
	String cls;
	
	public void addArgSet(int index, Set<RightValue> argSet){
		//TODO: ensure their all args are AtomRightValue and no NativeCallRetValue!
	}
	public void addThisArgSet(Set<RightValue> argSet){
		//TODO: ensure their all args are AtomRightValue and no NativeCallRetValue!
	}
	
	
	@Override
	public boolean equivTo(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object clone() {
		// TODO Auto-generated method stub
		return null;
	}

}
