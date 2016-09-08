package nu.analysis.values;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import soot.Type;
import soot.Unit;
import soot.UnitPrinter;
import soot.Value;
import soot.ValueBox;
import soot.util.Switch;

public class NewValue extends AtomRightValue {
	Unit defUnit;
	ArrayList<Set> args;
	
	
	public Unit getUnit(){
		return defUnit;
	}
	public NewValue(Unit u){
		defUnit = u;
		args = new ArrayList<Set>();
	}
	public int getArgCount(){
		return args.size();
	}
	public void addArgSet(int index, Set<RightValue> argSet){
		while(index >= args.size()){
			args.add(new HashSet<AtomRightValue>());
		}
		Set<RightValue> s = args.get(index);
		
		for(RightValue rv : argSet){
			if(rv instanceof AtomRightValue){
				s.add((AtomRightValue)rv);
			}
			else if(rv instanceof InstanceFieldValue)
				s.add(rv);
			else if(rv instanceof CallRetValue){
				CallRetValue crv = (CallRetValue)rv;
				if(crv.getThisArgs() != null){
					for(RightValue rrv : crv.getThisArgs())
						s.add(rrv);
				}
				for(int i=0; i<crv.getArgCount(); i++){
					for(RightValue rrv : crv.getArgs(i)){
						s.add(rrv);
					}
				}
			}
			else {
				System.out.println("ALERT: value "+rv+" cannot be added as arg");
			}
		}
	}
	
	@Override
	public void apply(Switch sw) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean equivTo(Object o) {
		//TODO: compare unit tag
		/*if(o instanceof NewValue){
			NewValue r = (NewValue)o;
			if(r.getUnit().equals(defUnit))
				return true;
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
		//TODO: add unit tag
		//return defUnit.hashCode();
		return Objects.hash("NEW-MAGICSTRING", defUnit);
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
	
	@Override
	public String toString(){
		return "NewValue"+defUnit.hashCode();
	}

	@Override
	public Object clone(){
		return new NewValue(defUnit);
	}
}
