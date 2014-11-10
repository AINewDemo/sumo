package org.geoimage.impl;

import java.awt.Rectangle;

import jrc.it.xml.wrapper.SumoJaxbSafeReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.media.imageio.plugins.tiff.TIFFImageReadParam;

/**
 * 
 * 
 * @author 
 */
public class  Sentinel1GRD extends Sentinel1 {
	private Logger logger= LoggerFactory.getLogger(Sentinel1GRD.class);
	protected int[] preloadedData;
    
    public Sentinel1GRD(SumoJaxbSafeReader safeReader,String swath) {
    	super(safeReader,swath);
    }


    @Override
    public int[] readTile(int x, int y, int width, int height) {
        Rectangle rect = new Rectangle(x, y, width, height);
        rect = rect.intersection(getActiveImage().bounds);
        int[] tile = new int[height * width];
        if (rect.isEmpty()) {
            return tile;
        }

        if (rect.y != preloadedInterval[0] || rect.y + rect.height != preloadedInterval[1]) {
            preloadLineTile(rect.y, rect.height);
        }

        int yOffset = getActiveImage().xSize;
        int xinit = rect.x - x;
        int yinit = rect.y - y;
        for (int i = 0; i < rect.height; i++) {
            for (int j = 0; j < rect.width; j++) {
                int temp = i * yOffset + j + rect.x;
               tile[(i + yinit) * width + j + xinit] = preloadedData[temp];
            }
            }
        return tile;
    }

    @Override
    public void preloadLineTile(int y, int length) {
        if (y < 0) {
            return;
        }
        preloadedInterval = new int[]{y, y + length};
        Rectangle rect = new Rectangle(0, y, getActiveImage().xSize, length);
        TIFFImageReadParam tirp = new TIFFImageReadParam();
        tirp.setSourceRegion(rect);
        try {
            preloadedData = getActiveImage().reader.read(0, tirp).getRaster().getSamples(0, 0, getActiveImage().xSize, length, 0, (int[]) null);
        } catch (Exception ex) {
            logger.error(ex.getMessage(),ex);
        }
    }
    
    
    @Override
	public int getWidth() {
		return getActiveImage().xSize;
	}


	@Override
	public int getHeight() {
		return getActiveImage().ySize;
	}
	
	public TIFF getActiveImage(){
		return tiffImages.get(getBandName(getBand()));
	}
	
}
