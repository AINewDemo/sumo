package org.geoimage.viewer.core.analysisproc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.geoimage.analysis.AzimuthAmbiguity;
import org.geoimage.analysis.BlackBorderAnalysis;
import org.geoimage.analysis.Boat;
import org.geoimage.analysis.DetectedPixels;
import org.geoimage.analysis.KDistributionEstimation;
import org.geoimage.analysis.S1ArtefactsAmbiguity;
import org.geoimage.analysis.VDSAnalysis;
import org.geoimage.def.GeoImageReader;
import org.geoimage.def.SarImageReader;
import org.geoimage.impl.s1.Sentinel1;
import org.geoimage.utils.GeometryExtractor;
import org.geoimage.utils.IMask;
import org.geoimage.viewer.core.Platform;
import org.geoimage.viewer.core.api.ILayer;
import org.geoimage.viewer.core.configuration.PlatformConfiguration;
import org.geoimage.viewer.core.layers.GeometricLayer;
import org.geoimage.viewer.core.layers.visualization.vectors.ComplexEditVDSVectorLayer;
import org.geoimage.viewer.core.layers.visualization.vectors.MaskVectorLayer;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;


/**
 * 
 * @author Pietro Argentieri
 *
 */
public  class AnalysisProcess implements Runnable,VDSAnalysis.ProgressListener {
		private float ENL;
		private VDSAnalysis analysis;
		private IMask[] bufferedMask=null;
		private int buffer;
		private List<ComplexEditVDSVectorLayer>resultLayers;
		private GeoImageReader gir;
		private boolean stop=false;
		private double neighbouringDistance;
		private int tilesize;
		private boolean removelandconnectedpixels;
		private boolean display;
		private int numPointLimit=0;
		private boolean displaybandanalysis;
		private String agglomerationMethodology;
		
		private static org.slf4j.Logger logger=LoggerFactory.getLogger(AnalysisProcess.class);

		
		public boolean isStop() {
			return stop;
		}

		public void setStop(boolean stop) {
			this.stop = stop;
		}

		private List<VDSAnalysisProcessListener> listeners; 
		
		
		/**
		 * 
		 * @return the list of the layers with the target results
		 */
		public List<ComplexEditVDSVectorLayer> getResultLayers() {
			return resultLayers;
		}

		public void setResultLayers(List<ComplexEditVDSVectorLayer> resultLayers) {
			this.resultLayers = resultLayers;
		}

		/**
		 * 
		 * @param gir
		 * @param ENL
		 * @param analysis
		 * @param bufferedMask
		 * @param thresholds
		 * @param buffer
		 * @param blackBorderAnalysis
		 * @param numLimitPoint   num max of point that we can analize (0=all) 
		 */
		public AnalysisProcess(GeoImageReader gir,float ENL,VDSAnalysis analysis,IMask[] bufferedMask,int buffer,int numLimitPoint) {
			this.ENL=ENL;
			this.analysis=analysis;
			this.bufferedMask=bufferedMask;
			this.buffer=buffer;
			this.resultLayers=new ArrayList<ComplexEditVDSVectorLayer>();
			listeners=new ArrayList<VDSAnalysisProcessListener>();
			this.gir=gir;//.clone();
			this.numPointLimit=numLimitPoint;
			
			neighbouringDistance=Platform.getConfiguration().getNeighbourDistance(1.0);
            tilesize=Platform.getConfiguration().getTileSize(200);
            removelandconnectedpixels = PlatformConfiguration.getConfigurationInstance().removeLandConnectedPixel();
            display = Platform.getConfiguration().getDisplayPixel()&&!Platform.isBatchMode();
            displaybandanalysis= Platform.getConfiguration().getDisplayBandAnalysis();
            agglomerationMethodology = Platform.getConfiguration().getAgglomerationAlg();
		}
   
		/**
		 *  Exec the analysis process
		 */
		public void run() {
			notifyStartProcessListener();
			SarImageReader reader=((SarImageReader)gir);
			
			String[] thresholdsString=StringUtils.join(analysis.getThresholdsParams(),",").split(",");
			
			//run the blackborder analysis for the s1 images
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
             if(blackBorderAnalysis!=null){
            	 blackBorderAnalysis.analyse(Platform.getConfiguration().getNumTileBBAnalysis());
             }
             //end blackborder analysis
             
             analysis.setBlackBorderAnalysis(blackBorderAnalysis);
             
             // list of bands
             int numberofbands = gir.getNBand();
             int[] bands = new int[numberofbands];

             // create K distribution
             final KDistributionEstimation kdist = new KDistributionEstimation(ENL);
             
             DetectedPixels mergePixels = new DetectedPixels(reader.getRangeSpacing(),reader.getAzimuthSpacing());
             DetectedPixels banddetectedpixels[]=new DetectedPixels[numberofbands];
             
             //landmask name
             String bufferedMaskName="";
             if(bufferedMask!=null && bufferedMask.length>0){
            	 bufferedMaskName=bufferedMask[0].getName(); 
             }
             
             String timeStampStart=reader.getTimeStampStart();
             double azimuth=reader.getAzimuthSpacing();
             
             try{
	             for (int band = 0; band < numberofbands&&!stop; band++) {
	            	 notifyAnalysisBand( new StringBuilder().append("VDS: analyzing band ").append(gir.getBandName(band)).toString());
	            	 
	            	 int vTiles=analysis.getVerTiles();
	            	 notifyVDSAnalysis("Performing VDS Analysis",vTiles);
	            	 analysis.addProgressListener(this);

	            	 //identify probably target
	            	 banddetectedpixels[band]=analysis.analyse(kdist,band);
	            	 //banddetectedpixels[band]=analysis.getPixels();
	            	 
	            	 if(numPointLimit!=0&&banddetectedpixels[band].getAllDetectedPixels().size()>numPointLimit){
	            		 logger.warn("Too much points. Stop Image analysis!!!");
	            		 return;
	            	 }
	            	 
	            	 if (mergePixels == null) {
	            		mergePixels=banddetectedpixels[band];
	            	}else{
	            		mergePixels.merge(banddetectedpixels[band]);	
	            	}
	            	 
	            	 bands[band] = band;
	            	 String polarization=reader.getBands()[band];
	                 
	                 if (numberofbands < 1 || displaybandanalysis) {
	                     notifyAgglomerating( new StringBuilder().append("VDS: agglomerating detections for band ").append(polarization).toString());

	                     Boat[] boats=null;
		            	 //merge pixel to build the "boats"
	                     if (agglomerationMethodology.startsWith("d")) {
	                         // method distance used
	                         banddetectedpixels[band].agglomerate();
	                         banddetectedpixels[band].computeBoatsAttributes(polarization);
	                     } else {
	                         // method neighbours used
	                         if(stop)
	                        	 break;
	                         
	                         boats=analysis.agglomerateNeighbours(banddetectedpixels[band],neighbouringDistance, tilesize,removelandconnectedpixels, 
	                         		 (bufferedMask != null) && (bufferedMask.length != 0) ? bufferedMask[0] : null, kdist,polarization,band);
	                     }
	                     
	                     String layerName=new StringBuilder("VDS analysis ").append(polarization).append(" ").append(analysis.getThresholdParam(polarization)).toString();
	                     
	                     ComplexEditVDSVectorLayer vdsanalysisLayer = new ComplexEditVDSVectorLayer(Platform.getCurrentImageLayer(),layerName,
	                    		 "point", new GeometricLayer("VDS Analysis","point",timeStampStart,azimuth, boats),
	                    		 thresholdsString,ENL,buffer,bufferedMaskName,""+band);

	                     vdsanalysisLayer.addDetectedPixels(banddetectedpixels[band].getAllDetectedPixels(), display);
	                     
	                     if (!agglomerationMethodology.startsWith("d")) {
	                         vdsanalysisLayer.addThreshAggPixels(banddetectedpixels[band].getThresholdaggregatePixels(), display);
	                         vdsanalysisLayer.addThresholdPixels(banddetectedpixels[band].getThresholdclipPixels(), display);
	                     }
	                     
	                     //Azimuth Ambiguities
	                     notifyCalcAzimuth("VDS: looking for azimuth ambiguities...");
	                     AzimuthAmbiguity azimuthAmbiguity = new AzimuthAmbiguity(boats, (SarImageReader) gir,band);
	                     azimuthAmbiguity.process();
	                     List<Geometry> az=azimuthAmbiguity.getAmbiguityboatgeometry();
	                     vdsanalysisLayer.addAzimuthAmbiguities(az, display);
	                     
	                     //Azimuth Ambiguities ONLY FOR S1
	                     if(gir instanceof Sentinel1){
	                    	 if(((Sentinel1)gir).getInstumentationMode().equalsIgnoreCase("EW")||((Sentinel1)gir).getInstumentationMode().equalsIgnoreCase("IW")){
			                     notifyCalcAzimuth("VDS: looking for artefacts ambiguities...");
			                     S1ArtefactsAmbiguity arAmbiguity  = new S1ArtefactsAmbiguity(boats, (SarImageReader) gir,band);	
			                     arAmbiguity.process();
			                     List<Geometry> artefactsA=arAmbiguity.getAmbiguityboatgeometry();
			                     vdsanalysisLayer.addArtefactsAmbiguities(artefactsA, display);
	                    	 }    
	                     }    
                         
                         if ((bufferedMask != null) && (bufferedMask.length > 0)) {
	                        vdsanalysisLayer.addGeometries("bufferedmask", Color.BLUE, 1, GeometricLayer.POLYGON, bufferedMask[0].getGeometries(), true);
	                     }
	                     //leave display params forced to false
	                     vdsanalysisLayer.addGeometries("tiles", new Color(0xFF00FF), 1, GeometricLayer.LINESTRING, GeometryExtractor.getTiles(gir.getWidth(),gir.getHeight(),analysis.getTileSize()), false);
	
	                     notifyLayerReady(vdsanalysisLayer); 
	                     resultLayers.add(vdsanalysisLayer);
	                 }
	             }
	             if(stop){
	            	 stop();
	            	 return;
	             }	 
	             
	             // create the merged Layers if we have more than one band
	             if (bands.length > 1) {
	                 notifyAgglomerating("VDS: agglomerating detections...");
	                 Boat[] boats=null;
	                 if (agglomerationMethodology.startsWith("d")) {
	                     // method distance used
	                     mergePixels.agglomerate();
	                     mergePixels.computeBoatsAttributes("merge");
	                 } else {
	                     // method neighbours used
	                	 boats=analysis.agglomerateNeighbours(mergePixels,neighbouringDistance, tilesize, removelandconnectedpixels, (bufferedMask != null) && (bufferedMask.length != 0) ? bufferedMask[0] : null, kdist,"merge",bands);
	                 }
	                 if(stop){
	                     stop();
	                	 return;
	                 }
	                 //TODO: per ora Merged viene utilizzato per indicare che e' il layer del merge e non delle bande ma VA CAMBIATO!!!
	                 ComplexEditVDSVectorLayer vdsanalysisLayer = new ComplexEditVDSVectorLayer(Platform.getCurrentImageLayer(),"VDS analysis all bands merged", 
	                		 																	"point", new GeometricLayer("VDS Analysis","point",timeStampStart,azimuth, boats),
	                		 																	thresholdsString,ENL,buffer,bufferedMaskName,"Merged");
	                 boolean display = Platform.getConfiguration().getDisplayPixel();
	                 if (!agglomerationMethodology.startsWith("d")) {
	                     vdsanalysisLayer.addThreshAggPixels(mergePixels.getThresholdaggregatePixels(), display);
	                     vdsanalysisLayer.addThresholdPixels(mergePixels.getThresholdclipPixels(), display);
	                 }
	                 vdsanalysisLayer.addDetectedPixels(mergePixels.getAllDetectedPixels(), display);
	                 
	                 if ((bufferedMask != null) && (bufferedMask.length > 0)) {
	                     vdsanalysisLayer.addGeometries("bufferedmask", new Color(0x0000FF), 1, GeometricLayer.POLYGON, bufferedMask[0].getGeometries(), true);
	                 }
	                 vdsanalysisLayer.addGeometries("tiles", new Color(0xFF00FF), 1, GeometricLayer.LINESTRING,GeometryExtractor.getTiles(gir.getWidth(),gir.getHeight(),analysis.getTileSize()), false);
	                 
	                 
	               //Azimuth Ambiguities
                     notifyCalcAzimuth("VDS: looking for azimuth ambiguities...");
                     AzimuthAmbiguity azimuthAmbiguity = new AzimuthAmbiguity(boats, (SarImageReader) gir,bands);
                     azimuthAmbiguity.process();
                     List<Geometry> az=azimuthAmbiguity.getAmbiguityboatgeometry();
                     vdsanalysisLayer.addAzimuthAmbiguities(az, display);
                     
                     //Azimuth Ambiguities ONLY FOR S1
                     if(gir instanceof Sentinel1){
                    	 if(((Sentinel1)gir).getInstumentationMode().equalsIgnoreCase("EW")||((Sentinel1)gir).getInstumentationMode().equalsIgnoreCase("IW")){
		                     notifyCalcAzimuth("VDS: looking for artefacts ambiguities...");
		                     S1ArtefactsAmbiguity arAmbiguity  = new S1ArtefactsAmbiguity(boats, (SarImageReader) gir,bands);	
		                     arAmbiguity.process();
		                     List<Geometry> artefactsA=arAmbiguity.getAmbiguityboatgeometry();
		                     vdsanalysisLayer.addArtefactsAmbiguities(artefactsA, display);
                    	 }    
                     }    
	                 notifyLayerReady(vdsanalysisLayer);
	                 resultLayers.add(vdsanalysisLayer);
	             }
	             stop();
             }catch(Exception ee){
            	 ee.printStackTrace();
             }    
         }
		
		private void stop(){
           notifyEndProcessListener();
           removeAllProcessListener();
		}
		 
		/**
		 *  
		 * @param listener
		 */
		public void addProcessListener(VDSAnalysisProcessListener listener){
			this.listeners.add(listener);
		} 
		/**
		 * 
		 * @param listener
		 */
		public void removeProcessListener(VDSAnalysisProcessListener listener){
			this.listeners.remove(listener);
		}
		public void removeAllProcessListener(){
			this.listeners.clear();
		}
		public void notifyEndProcessListener(){
			for(VDSAnalysisProcessListener listener:listeners){
				listener.endAnalysis();
			}
		} 
		public void notifyStartProcessListener(){
			for(VDSAnalysisProcessListener listener:listeners){
				listener.startAnalysis();
			}
		} 
		public void notifyVDSAnalysis(String message,int maxSteps){
			for(VDSAnalysisProcessListener listener:listeners){
				listener.performVDSAnalysis(message,maxSteps);
			}
		}
		public void notifyAgglomerating(String message){
			for(VDSAnalysisProcessListener listener:listeners){
				listener.agglomerating(message);
			}
		}
		public void notifyLayerReady(ILayer layer){
			for(VDSAnalysisProcessListener listener:listeners){
				listener.layerReady(layer);
			}
		}
		public void notifyAnalysisBand(String message){
			for(VDSAnalysisProcessListener listener:listeners){
				listener.startAnalysisBand(message);
			}
		}
		public void notifyCalcAzimuth(String message){
			for(VDSAnalysisProcessListener listener:listeners){
				listener.calcAzimuthAmbiguity(message);
			}
		}
		@Override
		public void startRowProcesseing(int step) {
			for(VDSAnalysisProcessListener listener:listeners){
				listener.nextVDSAnalysisStep(step);
			}
		}
		@Override
		public void endRowProcesseing(int row) {
		}
		
		
		public void dispose(){
			analysis.dispose();
		}
     }

