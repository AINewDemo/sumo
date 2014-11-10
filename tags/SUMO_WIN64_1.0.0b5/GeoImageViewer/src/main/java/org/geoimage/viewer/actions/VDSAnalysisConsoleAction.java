/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoimage.viewer.actions;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import java.awt.Color;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.geoimage.analysis.DetectedPixels;
import org.geoimage.analysis.AzimuthAmbiguity;
import org.geoimage.analysis.KDistributionEstimation;
import org.geoimage.analysis.VDSAnalysis;
import org.geoimage.analysis.VDSSchema;
import org.geoimage.def.GeoImageReader;
import org.geoimage.factory.GeoImageReaderFactory;
import org.geoimage.impl.ENL;
import org.geoimage.impl.SarImageReader;
import org.geoimage.impl.TiledBufferedImage;
import org.geoimage.viewer.core.api.IConsoleAction;
import org.geoimage.viewer.core.api.IImageLayer;
import org.geoimage.viewer.core.api.ILayer;
import org.geoimage.utils.IMask;
import org.geoimage.viewer.core.Platform;
import org.geoimage.viewer.core.api.Argument;
import org.geoimage.viewer.core.api.Attributes;
import org.geoimage.viewer.core.api.GeometricLayer;
import org.geoimage.utils.IProgress;
import org.geoimage.viewer.core.layers.ComplexEditVDSVectorLayer;
import org.geoimage.viewer.core.layers.SimpleVectorLayer;

/**
 *
 * @author thoorfr
 */
public class VDSAnalysisConsoleAction implements IConsoleAction, IProgress {

    private String message = "starting VDS Analysis...";
    private int current = 0;
    private int maximum = 3;
    private boolean done = false;
    private boolean indeterminate;
    private boolean running = false;
    private GeoImageReader gir = null;
    private IImageLayer il = null;
    private Vector<IMask> mask = null;
    private static final String DISPLAY_PIXELS = "VDS Analaysis - Display detected pixels";
    private static final String DISPLAY_BANDS = "VDS Analaysis - Display all bands detection results";
    private static final String TARGETS_COLOR_BAND_0 = "VDS Analaysis - Target Color - Band 0";
    private static final String TARGETS_SIZE_BAND_0 = "VDS Analaysis - Target Size - Band 0";
    private static final String TARGETS_SYMBOL_BAND_0 = "VDS Analaysis - Target Symbol - Band 0";
    private static final String TARGETS_COLOR_BAND_1 = "VDS Analaysis - Target Color - Band 1";
    private static final String TARGETS_COLOR_BAND_2 = "VDS Analaysis - Target Color - Band 2";
    private static final String TARGETS_COLOR_BAND_3 = "VDS Analaysis - Target Color - Band 3";
    private static final String TARGETS_SIZE_BAND_1 = "VDS Analaysis - Target Size - Band 1";
    private static final String TARGETS_SIZE_BAND_2 = "VDS Analaysis - Target Size - Band 2";
    private static final String TARGETS_SIZE_BAND_3 = "VDS Analaysis - Target Size - Band 3";
    private static final String TARGETS_SYMBOL_BAND_1 = "VDS Analaysis - Target Symbol - Band 1";
    private static final String TARGETS_SYMBOL_BAND_2 = "VDS Analaysis - Target Symbol - Band 2";
    private static final String TARGETS_SYMBOL_BAND_3 = "VDS Analaysis - Target Symbol - Band 3";
    private static final String TARGETS_COLOR_BAND_MERGED = "VDS Analaysis - Target Color - Band Merged";
    private static final String TARGETS_SIZE_BAND_MERGED = "VDS Analaysis - Target Size - Band Merged";
    private static final String TARGETS_SYMBOL_BAND_MERGED = "VDS Analaysis - Target Symbol - Band Merged";
    private static final String BUFFERING_DISTANCE = "VDS Analaysis - Buffering Distance in pixels";
    private static final String AGGLOMERATION_METHODOLOGY = "VDS Analaysis - Agglomeration Method";
    private static final String NEIGHBOUR_DISTANCE = "VDS Analaysis - Neighbours distance";
    private static final String NEIGHBOUR_TILESIZE = "VDS Analaysis - Neighbours tile size";
    private static final String REMOVE_LANDCONNECTEDPIXELS = "VDS Analaysis - Remove pixels connected to land in neighbour mode";

    public VDSAnalysisConsoleAction() {
        // creates preferences fields
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.DISPLAY_PIXELS, "true");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.DISPLAY_BANDS, "true");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.TARGETS_COLOR_BAND_0, "0x0000FF");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.TARGETS_SIZE_BAND_0, "1.0");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.TARGETS_SYMBOL_BAND_0, "square");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.TARGETS_SYMBOL_BAND_1, "triangle");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.TARGETS_COLOR_BAND_1, "0x00FF00");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.TARGETS_SIZE_BAND_1, "1.0");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.TARGETS_COLOR_BAND_2, "0xFF0000");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.TARGETS_SIZE_BAND_2, "1.0");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.TARGETS_SYMBOL_BAND_2, "square");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.TARGETS_COLOR_BAND_3, "0xFFFF00");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.TARGETS_SIZE_BAND_3, "1.0");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.TARGETS_SYMBOL_BAND_3, "triangle");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.TARGETS_SYMBOL_BAND_MERGED, "cross");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.TARGETS_COLOR_BAND_MERGED, "0xFFAA00");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.TARGETS_SIZE_BAND_MERGED, "1.0");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.BUFFERING_DISTANCE, "15.0");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.AGGLOMERATION_METHODOLOGY, "neighbours");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.NEIGHBOUR_DISTANCE, "2.0");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.NEIGHBOUR_TILESIZE, "200");
        Platform.getPreferences().insertIfNotExistRow(VDSAnalysisConsoleAction.REMOVE_LANDCONNECTEDPIXELS, "true");
    }

    public String getName() {
        return "vds";
    }

    public String getDescription() {
        return "Compute a VDS (Vessel Detection System) analysis.\n"
                + "Use \"vds k-dist 1.5 GSHHS\" to run a analysis with k-distribuion clutter model with a threshold of 1.5 using the land mask \"GSHHS...\"";
    }

    public boolean execute(String[] args) {
        // initialise the buffering distance value
        double bufferingDistance = Double.parseDouble((Platform.getPreferences()).readRow(BUFFERING_DISTANCE));

        if (args.length < 2) {
            return true;
        } else {

            if (args[0].equals("k-dist")) {

                done = false;
                message = "VDS: initialising parameters...";
                setCurrent(1);

                for (ILayer l : Platform.getLayerManager().getLayers()) {
                    if (l instanceof IImageLayer & l.isActive()) {
                        GeoImageReader temp = ((IImageLayer) l).getImageReader();
                        if (temp instanceof SarImageReader || temp instanceof TiledBufferedImage) {
                            gir = temp;//GeoImageReaderFactory.createReaderForName(((IImageLayer) l).getImageReader().getFilesList()[0]).get(0);
                            il = (IImageLayer) l;
                            break;
                        }
                    }
                }
                if (gir == null) {
                    done = true;
                    return false;
                }

                //this part mange the different thresholds for different bands
                //in particular is also looking for which band is available and leave the threshold to 0 for the not available bands
                float thrHH = 0;
                float thrHV = 0;
                float thrVH = 0;
                float thrVV = 0;
                int numberofbands = gir.getNBand();
                for (int bb = 0; bb < numberofbands; bb++) {
                    if (gir.getBandName(bb).equals("HH") || gir.getBandName(bb).equals("H/H")) {
                        thrHH = Float.parseFloat(args[bb + 1]);
                    } else if (gir.getBandName(bb).equals("HV") || gir.getBandName(bb).equals("H/V")) {
                        thrHV = Float.parseFloat(args[bb + 1]);
                    } else if (gir.getBandName(bb).equals("VH") || gir.getBandName(bb).equals("V/H")) {
                        thrVH = Float.parseFloat(args[bb + 1]);
                    } else if (gir.getBandName(bb).equals("VV") || gir.getBandName(bb).equals("V/V")) {
                        thrVV = Float.parseFloat(args[bb + 1]);
                    }
                }
                final float thresholdHH = thrHH;
                final float thresholdHV = thrHV;
                final float thresholdVH = thrVH;
                final float thresholdVV = thrVV;

                //read the land mask
                mask = new Vector<IMask>();
                //if (args.length > 5) {
                for (ILayer l : il.getLayers()) {
                    if (l instanceof IMask & l.getName().startsWith(args[numberofbands + 1])) {
                        mask.add((IMask) l);
                    }
                }
                //  if (args.length > 6) {
                //read the buffer distance
                bufferingDistance = Double.parseDouble(args[numberofbands + 2]);
                //}
                //}
                final float ENL = Float.parseFloat(args[numberofbands + 3]);

                /*
                float widthdisplay = Float.parseFloat(Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_SIZE));
                if (args.length > 3) {
                widthdisplay = Float.parseFloat(args[3]);
                }

                String colorString = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_COLOR);
                Color colordisplay = new Color(Integer.parseInt(colorString, 16));
                if (args.length > 4) {
                colordisplay = new Color(Integer.parseInt(args[4], 16));
                }
                 */

                // create new buffered mask with bufferingDistance using the mask in parameters
                final ArrayList<IMask> bufferedMask = new ArrayList<IMask>();
                for (IMask maskList : mask) {
                    bufferedMask.add(maskList.createBufferedMask(bufferingDistance));
                }
                final VDSAnalysis analysis = new VDSAnalysis((SarImageReader) gir, bufferedMask, ENL, thresholdHH, thresholdHV, thresholdVH, thresholdVV, this);
                new Thread(new Runnable() {

                    public void run() {

                        running = false;

                        // create K distribution
                        KDistributionEstimation kdist = new KDistributionEstimation(ENL);

                        DetectedPixels pixels = new DetectedPixels((SarImageReader) gir);
                        // list of bands
                        int numberofbands = gir.getNBand();
                        int[] bands = new int[numberofbands];
                        String[] thresholds = new String[numberofbands];
                        //management of the strings added at the end of the layer name in order to remember the used threshold
                        for (int bb = 0; bb < numberofbands; bb++) {
                            if (gir.getBandName(bb).equals("HH") || gir.getBandName(bb).equals("H/H")) {
                                thresholds[bb] = "" + thresholdHH;
                            } else if (gir.getBandName(bb).equals("HV") || gir.getBandName(bb).equals("H/V")) {
                                thresholds[bb] = "" + thresholdHV;
                            } else if (gir.getBandName(bb).equals("VH") || gir.getBandName(bb).equals("V/H")) {
                                thresholds[bb] = "" + thresholdVH;
                            } else if (gir.getBandName(bb).equals("VV") || gir.getBandName(bb).equals("V/V")) {
                                thresholds[bb] = "" + thresholdVV;
                            }
                        }
                        // compute detections for each band separately
                        for (int band = 0; band < numberofbands; band++) {
                            gir.setBand(band);
                            bands[band] = band;

                            message = "VDS: analysing image...";
                            if (numberofbands > 1) {
                                message = message + " for band " + gir.getBandName(band);
                            }

                            setCurrent(2);

                            analysis.run(kdist);
                            DetectedPixels banddetectedpixels = analysis.getPixels();
                            pixels.merge(banddetectedpixels);
                            if (pixels == null) {
                                done = true;
                                return;
                            }

                            boolean displaybandanalysis = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.DISPLAY_BANDS).equalsIgnoreCase("true");
                            if ((numberofbands < 1) || displaybandanalysis) {
                                message = "VDS: agglomerating detections for band " + gir.getBandName(band) + "...";
                                setCurrent(3);

                                String agglomerationMethodology = (Platform.getPreferences()).readRow(AGGLOMERATION_METHODOLOGY);
                                if (agglomerationMethodology.startsWith("d")) {
                                    // method distance used
                                    banddetectedpixels.agglomerate();
                                    banddetectedpixels.computeBoatsAttributes();
                                } else {
                                    // method neighbours used
                                    double neighbouringDistance;
                                    try {
                                        neighbouringDistance = Double.parseDouble((Platform.getPreferences()).readRow(NEIGHBOUR_DISTANCE));
                                    } catch (NumberFormatException e) {
                                        neighbouringDistance = 1.0;
                                    }
                                    int tilesize;
                                    try {
                                        tilesize = Integer.parseInt((Platform.getPreferences()).readRow(NEIGHBOUR_TILESIZE));
                                    } catch (NumberFormatException e) {
                                        tilesize = 200;
                                    }
                                    boolean removelandconnectedpixels = (Platform.getPreferences().readRow(REMOVE_LANDCONNECTEDPIXELS)).equalsIgnoreCase("true");
                                    banddetectedpixels.agglomerateNeighbours(neighbouringDistance, tilesize, removelandconnectedpixels, new int[]{band}, (bufferedMask != null) && (bufferedMask.size() != 0) ? bufferedMask.get(0) : null, kdist);
                                }

                                // look for Azimuth ambiguities in the pixels
                                message = "VDS: looking for azimuth ambiguities...";
                                setCurrent(4);
                                AzimuthAmbiguity azimuthAmbiguity = new AzimuthAmbiguity(banddetectedpixels.getBoats(), (SarImageReader) gir);//GeoImageReaderFactory.createReaderForName(gir.getFilesList()[0]).get(0));

                                ComplexEditVDSVectorLayer vdsanalysis = new ComplexEditVDSVectorLayer("VDS analysis " + gir.getBandName(band) + " " + thresholds[band], il, "point", createGeometricLayer(gir, banddetectedpixels));
                                boolean display = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.DISPLAY_PIXELS).equalsIgnoreCase("true");
                                if (!agglomerationMethodology.startsWith("d")) {
                                    vdsanalysis.addGeometries("thresholdaggregatepixels", new Color(0x0000FF), 1, SimpleVectorLayer.POINT, banddetectedpixels.getThresholdaggregatePixels(), display);
                                    vdsanalysis.addGeometries("thresholdclippixels", new Color(0x00FFFF), 1, SimpleVectorLayer.POINT, banddetectedpixels.getThresholdclipPixels(), display);
                                }
                                vdsanalysis.addGeometries("detectedpixels", new Color(0x00FF00), 1, SimpleVectorLayer.POINT, banddetectedpixels.getAllDetectedPixels(), display);
                                vdsanalysis.addGeometries("azimuthambiguities", new Color(0xFFD000), 5, SimpleVectorLayer.POINT, azimuthAmbiguity.getAmbiguityboatgeometry(), display);
                                if ((bufferedMask != null) && (bufferedMask.size() > 0)) {
                                    vdsanalysis.addGeometries("bufferedmask", new Color(0x0000FF), 1, SimpleVectorLayer.POLYGON, bufferedMask.get(0).getGeometries(), display);
                                }
                                vdsanalysis.addGeometries("tiles", new Color(0xFF00FF), 1, SimpleVectorLayer.LINESTRING, analysis.getTiles(), false);
                                // set the color and symbol values for the VDS layer
                                try {
                                    String widthstring = "";
                                    if (band == 0) {
                                        widthstring = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_SIZE_BAND_0);
                                    }
                                    if (band == 1) {
                                        widthstring = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_SIZE_BAND_1);
                                    }
                                    if (band == 2) {
                                        widthstring = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_SIZE_BAND_2);
                                    }
                                    if (band == 3) {
                                        widthstring = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_SIZE_BAND_3);
                                    }
                                    int displaywidth = Integer.parseInt(widthstring);
                                    vdsanalysis.setWidth(displaywidth);
                                } catch (NumberFormatException e) {
                                    vdsanalysis.setWidth(1);
                                }
                                try {
                                    String colorString = "";
                                    if (band == 0) {
                                        colorString = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_COLOR_BAND_0);
                                    }
                                    if (band == 1) {
                                        colorString = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_COLOR_BAND_1);
                                    }
                                    if (band == 2) {
                                        colorString = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_COLOR_BAND_2);
                                    }
                                    if (band == 3) {
                                        colorString = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_COLOR_BAND_3);
                                    }
                                    Color colordisplay = new Color(Integer.decode(colorString));
                                    vdsanalysis.setColor(colordisplay);
                                } catch (NumberFormatException e) {
                                    vdsanalysis.setColor(new Color(0x0000FF));
                                }
                                try {
                                    String symbolString = "";
                                    if (band == 0) {
                                        symbolString = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_SYMBOL_BAND_0);
                                    }
                                    if (band == 1) {
                                        symbolString = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_SYMBOL_BAND_1);
                                    }
                                    if (band == 2) {
                                        symbolString = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_SYMBOL_BAND_2);
                                    }
                                    if (band == 3) {
                                        symbolString = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_SYMBOL_BAND_3);
                                    }
                                    vdsanalysis.setDisplaysymbol(SimpleVectorLayer.symbol.valueOf(symbolString));
                                } catch (EnumConstantNotPresentException e) {
                                    vdsanalysis.setDisplaysymbol(SimpleVectorLayer.symbol.square);
                                }
                                il.addLayer(vdsanalysis);

                            }
                        }

                        // display merged results if there is more than one band
                        if (bands.length > 1) {
                            message = "VDS: agglomerating detections...";
                            setCurrent(3);

                            String agglomerationMethodology = (Platform.getPreferences()).readRow(AGGLOMERATION_METHODOLOGY);
                            if (agglomerationMethodology.startsWith("d")) {
                                // method distance used
                                pixels.agglomerate();
                                pixels.computeBoatsAttributes();
                            } else {
                                // method neighbours used
                                double neighbouringDistance;
                                try {
                                    neighbouringDistance = Double.parseDouble((Platform.getPreferences()).readRow(NEIGHBOUR_DISTANCE));
                                } catch (NumberFormatException e) {
                                    neighbouringDistance = 1.0;
                                }
                                int tilesize;
                                try {
                                    tilesize = Integer.parseInt((Platform.getPreferences()).readRow(NEIGHBOUR_TILESIZE));
                                } catch (NumberFormatException e) {
                                    tilesize = 200;
                                }
                                boolean removelandconnectedpixels = (Platform.getPreferences().readRow(REMOVE_LANDCONNECTEDPIXELS)).equalsIgnoreCase("true");
                                pixels.agglomerateNeighbours(neighbouringDistance, tilesize, removelandconnectedpixels, bands, (bufferedMask != null) && (bufferedMask.size() != 0) ? bufferedMask.get(0) : null, kdist);
                            }

                            // look for Azimuth ambiguities in the pixels
                            AzimuthAmbiguity azimuthAmbiguity = new AzimuthAmbiguity(pixels.getBoats(), (SarImageReader)gir);// GeoImageReaderFactory.createReaderForName(gir.getFilesList()[0]).get(0));

                            ComplexEditVDSVectorLayer vdsanalysis = new ComplexEditVDSVectorLayer("VDS analysis all bands merged", il, "point", createGeometricLayer(gir, pixels));
                            boolean display = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.DISPLAY_PIXELS).equalsIgnoreCase("true");
                            if (!agglomerationMethodology.startsWith("d")) {
                                vdsanalysis.addGeometries("thresholdaggregatepixels", new Color(0x0000FF), 1, SimpleVectorLayer.POINT, pixels.getThresholdaggregatePixels(), display);
                                vdsanalysis.addGeometries("thresholdclippixels", new Color(0x00FFFF), 1, SimpleVectorLayer.POINT, pixels.getThresholdclipPixels(), display);
                            }
                            vdsanalysis.addGeometries("detectedpixels", new Color(0x00FF00), 1, SimpleVectorLayer.POINT, pixels.getAllDetectedPixels(), display);
                            vdsanalysis.addGeometries("azimuthambiguities", new Color(0xFFD000), 5, SimpleVectorLayer.POINT, azimuthAmbiguity.getAmbiguityboatgeometry(), display);
                            if ((bufferedMask != null) && (bufferedMask.size() > 0)) {
                                vdsanalysis.addGeometries("bufferedmask", new Color(0x0000FF), 1, SimpleVectorLayer.POLYGON, bufferedMask.get(0).getGeometries(), display);
                            }
                            vdsanalysis.addGeometries("tiles", new Color(0xFF00FF), 1, SimpleVectorLayer.LINESTRING, analysis.getTiles(), false);
                            // set the color and symbol values for the VDS layer
                            try {
                                String widthstring = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_SIZE_BAND_MERGED);
                                int displaywidth = Integer.parseInt(widthstring);
                                vdsanalysis.setWidth(displaywidth);
                            } catch (NumberFormatException e) {
                                vdsanalysis.setWidth(1);
                            }
                            try {
                                String colorString = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_COLOR_BAND_MERGED);
                                Color colordisplay = new Color(Integer.decode(colorString));
                                vdsanalysis.setColor(colordisplay);
                            } catch (NumberFormatException e) {
                                vdsanalysis.setColor(new Color(0xFFAA00));
                            }
                            try {
                                String symbolString = Platform.getPreferences().readRow(VDSAnalysisConsoleAction.TARGETS_SYMBOL_BAND_MERGED);
                                vdsanalysis.setDisplaysymbol(SimpleVectorLayer.symbol.valueOf(symbolString));
                            } catch (EnumConstantNotPresentException e) {
                                vdsanalysis.setDisplaysymbol(SimpleVectorLayer.symbol.square);
                            }
                            il.addLayer(vdsanalysis);
                        }

                     //   gir.dispose();
                        done = true;
                    }
                }).start();
            }

            return true;
        }
    }

    public static GeometricLayer createGeometricLayer(GeoImageReader gir, DetectedPixels pixels) {
        GeometricLayer out = new GeometricLayer("point");
        out.setName("VDS Analysis");
        GeometryFactory gf = new GeometryFactory();
        long runid = System.currentTimeMillis();
        int count=0;
        for (double[] boat : pixels.getBoats()) {

            String[] schema = VDSSchema.getSchema();
            String[] types = VDSSchema.getTypes();

            Attributes atts = Attributes.createAttributes(schema, types);
            atts.set(VDSSchema.ID, count++);
            atts.set(VDSSchema.MAXIMUM_VALUE, boat[3]);
            atts.set(VDSSchema.TILE_AVERAGE, boat[4]);
            atts.set(VDSSchema.TILE_STANDARD_DEVIATION, boat[5]);
            atts.set(VDSSchema.THRESHOLD, boat[6]);
            atts.set(VDSSchema.RUN_ID, runid + "");
            atts.set(VDSSchema.NUMBER_OF_AGGREGATED_PIXELS, boat[7]);
            atts.set(VDSSchema.ESTIMATED_LENGTH, boat[8]);
            atts.set(VDSSchema.ESTIMATED_WIDTH, boat[9]);
            atts.set(VDSSchema.SIGNIFICANCE, (boat[3] - boat[4]) / (boat[4] * boat[5]));
            atts.set(VDSSchema.DATE, Timestamp.valueOf("" + gir.getMetadata(gir.TIMESTAMP_START)));
            atts.set(VDSSchema.VS, 0);
            //compute the direction of the vessel considering the azimuth of the image
            //result is between 0 and 180 degree
            double azimuth = gir.getImageAzimuth();
            double degree = boat[10] + 90 + azimuth;
            if (degree > 180) {
                degree = degree - 180;
            }
            if (degree < 0) {
                int i = 0;
            }
            atts.set(VDSSchema.ESTIMATED_HEADING, degree);
            out.put(gf.createPoint(new Coordinate(boat[1], boat[2])), atts);
        }
        return out;
    }
    /**
 *
 * @param gir
 * @param pixels
 * @param runid
 * @return
 */
    public static GeometricLayer createGeometricLayer(GeoImageReader gir, DetectedPixels pixels, long runid) {
        GeometricLayer out = new GeometricLayer("point");
        out.setName("VDS Analysis");
        GeometryFactory gf = new GeometryFactory();
        int count=0;
        for (double[] boat : pixels.getBoats()) {

            String[] schema = VDSSchema.getSchema();
            String[] types = VDSSchema.getTypes();

            Attributes atts = Attributes.createAttributes(schema, types);
            atts.set(VDSSchema.ID, count++);
            atts.set(VDSSchema.MAXIMUM_VALUE, boat[3]);
            atts.set(VDSSchema.TILE_AVERAGE, boat[4]);
            atts.set(VDSSchema.TILE_STANDARD_DEVIATION, boat[5]);
            atts.set(VDSSchema.THRESHOLD, boat[6]);
            atts.set(VDSSchema.RUN_ID, runid + "");
            atts.set(VDSSchema.NUMBER_OF_AGGREGATED_PIXELS, boat[7]);
            atts.set(VDSSchema.ESTIMATED_LENGTH, boat[8]);
            atts.set(VDSSchema.ESTIMATED_WIDTH, boat[9]);
            atts.set(VDSSchema.SIGNIFICANCE, (boat[3]-boat[4])/(boat[4]*boat[5]));
            try{
                atts.set(VDSSchema.DATE,Timestamp.valueOf(""+gir.getMetadata(gir.TIMESTAMP_START)));
            }catch (java.lang.IllegalArgumentException iae){
               //stores directly timestamp
               atts.set(VDSSchema.DATE,gir.getMetadata(gir.TIMESTAMP_START));
            }

            atts.set(VDSSchema.SIGNIFICANCE, (boat[3] - boat[4]) / (boat[4] * boat[5]));
            atts.set(VDSSchema.DATE, Timestamp.valueOf("" + gir.getMetadata(gir.TIMESTAMP_START)));
            atts.set(VDSSchema.VS, 0);
            //compute the direction of the vessel considering the azimuth of the image
            //result is between 0 and 180 degree
            double azimuth = gir.getImageAzimuth();
            double degree = boat[10] + 90 + azimuth;
            if (degree > 180) {
                degree = degree - 180;
            }
            if (degree < 0) {
                int i = 0;
            }
            atts.set(VDSSchema.ESTIMATED_HEADING, degree);
            out.put(gf.createPoint(new Coordinate(boat[1], boat[2])), atts);
        }

        return out;
    }
/**
 *
 * @return
 */
    public String getPath() {
        return "Analysis/VDS";
    }

    public List<Argument> getArgumentTypes() {
        Argument a1 = new Argument("algorithm", Argument.STRING, false, "k-dist");
        a1.setPossibleValues(new Object[]{"k-dist"});
        Argument a2 = new Argument("thresholdHH", Argument.FLOAT, false, 1.5);
        Argument a21 = new Argument("thresholdHV", Argument.FLOAT, false, 1.2);
        Argument a22 = new Argument("thresholdVH", Argument.FLOAT, false, 1.5);
        Argument a23 = new Argument("thresholdVV", Argument.FLOAT, false, 1.5);

        Argument a3 = new Argument("mask", Argument.STRING, true, "no mask choosen");
        ArrayList<String> vectors = new ArrayList<String>();
        for (ILayer l : Platform.getLayerManager().getLayers()) {
            if (l.isActive() && l instanceof IImageLayer) {
                il = (IImageLayer) l;
                break;
            }
        }

        if (il != null) {
            for (ILayer l : il.getLayers()) {
                if (l instanceof SimpleVectorLayer && !((SimpleVectorLayer) l).getType().equals(SimpleVectorLayer.POINT)) {
                    vectors.add(l.getName());
                }
            }
        }
        a3.setPossibleValues(vectors.toArray());
        Vector<Argument> out = new Vector<Argument>();

        Argument a4 = new Argument("Buffer (pixels)", Argument.FLOAT, false, (Platform.getPreferences()).readRow(BUFFERING_DISTANCE));

        //management of the different threshold in the VDS parameters panel
        out.add(a1);
        int numberofbands = il.getImageReader().getNBand();
        for (int bb = 0; bb < numberofbands; bb++) {
            if (il.getImageReader().getBandName(bb).equals("HH") || il.getImageReader().getBandName(bb).equals("H/H")) {
                out.add(a2);
            } else if (il.getImageReader().getBandName(bb).equals("HV") || il.getImageReader().getBandName(bb).equals("H/V")) {
                out.add(a21);
            } else if (il.getImageReader().getBandName(bb).equals("VH") || il.getImageReader().getBandName(bb).equals("V/H")) {
                out.add(a22);
            } else if (il.getImageReader().getBandName(bb).equals("VV") || il.getImageReader().getBandName(bb).equals("V/V")) {
                out.add(a23);
            }
        }

        out.add(a3);
        out.add(a4);
        if (il.getImageReader() instanceof SarImageReader) {
            Argument aEnl = new Argument("ENL", Argument.FLOAT, false, ENL.getFromGeoImageReader((SarImageReader) il.getImageReader()));
            out.add(aEnl);
        }

        return out;
    }

    public boolean isIndeterminate() {
        return this.indeterminate;
    }

    public boolean isDone() {
        return this.done;
    }

    public int getMaximum() {
        return this.maximum;
    }

    public int getCurrent() {
        return this.current;
    }

    public String getMessage() {
        return this.message;
    }

    public void setCurrent(int i) {
        current = i;
    }

    public void setMaximum(int size) {
        maximum = size;
    }

    public void setMessage(String string) {
        message = string;
    }

    public void setIndeterminate(boolean value) {
        indeterminate = value;
    }

    public void setDone(boolean value) {
        done = value;
    }
}
