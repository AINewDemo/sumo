package org.geoimage.viewer.core.batch;

import java.awt.Color;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geoimage.analysis.DetectedPixels;
import org.geoimage.analysis.KDistributionEstimation;
import org.geoimage.analysis.VDSAnalysis;
import org.geoimage.def.GeoImageReader;
import org.geoimage.def.SarImageReader;
import org.geoimage.factory.GeoImageReaderFactory;
import org.geoimage.utils.IMask;
import org.geoimage.viewer.actions.VDSAnalysisConsoleAction;
import org.geoimage.viewer.actions.VDSAnalysisConsoleAction.AnalysisProcess;
import org.geoimage.viewer.core.GeoImageViewerView;
import org.geoimage.viewer.core.api.GeometricLayer;
import org.geoimage.viewer.core.api.IVectorLayer;
import org.geoimage.viewer.core.factory.FactoryLayer;
import org.geoimage.viewer.core.factory.VectorIOFactory;
import org.geoimage.viewer.core.io.AbstractVectorIO;
import org.geoimage.viewer.core.io.SumoXmlIOOld;
import org.geoimage.viewer.core.layers.vectors.ComplexEditVDSVectorLayer;
import org.slf4j.LoggerFactory;


class AnalysisParams{
	//starting params
	public static final String TRESH_HH_PARAM="-thh";
	public static  final String TRESH_HV_PARAM="-thv";
	public static  final String TRESH_VH_PARAM="-tvh";
	public static  final String TRESH_VV_PARAM="-tvv";
	public static  final String[] TRESH_PARAMS={TRESH_HH_PARAM, TRESH_VH_PARAM , TRESH_HV_PARAM ,TRESH_VV_PARAM };
	public static  final String IMG_PARAM="-i";
	public static  final String IMG_FOLD_PARAM="-d";
	public static  final String BUFFER_FOLD_PARAM="-b";
	public static  final String OUTPUT_FOLD_PARAM="-o";

	
	//HH HV VH VV
	public int[] thresholdArrayValues={1,1,1,1};
	public String pathImg="";
	public String shapeFile="";
	public String outputFolder="";
	public float enl=1;
	public double buffer=15.0;
	public String epsg="EPSG:4326";	
}


public class Sumo {

	//status
	private final int PARAM_ERROR=-1;
	private final int SINGLE_IMG_ANALYSIS=1;
	private final int FOLDER_IMG_ANALYSIS=2;
	
	private int status=SINGLE_IMG_ANALYSIS;
	private String msg="";
	
	private final  String TOO_MUCH_PARAMS="-d and -f are exsclusive params";
	
	private AnalysisParams params;
	private  List<ComplexEditVDSVectorLayer>layerResults=null;
	
	private static org.slf4j.Logger logger=LoggerFactory.getLogger(GeoImageViewerView.class);
	
	public Sumo(){
		params=new AnalysisParams();
	}
	
	/**
	 * 
	 */
	public void execAnalysis(){
		if(status==SINGLE_IMG_ANALYSIS){
			startAnalysis();
		}
	}
	
	/**
	 * 
	 * @param reader
	 * @return
	 */
	private GeometricLayer readShapeFile(SarImageReader reader){
		GeometricLayer gl=null;
  	    Map<String,Object> config = new HashMap<String,Object>();
	    try {
            config.put("url", new File(params.shapeFile).toURI().toURL());
            AbstractVectorIO shpio = VectorIOFactory.createVectorIO(VectorIOFactory.SIMPLE_SHAPEFILE, config);
            gl = shpio.read(reader);
        } catch (Exception e) {
        	logger.error(e.getMessage(),e);
        }
        
        return gl;
        
	}
	
	/**
	 * run analysis for 1 image
	 */
	public void startAnalysis(){
		List<GeoImageReader> readers =  GeoImageReaderFactory.createReaderForName(params.pathImg);
		SarImageReader reader=(SarImageReader) readers.get(0);
		
		GeometricLayer gl=readShapeFile(reader);
		
		IVectorLayer ivl = FactoryLayer.createVectorLayer(FactoryLayer.TYPE_COMPLEX, gl,reader);
        ivl.setColor(Color.GREEN);
        ivl.setWidth(5);
		
		IMask[] masks = new IMask[1];
		
        VDSAnalysis analysis = new VDSAnalysis(reader,
        		masks, 
        		params.enl, 
        		params.thresholdArrayValues[0], 
        		params.thresholdArrayValues[1], 
        		params.thresholdArrayValues[2], 
        		params.thresholdArrayValues[3], 
        		false);
        analysis.run(null);
        
        int numberofbands = reader.getNBand();
        final String[] thresholds = new String[numberofbands];
        //management of the strings added at the end of the layer name in order to remember the used threshold
        for (int bb = 0; bb < numberofbands; bb++) {
            if (reader.getBandName(bb).equals("HH") || reader.getBandName(bb).equals("H/H")) {
                thresholds[bb] = "" + params.thresholdArrayValues[0];
            } else if (reader.getBandName(bb).equals("HV") || reader.getBandName(bb).equals("H/V")) {
                thresholds[bb] = "" + params.thresholdArrayValues[1];
            } else if (reader.getBandName(bb).equals("VH") || reader.getBandName(bb).equals("V/H")) {
                thresholds[bb] = "" + params.thresholdArrayValues[2];
            } else if (reader.getBandName(bb).equals("VV") || reader.getBandName(bb).equals("V/V")) {
                thresholds[bb] = "" + params.thresholdArrayValues[3];
            }
        }
        
        
       VDSAnalysisConsoleAction action= new VDSAnalysisConsoleAction();
       layerResults=action.runBatchAnalysis(params.enl,analysis,masks,thresholds); 
	}

	/**
	 * 
	 */
	private void saveResults(){
		if(layerResults!=null){
    	   for(ComplexEditVDSVectorLayer l:layerResults){
    		   String out="C:\\test\\"+l.getName();
    		   l.save(out,ComplexEditVDSVectorLayer.OPT_EXPORT_XML_SUMO_OLD,params.epsg);
    	   }
        }
	}
	
	
	/**
	 * 
	 * @param params
	 * @return
	 */
	private int parseParams(List<String> inputParams){
		int status=0; //OK
		//XOR se i parametri non contengono o contengono entrambi 
        if (!inputParams.contains(AnalysisParams.IMG_FOLD_PARAM)  ^ !inputParams.contains(AnalysisParams.IMG_PARAM) ){
        	status=PARAM_ERROR;
        	msg=TOO_MUCH_PARAMS;
        }else if(inputParams.contains(AnalysisParams.IMG_FOLD_PARAM)){
        	int idx=inputParams.indexOf(AnalysisParams.IMG_FOLD_PARAM);
        	String dir=inputParams.get(idx+1);
        	File f=new File(dir);
        	if(f.exists()&&f.isDirectory()){
        		status=FOLDER_IMG_ANALYSIS;
        		params.pathImg=dir;
        	}else{
        		status=PARAM_ERROR;
        	}
        }else if(inputParams.contains(AnalysisParams.IMG_FOLD_PARAM)){
        	int idx=inputParams.indexOf(AnalysisParams.IMG_FOLD_PARAM);
        	String dir=inputParams.get(idx+1);
        	File f=new File(dir);
        	if(f.exists()&&f.isFile()){
        		status=SINGLE_IMG_ANALYSIS;
        		params.pathImg=dir;
        	}else{
        		status=PARAM_ERROR;
        	}
        }

        //parameters are OK
        if(status==0){
        	int index=inputParams.indexOf(AnalysisParams.BUFFER_FOLD_PARAM);
        	if(index!=-1){
        		params.buffer=Double.parseDouble(inputParams.get(index+1));
        	}

        	//set the treshold params
        	for(int i=0;i<AnalysisParams.TRESH_PARAMS.length;i++){
        		//leggo i valori di threshold se li trovo 
        		index=inputParams.indexOf(AnalysisParams.TRESH_PARAMS[i]);
        		if(i!=-1){
        			//li setto nell'array dei threshold con questo ordine HH HV VH VV
        			int val=Integer.parseInt(inputParams.get(index+1));
        			params.thresholdArrayValues[i]=val;
        		}else{
        			//we need a default value??
        		}
        	}
        	
        	//check for the output folder 
        	index=inputParams.indexOf(AnalysisParams.OUTPUT_FOLD_PARAM);
        	if(index!=-1){
        		params.outputFolder=inputParams.get(index+1);
        	}else{
        		//if the output folder is not setted we create an output folder under the current folder
        		File f=new File("./outupt_"+System.currentTimeMillis()+"/");
        		f.mkdir();
        		params.outputFolder=f.getAbsolutePath();
        	}
        }
        
		return status;
	}
	
	
	public static void main(String[] args) {
		/**@todo
         * arguments
         * -i : path to single image folder
         * -d : path to the directory that contains the image folders
         * 
         * 
         * -s: specify shape file (if is not passed , use land mask for the analysis)
         * 
         * -gf: specify global file configuration
         * 
         * -lf: (Y/N) search local file default is N 
         * 
         * -enl : equinvalent number of looks
         * -thh threshold HH 
         * -thv threshold HV 
         * -tvh threshold VH 
         * -tvv threshold HV 
         * 
         * -o output dir to store files if no ddbb storage
         * 
         * 
         *
         */
		List<String> params=Arrays.asList(args);
		Sumo s=new Sumo();
		s.parseParams(params);
		s.execAnalysis();
		s.saveResults();
		
	}

}
