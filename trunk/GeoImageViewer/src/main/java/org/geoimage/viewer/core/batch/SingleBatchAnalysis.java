package org.geoimage.viewer.core.batch;

import java.util.List;

import org.geoimage.def.GeoImageReader;
import org.geoimage.def.SarImageReader;
import org.geoimage.factory.GeoImageReaderFactory;
import org.geoimage.utils.IMask;
import org.geoimage.viewer.core.api.GeometricLayer;
import org.geoimage.viewer.core.factory.FactoryLayer;


public class SingleBatchAnalysis extends AbstractBatchAnalysis {

	
	public SingleBatchAnalysis(AnalysisParams analysisParams) {
		super(analysisParams);
	}

	/**
	 * 
	 */
	protected void startAnalysis(){
		//crate the reader
		List<GeoImageReader> readers =  GeoImageReaderFactory.createReaderForName(params.pathImg);
		SarImageReader reader=(SarImageReader) readers.get(0);
		
		GeometricLayer gl=null;
		if(params.shapeFile!=null)
			gl=readShapeFile(reader);
		
		IMask[] masks = null;
		if(params.buffer!=0&&gl!=null){
			masks=new IMask[1];
			masks[0]=FactoryLayer.createBufferedLayer("buffered", FactoryLayer.TYPE_COMPLEX, params.buffer, reader, gl);
		}	
		
		analizeImage(reader,masks);
		saveResults(reader.getName());
	}	
}
