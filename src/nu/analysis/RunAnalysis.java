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
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.queue.QueueReader;

public class RunAnalysis {

	public static void main(String[] args) {
		
		//Android App
		/*
		String apkLocation = "/Users/a/Projects/FlowDroid/sample.apk";
		String platformLocation = "/Users/a/Projects/android/android-sdk-macosx/platforms/";
		MethodRWAnalyzer analyzer = new MethodRWAnalyzer();
		Map<SootMethod, IntraProcedureAnalysis> results = analyzer.startAnalysis(apkLocation, platformLocation);
		*/
		MethodRWAnalyzer analyzer = new MethodRWAnalyzer();
		Map<SootMethod, IntraProcedureAnalysis> results = analyzer.startAnalysisJarFile();
		
		/*
		String mainJarFile = "/Users/a/Projects/AndroidDataFlow/apps/TestJava.jar";
		String rtJarLocation = "/Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk//Contents/Home/jre/lib/rt.jar";
		String jceJarLocation = "/Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk//Contents/Home/jre/lib/jce.jar";
		
		//String mainJarFile = "/home/xpan/Projects/AndroidDataflow/apps/TestJava.jar";
		//String rtJarLocation = "/home/xpan/Libs/java/jdk1.8.0_101/jre/lib/rt.jar";
		//String jceJarLocation = "/home/xpan/Libs/java/jdk1.8.0_101/jre/lib/jce.jar";
		String classPath = rtJarLocation+ ":"+jceJarLocation+":"+System.getProperty("java.class.path")+":.:"+mainJarFile;
		System.out.println(classPath);
		String[] sootArgs = {"nu.dataflow.MainClass",
				"-cp", classPath,  
				"-app", "-w","-ire","-no-bodies-for-excluded",
				"-main-class", "nu.dataflow.MainClass",
				"-f", "none" 
		};
		
		Map<SootMethod, IntraProcedureAnalysis> results = new HashMap<SootMethod, IntraProcedureAnalysis>();
		PackManager.v().getPack("jtp").add(
			    new Transform("jtp.myTransform", new BodyTransformer() {
			   

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
						PrintWriter out = new PrintWriter("./tmp/"+b.getMethod().getName()+"-"+b.getMethod().getDeclaringClass().getName()+".txt");
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
				
				
			    }));
		Options.v().set_src_prec(Options.src_prec_apk);
		Main.v().main(sootArgs);
		
		//============
		
		
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
		*/
		

	}

}
