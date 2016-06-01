/*
 * 
 */
package org.geoimage.impl.radarsat;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;

import org.geoimage.impl.imgreader.GeoToolsGDALReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that reads Radarsat 2 SLC images
 * @author gabbaan
 */
public class Radarsat2Image_SLCGDAL extends Radarsat2ImageGDAL {

    private int[] preloadedDataReal;
    private int[] preloadedDataImg;
	private Logger logger= LoggerFactory.getLogger(Radarsat2Image_SLCGDAL.class);

    public Radarsat2Image_SLCGDAL(File f) {
    	super(f);
    }

 

    @Override
    public int[] readTile(int x, int y, int width, int height,int band) {

        Rectangle rect = new Rectangle(x, y, width, height);
        rect = rect.intersection(bounds);
        int[] tile = new int[height * width];
        if (rect.isEmpty()) {
            return tile;
        }
        if (rect.y != preloadedInterval[0] | rect.y + rect.height != preloadedInterval[1]) {
            preloadLineTile(rect.y, rect.height,band);
        }
        int yOffset =  getImage(band).getxSize();
        int xinit = rect.x - x;
        int yinit = rect.y - y;
        int temp =0;
        try{
	        for (int i = 0; i < rect.height; i++) {
	            for (int j = 0; j < rect.width; j++) {
	                temp = (i * yOffset + j + rect.x);
	                long real=preloadedDataReal[temp];
	                long img=preloadedDataImg[temp];
	                tile[(i + yinit) * width + j + xinit] = (int)Math.sqrt(real*real+img*img);
	            }
	        }
        }catch(Exception e ){
        	e.printStackTrace();
        }    
        return tile;
    }
/*
    @Override
    public long readPixel(int x, int y,int band) {
        TIFFImageReadParam t = new TIFFImageReadParam();
        Rectangle rect=new Rectangle(x, y, 1, 1);
        rect = rect.intersection(bounds);
        t.setSourceRegion(rect);
        TIFF tiff=(TIFF)getImage(band);
        try {            
            int img =  tiff.read(0, t).getRaster().getSample(0,0, 1);
            int real =  tiff.read(0, t).getRaster().getSample(0, 0, 0);
            return (int) Math.sqrt(real * real + img * img);

        } catch (IOException ex) {
        	logger.error(ex.getMessage(),ex);
        } catch (ArrayIndexOutOfBoundsException ex) {
        	logger.warn("Pixel value not readed "+ex.getMessage());
        }catch(IllegalArgumentException iae){
        	logger.warn("Pixel value not readed "+iae.getMessage());
        }finally{
        }
        return -1;
    }*/
    
    @Override
    public synchronized long readPixel(int x, int y,int band) {
    	GeoToolsGDALReader tiff=null;
        try {
        	String b=getBandName(band);
        	tiff=(GeoToolsGDALReader)tiffImages.get(b);
        	int px[][]=tiff.readComplexPixValues(x, y,1,1);
        	Double v=(Math.sqrt((px[0][0]*px[0][0]+px[1][0]*px[1][0])));
            return v.longValue();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }finally{
        }
       
        return -1;
    }
    
    @Override
    public int[] read(int x, int y,int w,int h,int band) {
    	int[] data=new int[w*h];
        
    	GeoToolsGDALReader tiff=(GeoToolsGDALReader)getImage(band);
        try {            
            int px[][]=tiff.readComplexPixValues(0,0,w,h);
            
            for(int i=0;i<px[0].length;i++){
            	data[i]= (int) Math.sqrt(px[0][i] * px[0][i]  + px[1][i]  * px[1][i] );
            }
            
        } catch (ArrayIndexOutOfBoundsException ex) {
        	logger.warn(ex.getMessage());
        }catch(IllegalArgumentException iae){
        	logger.warn(iae.getMessage());
        }finally{
        }
        return data;
    }
    
    
    
    @Override
    public synchronized void preloadLineTile(int y, int length,int band) {
        if (y < 0) {
            return;
        }
        preloadedInterval = new int[]{y, y + length};
        Rectangle rect = new Rectangle(0, y,  getImage(band).getxSize(), length);
        GeoToolsGDALReader tiff=(GeoToolsGDALReader)getImage(band);
        try {
        	int px[][]=tiff.readComplexPixValues(0,0,rect.width, rect.height);
            preloadedDataReal =  px[0];
            preloadedDataImg =   px[1];
        } catch (Exception ex) {
        	logger.error(ex.getMessage(),ex);
        }
    }


    @Override
    public int getNumberOfBytes() {
        return 4;
    }

    @Override
    public int getType(boolean oneBand) {
        ////if (oneBand || bands.length < 2) {
            return BufferedImage.TYPE_USHORT_GRAY;
        //} else {
        //    return BufferedImage.TYPE_INT_RGB;
        //}
    }

    @Override
    public String getFormat() {
        return getClass().getCanonicalName();
    }
}

