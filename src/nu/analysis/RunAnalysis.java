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
import soot.G;
import soot.Main;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.queue.QueueReader;

public class RunAnalysis {

	public static void main(String[] args) {
		String classPath = ".";		
		String mainClass = null;
		
		/* ------------------- OPTIONS ---------------------- */
		/*try {
			int i=0;
			while(true){
				if (args[i].equals("-cp")) {
					classPath = args[i+1];
					i += 2;
				} else {
					mainClass = args[i];
					i++;
					break;
				}NewInitialFlow
			}
			if (i != args.length || mainClass == null)
				throw new Exception();
		} catch (Exception e) {
			System.err.println("Usage: java vasco.soot.examples.CopyConstantTest [-cp CLASSPATH] MAIN_CLASS");
			System.exit(1);
		}*/
		
		String mainJarFile = "/Users/a/Projects/AndroidDataFlow/apps/TestJava.jar";
		String rtJarLocation = "/Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk//Contents/Home/jre/lib/rt.jar";
		String jceJarLocation = "/Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk//Contents/Home/jre/lib/jce.jar";
		
		//String mainJarFile = "/home/xpan/Projects/AndroidDataflow/apps/TestJava.jar";
		//String rtJarLocation = "/home/xpan/Libs/java/jdk1.8.0_101/jre/lib/rt.jar";
		//String jceJarLocation = "/home/xpan/Libs/java/jdk1.8.0_101/jre/lib/jce.jar";
		
		classPath = rtJarLocation+ ":"+jceJarLocation+":"+mainJarFile+":"+System.getProperty("java.class.path") ;
		System.out.println(classPath);
		String[] sootArgs = {"nu.dataflow.MainClass",
				"-cp", classPath,  
				"-app", "-w","-ire","-no-bodies-for-excluded",
				"-main-class", "nu.dataflow.MainClass",
				"-f", "none" 
		};
		
		Map<SootMethod, Set<RightValue>> methodReadParam = new HashMap<SootMethod, Set<RightValue>>();
		Map<SootMethod, Set<RightValue>> methodWriteParam = new HashMap<SootMethod, Set<RightValue>>();
		Map<SootMethod, IntraProcedureAnalysis> results = new HashMap<SootMethod, IntraProcedureAnalysis>();
		PackManager.v().getPack("jtp").add(
			    new Transform("jtp.myTransform", new BodyTransformer() {
			   

				@Override
				protected void internalTransform(Body b, String phaseName,
						Map<String, String> options) {
					if(b.getMethod().getDeclaringClass().getName().startsWith("nu.dataflow") ){
						try{
							PrintWriter out = new PrintWriter(b.getMethod().getName()+".txt");
							out.println("@@Start analyzing:"+b.getMethod().getName());
							UnitGraph g = new ExceptionalUnitGraph(b);
							IntraProcedureAnalysis analysis = new IntraProcedureAnalysis(g, b.getMethod());
							results.put(b.getMethod(), analysis);
							
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
				
			    }));
		
		
		
		
		Main.v().main(sootArgs);
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod method = rdr.next().method();
			if(method.getName().startsWith("nu.dataflow") ){
				System.out.println("METHOD: "+method.getActiveBody());
			}
		}
		
		//============
		System.out.println("start analysis");
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
				System.out.println("BEG R:"+readSize+" W:"+writeSize+" ||"+m);
				Set<RightValue> funcalls = analysis.getFuncalls();
				for(RightValue rv : funcalls){
					CallRetValue crv = (CallRetValue)rv;
					if(!results.containsKey(crv.getMethod())){
						System.out.println("  give up funcall: "+crv.getMethod());
						continue;
					}
					System.out.println("  process "+rv);
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
											System.out.println("  replace 1.");
										}
										else if(bb instanceof InstanceFieldValue){
											count++;
											InstanceFieldValue ibb = (InstanceFieldValue)bb.clone();
											ibb.getFields().addAll(ifv.getFields());
											readFields.add(ibb);
											System.out.println("  replace 2."+ibb+" "+ifv);
										}
										else if(bb instanceof StaticFieldValue){
											count++;
											InstanceFieldValue newIFV = (InstanceFieldValue)ifv.clone();
											newIFV.setBase((StaticFieldValue)bb);
											readFields.add((RightValue)newIFV);
											System.out.println("  replace 3."+bb);
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
											System.out.println("  replace 11.");
										}
										else if(bb instanceof InstanceFieldValue){
											count++;
											InstanceFieldValue ibb = (InstanceFieldValue)bb.clone();
											ibb.getFields().addAll(ifv.getFields());
											readFields.add(ibb);
											System.out.println("  replace 22."+ibb+" "+ifv);
										}
										else if(bb instanceof StaticFieldValue){
											count++;
											InstanceFieldValue newIFV = (InstanceFieldValue)ifv.clone();
											newIFV.setBase((StaticFieldValue)bb);
											readFields.add((RightValue)newIFV);
											System.out.println("  replace 33."+bb);
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
											System.out.println("  replace w1.");
										}
										else if(bb instanceof InstanceFieldValue){
											count++;
											InstanceFieldValue ibb = (InstanceFieldValue)bb.clone();
											ibb.getFields().addAll(ifv.getFields());
											writeFields.add(ibb);
											System.out.println("  replace w2."+ibb+" "+ifv);
										}
										else if(bb instanceof StaticFieldValue){
											count++;
											InstanceFieldValue newIFV = (InstanceFieldValue)ifv.clone();
											newIFV.setBase((StaticFieldValue)bb);
											writeFields.add((RightValue)newIFV);
											System.out.println("  replace 23."+bb);
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
											ibb.getFields().addAll(ifv.getFields());
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

}
