package nu.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.analysis.values.AtomRightValue;
import nu.analysis.values.CallRetValue;
import nu.analysis.values.ConstantValue;
import nu.analysis.values.InstanceFieldValue;
import nu.analysis.values.NewArrayValue;
import nu.analysis.values.NewValue;
import nu.analysis.values.ParamValue;
import nu.analysis.values.RightValue;
import nu.analysis.values.StaticFieldValue;
import nu.analysis.values.ThisValue;
import nu.analysis.values.UndefinedValue;
import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AnyNewExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.DynamicInvokeExpr;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.Ref;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.ThisRef;
import soot.jimple.UnopExpr;
import soot.shimple.ShimpleExpr;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

//find variable defs.
public class IntraProcedureAnalysis extends ForwardFlowAnalysis<Unit, DefAnalysisMap>{
	DirectedGraph<Unit> graph = null;
	Pattern thisPattern = Pattern.compile("^@this:");
	Pattern paramPattern = Pattern.compile("^@parameter(\\d+):");
	SootMethod method = null;
	Comparator<Value> ValueComparator = (Value a, Value b) -> {
	    return a.toString().compareTo(b.toString());
	};
	
	public IntraProcedureAnalysis(DirectedGraph<Unit> graph, SootMethod m) {
		super(graph);
		this.graph = graph;
		this.method = m;
		doAnalysis();
	}

	@Override
	protected DefAnalysisMap newInitialFlow() {
		//System.out.println("NewInitialFlow is called.");
		return new DefAnalysisMap();
	}
	@Override
	protected DefAnalysisMap entryInitialFlow() {
		//System.out.println("EntryInitialFlow is called.");
		return new DefAnalysisMap();
	}

	@Override
	protected void flowThrough(DefAnalysisMap in, Unit d,
			DefAnalysisMap out) {
		//copy in to out.
		copy(in, out);
		
		if(d instanceof IdentityStmt){
			IdentityStmt is = (IdentityStmt)d;
			Value left = is.getLeftOp();
			if(left instanceof Ref){
				//TODO: double check IdentityStmt's left cannot be a ref.
				System.out.println("ALERT: IdentityStmt TODO: left is ref. "+is);
			}
			if(in.containsKey(left) ){
				System.out.println("ALERT: contains IdentityStmt: "+in.get(is.getLeftOp()).size());
			}
			if(is.getRightOp() instanceof ThisRef){
				out.setNewValue(left, new ThisValue(this.method.getDeclaringClass()));
			}
			else if(is.getRightOp() instanceof ParameterRef){
				ParameterRef pr = (ParameterRef)is.getRightOp();
				out.setNewValue(left, new ParamValue(pr.getIndex()));
			}
			else{
				System.err.println("error: IdentityStmt is neither this or param");
			}
		}
		else if(d instanceof AssignStmt){
			AssignStmt as = (AssignStmt)d;
			System.out.println("AssignStmt: "+as);
			
			Value rightOp = as.getRightOp();
			Value leftOp = as.getLeftOp();
			List<RightValue> leftOps = null;
			if(leftOp instanceof InstanceFieldRef)
				leftOps = fromInstanceFieldRef2InstanceFieldValue((InstanceFieldRef)leftOp, in);
			else if(leftOp instanceof ArrayRef)
				leftOps = fromArrayRef2RightValue((ArrayRef)leftOp, in);
			else if(leftOp instanceof Ref){
				if(!(leftOp instanceof StaticFieldRef))
					System.out.println("  ALERT: leftOp unknown FieldRef: "+leftOp.getClass());
			}
			if(leftOps==null )
				leftOps = new ArrayList<RightValue>();
			
			out.remove(leftOp); //kill existing one.
			for(RightValue rv : leftOps)
				out.remove(rv);
			
			RightValue newRightValue = null;
			//it is guaranteed that the IN set contains the top value.
			//For example, if it contains a mapping of this.field1 -> this.field1
			//this means that this.field1's value is from outside of the method at 
			//this unit.
			if(as.containsInvokeExpr()){
				InvokeExpr ie = (InvokeExpr)as.getRightOp();
				Set<RightValue> values = new HashSet<RightValue>();
				if(ie instanceof InstanceInvokeExpr){
					InstanceInvokeExpr iie = (InstanceInvokeExpr)ie;
					Set<RightValue> vs = resolveRightValue(iie.getBase(), in, "InstanceInvokeExpr");
					for(RightValue v : vs)
						values.add(v);
				}
				else if(ie instanceof DynamicInvokeExpr){
					System.out.println("DYNAMICINVOKEEXPR ");
				}
				System.out.println("  DEBUG: InvokeExpr "+as.getRightOp().getType());
				
				//there is no order for arguments.
				for(int i=0; i<ie.getArgCount(); i++){
					Value arg = ie.getArg(i);
					Set<RightValue> vs = resolveRightValue(arg, in, "InvokeExprArgs");
					for(RightValue v : vs)
						values.add(v);
				}
				newRightValue = new CallRetValue(ie.getMethod(), values);
				out.setNewValue(leftOp, newRightValue);
				for(RightValue rv : leftOps)
					out.setNewValue(rv, newRightValue);
				System.out.println("    DEBUG: InvokeExpr RS:"+newRightValue);
			}
			else if(rightOp instanceof AnyNewExpr){
				System.out.println("  AnyNewExpr.");
				newRightValue = new NewValue(as);
				out.setNewValue(leftOp, newRightValue);
				for(RightValue rv : leftOps)
					out.setNewValue(rv, newRightValue);
			}
			else if(rightOp instanceof BinopExpr){
				System.out.println("  DEBUG: BinopExpr");
				BinopExpr be = (BinopExpr)rightOp;
				Value arg1 = be.getOp1();
				Set<RightValue> values1 = resolveRightValue(arg1, in, "BinopExpr");
				Value arg2 = be.getOp2();
				Set<RightValue> values2 = resolveRightValue(arg2, in, "BinopExpr");
				for(RightValue rv : values2){
					//System.out.println("  DEBUG: BinopExpr RS:"+rv);
					values1.add(rv);
				}
				for(RightValue rv : values1){
					System.out.println("  DEBUG: BinopExpr TO:" +leftOp+ " add RV:"+rv+" ");
					out.addNewValue(leftOp, rv);
					for(RightValue v2 : leftOps)
						out.addNewValue(v2, rv);
				}
				System.out.println("  DEBUG: BinopExpr RS:"+out.toString());
			}
			else if(rightOp instanceof InstanceOfExpr){
				System.out.println("  DEBUG: InstanceOfExpr do nothing.");
			}
			else if(rightOp instanceof NewArrayExpr){
				System.out.println("  DEBUG: NewArrayExpr");
				newRightValue = new NewArrayValue(as);
				out.setNewValue(leftOp, newRightValue);
				for(RightValue rv : leftOps)
					out.setNewValue(rv, newRightValue);
			}
			else if(rightOp instanceof NewExpr){
				System.out.println("  DEBUG: NewExpr.");
				newRightValue = new NewValue(as);
				out.setNewValue(leftOp, newRightValue);
				for(RightValue rv : leftOps)
					out.setNewValue(rv, newRightValue);
			}
			else if(rightOp instanceof NewMultiArrayExpr){
				System.out.println("  DEBUG: NewMultiArrayExpr.");
				newRightValue = new NewValue(as);
				out.setNewValue(leftOp, newRightValue);
				for(RightValue rv : leftOps)
					out.setNewValue(rv, newRightValue);
			}
			else if(rightOp instanceof ShimpleExpr){
				System.out.println("  DEBUG: ShimpleExpr do nothing.");
			}
			else if(rightOp instanceof UnopExpr){
				System.out.println("  DEBUG: UnopExpr");
				UnopExpr ue = (UnopExpr)rightOp;
				Value arg = ue.getOp();
				Set<RightValue> values = resolveRightValue(arg, in, "UnopExpr");
				for(RightValue rv : values){
					System.out.println("  DEBUG: UnopExpr RS:"+rv);
				}
				for(RightValue rv : values){
					out.addNewValue(leftOp, rv);
					for(RightValue v2 : leftOps)
						out.addNewValue(v2, rv);
				}
			}
			else if(rightOp instanceof CastExpr){
				System.out.println("  DEBUG: CastExpr");
				CastExpr ce = (CastExpr)rightOp;
				Value arg = ce.getOp();
				Set<RightValue> values = resolveRightValue(arg, in, "CastExpr");
				for(RightValue rv : values){
					System.out.println("  DEBUG: UnopExpr RS:"+rv);
				}
				for(RightValue rv : values){
					out.addNewValue(leftOp, rv);
					for(RightValue v2 : leftOps)
						out.addNewValue(v2, rv);
				}
			}
			else if(rightOp instanceof Constant){
				System.out.println("  DEBUG: Constant");
				newRightValue = new ConstantValue(rightOp);
				out.setNewValue(leftOp, newRightValue);
				for(RightValue rv : leftOps)
					out.setNewValue(rv, newRightValue);
			}
			else if((rightOp instanceof Local)           ||
					(rightOp instanceof ArrayRef)        ||
					(rightOp instanceof InstanceFieldRef)||
					(rightOp instanceof StaticFieldRef)){
				System.out.println("  DEBUG: "+rightOp.getClass());
				Set<RightValue> values = resolveRightValue(rightOp, in, rightOp.getClass().toString());
				for(RightValue rv : values){
					System.out.println("  DEBUG: "+rightOp.getClass()+" RS:"+rv);
					out.addNewValue(leftOp, rv);
					for(RightValue v2 : leftOps)
						out.addNewValue(v2, rv);
				}
			}
			else if(rightOp instanceof ThisRef){
				System.out.println("  ALERT: THISREF"+rightOp);
			}
			else if(rightOp instanceof ParameterRef){
				System.out.println("  ALERT: PARAMREF:"+rightOp);
			}
			else{
				System.err.println("error: unknow expr in assignStmt: "+rightOp.getClass()+" "+d);
			}
		}
		else if(d instanceof InvokeStmt){
			InvokeStmt is = (InvokeStmt)d;
			if(is.getInvokeExpr().getMethod().isConstructor()){
				System.out.println("InvokeStmt constructor: "+is);
				if(is.getInvokeExpr() instanceof InstanceInvokeExpr){
					InstanceInvokeExpr iie = (InstanceInvokeExpr)is.getInvokeExpr();
					Set<RightValue> values = in.get(iie.getBase());
					if(values == null){
						System.err.println("error: cannot find constructor's base.");
					}
					else{
						boolean replace = false;
						Set<RightValue> args = new HashSet<RightValue>();
						List<NewValue> deleted = new ArrayList(2);
						for(RightValue v : values){
							if (v instanceof NewValue){
								for(int i=0; i<iie.getArgCount(); i++){
									replace = true;
									deleted.add((NewValue)v);
									Value arg = iie.getArg(i);
									Set<RightValue> vs = resolveRightValue(arg, in, "ConstructorInvokeExprArgs");
									for(RightValue vv : vs){
										args.add(vv);
										System.out.println("  DEBUG: constructorArg RS:"+vv);
									}
								}
							}
						}
						if(replace){
							RightValue crv = new CallRetValue(is.getInvokeExpr().getMethod(), args);
							Value leftOp = iie.getBase();
							out.addNewValue(leftOp, crv);
							int c1 = out.get(leftOp).size();
							Iterator<RightValue> it = out.get(leftOp).iterator();
							while(it.hasNext()){
								RightValue rv = it.next();
								if(deleted.contains(rv))
									it.remove();
								else{
									System.out.println("NOTMATCH"+rv+" "+deleted.iterator().next());
								}
								
							}
							System.out.println("Deleted a NewValue: "+c1+" "+out.get(leftOp).size());
						}
					}
				}
				
			}
		}	
	}

	@Override
	protected void merge(DefAnalysisMap in1, DefAnalysisMap in2,
			DefAnalysisMap out) {
		//TODO: 
		//in1.contains(X.f1) 
		//!in2.contains(X.f1) && in2.values.contains(X.f1)
		//out[X.f1] = [in1.contains(X.f1)  + X.f1].
		out.clear();
		for(Value k : in1.keySet()){
			for(RightValue v : in1.get(k))
				out.addNewValue(k, v);
			if(!in2.keySet().contains(k))
				out.addNewValue(k, new UndefinedValue(k));
		}
		for(Value k : in2.keySet()){
			for(RightValue v : in2.get(k))
				out.addNewValue(k, v);
			if(!in1.keySet().contains(k))
				out.addNewValue(k, new UndefinedValue(k));
		}
	}
	
	private boolean defAnalysisMapValueContainKey(DefAnalysisMap in, Value key){
		for(Set<RightValue> valueSet : in.values()){
			if(valueSet.contains(key))
				return true;
			else{
				for(RightValue rv : valueSet)
					System.out.println("  DAMCK:"+rv+" VS "+key);
			}
		}
		return false;
	}

	@Override
	protected void copy(DefAnalysisMap source, DefAnalysisMap dest) {
		if(dest==null){
			System.err.println("error: copy dest is null");
		}
		dest.clear();
		for(Value k : source.keySet()){
			Set<RightValue> tmp = new HashSet<RightValue>();
			for(RightValue rv : source.get(k))
				tmp.add((RightValue)rv.clone());
			dest.put(k, tmp);
		}	
	}

	/*
	private Set<Value> findRightRefValueDef(Value arg, DefAnalysisMap in){
		if(in.containsKey(arg))
			return in.get(arg);
		
		Set<Value> rs = new HashSet<Value>();
		
		if(arg instanceof ArrayRef){
			//ArrayRef returns the base Value
			System.out.println("  ArrayRef");
			ArrayRef ar = (ArrayRef)arg;
			Value b = ar.getBase();
			if(in.containsKey(b))
				return in.get(b);
			else
				System.out.println("  DEBUG: findRightValue 1 cannot find right ref.");
		}
		else if(arg instanceof InstanceFieldRef){
			InstanceFieldRef ir = (InstanceFieldRef)arg;
			Value b = ir.getBase();
			if(in.containsKey(b)){
				for(Value v : in.get(b)){
					Value nn = new InstanceFieldValue(v, ir.getField());
					if(in.containsKey(nn)){
						rs.addAll(in.get(nn));
						System.out.println("  DEBUG: findRightValue 2 instanceFiledRef right: in.containsKey. Add:"+in.get(nn).size());
					}
					else{
						rs.add(nn);
						System.out.println("  DEBUG: findRightValue 3 instanceFiledRef right: in not containsKey. Add:"+nn);
					}
				}
			}
			else if(b instanceof ThisValue || b instanceof ParamValue){
				Value tmp = new InstanceFieldValue(b, ir.getField());
				rs.add(tmp);
				System.out.println("  DEBUG: findRightValue 4 base not in, but is this or param."+tmp);
			}
			Value newKey = new InstanceFieldValue(ir.getBase(), ir.getField());
		}
		else if(arg instanceof StaticFieldRef){
			StaticFieldRef sr = (StaticFieldRef)arg;
			return new StaticFieldValue(sr.getField().getDeclaringClass(), sr.getField());
		}
		else if(arg instanceof ThisRef){
			System.out.println("  ThisRef: ALERT:"+arg+" shouldn't be here");
		}
		else if(arg instanceof ParameterRef){
			System.out.println("  ParameterRef: ALERT:"+" should't be here");
		}
		
		return rs;
	}
	private List<Value> processLeftRefValue(Value leftOp,  DefAnalysisMap in){
		List<Value> leftOpRefs = new ArrayList<Value>();
		if(leftOp instanceof ArrayRef){
			//for ArrayRef, we don't remove its base defs.
			ArrayRef lar = (ArrayRef)leftOp;
			if(in.containsKey(lar.getBase())){
				leftOpRefs.addAll(in.get(lar.getBase()));
				System.out.println("  DEBUG: ArrayRef find base refs:"+in.get(lar.getBase()));
			}
			else
				System.out.println("  DEBUG: ArrayRef cannot find base defs.");
		}
		else if(leftOp instanceof InstanceFieldRef){
			InstanceFieldRef ifr = (InstanceFieldRef)leftOp;
			if(in.containsKey(ifr.getBase())){
				System.out.println("  DEBUG InstanceRef find base refs:"+in.get(ifr.getBase()).size());
				for(Value v : in.get(ifr.getBase())){
					Value refKey = new InstanceFieldValue(v, ifr.getField());
					leftOpRefs.add(refKey);
				}
			}
			else{
				System.out.println("  DEBUG InstanceRef cannot find base refs:"+ifr.getBase());
			}
		}
		else if(leftOp instanceof StaticFieldRef){	
			StaticFieldRef srf = (StaticFieldRef)leftOp;
			Value refKey = new StaticFieldValue(srf.getField().getDeclaringClass(), srf.getField());
			leftOpRefs.add(refKey);
			System.out.println("  DEBUG StaticFieldRef found:"+refKey);
		}
		return leftOpRefs;
	}
	*/
	
	private List<RightValue> fromArrayRef2RightValue(ArrayRef a, DefAnalysisMap in){
		List<RightValue> rs = new ArrayList<RightValue>();
		if(in.containsKey(a.getBase()))
			return new ArrayList<RightValue>(in.get(a.getBase()));
		return rs;
	}
	
	//mapping 1 -> n
	//Results List<instanceFieldValue>
	private List<RightValue> fromInstanceFieldRef2InstanceFieldValue(InstanceFieldRef v, DefAnalysisMap in){
		List<RightValue> rs = new ArrayList<RightValue>();
		LinkedList<SootField> fields = new LinkedList<SootField>();
		
		InstanceFieldRef tmp = (InstanceFieldRef)v;
		Value b = tmp.getBase();
		SootField f = tmp.getField();
		fields.addFirst(f);
		if(in.containsKey(b)){
			for(RightValue rv : in.get(b)){
				if(rv instanceof AtomRightValue){
					List<SootField> newFields = copyList(fields);
					rs.add(new InstanceFieldValue((AtomRightValue)rv, newFields));
				}
				else if(rv instanceof InstanceFieldValue){
					InstanceFieldValue ifv = (InstanceFieldValue)rv;
					List<SootField> newFields = copyList(ifv.getFields());
					newFields.add(f);
					rs.add(new InstanceFieldValue(ifv.getBase(), newFields));
				}
				else{
					System.out.println("  DEBUG: getInstanceFieldValue ignore "+rv+" "+rv.getClass());
				}
			}
		}
		else if(b instanceof Ref){
			//TODO: needs further testing if ref's base can be ref.
			//Now we assume it shouldn't.
			System.out.println("ALERT: getInstanceFieldValue TODO: ref's base can be ref."+b);
		}
		else{
			System.out.println("  ALERT: getInstanceFieldValue: cannot find ref's base.");
		}
		
		return rs;
	}
	
	private Set<RightValue> resolveRightValue(Value arg,  DefAnalysisMap in, String msg){
		Set<RightValue> values = new HashSet<RightValue>();
		if(arg instanceof Constant){
			values.add(new ConstantValue(arg));
		}
		else if(in.containsKey(arg)){
			List<RightValue> tmp = new ArrayList<RightValue>(in.get(arg));
			Collections.sort(tmp, ValueComparator);
			for(RightValue v : tmp)
				values.add(v);
		}
		else if(arg instanceof Ref){
			//note that at this point, we cannot find original value from in.
			System.out.println("  DEBUG: "+msg+" do base searching for ref value: "+arg);
			if(arg instanceof ArrayRef){
				ArrayRef arrRef = (ArrayRef)arg;
				List<RightValue> tmp = fromArrayRef2RightValue(arrRef, in);
				System.out.println("    DEBUG:  ArrayRef: found "+tmp.size()+" base values.");
				for(RightValue rv : tmp){
					if(in.containsKey(rv)){
						for(RightValue v : in.get(rv)){
							System.out.println("      Found source:"+v);
							values.add(v);
						}
					}
					else{
						System.out.println("      Not found source, add new rightValue: "+rv);
						values.add(rv);
					}
				}
			}
			else if(arg instanceof InstanceFieldRef){
				InstanceFieldRef ifr = (InstanceFieldRef)arg;
				List<RightValue> tmp = fromInstanceFieldRef2InstanceFieldValue(ifr, in);
				System.out.println("    DEBUG:  InstanceFieldRef: found "+tmp.size()+" base values.");
				for(RightValue rv : tmp){
					if(in.containsKey(rv)){
						for(RightValue v : in.get(rv)){
							System.out.println("      Found source:"+v);
							values.add(v);
						}
					}
					else{
						System.out.println("      Not found source, add new rightValue: "+rv);
						values.add(rv);
					}
				}
			}
			else if(arg instanceof StaticFieldRef){
				StaticFieldRef sfr = (StaticFieldRef)arg;
				System.out.println("    DEBUG:  StaticFieldRef: ");
				values.add(new StaticFieldValue(sfr.getField().getDeclaringClass(), sfr.getField()));
			}
			else{
				System.out.println("  ALERT: "+msg+" unknown ref: "+arg);
			}
		}
		return values;
	}
	
	private <T> List<T> copyList(List<T> another){
		List<T> newList = new ArrayList<T>(another.size());
		for(T o : another){
			newList.add(o);
		}
		return newList;
	}

}
