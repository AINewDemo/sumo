/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoimage.viewer.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import org.geoimage.analysis.BlackBorderAnalysis;
import org.geoimage.analysis.VDSAnalysis;
import org.geoimage.def.GeoImageReader;
import org.geoimage.def.SarImageReader;
import org.geoimage.impl.ENL;
import org.geoimage.impl.TiledBufferedImage;
import org.geoimage.impl.s1.Sentinel1;
import org.geoimage.utils.IMask;
import org.geoimage.utils.IProgress;
import org.geoimage.viewer.core.Platform;
import org.geoimage.viewer.core.analysisproc.AnalysisProcess;
import org.geoimage.viewer.core.analysisproc.VDSAnalysisProcessListener;
import org.geoimage.viewer.core.api.Argument;
import org.geoimage.viewer.core.api.ILayer;
import org.geoimage.viewer.core.api.iactions.AbstractAction;
import org.geoimage.viewer.core.factory.FactoryLayer;
import org.geoimage.viewer.core.layers.GeometricLayer;
import org.geoimage.viewer.core.layers.image.ImageLayer;
import org.geoimage.viewer.core.layers.visualization.vectors.ComplexEditVDSVectorLayer;
import org.geoimage.viewer.core.layers.visualization.vectors.MaskVectorLayer;

/**
 *
 * @author thoorfr
 */
public class VDSAnalysisConsoleAction extends AbstractAction implements  IProgress,VDSAnalysisProcessListener,ActionListener{
    private String message = "";
    
    
    private int current = 0;
    private int maximum = 3;
    private boolean done = false;
    private boolean indeterminate;
	//private boolean running = false;
    private GeoImageReader gir = null;
    private List<IMask> mask = null;
    private AnalysisProcess proc=null;
    private boolean stopping=false;
   
    public VDSAnalysisConsoleAction() {
        
    }

    public String getName() {
        return "vds";
    }

    public String getDescription() {
        return "Compute a VDS (Vessel Detection System) analysis.\n"
                + "Use \"vds k-dist 1.5 GSHHS\" to run a analysis with k-distribuion clutter model with a threshold of 1.5 using the land mask \"GSHHS...\"";
    }

    /**
     * run the analysis called from ActionDialog
     */
    public boolean execute(String[] args) {
        // initialise the buffering distance value
        int bufferingDistance = Double.valueOf((Platform.getConfiguration()).getBufferingDistance()).intValue();
        Platform.getMain().addStopListener(this);
        
        
        if (args.length < 2) {
            return true;
        } else {

            if (args[0].equals("k-dist")) {
                done = false;
                
                ImageLayer cl=Platform.getCurrentImageLayer();
                GeoImageReader reader = ((ImageLayer) cl).getImageReader();
                if (reader instanceof SarImageReader || reader instanceof TiledBufferedImage) {
                    gir = reader;
                }
                if (gir == null) {
                    done = true;
                    return false;
                }

                //this part mange the different thresholds for different bands
                //in particular is also looking for which band is available and leave the threshold to 0 for the not available bands
                float thrHH = 0;
                float thrHV = 0;
                float thrVH = 0;
                float thrVV = 0;
                
                int numberofbands = gir.getNBand();
                for (int bb = 0; bb < numberofbands; bb++) {
                    if (gir.getBandName(bb).equals("HH") || gir.getBandName(bb).equals("H/H")) {
                        thrHH = Float.parseFloat(args[bb + 1]);
                    } else if (gir.getBandName(bb).equals("HV") || gir.getBandName(bb).equals("H/V")) {
                        thrHV = Float.parseFloat(args[bb + 1]);
                    } else if (gir.getBandName(bb).equals("VH") || gir.getBandName(bb).equals("V/H")) {
                        thrVH = Float.parseFloat(args[bb + 1]);
                    } else if (gir.getBandName(bb).equals("VV") || gir.getBandName(bb).equals("V/V")) {
                        thrVV = Float.parseFloat(args[bb + 1]);
                    }
                }
                final float thresholdHH = thrHH;
                final float thresholdHV = thrHV;
                final float thresholdVH = thrVH;
                final float thresholdVV = thrVV;

                //read the land mask
                mask = new ArrayList<IMask>();
                for (ILayer l : Platform.getLayerManager().getChilds(cl)) {
                    if (l instanceof IMask & l.getName().startsWith(args[numberofbands + 1])) {
                        mask.add((IMask) l);
                    }
                }

                //read the buffer distance
                bufferingDistance = Integer.parseInt(args[numberofbands + 2]);
                final float ENL = Float.parseFloat(args[numberofbands + 3]);

                // create new buffered mask with bufferingDistance using the mask in parameters
                final IMask[] bufferedMask = new IMask[mask.size()];
                
                for (int i=0;i<mask.size();i++) {
                	IMask maskList = mask.get(i);
               		bufferedMask[i]=FactoryLayer.createMaskLayer(maskList.getName(), maskList.getType(), bufferingDistance, ((MaskVectorLayer)maskList).getGeometriclayer());
                }
                
                //for test only
               /* try {
					SimpleShapefile.exportGeometriesToShapeFile(bufferedMask[0].getGeometries(),new File("F:\\SumoImgs\\export\\aaa.shp") ,"Polygon");
				} catch (IOException | SchemaException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
                
                final VDSAnalysis analysis = new VDSAnalysis((SarImageReader) gir, bufferedMask, ENL, thresholdHH, thresholdHV, thresholdVH, thresholdVV);
                
                final String[] thresholds = {""+thrHH,""+thrHV,""+thrVH,""+thrVV};
                
                BlackBorderAnalysis blackBorderAnalysis=null;
                if(gir instanceof Sentinel1){
	                MaskVectorLayer mv=null;
	           	 	if(bufferedMask!=null&&bufferedMask.length>0)
	           	 		mv=(MaskVectorLayer)bufferedMask[0];
	           	 	if(mv!=null)
	           	 		blackBorderAnalysis= new BlackBorderAnalysis(gir,mv.getGeometries());
	           	 	else 
	           		    blackBorderAnalysis= new BlackBorderAnalysis(gir,null);
                } 	
                
                proc=new AnalysisProcess(reader,ENL, analysis, bufferedMask, thresholds,bufferingDistance,blackBorderAnalysis,0);
                proc.addProcessListener(this);
                
                Thread t=new Thread(proc);
                t.setName("VDS_analysis_"+gir.getDisplayName(0));
                t.start();
                
            }

            return true;
        }
    }
    
    
    
    
   
	/**
	 *
	 * @return
	 */
    public String getPath() {
        return "Analysis/VDS";
    }

    public List<Argument> getArgumentTypes() {
        Argument a1 = new Argument("algorithm", Argument.STRING, false, "k-dist");
        a1.setPossibleValues(new Object[]{"k-dist"});
        Argument a2 = new Argument("thresholdHH", Argument.FLOAT, false, 1.5);
        Argument a21 = new Argument("thresholdHV", Argument.FLOAT, false, 1.2);
        Argument a22 = new Argument("thresholdVH", Argument.FLOAT, false, 1.5);
        Argument a23 = new Argument("thresholdVV", Argument.FLOAT, false, 1.5);

        Argument a3 = new Argument("mask", Argument.STRING, true, "no mask choosen");
        ArrayList<String> vectors = new ArrayList<String>();
        ImageLayer il=Platform.getCurrentImageLayer();

        if (il != null) {
          //  for (ILayer l : il.getLayers()) {
            for (ILayer l : Platform.getLayerManager().getAllLayers()) {
                if (l instanceof MaskVectorLayer && !((MaskVectorLayer) l).getType().equals(GeometricLayer.POINT)) {
                    vectors.add(l.getName());
                }
            }
        }
        a3.setPossibleValues(vectors.toArray());
        List<Argument> out = new ArrayList<Argument>();

        Argument a4 = new Argument("Buffer (pixels)", Argument.FLOAT, false, Platform.getConfiguration().getBufferingDistance());

        //management of the different threshold in the VDS parameters panel
        out.add(a1);
        int numberofbands = il.getImageReader().getNBand();
        for (int bb = 0; bb < numberofbands; bb++) {
            if (il.getImageReader().getBandName(bb).equals("HH") || il.getImageReader().getBandName(bb).equals("H/H")) {
                out.add(a2);
            } else if (il.getImageReader().getBandName(bb).equals("HV") || il.getImageReader().getBandName(bb).equals("H/V")) {
                out.add(a21);
            } else if (il.getImageReader().getBandName(bb).equals("VH") || il.getImageReader().getBandName(bb).equals("V/H")) {
                out.add(a22);
            } else if (il.getImageReader().getBandName(bb).equals("VV") || il.getImageReader().getBandName(bb).equals("V/V")) {
                out.add(a23);
            }
        }

        out.add(a3);
        out.add(a4);
        if (il.getImageReader() instanceof SarImageReader) {
            Argument aEnl = new Argument("ENL", Argument.FLOAT, false, ENL.getFromGeoImageReader((SarImageReader) il.getImageReader()));
            out.add(aEnl);
        }

        return out;
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
		Platform.getMain().removeStopListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(proc!=null&&e.getActionCommand().equals("STOP")){
			this.proc.setStop(true);
			this.message="stopping";
			Platform.getMain().removeStopListener(this);
			this.proc=null;
		}	
	}

	@Override
	public void layerReady(ILayer layer) {
		if(!Platform.isBatchMode()){
			Platform.getLayerManager().addLayer(layer);
		}
		
	}

	
}
