package org.geoimage.impl.alos;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileFilter;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.geoimage.def.SarImageReader;
import org.geoimage.impl.imgreader.IReader;
import org.geoimage.impl.imgreader.TIFF;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Alos extends SarImageReader {
	private Logger logger= LoggerFactory.getLogger(Alos.class);

	protected int[] preloadedInterval = new int[]{0, 0};
	protected short[] preloadedData;

	protected AlosProperties props=null;
	protected List<String> polarizations=null;
	protected Map<String, IReader> alosImages;
	
	public Alos(File manifest){
		super(manifest);
		props=new AlosProperties(manifest);
	}
	
	@Override
	public int getNBand() {
		return polarizations.size();
	}

	@Override
	public String getFormat() {
		return getClass().getCanonicalName();
	}

	@Override
	public int getType(boolean oneBand) {
		if(oneBand || polarizations.size()<2) return BufferedImage.TYPE_USHORT_GRAY;
        else return BufferedImage.TYPE_INT_RGB;
	}

	@Override
	public String[] getFilesList() {
		return new String[]{manifestFile.getAbsolutePath()};
	}

	@Override
	public abstract boolean initialise();
	

    //TODO 
    @Override
    public int[] getAmbiguityCorrection(final int xPos,final int yPos) {
    	return new int[]{0};
    }

	@Override
	public String getBandName(int band) {
		return polarizations.get(band);
	}
	
	@Override
	public String[] getBands() {
		return polarizations.toArray(new String[0]);
	}
	
	@Override
	public String getImgName() {
		return null;
	}

	@Override
	public String getDisplayName(int band) {
		try{
        	return alosImages.get(getBandName(band)).getImageFile().getName();
    	}catch(Exception e){
    		return "Alos-IMG-"+System.currentTimeMillis();
    	}
	}

	@Override
	public int getWidth() {
		return ((TIFF)getImage(0)).xSize;
	}


	@Override
	public int getHeight() {
		return ((TIFF)getImage(0)).ySize;
	}

	@Override
	public double getPRF(int x, int y) {
		return 0;
	}

	@Override
	public File getOverviewFile() {
		File folder=manifestFile.getParentFile();
		File[] files=folder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if(pathname.getName().startsWith("BRS")&&pathname.getName().endsWith("jpg"))
					return true;
				return false;	
			}
		});
		return files[0];
	}

	@Override
	public String getSensor() {
		return "ALOS";
	}

	
	public IReader getImage(int band){
		IReader img=null;
		try{
			img = alosImages.get(getBandName(band));
		}catch(Exception e){ 
			logger.error(this.getClass().getName()+":getImage function  "+e.getMessage());
		}
		return img;
	}
	
	
	//----------------------------------------------
	
 /**
  * 
  * @param x
  * @param y
  * @param width
  * @param height
  * @param band
  * @return
  */
  public int[] read(int x, int y,int w,int h, int band) {
	  TIFF tiff=(TIFF)getImage(band);
      Rectangle rect = new Rectangle(x, y, w, h);
      rect = rect.intersection(tiff.getBounds());
      int data[]=null;

       try {
	       	BufferedImage bi=null;
	   		bi=tiff.read(0, rect);
	   		DataBufferUShort raster=(DataBufferUShort)bi.getRaster().getDataBuffer();
	   		short[] b=raster.getData();
	   		data=new int[b.length];
	       	for(int i=0;i<b.length;i++)
	       		data[i]=b[i];
   		
       } catch (Exception ex) {
           logger.warn(ex.getMessage());
       }
      
      return data;
  }


	
	@Override
	public int[] readTile(int x, int y, int width, int height, int band) {
		TIFF tiff=(TIFF)getImage(band);
		
		Rectangle rect = new Rectangle(x, y, width, height);
        rect = rect.intersection(tiff.getBounds());
        
        int[] tile = new int[height * width];
        if (rect.isEmpty()) {
            return tile;
        }

        if (rect.y != preloadedInterval[0] || rect.y + rect.height != preloadedInterval[1]||preloadedData.length<(rect.width*rect.height-1)) {
            preloadLineTile(rect.y, rect.height,band);
        }else{
        	logger.debug("using preloaded data");
        }

        int yOffset = tiff.xSize;
        int xinit = rect.x - x;
        int yinit = rect.y - y;
        for (int i = 0; i < rect.height; i++) {
            for (int j = 0; j < rect.width; j++) {
                int temp = i * yOffset + j + rect.x;
                try{
                	tile[(i + yinit) * width + j + xinit] = preloadedData[temp];
                }catch(ArrayIndexOutOfBoundsException e ){
                	logger.warn("readTile function:"+e.getMessage());
                }	
            }
            }
        return tile;
	}

	@Override
	public int readPixel(int x, int y, int band) {
			return read(x,y,1,1,band)[0];
	}

	

	@Override
	public void preloadLineTile(int y, int length, int band) {
		TIFF tiff=(TIFF)getImage(band);
		if (y < 0) {
            return;
        }
        preloadedInterval = new int[]{y, y + length};
        Rectangle rect = new Rectangle(0, y, tiff.xSize, length);
        
        
        rect=tiff.getBounds().intersection(rect);
        
        try {
        	BufferedImage bi=null;
        	try{
        			bi=tiff.read(0, rect);
        	}catch(Exception e){
        		logger.warn("Problem reading image POS x:"+0+ "  y: "+y +"   try to read again");
        		try {
    			    Thread.sleep(100);                 
    			} catch(InterruptedException exx) {
    			    Thread.currentThread().interrupt();
    			}
        		bi=tiff.read(0, rect);
        	}	
        	WritableRaster raster=bi.getRaster();
        	preloadedData=(short[])raster.getDataElements(0, 0, raster.getWidth(), raster.getHeight(), null);//tSamples(0, 0, raster.getWidth(), raster.getHeight(), 0, (short[]) null);
        } catch (Exception ex) {
            logger.error(ex.getMessage(),ex);
        }finally{
        	//tiff.reader.addIIOReadProgressListener(this);
        	//readComplete=false;
        	
        }

	}

	 /**
     * 
     * @param productxml
     * @param safeReader
     * @param annotationReader
     * @throws TransformException
     */
    protected void setXMLMetaData() {
        	setSatellite(new String("ALOS"));
        	
        	//polarizations string
        	List<String> pols=props.getPolarizations();
        	String strPol="";
            for (String p:pols) {
            	strPol=strPol.concat(p).concat(" ");
            }
            setPolarization(strPol);
            setSensor("ALOS");
            
            setSatelliteOrbitInclination(98.18);

            setRangeSpacing(new Float(props.getPixelSpacing()));
            setAzimuthSpacing(new Float(props.getPixelSpacing()));
            setMetaHeight(props.getNumberOfLines());
            setMetaWidth(props.getNumberOfPixels());
            setNumberBands(pols.size());
            setNumberOfBytes(16);
            
            //TODO:check this value
            //float enl=org.geoimage.impl.ENL.getFromGeoImageReader(this);
            setENL("1");//String.valueOf(enl));

            /*String start=header.getStartTime().toString().replace('T', ' ');	
            String stop=header.getStopTime().toString().replace('T', ' ');*/
            
            Date st=props.getStartDate();
            Date end=props.getEndDate();
            Timestamp t=new Timestamp(st.getTime());
            setTimeStampStart(t.toString());//Timestamp.valueOf(start));
            t.setTime(end.getTime());
            setTimeStampStop(t.toString());//Timestamp.valueOf(stop));
    }
	
	@Override
	public String getInternalImage() {
		// TODO Auto-generated method stub
		return null;
	}

	
}
