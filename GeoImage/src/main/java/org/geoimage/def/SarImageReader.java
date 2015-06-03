/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoimage.def;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.FastMath;
import org.geoimage.factory.GeoImageReaderFactory;
import org.geoimage.impl.Gcp;
import org.geoimage.utils.Constant;
import org.geoimage.utils.Corners;
import org.geoimage.utils.IProgress;
import org.geotools.referencing.GeodeticCalculator;

/**
 * this is a class that implememts default method to access raster data.
 * Your own reader should extends this class in most cases:
 * class MySarReader extends SarImageReader { ... }
 * @author leforth
 */
public abstract class SarImageReader extends SUMOMetadata implements GeoImageReader{

    protected static int MAXTILESIZE = 16 * 1024 * 1024;
    protected String displayName = "";
    protected String imgName = "";
    protected List<Gcp> gcps;
    
    protected GeoTransform geotransform;

    protected boolean containsMultipleImage=false;
    protected Corners originalCorners=null;
    BufferedImage overViewImage;

   

    /**
     * used for XML
     */
	public abstract String getImgName();
	@Override
	public abstract String getDisplayName(int band); 
	@Override
    public abstract int getWidth();

    @Override
    public abstract int getHeight();

  

    public List<Gcp> getGcps() {
        return gcps;
    }

    public GeoTransform getGeoTransform() {
        return geotransform;
    }
    
    @Override
    public int[] readAndDecimateTile(int x, int y, int width, int height, int outWidth, int outHeight,int xSize,int ySize, boolean filter,int band) {
        if (x + width < 0 || y + height < 0 || x > xSize || y > ySize) {
            return new int[outWidth * outHeight];
        }

        if (height < 257) {
            int[] outData = new int[outWidth * outHeight];
            int[] data = readTile(x, y, width, height,band);
            int decX = FastMath.round(width / (1f * outWidth));
            int decY = FastMath.round(height / (1f * outHeight));
            if (data != null) {
                int index = 0;
                for (int j = 0; j < outHeight; j++) {
                    int temp = (int) (j * decY) * width;
                    for (int i = 0; i < outWidth; i++) {
                        if (filter) {
                            for (int h = 0; h < decY; h++) {
                                for (int w = 0; w < decX; w++) {
                                    outData[index] += data[temp + h * width + (int) (i * decX + w)];
                                }
                            }
                            if (decX > 1) {
                                outData[index] /= (int) decX;
                            }
                            if (decY > 1) {
                                outData[index] /= (int) decY;
                            }
                        } else {
                            outData[index] = data[temp + (int) (i * decX)];
                        }
                        index++;
                    }
                }
            }
            return outData;
        } else {
            float incy = height / 256f;
            int[] outData = new int[outWidth * outHeight];
            float decY = height / (1f * outHeight);
            int index = 0;
            for (int i = 0; i < FastMath.ceil(incy); i++) {
                int tileHeight = (int) FastMath.min(Constant.TILE_SIZE, height - i * Constant.TILE_SIZE);
                if (tileHeight > decY) {
                    int[] temp = readAndDecimateTile(x, y + i * Constant.TILE_SIZE, width, tileHeight, outWidth, FastMath.round(tileHeight / decY),xSize,ySize, filter,band);
                    if (temp != null) {
                        for (int j = 0; j < temp.length; j++) {
                            if (index < outData.length) {
                                outData[index++] = temp[j];
                            }
                        }
                    } else {
                        index += outWidth * (int) (Constant.TILE_SIZE / decY);
                    }
                    temp = null;
                }
            }
            return outData;
        }
    }

    public int[] readAndDecimateTile(int x, int y, int width, int height, double scalingFactor, boolean filter, IProgress progressbar,int band) {
        int outWidth = (int) (width * scalingFactor);
        int outHeight = (int) (height * scalingFactor);
        double deltaPixelsX = (double) width / outWidth;
        double deltaPixelsY = (double) height / outHeight;
        double tileHeight = height / (((double) (width * height) / MAXTILESIZE));
        int[] outData = new int[outWidth * outHeight];
        if (height / outHeight > 4) {
            double a = width*1.0 / outWidth;  //moltiplico per 1.0 per avere un risultato con i decimali
            double b = height*1.0 / outHeight;
            for (int i = 0; i < outHeight; i++) {
                for (int j = 0; j < outWidth; j++) {
                    try {
                        outData[i * outWidth + j] = readTile((int) (x + j * a), (int) (y + i * b), 1, 1,band)[0];
                    } catch (Exception e) {
                    }
                }
            }
            return outData;
        }
        // load first tile
        int currentY = 0;
        int[] tile = readTile(0, currentY, width, (int) FastMath.ceil(tileHeight),band);
        if (progressbar != null) {
            progressbar.setMaximum(outHeight / 100);
        // start going through the image one Tile at a time
        }
        double posY = 0.0;
        for (int j = 0; j < outHeight; j++, posY += deltaPixelsY) {
            // update progress bar
            if (j / 100 - FastMath.floor(j / 100) == 0) {
                if (progressbar != null) {
                    progressbar.setCurrent(j / 100);
                // check if Tile needs loading
                }
            }
            if (posY > (int) FastMath.ceil(tileHeight)) {
                tile = readTile(0, currentY + (int) FastMath.ceil(tileHeight), width, (int) FastMath.ceil(tileHeight),band);
                posY -= (int) FastMath.ceil(tileHeight);
                currentY += (int) FastMath.ceil(tileHeight);

            }

            double posX = 0.0;
            for (int i = 0; i < outWidth; i++, posX += deltaPixelsX) {
                //System.out.println("i = " + i + ", j = " + j + ", posX = " + posX + ", posY = " + posY);
                outData[i + j * outWidth] = tile[(int) posX * (int) posY];
            }
            //System.gc();
        }

        return outData;
    }

    public double getImageAzimuth() {
        double az = 0;

        //compute the azimuth considering the two left corners of the image
        //azimuth angle in degrees between 0 and +180
        double[] endingPoint = getGeoTransform().getGeoFromPixel(getWidth() / 2, 0);//, "EPSG:4326");
        double[] startingPoint = getGeoTransform().getGeoFromPixel(getWidth() / 2, getHeight() - 1);//, "EPSG:4326");
        GeodeticCalculator gc = new GeodeticCalculator();
        gc.setStartingGeographicPoint(startingPoint[0], startingPoint[1]);
        gc.setDestinationGeographicPoint(endingPoint[0], endingPoint[1]);
        az = gc.getAzimuth();
        return az;
    }

    public void dispose() {
    }

    public String getDescription() {
        StringBuilder description = new StringBuilder("Image Acquisition and Generation Parameters:\n")
        	 .append("--------------------\n\n")
        	 .append("Satellite and Instrument: ")
	         .append("\n").append(getSatellite()).append("  " + getSensor())
	         .append("\nProduct: ")
	         .append(getProduct())
	         .append("\nMode: ")
	         .append(getMode())
	         .append("\nBeam: ")
	         .append(getBeam())
	         .append("\nPolarisations: ")
	         .append(getPolarization())
	         .append("\nHeading Angle: ")
	         .append(getHeadingAngle())
	         .append("\nOrbit Direction: ")
	         .append(getOrbitDirection())
	         .append("\nImage Dimensions:\n")
	         .append("\tWidth:").append(getWidth())
	         .append("\n\tHeight:").append(getHeight())
	         .append("\nImage Acquisition Time:\n")
	         .append("\tStart:").append(getTimeStampStart())
	         .append("\n\tStop:").append(getTimeStampStop())
	         .append("\nImage Pixel Spacing:\n")
	         .append("\tAzimuth:").append(getAzimuthSpacing())
	         .append("\n\tRange:").append(getRangeSpacing())
	         .append("\nImage Processor and Algorithm: ")
	         .append(getProcessor())
	         .append("\nImage ENL: ")
	         .append(getENL())
	         .append("\nSatellite Altitude (m): ")
	         .append(getSatelliteAltitude())
	         .append("\nSatellite Speed (m/s): ")
	         .append(getSatelliteSpeed())
	         .append("\nIncidence Angles (degrees):\n")
	         .append("\tNear: ").append(getIncidenceNear())
	         .append("\n\tFar: ").append(getIncidenceFar());

        return description.toString();
    }
    
    

    public int[] getAmbiguityCorrection(int xPos,int yPos) {

        double temp, deltaAzimuth, deltaRange;
        int[] output = new int[2];

        try {

            double slantRange = getSlantRange(xPos);
            // already in radian
            double incidenceAngle = getIncidence(xPos);
            double satelliteSpeed = calcSatelliteSpeed();
            double radarWavelength = getRadarWaveLenght();
            double prf = getPRF(xPos,yPos); 
            double orbitInclination = Math.toRadians(getSatelliteOrbitInclination());
            double revolutionsPerDay = getRevolutionsPerday();
            double sampleDistAzim = getGeoTransform().getPixelSize()[0];
            double sampleDistRange = getGeoTransform().getPixelSize()[1];

            temp = (radarWavelength * slantRange * prf) /
                    (2 * satelliteSpeed * (1 - Math.cos(orbitInclination) / revolutionsPerDay));

            //azimuth and delta in number of pixels
            deltaAzimuth = temp / sampleDistAzim;
            deltaRange = (temp * temp) / (2 * slantRange * sampleDistRange * Math.sin(incidenceAngle));

            output[0] = (int) Math.floor(deltaAzimuth);
            output[1] = (int) Math.floor(deltaRange);

        } catch (Exception ex) {
        	ex.printStackTrace();
        }
        return output;

    }

    public double getIncidence(int position) {
        double incidenceangle = 0.0;
        // estimation of incidence angle based on near and range distance values
        double nearincidence = Math.toRadians(getIncidenceNear().doubleValue());
        double sataltitude=getSatelliteAltitude();
        
        
        double distancerange = sataltitude * Math.tan(nearincidence) + position * getGeoTransform().getPixelSize()[1];
        incidenceangle = Math.atan(distancerange / sataltitude);
        return incidenceangle;
    }

    private double calcSatelliteSpeed() {
        // calculate satellite speed
/*
        double seconds = ((double)(getTimestamp(GeoImageReaderBasic.TIMESTAMP_STOP).getTime() - getTimestamp(GeoImageReaderBasic.TIMESTAMP_START).getTime())) / 1000;
        // calculate satellite speed in azimuth pixels / seconds
        double azimuthpixelspeed = ((double)getHeight() * (6400000 + sataltitude) / 6400000) / seconds;
        return azimuthpixelspeed * getGeoTransform().getPixelSize()[0];
         */
        double satellite_speed = 0.0;

        // check if satellite speed has been calculated
        if (getSatelliteSpeed() != null) {
            satellite_speed = getSatelliteSpeed();
        } else {
            // Ephemeris --> R + H
            //Approaching the orbit as circular V=SQRT(GM/(R+H))
            double sataltitude = getSatelliteAltitude();
            satellite_speed = Math.pow(3.986005e14 / (6371000 + sataltitude), 0.5);
            setSatelliteSpeed(satellite_speed);
        }

        return satellite_speed;
    }

    public double getSlantRange(int position) {
        double slantrange = 0.0;
        double incidenceangle = getIncidence(position);
        double sataltitude = getSatelliteAltitude();
        // calculate slant range
        if (Math.cos(incidenceangle) != 0.0) {
            slantrange = sataltitude / Math.cos(incidenceangle);
        }
        return slantrange;
    }

    public abstract double getPRF(int x,int y); 
    
    public double getBetaNought(int x, double DN) {
        double Kvalue = getK();
        return Math.pow(DN, 2) / Kvalue;
    }

    public double getBetaNoughtDb(int x, double DN) {
        double betaNoughtDb;
        double betaNought;

        betaNought = this.getBetaNought(x, DN);
        if (betaNought > 0) {
            betaNoughtDb = 10 * Math.log(betaNought);
        } else {
            betaNoughtDb = -1;
        }

        // System.out.println("beta nought in db:" +betaNoughtDb );
        return betaNoughtDb;
    }

    public double getSigmaNoughtDb(int[] pixel, double value,
            double incidence_angle) {
        double sigmaNoughtDb;
        double betaNoughtDb;

        betaNoughtDb = getBetaNoughtDb(pixel[0], value);

        if (betaNoughtDb != -1) {
            sigmaNoughtDb = betaNoughtDb + 10 * Math.log(Math.sin(incidence_angle));
        } else {
            sigmaNoughtDb = -1;
        }
        return sigmaNoughtDb;
    }

    public double getSigmaNoughtDb(double betaNoughtDb, double incidence_angle) {
        double sigmaNoughtDb;

        sigmaNoughtDb = betaNoughtDb + 10 * Math.log(Math.sin(incidence_angle));
        return sigmaNoughtDb;
    }

    /**
     * @param file
     *            The file in which it reads
     * @param pointer
     *            Location in the file
     * @param nbBytes
     * @return
     * @throws IOException
     */
    protected float getFloatValue(RandomAccessFile file, int pointer, int nbBytes) throws IOException {
        String convert = "";
        Float temp = null;
        float data = 0;
        int i;
        for (i = 0; i < nbBytes; i++) {
            convert += getCharValue(file, pointer++);
        }
        System.out.println("getFloatValue: " + convert);
        convert = convert.trim();
        if (convert.equalsIgnoreCase("")) {
            System.out.println("getFloatValue : nothing at this place");
            data = -1;
        } else {
            temp = new Float(convert);
            data = temp.floatValue();
        }
        return data;
    }

    protected char getCharValue(RandomAccessFile file, int pointer) throws IOException {
        file.seek(pointer);
        return (char) file.readByte();
    }

    public List<double[]> getFrameLatLon(int xSize,int ySize) {
        if (geotransform != null) {
            ArrayList<double[]> latlonframe = new ArrayList<double[]>();

            // use the four image corners to define the image frame
            latlonframe.add(geotransform.getGeoFromPixel(0, 0));//, "EPSG:4326"));
            latlonframe.add(geotransform.getGeoFromPixel(xSize, 0));//, "EPSG:4326"));
            latlonframe.add(geotransform.getGeoFromPixel(xSize, ySize));//, "EPSG:4326"));
            latlonframe.add(geotransform.getGeoFromPixel(0, ySize));//, "EPSG:4326"));

            return latlonframe;
        }

        return null;
    }

    //TODO check the clone object
    @Override
    public GeoImageReader clone(){
        return GeoImageReaderFactory.cloneReader(this);
    }


	public boolean isContainsMultipleImage() {
		return containsMultipleImage;
	}

	public void setContainsMultipleImage(boolean containsMultipleImage) {
		this.containsMultipleImage = containsMultipleImage;
	}
	

	@Override
	public abstract File getOverviewFile();
	
	
	public BufferedImage getOverViewImage() {
		return overViewImage;
	}

	public void setOverViewImage(BufferedImage overViewImage) {
		this.overViewImage = overViewImage;
	}

}
