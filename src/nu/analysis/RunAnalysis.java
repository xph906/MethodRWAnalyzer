package nu.analysis;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

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
		
		//String mainJarFile = "/Users/a/Projects/AndroidDataFlow/apps/TestJava.jar";
		//String rtJarLocation = "/Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk//Contents/Home/jre/lib/rt.jar";
		//String jceJarLocation = "/Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk//Contents/Home/jre/lib/jce.jar";
		
		String mainJarFile = "/home/xpan/Projects/AndroidDataflow/apps/TestJava.jar";
		String rtJarLocation = "/home/xpan/Libs/java/jdk1.8.0_101/jre/lib/rt.jar";
		String jceJarLocation = "/home/xpan/Libs/java/jdk1.8.0_101/jre/lib/jce.jar";
		
		classPath = rtJarLocation+ ":"+jceJarLocation+":"+mainJarFile+":"+System.getProperty("java.class.path") ;
		System.out.println(classPath);
		String[] sootArgs = {"nu.dataflow.MainClass",
				"-cp", classPath,  
				"-app", "-w","-ire","-no-bodies-for-excluded",
				"-main-class", "nu.dataflow.MainClass",
				"-f", "none" 
		};
		
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
		//Scene.v().loadClassAndSupport("TestJava");
		/*---------------------------------------------------------*/
		
		//IntraProcedureAnalysis cgt = new IntraProcedureAnalysis();
		//PackManager.v().getPack("jtp").add(new Transform("jtp.nt", cgt));
		/*Main m = Main.v();
		m.processCmdLine(sootArgs);
		m.autoSetOptions();
		Scene.v().loadNecessaryClasses();
		
		
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod method = rdr.next().method();
			System.out.println("Method: "+method.getName()+" "+method.getDeclaringClass().getName()+" "+method.hasActiveBody());
			if(!method.hasActiveBody() &&method.getDeclaringClass().getName().startsWith("nu.analysis") ){
				
			}
			else if(!method.hasActiveBody())
				continue;
			
			if(!method.getDeclaringClass().getName().startsWith("nu.analysis"))
				continue;
			try{
				System.out.println("  loaded method: "+method+ method.getActiveBody());
				UnitGraph g = new ExceptionalUnitGraph(method.getActiveBody());
				//IntraProcedureAnalysis cgt = new IntraProcedureAnalysis(g);
			}
			catch(Exception e){
					System.err.println(e);
			}
		} */
		

	}

}
