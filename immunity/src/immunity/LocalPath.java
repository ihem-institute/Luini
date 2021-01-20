
	//CAMBIO
	package immunity;

	import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

	public class LocalPath {
		
	private String mypath;
	private String mypathOut;
	private String mypath1;
	private String mypath2;
	private String mypath3;
	private String mypath4;
	private String mypath5;
	private String mypath6;
		
		File myDir = new File (".");
		{
	    try {
	      
	      mypath=myDir.getCanonicalPath().replace('\\','/');
//	      FOR BATCH, THE PATH MUST BE ABSOLUTE BECAUSE THE BATCH RUNS FROM A
//	      TEMPORARY FOLDER THAT IS DELETED. SO IF RELATIVE, THE OUTPUT IS LOST
//	      SAME FOR INPUT, THE FILE MUST BE IN THE "data" FOLDER
//	      to get the results from the batch in different folders, the directory must be created
//	      Cannot stores de files in a non existing directory
			if (RunEnvironment.getInstance().isBatch()) {
			      String folderName = new SimpleDateFormat("yyyy-MM-dd-HH-mmss").format(new Date());
					Parameters parm = RunEnvironment.getInstance().getParameters();
					double p_EndosomeSplitStep =(double) parm.getValue("p_EndosomeSplitStep");
					int repeat = (int) parm.getValue("repeat");
			      folderName = String.valueOf(p_EndosomeSplitStep) + "-" + String.valueOf(repeat);
			      mypathOut="C:/Users/lmayo/workspace-golgi/output/"+folderName+"/";
			      Path path = Paths.get(mypathOut);
			      Files.createDirectory(path);
			}
			else 
				mypathOut="C:/Users/lmayo/workspace-golgi/output/";
	      
	      }
	    catch(Exception e) {
	      e.printStackTrace();
	      }

		}
		
		public String getPath(){ 
			return this.mypath; 
			} 
		
		public String getPathOutExcel(){ 
			return this.mypathOut;
			} 
		public String getPathResultsIT(){ 
			
			mypath1=mypathOut+"/ResultsIntrTransp3.csv";
			return this.mypath1; 
			} 

		public String getPathResultsMarkers(){ 
			
			mypath2=mypathOut+"/ResultsMarker.csv";
			return this.mypath2; 
			} 

		public String getPathInputIT(){ 
//	Not used for batch		
			mypath3=mypath+"/inputIntrTransp3.csv";
			return this.mypath3; 
			} 
		
		public String getPathOutputFE(){ 
			
			mypath4=mypathOut+"/outputFrozenEndosomes.csv";
			return this.mypath4; 
			} 
			
		public String getPathTotalRabs(){ 
			
			mypath5=mypathOut+"/totalRabs.csv";
			return this.mypath5; 
			}

		public String getPathCisternsArea() {
			
			mypath6 = mypathOut+"/cisternsArea.csv";
			return this.mypath6; 
		}
	}
