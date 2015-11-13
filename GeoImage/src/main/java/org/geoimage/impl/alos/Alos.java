package org.geoimage.impl.alos;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geoimage.def.SarImageReader;
import org.geoimage.factory.GeoTransformFactory;
import org.geoimage.impl.Gcp;
import org.geoimage.impl.TIFF;
import org.geoimage.impl.s1.Sentinel1GRD;
import org.geoimage.impl.s1.Swath;
import org.geoimage.viewer.core.SumoPlatform;
import org.geoimage.viewer.util.Constant;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeocentricCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

import jrc.it.annotation.reader.jaxb.AdsHeaderType;
import jrc.it.annotation.reader.jaxb.DownlinkInformationType;
import jrc.it.annotation.reader.jaxb.ImageInformationType;
import jrc.it.annotation.reader.jaxb.OrbitType;
import jrc.it.annotation.reader.jaxb.SwathMergeType;
import jrc.it.annotation.reader.jaxb.VelocityType;
import jrc.it.safe.reader.jaxb.StandAloneProductInformation;
import jrc.it.xml.wrapper.SumoAnnotationReader;
import jrc.it.xml.wrapper.SumoJaxbSafeReader;

public class Alos extends SarImageReader {
	private Logger logger= LoggerFactory.getLogger(Alos.class);

	
	
	private AlosProperties props=null;
	private List<String> polarizations=null;
	protected Map<String, TIFF> alosImages;
	
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
		return new String[]{manifestFile.getName()};
	}

	@Override
	public boolean initialise() {
		try {
			File mainFolder=manifestFile.getParentFile();
			
        	polarizations=props.getPolarizations();
        	
        	//set image properties
        	alosImages=new HashMap<>();
        	List<String> imgNames=props.getImageNames();
        	
        	for(int i=0;i<imgNames.size();i++){
        		String img=imgNames.get(i);
        		File imgFile=new File(mainFolder.getAbsolutePath()+File.pathSeparator+img);
        		TIFF t=new TIFF(imgFile,i);
        		alosImages.put(img.substring(4,6),t);
        	}
            
            String bandName=getBandName(0);
            String nameFirstFile=alosImages.get(bandName).getImageFile().getName();
        	super.pixelsize[0]=props.getPixelSpacing();
        	super.pixelsize[1]=props.getPixelSpacing();

			
        	//read and set the metadata from the manifest and the annotation
			setXMLMetaData();
			
			Coordinate[] corners=props.getCorners();
			int lines=props.getNumberOfLines();
			int pix=props.getNumberOfPixels();
            //we have only the corners
            gcps = new ArrayList<>();
            gcps.add(new Gcp(corners[0].x,corners[0].y,0,0));
            gcps.add(new Gcp(corners[1].x,corners[1].y,pix,0));
            gcps.add(new Gcp(corners[2].x,corners[2].y,pix,lines));
            gcps.add(new Gcp(corners[3].x,corners[3].y,0,lines));
            
           	String epsg = "EPSG:4326";
           	geotransform = GeoTransformFactory.createFromGcps(gcps, epsg);
            
            
            
            double[] latlon = geotransform.getGeoFromPixel(0, 0);
            double[] position = new double[3];
            MathTransform convert = CRS.findMathTransform(DefaultGeographicCRS.WGS84, DefaultGeocentricCRS.CARTESIAN);
            convert.transform(latlon, 0, position, 0, 1);
            

            // get incidence angles from gcps
            float firstIncidenceangle = (float) (this.gcps.get(0).getAngle());
            float lastIncidenceAngle = (float) (this.gcps.get(this.gcps.size() - 1).getAngle());
            setIncidenceNear(firstIncidenceangle < lastIncidenceAngle ? firstIncidenceangle : lastIncidenceAngle);
            setIncidenceFar(firstIncidenceangle > lastIncidenceAngle ? firstIncidenceangle : lastIncidenceAngle);

            return true;
        } catch (TransformException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (FactoryException ex) {
        	logger.error(ex.getMessage(), ex);
        } catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
        
        return false;
	}
	
	 /**
     * 
     * @param productxml
     * @param safeReader
     * @param annotationReader
     * @throws TransformException
     */
    private void setXMLMetaData() {
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

            //TODO:check this value
            //float enl=org.geoimage.impl.ENL.getFromGeoImageReader(this);
            setENL("2.3");//String.valueOf(enl));

            /*String start=header.getStartTime().toString().replace('T', ' ');	
            String stop=header.getStopTime().toString().replace('T', ' ');
            setTimeStampStart(start);//Timestamp.valueOf(start));
            setTimeStampStop(stop);//Timestamp.valueOf(stop));
            */
            
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
		// TODO Auto-generated method stub
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
		return getImage(0).xSize;
	}


	@Override
	public int getHeight() {
		return getImage(0).ySize;
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

	
	public TIFF getImage(int band){
		TIFF img=null;
		try{
			img = alosImages.get(getBandName(band));
		}catch(Exception e){ 
			logger.error(this.getClass().getName()+":getImage function  "+e.getMessage());
		}
		return img;
	}
	
	
	//----------------------------------------------
	
	@Override
	public int[] read(int x, int y, int width, int height, int band) throws IOException {
		return null;
	}


	
	@Override
	public int[] readTile(int x, int y, int width, int height, int band) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int readPixel(int x, int y, int band) {
		// TODO Auto-generated method stub
		return 0;
	}

	

	@Override
	public void preloadLineTile(int y, int height, int band) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getInternalImage() {
		// TODO Auto-generated method stub
		return null;
	}

	
}
