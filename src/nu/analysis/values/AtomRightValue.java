package nu.analysis.values;

abstract public class AtomRightValue extends RightValue{
	@Override
	public int hashCode() {
	   return equivHashCode();
	}
	
	@Override
	public boolean equals(Object o){
		return hashCode() == o.hashCode();
	}
}