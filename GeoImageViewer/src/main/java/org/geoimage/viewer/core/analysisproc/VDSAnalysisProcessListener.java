package org.geoimage.viewer.core.analysisproc;

import org.geoimage.viewer.core.api.ILayer;

public interface VDSAnalysisProcessListener {
	
	public void startAnalysis();
	public void startAnalysisBand(String message);
	public void calcAzimuthAmbiguity(String message);
	public void agglomerating(String message);
	public void endAnalysis();
	public void layerReady(ILayer layer);
	
	
	
}
