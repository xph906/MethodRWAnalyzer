package nu.analysis;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import nu.analysis.values.AtomRightValue;
import nu.analysis.values.CallRetValue;
import nu.analysis.values.InstanceFieldValue;
import nu.analysis.values.ParamValue;
import nu.analysis.values.RightValue;
import nu.analysis.values.StaticFieldValue;
import nu.analysis.values.ThisValue;
import soot.Body;
import soot.BodyTransformer;
import soot.Main;
import soot.PackManager;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class MethodRWAnalyzer {
	Map<SootMethod, IntraProcedureAnalysis> results;
	
	public MethodRWAnalyzer(){
		results = new HashMap<SootMethod, IntraProcedureAnalysis>();
	}
	
	public Map<SootMethod, IntraProcedureAnalysis> startAnalysis(String apkLocation, String platformLocation){
		
		String rtJarLocation = "/Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk//Contents/Home/jre/lib/rt.jar";
		String jceJarLocation = "/Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk//Contents/Home/jre/lib/jce.jar";
		//platformLocation = "/Users/a/Projects/android/android-sdk-macosx/platforms/";
		//apkLocation = "/Users/a/Projects/FlowDroid/sample.apk";
		String classPath = rtJarLocation+ ":"+jceJarLocation+":"+System.getProperty("java.class.path");
		
		String[] sootArgs = {
				"-android-jars", platformLocation,
				"-process-dir", apkLocation,
				"-cp", classPath,
				"-app", "-w","-ire","-no-bodies-for-excluded","-f", "none" 
		};
		
		PackManager.v().getPack("jtp").add(
			    new Transform("jtp.myTransform", new RWAnalyzerBodyTransformer()));
		Options.v().set_src_prec(Options.src_prec_apk);
		Main.v().main(sootArgs);
		postInterprocedureAnalyze();
		displayResults();
		return results;
	}
	
	public Map<SootMethod, IntraProcedureAnalysis> startAnalysisInFlowDroid(){

		PackManager.v().getPack("jtp").add(
			    new Transform("jtp.myTransform", new RWAnalyzerBodyTransformer()));
		PackManager.v().runPacks();
		postInterprocedureAnalyze();
		displayResults();
		return results;
	}
	
	public void displayResults(){
		System.out.println("RESULTS: ");
		for(SootMethod m : results.keySet()){
			IntraProcedureAnalysis analysis = results.get(m);
			Set<RightValue> reads = analysis.getReadFields();
			Set<RightValue> writes = analysis.getWriteFields();
			System.out.println("RS: "+m);
			for(RightValue rv: reads){
				System.out.println("  R:"+rv);
			}
			for(RightValue rv: writes){
				System.out.println("  W:"+rv);
			}
		}
	}
	
	private void postInterprocedureAnalyze(){
		System.out.println("PHASE: start post inter-procedure analysis");
		boolean changed = false;
		int iterCount = 0;
		do{
			changed = false;
			iterCount++;
			for(SootMethod m : results.keySet()){
				IntraProcedureAnalysis analysis = results.get(m);
				Set<RightValue> readFields = analysis.getReadFields();
				Set<RightValue> writeFields = analysis.getWriteFields();
				int readSize = readFields.size();
				int writeSize = writeFields.size();
				//System.out.println("BEG R:"+readSize+" W:"+writeSize+" ||"+m);
				Set<RightValue> funcalls = analysis.getFuncalls();
				for(RightValue rv : funcalls){
					CallRetValue crv = (CallRetValue)rv;
					if(!results.containsKey(crv.getMethod())){
						//System.out.println("  give up funcall: "+crv.getMethod());
						continue;
					}
					//System.out.println("  process "+rv);
					Set<RightValue> r = results.get(crv.getMethod()).getReadFields();
					Set<RightValue> w = results.get(crv.getMethod()).getWriteFields();
					
					int count = 0;
					for(RightValue rrv : r){
						if(rrv instanceof StaticFieldValue){
							count++;
							readFields.add((StaticFieldValue)rrv.clone());
						}
						else if(rrv instanceof InstanceFieldValue){
							InstanceFieldValue ifv = (InstanceFieldValue)rrv;
							AtomRightValue base = ifv.getBase();
							if(base instanceof ThisValue){
								Set<RightValue> b = crv.getThisArgs();
								if(b != null){
									for(RightValue bb : b){
										if(bb instanceof ThisValue){
											count++;
											readFields.add((RightValue)ifv.clone());
											//System.out.println("  replace 1.");
										}
										else if(bb instanceof InstanceFieldValue){
											count++;
											InstanceFieldValue ibb = (InstanceFieldValue)bb.clone();
											ibb.addFields(ifv.getCloneFields());
											readFields.add(ibb);
											//System.out.println("  replace 2."+ibb+" "+ifv);
										}
										else if(bb instanceof StaticFieldValue){
											count++;
											InstanceFieldValue newIFV = (InstanceFieldValue)ifv.clone();
											newIFV.setBase((StaticFieldValue)bb);
											readFields.add((RightValue)newIFV);
											//System.out.println("  replace 3."+bb);
										}
										else{
											System.out.println("  ignore base:"+bb);
										}
									}
								}
							}
							else if(base instanceof ParamValue){
								ParamValue param = (ParamValue)base;
								Set<RightValue> b = crv.getArgs(param.getIndex());
								if(b != null){
									for(RightValue bb : b){
										if(bb instanceof ThisValue){
											count++;
											InstanceFieldValue newVal = (InstanceFieldValue)ifv.clone();
											newVal.setBase((ThisValue)bb);
											readFields.add(newVal);
											//System.out.println("  replace 11.");
										}
										else if(bb instanceof InstanceFieldValue){
											count++;
											InstanceFieldValue ibb = (InstanceFieldValue)bb.clone();
											//ibb.getFields().addAll(ifv.getFields());
											ibb.addFields(ifv.getCloneFields());
											readFields.add(ibb);
											//System.out.println("  replace 22."+ibb+" "+ifv);
										}
										else if(bb instanceof StaticFieldValue){
											count++;
											InstanceFieldValue newIFV = (InstanceFieldValue)ifv.clone();
											newIFV.setBase((StaticFieldValue)bb);
											readFields.add((RightValue)newIFV);
											//System.out.println("  replace 33."+bb);
										}
										else{
											System.out.println("  ignore base:"+bb);
										}
									}
								}
							}
							else if(base instanceof StaticFieldValue){
								count++;
								readFields.add((InstanceFieldValue)rrv.clone());
							}
						}//rrv instanceof InstanceFieldValue
					}//read
					
					for(RightValue rrv : w){
						if(rrv instanceof StaticFieldValue){
							count++;
							writeFields.add((StaticFieldValue)rrv.clone());
						}
						else if(rrv instanceof InstanceFieldValue){
							InstanceFieldValue ifv = (InstanceFieldValue)rrv;
							AtomRightValue base = ifv.getBase();
							if(base instanceof ThisValue){
								Set<RightValue> b = crv.getThisArgs();
								if(b != null){
									for(RightValue bb : b){
										if(bb instanceof ThisValue){
											count++;
											writeFields.add((RightValue)ifv.clone());
											//System.out.println("  replace w1.");
										}
										else if(bb instanceof InstanceFieldValue){
											count++;
											InstanceFieldValue ibb = (InstanceFieldValue)bb.clone();
											//ibb.getFields().addAll(ifv.getFields());
											ibb.addFields(ifv.getCloneFields());
											
											writeFields.add(ibb);
											//System.out.println("  replace w2."+ibb+" "+ifv);
										}
										else if(bb instanceof StaticFieldValue){
											count++;
											InstanceFieldValue newIFV = (InstanceFieldValue)ifv.clone();
											newIFV.setBase((StaticFieldValue)bb);
											writeFields.add((RightValue)newIFV);
											//System.out.println("  replace 23."+bb);
										}
										else{
											System.out.println("  ignore 2base:"+bb);
										}
									}
								}
							}
							else if(base instanceof ParamValue){
								ParamValue param = (ParamValue)base;
								Set<RightValue> b = crv.getArgs(param.getIndex());
								if(b != null){
									for(RightValue bb : b){
										if(bb instanceof ThisValue){
											count++;
											InstanceFieldValue newVal = (InstanceFieldValue)ifv.clone();
											newVal.setBase((ThisValue)bb);
											writeFields.add(newVal);
											System.out.println("  replace 211.");
										}
										else if(bb instanceof InstanceFieldValue){
											count++;
											InstanceFieldValue ibb = (InstanceFieldValue)bb.clone();
											//ibb.getFields().addAll(ifv.getFields());
											ibb.addFields(ifv.getCloneFields());
											writeFields.add(ibb);
											System.out.println("  replace 222."+ibb+" "+ifv);
										}
										else if(bb instanceof StaticFieldValue){
											count++;
											InstanceFieldValue newIFV = (InstanceFieldValue)ifv.clone();
											newIFV.setBase((StaticFieldValue)bb);
											writeFields.add((RightValue)newIFV);
											System.out.println("  replace 233."+bb);
										}
										else{
											System.out.println("  ignore base:"+bb);
										}
									}
								}
							}
							else if(base instanceof StaticFieldValue){
								count++;
								readFields.add((InstanceFieldValue)rrv.clone());
							}
						}//rrv instanceof InstanceFieldValue
					}//write
					
				}//funcalls
				if(readFields.size() > readSize || writeFields.size()>writeSize){
					changed = true;
				}
				System.out.println("END R:"+readFields.size()+" W:"+writeFields.size()+" ||"+m);
			}
		}while(changed && iterCount<10);
	}
	
	class RWAnalyzerBodyTransformer extends BodyTransformer{
		@Override
		protected void internalTransform(Body b, String phaseName,
				Map<String, String> options) {
			if(b.getMethod().getDeclaringClass().getName().startsWith("android"))
				return;
			if(b.getMethod().getDeclaringClass().getName().startsWith("jdk"))
				return;
			if(b.getMethod().getDeclaringClass().getName().startsWith("org"))
				return;
			
			System.out.println("TEST:"+b.getMethod().getDeclaringClass().getName()+":"+b.getMethod().getName());
			
			try{
				PrintWriter out = new PrintWriter(b.getMethod().getName()+".txt");
				out.println("@@Start analyzing:"+b.getMethod().getName());
				
				UnitGraph g = new ExceptionalUnitGraph(b);
				IntraProcedureAnalysis analysis = new IntraProcedureAnalysis(g, b.getMethod());
				results.put(b.getMethod(), analysis);
				
				//Log info
				Set<RightValue> readFields = analysis.getReadFields();
				Set<RightValue> writeFields = analysis.getWriteFields();
				for(RightValue rf : readFields)
					out.println("READ: "+rf);
				for(RightValue wf : writeFields)
					out.println("WRITE: "+wf);
				
				out.println("  --Done analyzing:"+b.getMethod().getName()+" "+b);
				out.println("  --Method Body:"+b);
				out.println("  --Start displaying:"+b.getMethod().getName()+" ");
				Iterator<Unit> it = g.iterator();
				int count = 0;
				while(it.hasNext()){
					Unit u = it.next();
					out.println("  UNIT:"+count+" "+u);
					out.println("      [BEFOR]RS:"+count+"\n"+analysis.getFlowBefore(u).toString());
					out.println("      [AFTER]RS:"+count+"\n"+analysis.getFlowAfter(u).toString());
					count++;
				}
				out.println("@@End "+b.getMethod().getName()+"======================");
				out.close();
				}
			catch(Exception e){
				System.err.println(e+" "+b.getMethod());
				e.printStackTrace(System.err);
			}
			
		}
	}
}
