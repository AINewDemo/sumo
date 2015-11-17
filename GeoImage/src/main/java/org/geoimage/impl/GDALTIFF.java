/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoimage.impl;

import java.awt.Rectangle;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.GCP;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.slf4j.LoggerFactory;


/**
 * This is a convenience class that warp a tiff image to easily use in the case
 * of one geotiff per band (like radarsat 2 images)
 * @author thoorfr
 */
public class GDALTIFF {
	private static org.slf4j.Logger logger=LoggerFactory.getLogger(GDALTIFF.class);


    private File imageFile;
    private Dataset data;
    private Band b;
	public int xSize = -1;
    public int ySize = -1;
    public Rectangle bounds;
    private int buf_type ; 
    private List<GCP> gpcs=null;
    
    public List<GCP> getGpcs() {
		return gpcs;
	}


	public void setGpcs(List<GCP> gpcs) {
		this.gpcs = gpcs;
	}


	public Dataset getDataSet() {
		return data;
	}


    /**
     * 
     * @param imageFile
     * @param band form files with multiple band
     */
	public GDALTIFF(File imageFile,int band) {
		gdal.AllRegister();
    	this.imageFile=imageFile;
        try {
            boolean worked=false;
    		
    		data = gdal.Open(imageFile.getAbsolutePath(), gdalconstConstants.GA_ReadOnly);
    		b = data.GetRasterBand(band+1);
    		gpcs=data.GetGCPs();//((GCP)gpcs.get(0)).getGCPX()
    		buf_type=b.getDataType();
    		
    		xSize=b.getXSize();
    		ySize=b.getYSize();
    		bounds=new Rectangle(0,0,xSize,ySize);
    		worked=true;
            if(!worked){
            	logger.warn("No reader avalaible for this image");
            }
        } catch (Exception ex) {
        	logger.error(ex.getMessage(),ex);
        }	
	}
	
	
	public short[] readShortValues(int x,int y,int offsetx,int offsety){
		int pixels = xSize * offsetx;
		int buf_size = pixels * gdal.GetDataTypeSize(buf_type) / 8;

		ByteBuffer buffer = ByteBuffer.allocateDirect(buf_size);
		buffer.order(ByteOrder.nativeOrder());

		short[] dd = new short[buf_size];
		int ok = b.ReadRaster(x, y, offsetx, offsety,gdalconstConstants.GDT_UInt16, dd);
		buffer.clear();
		return dd;
	}
	
	
	
    public int getxSize() {
		return xSize;
	}

	public void setxSize(int xSize) {
		this.xSize = xSize;
	}

	public int getySize() {
		return ySize;
	}

	public void setySize(int ySize) {
		this.ySize = ySize;
	}

	public Rectangle getBounds() {
		return bounds;
	}

	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
	}
	public void refreshBounds() {
		bounds=new Rectangle(0,0,xSize,ySize);
	}
	
	public void dispose(){
     
    }
    public File getImageFile() {
		return imageFile;
	}

	public void setImageFile(File imageFile) {
		this.imageFile = imageFile;
	}
	
		

}
