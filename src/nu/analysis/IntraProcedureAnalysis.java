package nu.analysis;

import java.util.ArrayList;
import java.util.Collection;
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

import nu.analysis.values.ArrayDataValue;
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
import soot.jimple.InterfaceInvokeExpr;
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
import soot.jimple.StaticInvokeExpr;
import soot.jimple.ThisRef;
import soot.jimple.UnopExpr;
import soot.shimple.ShimpleExpr;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

//TODO: not handling the scenario that data get modified via function call.
//For example, considering analyzing the input of method A, which will call method B. 
//Method B will modify several fields and the parameters passed into it,
//Therefore, the analysis results of A should include those changes made by method B.
//TODO: for ArrayRef, it will be labeled as READ even if it's completely rewritten in 
//the beginning.
public class IntraProcedureAnalysis extends ForwardFlowAnalysis<Unit, DefAnalysisMap>{
	DirectedGraph<Unit> graph = null;
	Pattern thisPattern = Pattern.compile("^@this:");
	Pattern paramPattern = Pattern.compile("^@parameter(\\d+):");
	SootMethod method = null;
	
	DefAnalysisMap initialValue, prevInitialValue;
	Set<RightValue> relatedFields, newRelatedFields;
	Set<RightValue> readFields, writeFields, retFields, funcalls;
	
	public IntraProcedureAnalysis(DirectedGraph<Unit> graph, SootMethod m) {
		super(graph);
		this.graph = graph;
		this.method = m;
		//initialValue = new DefAnalysisMap();
		//System.out.println("Analyzing method: "+m.getSignature());
		/*for(SootField f : m.getDeclaringClass().getFields()){
			RightValue ref = null;
			if(f.isStatic())
				ref = new StaticFieldValue(f.getDeclaringClass(), f);
			else{
				List<SootField> fields = new ArrayList<SootField>();
				fields.add(f);
				ref = new InstanceFieldValue(new ThisValue(f.getDeclaringClass()), fields);
			}
			
			initialValue.addNewValue(ref, ref);
			//initialValue.addNewValue(arrDataKey, null);
		}
		
		prevInitialValue = initialValue;*/
		relatedFields = getInitalRelatedFields(m);
		initialValue = genInitialDefAnalysisMapFromFields(relatedFields);
		//backup
		newRelatedFields = new HashSet<RightValue>();
		for(RightValue rv : relatedFields)
			newRelatedFields.add(rv);
		boolean repeat = false;
		int count = 0;
		do{
			repeat = false;
			doAnalysis();
			if(newRelatedFields.size() != relatedFields.size()){
				System.out.println("ALERT: new fields: "+newRelatedFields.size()+" vs "+relatedFields.size());
				for(RightValue rv : newRelatedFields)
					relatedFields.add(rv);
				initialValue = genInitialDefAnalysisMapFromFields(relatedFields);
				repeat = true;
			}
			count++;
		}while(repeat && count<10);
		if(count >= 10){
			System.out.println("error: method: "+m+" has converage issue!");
		}
		analyzeReadWriteFields();
		analyzeRetValue();
	}
	
	public DirectedGraph<Unit> getGraph() {
		return graph;
	}
	
	public DefAnalysisMap getFlowAfterFromRemoteUnit(Unit t){
		for(Unit u : graph){
			if(u.toString().equals(t.toString()))
				return getFlowAfter(u);
			else {
				System.out.println("=======================");
				System.out.println(u.toString());
				System.out.println(t.toString());
				System.out.println("#######################");
			}
		}
		return null;
	}

	public Set<RightValue> getRetFields() {
		return retFields;
	}
	public Set<RightValue> getFuncalls(){
		return funcalls;
	}

	public void analyzeRetValue(){
		retFields = new HashSet<RightValue>();
		for(Unit u: this.graph.getTails()){
			if (u instanceof ReturnStmt){
				ReturnStmt rs = (ReturnStmt)u;
				Value v = rs.getOp();
				if(v!=null){
					DefAnalysisMap dam = this.getFlowAfter(u);
					Set<RightValue> tmp = dam.get(v);
					if(tmp != null){
						for(RightValue rv : tmp){
							if(rv instanceof InstanceFieldValue || 
									rv instanceof StaticFieldValue ||
									rv instanceof ParamValue ||
									rv instanceof ThisValue)
								retFields.add(rv);
							else{
								//System.out.println("ALERT: other type of retrun value: "+rv);
							}
						}
					}
				}
			}
		}
	}
	
	public Set<RightValue> getReadFields() {
		return readFields;
	}

	public Set<RightValue> getWriteFields() {
		return writeFields;
	}

	public void analyzeReadWriteFields(){
		readFields = new HashSet<RightValue>();
		writeFields = new HashSet<RightValue>();
		funcalls = new HashSet<RightValue>();
		Iterator<Unit> it = this.graph.iterator();
		while(it.hasNext()){
			Unit unit = it.next();
			DefAnalysisMap dam = this.getFlowAfter(unit);
			Collection<Value> keys = dam.getId2Value().values();
			for(Value k : keys){
				Set<RightValue> values = dam.get(k);
				
				//ignore those put by ourselves.
				if(k instanceof InstanceFieldValue){
					if(values.size()==1){
						RightValue rv = values.iterator().next();
						if(rv instanceof InstanceFieldValue && rv.equals(k)){
							//System.out.println("DEBUG: prestored InstanceFiedlValue: "+k+" VS "+rv);
							continue;
						}
					}
					writeFields.add((InstanceFieldValue)k);
				}
				else if(k instanceof StaticFieldValue){
					if(values.size()==1){
						RightValue rv = values.iterator().next();
						if(rv instanceof StaticFieldValue && rv.equals(k)){
							//System.out.println("DEBUG: prestored StaticFieldValue: "+k+" VS "+rv);
							continue;
						}
					}
					writeFields.add((StaticFieldValue)k);
				}
				
				
				for(RightValue v : values){
					if(k instanceof InvokeExpr){
						//System.out.println("addfuncall: "+v);
						CallRetValue crv = (CallRetValue)v;
						Set<RightValue> args = crv.getThisArgs();
						if(args != null){
							for(RightValue rv : args){
								//if(rv instanceof InstanceFieldValue || rv instanceof StaticFieldValue)
								if(rv instanceof InstanceFieldValue ||
									rv instanceof StaticFieldValue ||
									rv instanceof ParamValue ||
									rv instanceof ThisValue)
								readFields.add(rv);
							}
						}
						for(int i=0; i<crv.getArgCount(); i++){
							args = crv.getArgs(i);
							if(args != null){
								for(RightValue rv : args)
									//if(rv instanceof InstanceFieldValue || rv instanceof StaticFieldValue)
									if(rv instanceof InstanceFieldValue ||
											rv instanceof StaticFieldValue ||
											rv instanceof ParamValue ||
											rv instanceof ThisValue)
									readFields.add(rv);
							}
						}
						funcalls.add(v);
						
						continue;
					}
					
					if(v instanceof InstanceFieldValue){
						readFields.add((InstanceFieldValue)v);
					}
					else if(v instanceof StaticFieldValue){
						readFields.add((StaticFieldValue)v);
					}
					else if(v instanceof CallRetValue){
						CallRetValue crv = (CallRetValue)v;
						Set<RightValue> args = crv.getThisArgs();
						if(args != null){
							for(RightValue rv : args){
								//if(rv instanceof InstanceFieldValue || rv instanceof StaticFieldValue)
								if(rv instanceof InstanceFieldValue ||
									rv instanceof StaticFieldValue ||
									rv instanceof ParamValue ||
									rv instanceof ThisValue)
								readFields.add(rv);
							}
						}
						for(int i=0; i<crv.getArgCount(); i++){
							args = crv.getArgs(i);
							if(args != null){
								for(RightValue rv : args)
									//if(rv instanceof InstanceFieldValue || rv instanceof StaticFieldValue)
									if(rv instanceof InstanceFieldValue ||
											rv instanceof StaticFieldValue ||
											rv instanceof ParamValue ||
											rv instanceof ThisValue)
									readFields.add(rv);
							}
						}
						funcalls.add(v);
					}
					else if(v instanceof ArrayDataValue){
						ArrayDataValue adv = (ArrayDataValue)v;
						RightValue base = adv.getBase();
						if(base instanceof InstanceFieldValue ||
								base instanceof StaticFieldValue ||
								base instanceof ParamValue ||
								base instanceof ThisValue)
							writeFields.add(base);
						for(RightValue rv : adv.getData()){
							if(rv instanceof InstanceFieldValue ||
									rv instanceof StaticFieldValue ||
									rv instanceof ParamValue ||
									rv instanceof ThisValue)
								readFields.add(rv);
						}
					}
					
				}
			}
		}
//		System.out.println("FUN: "+method);
//		for(RightValue rv : funcalls){
//			System.out.println("  C:"+rv);
//		}
	}
	
	private Set<RightValue> getInitalRelatedFields(  SootMethod m){
		Set<RightValue> relatedFields = new  HashSet<RightValue>();
		for(SootField f : m.getDeclaringClass().getFields()){
			RightValue ref = null;
			if(f.isStatic())
				ref = new StaticFieldValue(f.getDeclaringClass(), f);
			else{
				List<SootField> fields = new ArrayList<SootField>();
				fields.add(f);
				ref = new InstanceFieldValue(new ThisValue(f.getDeclaringClass()), fields);
			}
			relatedFields.add(ref);
		}
		//System.out.println("DEBUG getInitalRelatedFields gen "+relatedFields.size()+" fields");
		return relatedFields;
	}
	private DefAnalysisMap genInitialDefAnalysisMapFromFields(Set<RightValue> relatedFields){
		DefAnalysisMap dam = new DefAnalysisMap();
		for(RightValue rv : relatedFields){
			dam.setNewValue(rv, rv);
		}
		return dam;
	}

	@Override
	protected DefAnalysisMap newInitialFlow() {
		//System.out.println("NewInitialFlow is called: "+this.method.getSignature());
		return (DefAnalysisMap)initialValue.clone();
		//return new DefAnalysisMap();
	}
	@Override
	protected DefAnalysisMap entryInitialFlow() {
		//System.out.println("EntryInitialFlow is called: "+this.method.getSignature());
		return (DefAnalysisMap)initialValue.clone();
	}

	@Override
	protected void flowThrough(DefAnalysisMap in, Unit d,
			DefAnalysisMap out) {
		//copy in to out.
		copy(in, out);
		assert(in.equals(out));
		
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
				System.err.println("error: IdentityStmt is neither this or param "+is);
			}
		}
		else if(d instanceof AssignStmt){
			AssignStmt as = (AssignStmt)d;
			//System.out.println("AssignStmt: "+as);
			
			boolean removeLeftOps = true;
			Value rightOp = as.getRightOp();
			Value leftOp = as.getLeftOp();
			List<RightValue> terminalOps = null;
			List<ArrayDataValue> arrDataOps = null;
			if(leftOp instanceof InstanceFieldRef)
				terminalOps = AnalysisUtility.fromInstanceFieldRef2InstanceFieldValue((InstanceFieldRef)leftOp, in);
			else if(leftOp instanceof ArrayRef){
				arrDataOps = AnalysisUtility.findLeftOpsForArrayRef((ArrayRef)leftOp, in);
				//TODO: ideally, we need to differentiate numeric index and variable index
				// for numeric index, we do removeLeftOps, while for numeric index, we don't.
				//TODO: currently, we will consider this.array as read even if it is just used as
				// write. Need to think about how to deal with it.
				//removeLeftOps = false;
			}
			else if(leftOp instanceof Ref){
				if(!(leftOp instanceof StaticFieldRef))
					System.out.println("  ALERT: leftOp unknown FieldRef: "+leftOp.getClass());
			}
			if(terminalOps==null )
				terminalOps = new ArrayList<RightValue>();
			
			out.remove(leftOp); //kill existing one.
			for(RightValue rv : terminalOps)
				out.remove(rv);
			
			//set newRelatedFileds
			//This is used to indicate whether new fields are discovered.
			if(leftOp instanceof InstanceFieldValue ){
				InstanceFieldValue ifv = (InstanceFieldValue)leftOp;
				if(ifv.isThisReference() )
					newRelatedFields.add(ifv);
				if(ifv.isParamReference())
					newRelatedFields.add(ifv);
			}
			if(leftOp instanceof StaticFieldValue){
				StaticFieldValue sfv = (StaticFieldValue)leftOp;
				newRelatedFields.add(sfv);
			}
			for(RightValue rv : terminalOps){
				if(rv instanceof InstanceFieldValue ){
					InstanceFieldValue ifv = (InstanceFieldValue)rv;
					if(ifv.isThisReference())
						newRelatedFields.add(ifv);
					if(ifv.isParamReference())
						newRelatedFields.add(ifv);
				}
				if(rv instanceof StaticFieldValue){
					StaticFieldValue sfv = (StaticFieldValue)rv;
					newRelatedFields.add(sfv);
				}
			}
			
			RightValue newRightValue = null;
			//it is guaranteed that the IN set contains the top value.
			//For example, if it contains a mapping of this.field1 -> this.field1
			//this means that this.field1's value is from outside of the method at 
			//this unit.
			if(as.containsInvokeExpr()){
				InvokeExpr ie = (InvokeExpr)as.getRightOp();
				Set<RightValue> thisArg = null;
				if(ie instanceof InstanceInvokeExpr){
					thisArg = new HashSet<RightValue>();
					InstanceInvokeExpr iie = (InstanceInvokeExpr)ie;
					Set<RightValue> vs = AnalysisUtility.resolveRightValue(iie.getBase(), in, "InstanceInvokeExpr");
					for(RightValue v : vs)
						thisArg.add(v);
				}
				else if(ie instanceof DynamicInvokeExpr){
					System.out.println("DYNAMICINVOKEEXPR ");
				}
			
				//System.out.println("  DEBUG: InvokeExpr "+as.getRightOp().getType());

				CallRetValue crv = new CallRetValue(ie.getMethod());
				crv.addThisArgSet(thisArg);
				newRightValue = crv;
				for(int i=0; i<ie.getArgCount(); i++){
					Value arg = ie.getArg(i);
					Set<RightValue> vs = AnalysisUtility.resolveRightValue(arg, in, "InvokeExprArgs");
					crv.addArgSet(i, vs);
				}
				
				out.setNewValue(leftOp, newRightValue);
				for(RightValue rv : terminalOps)
					out.setNewValue(rv, newRightValue);
				//ARRAY
				if(arrDataOps!=null){
					for(ArrayDataValue adv : arrDataOps){
						adv.addData(newRightValue);
						out.addNewArrayDataValue(adv);
					}
				}
				//System.out.println("    DEBUG: InvokeExpr RS:"+newRightValue);
			}
			else if(rightOp instanceof AnyNewExpr){
				//System.out.println("  AnyNewExpr.");
				newRightValue = new NewValue(as);
				out.setNewValue(leftOp, newRightValue);
				for(RightValue rv : terminalOps)
					out.setNewValue(rv, newRightValue);
				//ARRAY
				if(arrDataOps!=null){
					for(ArrayDataValue adv : arrDataOps){
						adv.addData(newRightValue);
						out.addNewArrayDataValue(adv);
					}
				}
			}
			else if(rightOp instanceof BinopExpr){
				//System.out.println("  DEBUG: BinopExpr");
				BinopExpr be = (BinopExpr)rightOp;
				Value arg1 = be.getOp1();
				Set<RightValue> values1 = AnalysisUtility.resolveRightValue(arg1, in, "BinopExpr");
				Value arg2 = be.getOp2();
				Set<RightValue> values2 = AnalysisUtility.resolveRightValue(arg2, in, "BinopExpr");
				for(RightValue rv : values2){
					//System.out.println("  DEBUG: BinopExpr RS:"+rv);
					values1.add(rv);
				}
				for(RightValue rv : values1){
					//System.out.println("  DEBUG: BinopExpr TO:" +leftOp+ " add RV:"+rv+" ");
					out.addNewValue(leftOp, rv);
					for(RightValue v2 : terminalOps)
						out.addNewValue(v2, rv);
					//ARRAY
					if(arrDataOps!=null){
						for(ArrayDataValue adv : arrDataOps){
							adv.addData(rv);
							out.addNewArrayDataValue(adv);
						}
					}
				}
				//System.out.println("  DEBUG: BinopExpr RS:"+out.toString());
			}
			else if(rightOp instanceof InstanceOfExpr){
				System.out.println("  DEBUG: InstanceOfExpr do nothing.");
			}
			else if(rightOp instanceof NewArrayExpr){
				//System.out.println("  DEBUG: NewArrayExpr");
				newRightValue = new NewArrayValue(as);
				out.setNewValue(leftOp, newRightValue);
				for(RightValue rv : terminalOps)
					out.setNewValue(rv, newRightValue);
				//ARRAY
				if(arrDataOps!=null){
					for(ArrayDataValue adv : arrDataOps){
						adv.addData(newRightValue);
						out.addNewArrayDataValue(adv);
					}
				}
			}
			else if(rightOp instanceof NewExpr){
				//System.out.println("  DEBUG: NewExpr.");
				newRightValue = new NewValue(as);
				out.setNewValue(leftOp, newRightValue);
				for(RightValue rv : terminalOps)
					out.setNewValue(rv, newRightValue);
				//ARRAY
				if(arrDataOps!=null){
					for(ArrayDataValue adv : arrDataOps){
						adv.addData(newRightValue);
						out.addNewArrayDataValue(adv);
					}
				}
			}
			else if(rightOp instanceof NewMultiArrayExpr){
				//System.out.println("  DEBUG: NewMultiArrayExpr.");
				newRightValue = new NewValue(as);
				out.setNewValue(leftOp, newRightValue);
				for(RightValue rv : terminalOps)
					out.setNewValue(rv, newRightValue);
				//ARRAY
				if(arrDataOps!=null){
					for(ArrayDataValue adv : arrDataOps){
						adv.addData(newRightValue);
						out.addNewArrayDataValue(adv);
					}
				}
			}
			else if(rightOp instanceof ShimpleExpr){
				System.out.println("  DEBUG: ShimpleExpr do nothing.");
			}
			else if(rightOp instanceof UnopExpr){
				//System.out.println("  DEBUG: UnopExpr");
				UnopExpr ue = (UnopExpr)rightOp;
				Value arg = ue.getOp();
				Set<RightValue> values = AnalysisUtility.resolveRightValue(arg, in, "UnopExpr");
				
				for(RightValue rv : values){
					out.addNewValue(leftOp, rv);
					for(RightValue v2 : terminalOps)
						out.addNewValue(v2, rv);
					//ARRAY
					if(arrDataOps!=null){
						for(ArrayDataValue adv : arrDataOps){
							adv.addData(rv);
							out.addNewArrayDataValue(adv);
						}
					}
				}
			}
			else if(rightOp instanceof CastExpr){
				//System.out.println("  DEBUG: CastExpr");
				CastExpr ce = (CastExpr)rightOp;
				Value arg = ce.getOp();
				Set<RightValue> values = AnalysisUtility.resolveRightValue(arg, in, "CastExpr");
				
				for(RightValue rv : values){
					out.addNewValue(leftOp, rv);
					for(RightValue v2 : terminalOps)
						out.addNewValue(v2, rv);
					
					//ARRAY
					if(arrDataOps!=null){
						for(ArrayDataValue adv : arrDataOps){
							adv.addData(rv);
							out.addNewArrayDataValue(adv);
						}
					}
				}
			}
			else if(rightOp instanceof Constant){
				//System.out.println("  DEBUG: Constant");
				newRightValue = new ConstantValue(rightOp);
				out.setNewValue(leftOp, newRightValue);
				for(RightValue rv : terminalOps)
					out.setNewValue(rv, newRightValue);
				//ARRAY
				if(arrDataOps!=null){
					for(ArrayDataValue adv : arrDataOps){
						adv.addData(newRightValue);
						out.addNewArrayDataValue(adv);
					}
				}
			}
			else if((rightOp instanceof Local)           ||
					(rightOp instanceof ArrayRef)        ||
					(rightOp instanceof InstanceFieldRef)||
					(rightOp instanceof StaticFieldRef)){
				//System.out.println("  DEBUG: "+rightOp.getClass());
				Set<RightValue> values = AnalysisUtility.resolveRightValue(rightOp, in, rightOp.getClass().toString());
				for(RightValue rv : values){
					//System.out.println("  DEBUG: "+rightOp.getClass()+" RS:"+rv);
					out.addNewValue(leftOp, rv);
					for(RightValue v2 : terminalOps)
						out.addNewValue(v2, rv);
					//ARRAY
					if(arrDataOps!=null){
						for(ArrayDataValue adv : arrDataOps){
							adv.addData(rv);
							out.addNewArrayDataValue(adv);
						}
					}
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
			/*
			if(is.getInvokeExpr().getMethod().isConstructor()){
				//TODO: what if a.b = new CLS(arg);
				//TODO: what if $a.init, but $a is an arrayRef
				System.out.println("InvokeStmt constructor: "+is);
				if(is.getInvokeExpr() instanceof InstanceInvokeExpr){
					InstanceInvokeExpr iie = (InstanceInvokeExpr)is.getInvokeExpr();
					Set<RightValue> values = out.get(iie.getBase());
					if(values == null){
						System.err.println("error: cannot find constructor's base.");
					}
					else{
						boolean replace = false;
						Set<RightValue> thisArg = new HashSet<RightValue>();
						CallRetValue crv = new CallRetValue(iie.getMethod());
						crv.addThisArgSet(thisArg);
						List<NewValue> deleted = new ArrayList(2);
						for(RightValue v : values){
							thisArg.add((RightValue)v.clone());
							if (v instanceof NewValue){
								replace = true;
								deleted.add((NewValue)v);
							}
						}
						
						for(int i=0; i<iie.getArgCount(); i++){
							Value arg = iie.getArg(i);
							Set<RightValue> vs = resolveRightValue(arg, in, "ConstructorInvokeExprArgs");
							crv.addArgSet(i, vs);
						}
						
						Value leftOp = iie.getBase();
						out.addNewValue(leftOp, crv);
						if(replace){
							Iterator<RightValue> it = out.get(leftOp).iterator();
							while(it.hasNext()){
								RightValue rv = it.next();
								if(deleted.contains(rv))
									it.remove();
								else{
									System.out.println("NOTMATCH"+rv+" "+deleted.iterator().next());
								}
							}
						}
						
					}
				}
				
			}*/
			//else{
			InvokeExpr ie = is.getInvokeExpr();
			if(ie instanceof InstanceInvokeExpr){
				InstanceInvokeExpr iie = (InstanceInvokeExpr)ie;
				out.remove(ie);
				Set<RightValue> thisArg = new HashSet<RightValue>();
				Set<RightValue> values = AnalysisUtility.resolveRightValue(iie.getBase(), in, "Invoke Base");
				if(values != null){
					for(RightValue v : values){
						thisArg.add((RightValue)v.clone());
					}
				}
				CallRetValue crv = new CallRetValue(ie.getMethod());
				crv.addThisArgSet(thisArg);
				
				for(int i=0; i<iie.getArgCount(); i++){
					Value arg = iie.getArg(i);
					Set<RightValue> vs = AnalysisUtility.resolveRightValue(arg, in, "RegularInstanceInvoke");
					crv.addArgSet(i, vs);
				}
				out.addNewValue(ie, crv);
			}
			else if(ie instanceof StaticInvokeExpr){
				StaticInvokeExpr sie = (StaticInvokeExpr)ie;
				out.remove(ie);
				CallRetValue crv = new CallRetValue(ie.getMethod());
				for(int i=0; i<sie.getArgCount(); i++){
					Value arg = sie.getArg(i);
					Set<RightValue> vs = AnalysisUtility.resolveRightValue(arg, in, "RegularStaticInvoke");
					crv.addArgSet(i, vs);
				}
				out.addNewValue(ie, crv);
			}
			//}
		}	
	}

	@Override
	protected void merge(DefAnalysisMap in1, DefAnalysisMap in2,
			DefAnalysisMap out) {
		out.clear();
		
		for(Value k : in1.keyValueSet()){
			for(RightValue v : in1.get(k)){
				if(k instanceof ArrayDataValue)
					out.addNewArrayDataValue((ArrayDataValue)v);
				else
					out.addNewValue(k, v);
			}
			/*if(!in2.keyValueSet().contains(k)){
				if(k instanceof ArrayRef){
					List<RightValue> tmp = fromArrayRef2RightValue((ArrayRef)k, in2);
					if(tmp == null)
						out.addNewValue(k, new UndefinedValue(k));
					else{
						for(RightValue rv : tmp){
							out.addNewValue(k, new UndefinedValue(rv));
						}
					}
				}
				else if(k instanceof InstanceFieldRef){
					List<RightValue> tmp = fromInstanceFieldRef2InstanceFieldValue((InstanceFieldRef)k, in2);
					if(tmp == null)
						out.addNewValue(k, new UndefinedValue(k));
					else{
						for(RightValue rv : tmp){
							out.addNewValue(k, new UndefinedValue(rv));
						}
					}
				}
				else if(k instanceof StaticFieldRef){
					StaticFieldRef tmp = (StaticFieldRef)k;
					StaticFieldValue sfr = new StaticFieldValue(tmp.getField().getDeclaringClass(), tmp.getField());
					out.addNewValue(k, new UndefinedValue(sfr));
				}
				else{
					out.addNewValue(k, new UndefinedValue(k));
				}
			}*/
		}
		for(Value k : in2.keyValueSet()){
			for(RightValue v : in2.get(k)){
				if(k instanceof ArrayDataValue)
					out.addNewArrayDataValue((ArrayDataValue)v);
				else
					out.addNewValue(k, v);
			}
			
			
			/*if(!in1.keyValueSet().contains(k)){
				if(k instanceof ArrayRef){
					List<RightValue> tmp = fromArrayRef2RightValue((ArrayRef)k, in1);
					if(tmp == null)
						out.addNewValue(k, new UndefinedValue(k));
					else{
						for(RightValue rv : tmp){
							out.addNewValue(k, new UndefinedValue(rv));
						}
					}
				}
				else if(k instanceof InstanceFieldRef){
					List<RightValue> tmp = fromInstanceFieldRef2InstanceFieldValue((InstanceFieldRef)k, in1);
					if(tmp == null)
						out.addNewValue(k, new UndefinedValue(k));
					else{
						for(RightValue rv : tmp){
							out.addNewValue(k, new UndefinedValue(rv));
						}
					}
				}
				else if(k instanceof StaticFieldRef){
					StaticFieldRef tmp = (StaticFieldRef)k;
					StaticFieldValue sfr = new StaticFieldValue(tmp.getField().getDeclaringClass(), tmp.getField());
					out.addNewValue(k, new UndefinedValue(sfr));
				}
				else{
					out.addNewValue(k, new UndefinedValue(k));
				}
			}*/
		}
	}
	
	@Override
	protected void copy(DefAnalysisMap source, DefAnalysisMap dest) {
		if(dest==null){
			System.err.println("error: copy dest is null");
		}
		dest.clear();
		
		for(Value k : source.getId2Value().values()){
			//System.err.println("COPY: VALUE: "+k+" C:"+source.containsKey(k));
			dest.addNewValueSet(k, source.get(k));
		}
	}
	
	
	
	
	
	

}
