package nu.analysis.values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

public class CallRetValue extends RightValue  {
	Comparator<RightValue> RightValueComparator = (RightValue a, RightValue b) -> {
	    return a.toString().compareTo(b.toString());
	};
	
	SootMethod method;
	//Set<RightValue> args;
	Set[] args;
	
	Set<RightValue> thisArg;
	String cls;
	
	//Args can only be AtomRightValue(including StaticFieldValue) or InstanceFieldValue
	public CallRetValue(SootMethod m){
		this.method = m;
		this.args = new Set[m.getParameterCount()];
		this.cls = (method.getDeclaringClass()==null) ? "null" : method.getDeclaringClass().getName();
		this.thisArg = null;
	}
	public void addArgSet(int index, Set<RightValue> argSet){
		assert(index < args.length);
		Set<RightValue> newArgSet = new HashSet<RightValue>();
		for(RightValue rv : argSet){
			if(rv instanceof AtomRightValue){
				newArgSet.add(rv);
			}
			else if(rv instanceof InstanceFieldValue)
				newArgSet.add(rv);
			else if(rv instanceof CallRetValue){
				CallRetValue crv = (CallRetValue)rv;
				if(crv.getThisArgs() != null){
					for(RightValue rrv : crv.getThisArgs())
						newArgSet.add(rrv);
				}
				for(int i=0; i<crv.getArgCount(); i++){
					for(RightValue rrv : crv.getArgs(i)){
						newArgSet.add(rrv);
					}
				}
			}
			else {
				System.out.println("ALERT: value "+rv+" cannot be added as arg");
			}
		}
		args[index] = newArgSet;
	}
	
	//
	public void addThisArgSet(Set<RightValue> argSet){
		//this.thisArg = argSet;
		if(argSet==null)
			return ;
		Set<RightValue> newArgSet = new HashSet<RightValue>();
		for(RightValue rv : argSet){
			if(rv instanceof AtomRightValue){
				newArgSet.add(rv);
			}
			else if(rv instanceof InstanceFieldValue)
				newArgSet.add(rv);
			else if(rv instanceof CallRetValue){
				CallRetValue crv = (CallRetValue)rv;
				if(crv.getThisArgs() != null){
					for(RightValue rrv : crv.getThisArgs())
						newArgSet.add(rrv);
				}
				for(int i=0; i<crv.getArgCount(); i++){
					for(RightValue rrv : crv.getArgs(i)){
						newArgSet.add(rrv);
					}
				}
			}
			else {
				System.out.println("ALERT: value "+rv+" cannot be added as arg");
			}
		}
		this.thisArg = newArgSet;
	}
	
	public SootMethod getMethod(){
		return method;
	}
	
	public Set<RightValue> getArgs(int index){
		return args[index];
	}
	public Set<RightValue> getThisArgs(){
		return thisArg;
	}
	
	public int getArgCount(){
		return args.length;
	}
	
	public String getCls(){
		return cls;
	}
	
	@Override
	public void apply(Switch sw) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public int equivHashCode() {
		return Objects.hash(toFullString());
	}
	@Override
	public boolean equivTo(Object o) {
		return hashCode() == o.hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		return equivTo(o);
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
		if((args==null || args.length==0) && thisArg==null)
			sb.append(")");
		else if(args==null || args.length==0){
			sb.append("[THIS:");
			List<RightValue> tmp= new ArrayList<RightValue>(thisArg);
			Collections.sort(tmp, RightValueComparator);
			for(Value v : tmp){
				sb.append(v.toString()+",");
			}
			sb.deleteCharAt(sb.length()-1);		
			sb.append("]");
			sb.append(")");
		}
		else{
			if(thisArg!=null){
				sb.append("[THIS:");
				List<RightValue> tmp= new ArrayList<RightValue>(thisArg);
				Collections.sort(tmp, RightValueComparator);
				for(Value v : tmp){
					sb.append(v.toString()+",");
				}
				sb.deleteCharAt(sb.length()-1);		
				sb.append("]");
			}
			else{
				sb.append("[THIS:null]");
			}
			for(int i=0; i<args.length; i++){		
				sb.append("[@"+i+":");
				Set<RightValue> argSet = args[i];
				if(argSet!=null && argSet.size()>0){
					List<RightValue> tmp= new ArrayList<RightValue>(argSet);
					Collections.sort(tmp, RightValueComparator);
					for(Value v : tmp){
						sb.append(v.toString()+",");
					}
					sb.deleteCharAt(sb.length()-1);
				}		
				sb.append("]");
			}
			sb.append(")");
		}
		return sb.toString();
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(method.getName());
		sb.append("(");
		if((args==null || args.length==0) && thisArg==null)
			sb.append(")");
		else if(args==null || args.length==0){
			sb.append("[THIS:");
			List<RightValue> tmp= new ArrayList<RightValue>(thisArg);
			Collections.sort(tmp, RightValueComparator);
			for(Value v : tmp){
				sb.append(v.toString()+",");
			}
			sb.deleteCharAt(sb.length()-1);		
			sb.append("]");
			sb.append(")");
		}
		else{
			if(thisArg!=null){
				sb.append("[THIS:");
				List<RightValue> tmp= new ArrayList<RightValue>(thisArg);
				Collections.sort(tmp, RightValueComparator);
				for(Value v : tmp){
					sb.append(v.toString()+",");
				}
				sb.deleteCharAt(sb.length()-1);		
				sb.append("]");
			}
			else{
				sb.append("[THIS:null]");
			}
			for(int i=0; i<args.length; i++){		
				sb.append("[@"+i+":");
				Set<RightValue> argSet = args[i];
				if(argSet!=null && argSet.size()>0){
					List<RightValue> tmp= new ArrayList<RightValue>(argSet);
					Collections.sort(tmp, RightValueComparator);
					for(Value v : tmp){
						sb.append(v.toString()+",");
					}
					sb.deleteCharAt(sb.length()-1);
				}		
				sb.append("]");
			}
			sb.append(")");
		}
		return sb.toString();
	}
	
	@Override
	public Object clone(){
		CallRetValue newValue = new CallRetValue(method);
		Set<RightValue> newThisArg = null;
		if(thisArg!=null){
			newThisArg = new HashSet<RightValue>(thisArg.size());
			for(RightValue rv : thisArg)
				newThisArg.add(rv);
		}
		newValue.addThisArgSet(newThisArg);;
		for(int i=0; i<args.length; i++){
			Set<RightValue> arg = args[i];
			Set<RightValue> newArg = null;
			if(arg!=null){
				newArg = new HashSet<RightValue>(arg.size());
				for(RightValue rv : arg)
					newArg.add(rv);
			}
			newValue.addArgSet(i, newArg);
		}
		assert(newValue.equals(this));
		return newValue;
	}

}
