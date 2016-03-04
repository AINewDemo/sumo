package org.geoimage.viewer.actions;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geoimage.analysis.BlackBorderAnalysis;
import org.geoimage.analysis.MaskGeometries;
import org.geoimage.analysis.VDSAnalysis;
import org.geoimage.def.GeoImageReader;
import org.geoimage.def.SarImageReader;
import org.geoimage.impl.s1.Sentinel1;
import org.geoimage.viewer.actions.console.AbstractConsoleAction;
import org.geoimage.viewer.core.SumoPlatform;
import org.geoimage.viewer.core.analysisproc.AnalysisProcess;
import org.geoimage.viewer.core.analysisproc.VDSAnalysisProcessListener;
import org.geoimage.viewer.core.api.ilayer.ILayer;
import org.geoimage.viewer.core.api.ilayer.IMask;
import org.geoimage.viewer.core.factory.FactoryLayer;
import org.geoimage.viewer.core.gui.manager.LayerManager;
import org.geoimage.viewer.core.layers.GenericLayer;
import org.geoimage.viewer.core.layers.GeometricLayer;
import org.geoimage.viewer.core.layers.image.ImageLayer;
import org.geoimage.viewer.core.layers.visualization.vectors.MaskVectorLayer;
import org.geoimage.viewer.util.IProgress;
import org.geoimage.viewer.widget.dialog.ActionDialog.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;


public class TileAnalysisAction extends AbstractConsoleAction implements VDSAnalysisProcessListener,IProgress,ActionListener{
	private Logger logger = LoggerFactory.getLogger(TileAnalysisAction.class);
	boolean done=false;
	private int current = 0;
    private int maximum = 3;
    private boolean indeterminate;
    private String message = "";
    private boolean stopping=false;
    private AnalysisProcess proc=null;


    public TileAnalysisAction(){
    	super("chktile","Tools/CheckTile");
    }


	@Override
	public String getDescription() {
		return "Analyze Tile Values";
	}


	private void runBBAnalysis(){
		//run the blackborder analysis for the s1 images
		BlackBorderAnalysis blackBorderAnalysis=null;
		GeoImageReader gir=SumoPlatform.getApplication().getCurrentImageReader();
        if(gir instanceof Sentinel1){
                /*MaskVectorLayer mv=null;
           	 	if(bufferedMask!=null&&bufferedMask.length>0)
           	 		mv=(MaskVectorLayer)bufferedMask[0];
           	 	if(mv!=null)
           	 		blackBorderAnalysis= new BlackBorderAnalysis(gir,mv.getGeometries());
           	 	else*/
           	blackBorderAnalysis= new BlackBorderAnalysis(gir,null);
         }
         if(blackBorderAnalysis!=null){
        	 int nTile=SumoPlatform.getApplication().getConfiguration().getNumTileBBAnalysis();
        	 blackBorderAnalysis.analyse(nTile,BlackBorderAnalysis.ANALYSE_ALL);
        	 List<Coordinate>cc=blackBorderAnalysis.getCoordinatesThresholds();
        	 GeometricLayer bbanal=new GeometricLayer("BBAnalysis",GeometricLayer.POINT,cc);
        	 GenericLayer ivl = FactoryLayer.createComplexLayer(bbanal);
        	 ivl.setColor(Color.GREEN);
        	 ivl.setWidth(5);
        	 LayerManager.addLayerInThread(ivl);

         }
         //end blackborder analysis
	}


	@Override
	public boolean executeFromConsole() {
		try {
			SarImageReader sar=(SarImageReader) SumoPlatform.getApplication().getCurrentImageReader();
			ImageLayer layer=LayerManager.getIstanceManager().getCurrentImageLayer();

			if(layer!=null && commandLine.length>=3){
				String arg0=commandLine[1];
				String arg1=commandLine[2];
				String arg2=commandLine[3];

				//run for the black border analysis
				if(arg0.equalsIgnoreCase("bb")){
					if(commandLine.length==2 && arg1.equalsIgnoreCase("test")){
						runBBAnalysis();
					}else{
						int row=Integer.parseInt(arg1);
						int col=Integer.parseInt(arg2);
						String direction="H"; //h= horizontal v=vertical
						if(paramsAction.size()==4)
							direction=arg2;
						BlackBorderAnalysis borderAn=new BlackBorderAnalysis(sar,0,null);
						borderAn.analyse(row,col,direction.equalsIgnoreCase("H"));
					}
			//run vds analysis on a single tile
				}else if(arg0.equalsIgnoreCase("vds")){

					int row=Integer.parseInt(arg1);//args[1]);
					int col=Integer.parseInt(arg2);

					Float hh=1.5f;
					Float hv=1.5f;
					Float vh=1.5f;
					Float vv=1.5f;
					Float buffer=0.0f;
					if(commandLine.length>=5){
						buffer=Float.parseFloat(commandLine[4]);
						hh=Float.parseFloat(commandLine[5]);
						hv=Float.parseFloat(commandLine[6]);
						vh=Float.parseFloat(commandLine[7]);
						vv=Float.parseFloat(commandLine[8]);
					}


					MaskVectorLayer coastlineMask = null;
				    MaskVectorLayer iceMasks = null;
					//read the land mask
	                for (ILayer l : LayerManager.getIstanceManager().getChilds(layer)) {
	                    if (l instanceof IMask ) {
	                    	if( ((MaskVectorLayer) l).getMaskType()==MaskVectorLayer.COASTLINE_MASK){
	                    			coastlineMask=(MaskVectorLayer) l;
	                    	}else if( ((MaskVectorLayer) l).getMaskType()==MaskVectorLayer.ICE_MASK){
	                    			iceMasks=(MaskVectorLayer) l;
	                    	}
	                    }
	                }

					IMask bufferedMask=null;
	                if(coastlineMask!=null)
	                	bufferedMask=FactoryLayer.createMaskLayer(coastlineMask.getName(),
	                		coastlineMask.getType(),0,((MaskVectorLayer)coastlineMask).getGeometriclayer(),
	           				coastlineMask.getMaskType());

	                IMask iceMask=null;
	                if(iceMasks!=null)
	                	 iceMask=FactoryLayer.createMaskLayer(iceMasks.getName(),
	                		iceMasks.getType(),0,((MaskVectorLayer)iceMasks).getGeometriclayer(),
	           				iceMasks.getMaskType());


	                MaskGeometries mg=new MaskGeometries("coast", bufferedMask.getGeometries());
	                MaskGeometries ice=new MaskGeometries("ice",   iceMask.getGeometries());

	                VDSAnalysis analysis = new VDSAnalysis(sar, mg,ice, Float.parseFloat(sar.getENL()), new Float[]{hh,hv,vh,vv});
					analysis.setAnalyseSingleTile(true);
					analysis.setxTileToAnalyze(col);
					analysis.setyTileToAnalyze(row);
					proc=new AnalysisProcess(sar,Float.parseFloat(sar.getENL()), analysis,0,0);
					proc.addProcessListener(this);
					Thread t=new Thread(proc);
	                t.setName("VDS_analysis_"+sar.getDisplayName(0));
	                t.start();
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			//proc.removeProcessListener(this);
			//proc.dispose();

			return false;
		}finally{
		}
		return true;
	}

	@Override
	public List<Argument> getArgumentTypes() {
		List <Argument> args=new  ArrayList<Argument>();
		return args;
	}

	 public void setCurrent(int i) {
	        current = i;
	    }

	    public void setMaximum(int size) {
	        maximum = size;
	    }

	    public void setMessage(String string) {
	        message =  string;
	    }

	    public void setIndeterminate(boolean value) {
	        indeterminate = value;
	    }

	    public void setDone(boolean value) {
	        done = value;
	    }


	@Override
	public void startAnalysis() {
		setCurrent(1);
		message="Starting VDS Analysis";
	}
	@Override
	public void performVDSAnalysis(String message,int numSteps) {
		if(!stopping){
			setMaximum(numSteps);
			setCurrent(1);
			this.message=message;
		}
	}

	@Override
	public void startAnalysisBand(String message) {
		if(!stopping){
			setCurrent(2);
			this.message=message;
		}
	}

	@Override
	public void calcAzimuthAmbiguity(String message) {
		if(!stopping){
			setCurrent(3);
			this.message=message;
		}
	}

	@Override
	public void agglomerating(String message) {
		if(!stopping){
			setCurrent(4);
			this.message=message;
		}
	}

	public void nextVDSAnalysisStep(int numSteps){
		//setMessage(numSteps+"/"+maximum);
		setCurrent(numSteps);
	}


	@Override
	public void endAnalysis() {
		setDone(true);
		SumoPlatform.getApplication().getMain().removeStopListener(this);

		if(proc!=null)
			proc.dispose();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(proc!=null&&e.getActionCommand().equals("STOP")){
			this.proc.setStop(true);
			this.message="stopping";
			SumoPlatform.getApplication().getMain().removeStopListener(this);
			this.proc=null;
		}
	}

	@Override
	public void layerReady(ILayer layer) {
		if(!SumoPlatform.isBatchMode()){
			LayerManager.getIstanceManager().addLayer(layer);
		}
	}

	public boolean isIndeterminate() {
        return this.indeterminate;
    }

    public boolean isDone() {
        return this.done;
    }

    public int getMaximum() {
        return this.maximum;
    }

    public int getCurrent() {
        return this.current;
    }

    public String getMessage() {
        return this.message;
    }

	@Override
	public void startBlackBorederAnalysis(String message) {
		if(!stopping){
			setCurrent(1);
			this.message=message;
		}

	}


	@Override
	public String getCommand() {
		return "chktile";
	}


	@Override
	public boolean execute() {
		return executeFromConsole();
	}


}
