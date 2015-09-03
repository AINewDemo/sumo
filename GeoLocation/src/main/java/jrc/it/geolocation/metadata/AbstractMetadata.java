package jrc.it.geolocation.metadata;


public abstract class AbstractMetadata implements IMetadata{
	//for S1 always Increasing
	protected boolean pixelTimeOrderingAscending;
	protected String type="";
	
	//for S1 is rangePixelSpacing
    protected float samplePixelSpacing;
    protected float azimuthPixelSpacing;
    

	//for S1 always 0
    protected double groundRangeOrigin=0; 
    protected double numberOfSamplesPerLine=0;
    protected String antennaPointing;
    protected String mode="";
    
    public abstract String getAntennaPointing();

	public abstract void initMetaData();
	
	public abstract double getNumberOfSamplesPerLine();
	
	
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type=type;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public void setAntennaPointing(String antennaPointing) {
		this.antennaPointing = antennaPointing;
	}

	
	public boolean isPixelTimeOrderingAscending() {
		return pixelTimeOrderingAscending;
	}

	public void setPixelTimeOrderingAscending(boolean pixelTimeOrderingAscending) {
		this.pixelTimeOrderingAscending = pixelTimeOrderingAscending;
	}

	public double getSamplePixelSpacing() {
		return samplePixelSpacing;
	}

	public void setSamplePixelSpacing(float samplePixelSpacing) {
		this.samplePixelSpacing = samplePixelSpacing;
	}
	
	public double getGroundRangeOrigin() {
		return groundRangeOrigin;
	}


	public void setGroundRangeOrigin(double groundRangeOrigin) {
		this.groundRangeOrigin = groundRangeOrigin;
	}

	public void setNumberOfSamplesPerLine(double numberOfSamplesPerLine) {
		this.numberOfSamplesPerLine = numberOfSamplesPerLine;
	}

	public float getAzimuthPixelSpacing() {
		return azimuthPixelSpacing;
	}

	public void setAzimuthPixelSpacing(float azimuthPixelSpacing) {
		this.azimuthPixelSpacing = azimuthPixelSpacing;
	}
	
}
