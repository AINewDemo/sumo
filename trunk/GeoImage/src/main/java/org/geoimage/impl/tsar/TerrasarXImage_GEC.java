package org.geoimage.impl.tsar;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoimage.factory.GeoTransformFactory;
import org.geoimage.impl.Gcp;
import org.geoimage.impl.TIFF;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeocentricCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.sun.media.imageio.plugins.tiff.TIFFImageReadParam;

/**
 * A class that reads Terrasar-X images including process to extract gcps from the
 * time - slant range xml dataset
 * @author thoorfr, gabbaan
 */
public class TerrasarXImage_GEC extends TerrasarXImage {
    protected int[] preloadedInterval = new int[]{0, 0};
    protected int[] preloadedData;
    protected File tfw;
    protected AffineTransform matrix;
    protected Rectangle bounds;
    protected Document doc;
    protected int MGRows = 0;
    protected int MGCols = 0;
    protected int MGRefRow = 0;
    protected int MGRefCol = 0;
    protected double MGRowSpacing = 0;
    protected double MGColSpacing = 0;
    protected double ImageRowSpacing = 0;
    protected double ImageColSpacing = 0;
    protected double MGtTime = 0;
    protected double MGtauTime = 0;
    protected double rangeTimeStart = 0;
    protected double rangeTimeStop = 0;
    protected double GGtTime = 0;
    protected double GGtauTime = 0;
    protected double xposition = 0;
    protected double yposition = 0;
    protected double zposition = 0;
    protected int[] stripBounds = {0, 0, 0};
    protected Map<String, TIFF> tiffImages;
    protected TIFF image;

    protected List<String> bands = new ArrayList<String>();

    public TerrasarXImage_GEC() {
    }

    public static float arr2float(byte[] arr, int start) {
        int i = 0;
        int len = 4;
        int cnt = 3;
        byte[] tmp = new byte[len];
        for (i = start; i < (start + len); i++) {
            tmp[cnt] = arr[i];
            cnt--;
        }
        int accum = 0;
        i = 0;
        for (int shiftBy = 0; shiftBy < 32; shiftBy += 8) {
            accum |= ((long) (tmp[i] & 0xff)) << shiftBy;
            i++;
        }
        return Float.intBitsToFloat(accum);
    }

    @Override
    public String getAccessRights() {
        return "r";
    }

    @Override
    public boolean initialise(File file) {
    	try {
    		this.displayName=file.getName();
	    	setFile(file);
	    	parseProductXML(productxml);
	    	tiffImages = getImages();
	    	
	        if(tiffImages==null) return false;
	        //Check if it is a complex product
	        if (getProduct().equals("SSC")) {
	            return false;
	        }
        
            //System.out.println(reader.getNumImages(false));
            image = tiffImages.values().iterator().next();
            image.xSize = getWidth();
            image.ySize = getHeight();
            bounds = new Rectangle(0, 0, image.xSize, image.ySize);
            
            //TODO check why is commented          gcps = getGcps();
            
            if (gcps == null) {
                dispose();
                return false;
            }

            //get satellite altitude
            geotransform = GeoTransformFactory.createFromGcps(gcps, "EPSG:4326");
            double radialdist = Math.pow(xposition * xposition + yposition * yposition + zposition * zposition, 0.5);
            MathTransform convert;
            double[] latlon = getGeoTransform().getGeoFromPixel(0, 0, "EPSG:4326");
            double[] position = new double[3];
            convert = CRS.findMathTransform(DefaultGeographicCRS.WGS84, DefaultGeocentricCRS.CARTESIAN);
            convert.transform(latlon, 0, position, 0, 1);
            double earthradial = Math.pow(position[0] * position[0] + position[1] * position[1] + position[2] * position[2], 0.5);
            setSatelliteAltitude(radialdist - earthradial);

            // get incidence angles from gcps
            // !!possible to improve
            float firstIncidenceangle = (float) (this.gcps.get(0).getAngle());
            float lastIncidenceAngle = (float) (this.gcps.get(this.gcps.size() - 1).getAngle());
            setIncidenceNear(firstIncidenceangle < lastIncidenceAngle ? firstIncidenceangle : lastIncidenceAngle);
            setIncidenceFar(firstIncidenceangle > lastIncidenceAngle ? firstIncidenceangle : lastIncidenceAngle);


        } catch (TransformException ex) {
            Logger.getLogger(TerrasarXImage_GEC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FactoryException ex) {
            Logger.getLogger(TerrasarXImage_GEC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            dispose();
            Logger.getLogger(TerrasarXImage.class.getName()).log(Level.SEVERE, null, ex);

            return false;
        }
        return true;
    }

    private Map<String, TIFF> getImages() {
        List elements = doc.getRootElement().getChild("productComponents").getChildren("imageData");
        Map<String, TIFF> tiffs = new HashMap<String, TIFF>();
        bands=new Vector<String>();
        for (Object o : elements) {
            if (o instanceof Element) {
                File f = new File(productxml.getParent()+"\\"+((Element) o).getChild("file").getChild("location").getChild("path").getText()+"\\"+((Element) o).getChild("file").getChild("location").getChild("filename").getText());
                String polarisation = ((Element) o).getChild("polLayer").getValue();
                tiffs.put(polarisation, new TIFF(f,0));
                bands.add(polarisation);
            }
        }
        return tiffs;
    }
  

// <editor-fold defaultstate="collapsed" desc=" UML Marker ">
// #[regen=yes,id=DCE.091BEB3C-7DA3-1625-A9FA-3CED510139EC]
// </editor-fold>
    @Override
    public int read(int x, int y) {
        TIFFImageReadParam t = new TIFFImageReadParam();
        t.setSourceRegion(new Rectangle(x, y, 1, 1));
        try {
            return image.reader.read(0, t).getRGB(x, y);
        } catch (IOException ex) {
            Logger.getLogger(TerrasarXImage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }




    @Override
    public void parseProductXML(File productxml) throws TransformException {
        try {
            SAXBuilder builder = new SAXBuilder();
            doc = builder.build(productxml);
            Element atts = doc.getRootElement().getChild("productInfo");
            setSatellite("TerraSAR-X");
            setSensor(atts.getChild("acquisitionInfo").getChild("sensor").getText());
            String pols = "";
            for (Object o : atts.getChild("acquisitionInfo").getChild("polarisationList").getChildren("polLayer")) {
                Element elem = (Element) o;
                pols = pols + elem.getText()+" ";
                //bands.add(elem.getText());
            }
            pols.substring(0, pols.length()-1);
            setPolarization(pols);

            setLookDirection(atts.getChild("acquisitionInfo").getChild("lookDirection").getText());
            setMode(atts.getChild("acquisitionInfo").getChild("imagingMode").getText());


            setProduct(atts.getChild("productVariantInfo").getChild("productType").getText());
            setOrbitDirection(atts.getChild("missionInfo").getChild("orbitDirection").getText());
            setHeight(Integer.parseInt(atts.getChild("imageDataInfo").getChild("imageRaster").getChild("numberOfRows").getText()));
            String xSize=atts.getChild("imageDataInfo").getChild("imageRaster").getChild("numberOfColumns").getText();
            setWidth(Integer.parseInt(xSize) );
            setRangeSpacing(Double.parseDouble(atts.getChild("imageDataInfo").getChild("imageRaster").getChild("rowSpacing").getText()));
            setAzimuthSpacing(Double.parseDouble(atts.getChild("imageDataInfo").getChild("imageRaster").getChild("columnSpacing").getText()));

            setNumberOfBytes(new Integer(atts.getChild("imageDataInfo").getChild("imageDataDepth").getText()) / 8);
            setENL(atts.getChild("imageDataInfo").getChild("imageRaster").getChild("azimuthLooks").getText());

            setHeadingAngle(Double.parseDouble(atts.getChild("sceneInfo").getChild("headingAngle").getText()));
            rangeTimeStart = Double.valueOf(atts.getChild("sceneInfo").getChild("rangeTime").getChild("firstPixel").getText());
            rangeTimeStop = Double.valueOf(atts.getChild("sceneInfo").getChild("rangeTime").getChild("lastPixel").getText());
            String time = atts.getChild("sceneInfo").getChild("start").getChild("timeUTC").getText();
            //time = time.substring(0, time.lastIndexOf("."));
            setTimeStampStart(time.replaceAll("T", " "));
            time = atts.getChild("sceneInfo").getChild("stop").getChild("timeUTC").getText();
            //time = time.substring(0, time.lastIndexOf("."));
            setTimeStampStop(time.replaceAll("T", " "));
            // calculate satellite speed using state vectors
            atts = doc.getRootElement().getChild("platform").getChild("orbit").getChild("stateVec");
            double xvelocity = Double.valueOf(atts.getChildText("velX"));
            double yvelocity = Double.valueOf(atts.getChildText("velY"));
            double zvelocity = Double.valueOf(atts.getChildText("velZ"));
            double satellite_speed = Math.sqrt(xvelocity * xvelocity + yvelocity * yvelocity + zvelocity * zvelocity);
            setSatelliteSpeed(satellite_speed);
            xposition = Double.valueOf(atts.getChildText("posX"));
            yposition = Double.valueOf(atts.getChildText("posY"));
            zposition = Double.valueOf(atts.getChildText("posZ"));

            float radarFrequency = new Float(doc.getRootElement().getChild("instrument").getChild("radarParameters").getChild("centerFrequency").getText());
            setRadarWaveLenght(299792457.9 / radarFrequency);

            setSatelliteOrbitInclination(97.44);
            setRevolutionsPerday(11.0);


            //metadata used for ScanSAR mode during the Azimuth ambiguity computation
            if (getMode().equals("SC")) {
                //extraction of the 4 PRF codes
                int prf_count = 1;
                for (Object o : doc.getRootElement().getChild("instrument").getChildren("settings")) {
                    Element elem = (Element) o;
                    setMetadata("PRF" + prf_count, elem.getChild("settingRecord").getChild("PRF").getText());
                    prf_count++;
                }
                setPRF(0); //to recognise the TSX SC in the azimuth computation

                //the SC mode presents 4 strips which overlap, the idea is to consider one strip till the middle of the overlap area
                int b = 1;
                for (Object o : doc.getRootElement().getChild("processing").getChildren("processingParameter")) {
                    if (b == 4) {
                        continue;
                    }
                    Element elem = (Element) o;
                    double start_range_time = new Double(elem.getChild("scanSARBeamOverlap").getChild("rangeTimeStart").getText());
                    double stop_range_time = new Double(elem.getChild("scanSARBeamOverlap").getChild("rangeTimeStop").getText());
                    double aver_range_time = start_range_time + (stop_range_time - start_range_time) / 2;

                    int stripBound = new Double(((aver_range_time - rangeTimeStart) * new Integer(xSize)) / (rangeTimeStop - rangeTimeStart)).intValue();
                    setMetadata("STRIPBOUND" + b++, new Integer(stripBound).toString());
                }
            }

            setMetadata(K, doc.getRootElement().getChild("calibration").getChild("calibrationConstant").getChild("calFactor").getText());

            //row and cols of the mapping_grid table used for geolocation
            MGRows = new Integer(doc.getRootElement().getChild("productSpecific").getChild("geocodedImageInfo").getChild("mappingGridInfo").getChild("imageRaster").getChild("numberOfRows").getText());
            MGCols = new Integer(doc.getRootElement().getChild("productSpecific").getChild("geocodedImageInfo").getChild("mappingGridInfo").getChild("imageRaster").getChild("numberOfColumns").getText());
            MGRefRow = new Integer(doc.getRootElement().getChild("productSpecific").getChild("geocodedImageInfo").getChild("mappingGridInfo").getChild("gridReferenceTime").getChild("refRow").getText());
            MGRefCol = new Integer(doc.getRootElement().getChild("productSpecific").getChild("geocodedImageInfo").getChild("mappingGridInfo").getChild("gridReferenceTime").getChild("refCol").getText());
            MGRowSpacing = Double.valueOf(doc.getRootElement().getChild("productSpecific").getChild("geocodedImageInfo").getChild("mappingGridInfo").getChild("imageRaster").getChild("rowSpacing").getText());
            MGColSpacing = Double.valueOf(doc.getRootElement().getChild("productSpecific").getChild("geocodedImageInfo").getChild("mappingGridInfo").getChild("imageRaster").getChild("columnSpacing").getText());
            String MGtTimes = doc.getRootElement().getChild("productSpecific").getChild("geocodedImageInfo").getChild("mappingGridInfo").getChild("gridReferenceTime").getChild("tReferenceTimeUTC").getText();
            MGtTimes = MGtTimes.substring(0, MGtTimes.length() - 1);
            MGtTime = Timestamp.valueOf(MGtTimes.replaceAll("T", " ")).getTime();
            MGtauTime = Double.valueOf(doc.getRootElement().getChild("productSpecific").getChild("geocodedImageInfo").getChild("mappingGridInfo").getChild("gridReferenceTime").getChild("tauReferenceTime").getText());
            ImageRowSpacing = Double.valueOf(doc.getRootElement().getChild("productInfo").getChild("imageDataInfo").getChild("imageRaster").getChild("rowSpacing").getText());
            ImageColSpacing = Double.valueOf(doc.getRootElement().getChild("productInfo").getChild("imageDataInfo").getChild("imageRaster").getChild("columnSpacing").getText());
            Element frame= doc.getRootElement().getChild("productSpecific").getChild("geocodedImageInfo").getChild("geoParameter").getChild("frameCoordsGeographic");

            atts = doc.getRootElement().getChild("productInfo");
            Gcp g1=new Gcp();
            g1.setXpix(0);
            g1.setYpix(0);
            g1.setXgeo(Double.valueOf(frame.getChild("upperLeftLongitude").getText()));
            g1.setYgeo(Double.valueOf(frame.getChild("upperLeftLatitude").getText()));

            Gcp g2=new Gcp();
            g2.setXpix(Double.valueOf(atts.getChild("imageDataInfo").getChild("imageRaster").getChild("numberOfColumns").getText()));
            g2.setYpix(0);
            g2.setXgeo(Double.valueOf(frame.getChild("upperRightLongitude").getText()));
            g2.setYgeo(Double.valueOf(frame.getChild("upperRightLatitude").getText()));

            Gcp g3=new Gcp();
            g3.setXpix(0);
            g3.setYpix(Double.valueOf(atts.getChild("imageDataInfo").getChild("imageRaster").getChild("numberOfRows").getText()));
            g3.setXgeo(Double.valueOf(frame.getChild("lowerLeftLongitude").getText()));
            g3.setYgeo(Double.valueOf(frame.getChild("lowerLeftLatitude").getText()));

            Gcp g4=new Gcp();
            g4.setXpix(Double.valueOf(atts.getChild("imageDataInfo").getChild("imageRaster").getChild("numberOfColumns").getText()));
            g4.setYpix(Double.valueOf(atts.getChild("imageDataInfo").getChild("imageRaster").getChild("numberOfRows").getText()));
            g4.setXgeo(Double.valueOf(frame.getChild("lowerRightLongitude").getText()));
            g4.setYgeo(Double.valueOf(frame.getChild("lowerRightLatitude").getText()));

            gcps=new ArrayList<Gcp>();
            gcps.add(g1);gcps.add(g2);gcps.add(g3);gcps.add(g4);

        } catch (JDOMException ex) {
            Logger.getLogger(TerrasarXImage_GEC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TerrasarXImage_GEC.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

   
}
