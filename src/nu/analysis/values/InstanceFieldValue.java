package nu.analysis.values;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import soot.SootField;
import soot.Type;
import soot.UnitPrinter;
import soot.Value;
import soot.ValueBox;
import soot.util.Switch;

public class InstanceFieldValue extends RightValue {
	//ThisValue, ParamValue, NewValue, CallRetValue, StaticFieldValue
	//Note that InstanceFieldValue's base cannot be another InstanceFieldValue.
	AtomRightValue base; 
	 List<SootField> fields;
	
	public InstanceFieldValue(AtomRightValue b, List<SootField> fields){
		base = b;
		this.fields = fields;
	}
	public AtomRightValue getBase(){
		return base;
	}
	public void setBase(AtomRightValue b){
		this.base = b;
	}
	private List<SootField> getFields(){
		return fields;
	}
	public List<SootField> getCloneFields(){
		List<SootField> newFields = new ArrayList<SootField>(fields.size());
		for(SootField f : fields)
			newFields.add(f);
		return newFields;
	}
	public void addField(SootField field){
		if(fields.size()<4)
			fields.add(field);
		else
			System.out.println("stop adding field because it's 3");
	}
	public void addFields(Collection<SootField> newFields){
		if(fields.size()+newFields.size()<=4)
			fields.addAll(newFields);
		else
			System.out.println("stop adding field because it's 3");
	}
	
	public boolean isThisReference(){
		if(base instanceof ThisValue)
			return true;
		return false;
	}
	public boolean isParamReference(){
		if(base instanceof ParamValue)
			return true;
		return false;
	}
	
	@Override
	public void apply(Switch sw) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean equivTo(Object o) {
		/*if(o instanceof InstanceFieldValue){
			return toString().equals(o.toString());
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
		return Objects.hash("INSTANCEFIELDREF-MAGICSTRING", toString());
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
		StringBuilder sb = new StringBuilder();
		for(SootField f : fields)
			sb.append("."+f.getName());
		return base.toString()+sb.toString();
	}

	@Override
	public Object clone(){
		List<SootField> newFields = new ArrayList<SootField>();
		for(SootField sf : fields)
			newFields.add(sf);
		return new InstanceFieldValue(base, newFields);
	}
}
