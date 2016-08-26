package nu.analysis.values;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import nu.analysis.values.RightValue;
import soot.SootMethod;
import soot.Type;
import soot.UnitPrinter;
import soot.Value;
import soot.ValueBox;
import soot.util.Switch;

public class CallRetValue extends AtomRightValue  {
	SootMethod method;
	Set<RightValue> args;
	String cls;
	public CallRetValue(SootMethod m, Set<RightValue> args){
		this.method = m;
		this.args = args;
		this.cls = (method.getDeclaringClass()==null) ? "null" : method.getDeclaringClass().getName();
	}
	
	public SootMethod getMethod(){
		return method;
	}
	
	public Set<RightValue> getArgs(){
		return args;
	}
	
	public String getCls(){
		return cls;
	}
	
	@Override
	public void apply(Switch sw) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean equivTo(Object o) {
		/*if(o instanceof CallRetValue){
			CallRetValue r = (CallRetValue)o;
			if(cls.equals(r.getCls()) && method.getName().equals(r.getMethod().getName())){
				Set<RightValue> args2 = r.getArgs();
				if(args==null && args2==null)
					return true;
				else if(args!=null && args2!=null){
					if(args.size() == args2.size()){
						for(RightValue rv : args){
							if(! args2.contains(rv))
								return false;
						}
						return true;
					}
				}
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
		return Objects.hash(toFullString());
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
	
	private String toFullString(){
		StringBuilder sb = new StringBuilder();
		sb.append(cls);
		sb.append(":");
		sb.append(method.getName());
		sb.append("(");
		if(args==null || args.size()==0)
			sb.append(")");
		else{
			for(Value v : args){
				sb.append(v.toString()+",");
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append(")");
		}
		return sb.toString();
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(method.getName());
		sb.append("(");
		if(args==null || args.size()==0)
			sb.append(")");
		else{
			for(Value v : args){
				sb.append(v.toString()+",");
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append(")");
		}
		return sb.toString();
	}
	
	@Override
	public Object clone(){
		Set<RightValue> newArgs = new HashSet<RightValue>(args.size());
		for(RightValue rv : args)
			newArgs.add(rv);
		return new CallRetValue(method, newArgs);
	}

}
