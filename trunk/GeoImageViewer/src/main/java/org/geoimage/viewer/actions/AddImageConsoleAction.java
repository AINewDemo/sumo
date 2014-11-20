/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoimage.viewer.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;

import org.geoimage.def.GeoImageReader;
import org.geoimage.factory.GeoImageReaderFactory;
import org.geoimage.impl.TiledBufferedImage;
import org.geoimage.utils.IProgress;
import org.geoimage.viewer.core.Platform;
import org.geoimage.viewer.core.api.Argument;
import org.geoimage.viewer.core.api.GeometricLayer;
import org.geoimage.viewer.core.api.IImageLayer;
import org.geoimage.viewer.core.factory.VectorIOFactory;
import org.geoimage.viewer.core.io.AbstractVectorIO;
import org.geoimage.viewer.core.io.SumoXmlIOOld;
import org.geoimage.viewer.core.layers.FastImageLayer;
import org.geoimage.viewer.core.layers.ThumbnailsLayer;
import org.geoimage.viewer.core.layers.image.CacheManager;
import org.geoimage.viewer.core.layers.thumbnails.ThumbnailsManager;

/**
 *
 * @author thoorfr
 * this class is called whenever we want to open an image. A new image consists in a new layer (SimpleVectorLayer).
 * thumbnails part need to be revised
 */
public class AddImageConsoleAction extends ConsoleAction implements IProgress {

    private JFileChooser fileChooser;
    private String lastDirectory;
    boolean done = false;
    private String message = "Adding Image. Please wait...";
    private static final String IMAGE_FOLDER = "Image Default Folder";
    private static final String USE_GEOLOCATIONCORRECTION = "Image - use geolocation correction file";

    public AddImageConsoleAction() {
        // create preference fields
        Platform.getPreferences().insertIfNotExistRow(AddImageConsoleAction.IMAGE_FOLDER, java.util.ResourceBundle.getBundle("GeoImageViewer").getString("image_directory"));
        Platform.getPreferences().insertIfNotExistRow(Platform.PREFERENCES_LASTIMAGE, "");
        Platform.getPreferences().insertIfNotExistRow(USE_GEOLOCATIONCORRECTION, "true");
        if(Platform.getPreferences().readRow(Platform.PREFERENCES_LASTIMAGE).equals("")){
            //AG set the default directory if no images have been opened before
            lastDirectory = Platform.getPreferences().readRow(AddImageConsoleAction.IMAGE_FOLDER);
        }else{
            lastDirectory = new File(Platform.getPreferences().readRow(Platform.PREFERENCES_LASTIMAGE)).getAbsolutePath();
        }
        fileChooser = new JFileChooser(lastDirectory);
    }

    @Override
    public String getName() {
        return "image";
    }

    @Override
    public String getDescription() {
        return " Add a vector layer, using geotools connection.\n"
                + " Use \"add image [file=/home/data/image.tiff]\" to add image.tiff\n";
    }

    @Override
    public boolean execute(final String[] args) {
        if (args.length == 0) {
            return true;
        }
        done = false;
        try {
            if (args[0].equals("image")) {
                addImage(args);


            } else if (args[0].startsWith("thumb")) {
                addThumbnails(args);
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorWindow("Problem opening file");
        }
        done = true;

        return true;
    }

    private void addImage(String[] args) {

        // set the done flag to false
        this.done = false;

        //part used by the batch mode
        if (args.length == 3) {
            String imagefile = "";
            String imagename = args[1].split("=")[1];
            final String imagenamefile = imagename.substring(imagename.lastIndexOf(File.separator) + 1);
            // check if a wildcard character is used
            if (imagename.contains("*")) {
                // find the most appropriate image
                File file = new File(imagename.substring(0, imagename.lastIndexOf(File.separator)));
                if (file != null) {
                    File filefiltered = listFiles(file, imagenamefile);
                    if (filefiltered != null) {
                        imagefile = filefiltered.getAbsolutePath();
                    } else {
                        imagefile = "";
                    }
                }
            } else {
                imagefile = imagename;
            }
            GeoImageReader temp = null;
            List<GeoImageReader> tempList = null;
            if (args[2].split("=")[1].equals("true")) {
                GeoImageReader gir1 = GeoImageReaderFactory.createReaderForName(imagefile).get(0);
                temp = new TiledBufferedImage(new File(CacheManager.getRootCacheInstance().getPath(), gir1.getFilesList()[0] + "/data"), gir1);
                if(temp!=null){
                	tempList=new ArrayList<GeoImageReader>();
                	tempList.add(temp);
                }
            } else {
            	tempList = GeoImageReaderFactory.createReaderForName(imagefile);
            	temp=tempList.get(0);
            }
            // save the file name in the preferences
            Platform.getPreferences().updateRow(Platform.PREFERENCES_LASTIMAGE, temp.getFilesList()[0]);

            if (tempList==null||tempList.isEmpty()) {
                this.done = true;
                this.setMessage("Could not open image file");
                final String filename = args[1].split("=")[1];
                errorWindow("Could not open image file\n" + filename);
            } else {
            	for(int i=0;i<tempList.size();i++){
            		temp=tempList.get(i);
	             
	                IImageLayer newImage = new FastImageLayer(Platform.getLayerManager(), temp);
	                Platform.getLayerManager().addLayer(newImage);
	                try {
	                    Thread.sleep(5000);
	                } catch (InterruptedException ex) {
	                    Logger.getLogger(AddImageConsoleAction.class.getName()).log(Level.SEVERE, null, ex);
	                }
	                
	                
            	}
            	try {
                    Platform.refresh();
                } catch (Exception ex) {
                    Logger.getLogger(AddImageConsoleAction.class.getName()).log(Level.SEVERE, null, ex);
                }
            	Platform.getConsoleLayer().execute("home");
            }

        } else {
            //part used by the graphical interface
            if (lastDirectory.equals(Platform.getPreferences().readRow(AddImageConsoleAction.IMAGE_FOLDER))) {
                if (fileChooser == null) {
                    fileChooser = new JFileChooser(lastDirectory);
                }
            } else {
                // if preference has changed, create a new File Chooser
                //lastDirectory = Platform.getPreferences().readRow(AddImageConsoleAction.IMAGE_FOLDER);
                fileChooser = new JFileChooser(lastDirectory);
            }

            int returnVal = fileChooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                lastDirectory = fileChooser.getSelectedFile().getAbsolutePath();
                GeoImageReader temp = null;
                List<GeoImageReader> tempList = null;
                if (args[1].equals("true")) {
                    GeoImageReader gir1 = GeoImageReaderFactory.createReaderForName(fileChooser.getSelectedFile().getAbsolutePath()).get(0);
                    temp = new TiledBufferedImage(new File(CacheManager.getCacheInstance(gir1.getName()).getPath(), gir1.getFilesList()[0] + "/data"), gir1);
                    if(temp!=null){
                    	tempList=new ArrayList<GeoImageReader>();
                    	tempList.add(temp);
                    }
                } else {
                	tempList = GeoImageReaderFactory.createReaderForName(fileChooser.getSelectedFile().getAbsolutePath());
                	temp=tempList.get(0);
                }
                if (temp == null) {
                    done = true;
                }
                for(int i=0;i<tempList.size();i++){
            		temp=tempList.get(i);
	                // save the file name in the preferences
	                Platform.getPreferences().updateRow(Platform.PREFERENCES_LASTIMAGE, temp.getFilesList()[0]);

	                Platform.getGeoContext().setX(0);
	                Platform.getGeoContext().setY(0);
	                //Platform.getGeoContext().setZoom(temp.getWidth() / Platform.getGeoContext().getWidth() + 1);
	                IImageLayer newImage = new FastImageLayer(Platform.getLayerManager(), temp);
	                Platform.getLayerManager().addLayer(newImage);
	                try {
	                    Thread.sleep(1000);
	                } catch (InterruptedException ex) {
	                    Logger.getLogger(AddImageConsoleAction.class.getName()).log(Level.SEVERE, null, ex);
	                }
	                Platform.getConsoleLayer().execute("home");

	                try {
	                    Platform.refresh();
	                } catch (Exception ex) {
	                    Logger.getLogger(AddImageConsoleAction.class.getName()).log(Level.SEVERE, null, ex);
	                }
                }
            } else {
                done = true;
            }
        }

        //AGabban added the loading of the coast mask
        /*new Thread(new Runnable() {

        public void run() {
        try {
        for (ILayer l : Platform.getLayerManager().getLayers()) {
        if (l instanceof IImageLayer & l.isActive()) {
        try {
        URL url = this.getClass().getResource("/org/geoimage/viewer/core/resources/shapefile/Global GSHHS Land Mask.shp");
        Map config=new HashMap();
        config.put("url", url);
        VectorIO shpio = VectorIO.createVectorIO(VectorIO.SIMPLE_SHAPEFILE, config, ((IImageLayer) l).getImageReader());
        GeometricLayer gl = shpio.read();
        addLayerInThread(gl, (IImageLayer) l);
        } catch (Exception ex) {
        Logger.getLogger(AddWorldVectorLayerAction.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
        }
        } catch (Exception e) {
        }
        }
        }).start();*/

    }

 /*   public void addLayerInThread(final GeometricLayer layer, final IImageLayer il) {
        if (layer != null) {
            new Thread(new Runnable() {

                public void run() {
                    il.addLayer(new SimpleVectorLayer(layer.getName(), il, layer.getGeometryType(), layer));
                    done = true;
                }
            }).start();
        } else {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    JOptionPane.showMessageDialog(null, "Empty layer, not added to layers", "Warning", JOptionPane.ERROR_MESSAGE);
                }
            });
            done = true;
        }
    }*/
    //AGabban end of the new part


    private void addThumbnails(String[] args) {

        AbstractVectorIO csv = null;
        if (args.length == 2) {
            //csv = new SimpleCSVIO(args[1].split("=")[1]);
        } else {
            int returnVal = fileChooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                lastDirectory = fileChooser.getSelectedFile().getParent();
                Map config = new HashMap();
                config.put(SumoXmlIOOld.CONFIG_FILE, fileChooser.getSelectedFile().getAbsolutePath());
                csv = VectorIOFactory.createVectorIO(VectorIOFactory.SUMO_OLD, config);
            }
        }
        if(csv!=null){
	        GeometricLayer positions = csv.read(null);
	        if (positions.getProjection() == null) {
	            Platform.getLayerManager().addLayer(new ThumbnailsLayer(Platform.getLayerManager(), positions, null, "id", new ThumbnailsManager(lastDirectory)));
	        } else {
	            ThumbnailsLayer tm = new ThumbnailsLayer(Platform.getLayerManager(), positions, positions.getProjection(), "id", new ThumbnailsManager(lastDirectory));
	            Platform.getLayerManager().addLayer(tm);
	        }
        }    
        try {
            Platform.refresh();
        } catch (Exception ex) {
            Logger.getLogger(AddImageConsoleAction.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public String getPath() {
        return "Import/Image";
    }

    public boolean isIndeterminate() {
        return true;
    }

    public boolean isDone() {
        return done;
    }

    public int getMaximum() {
        return 1;
    }

    public int getCurrent() {
        return 1;
    }

    public String getMessage() {
        return this.message;
    }

    public List<Argument> getArgumentTypes() {
        Argument a1 = new Argument("data type", Argument.STRING, false, "image");
        //AG import thumbnails removed
        //a1.setPossibleValues(new String[]{"image", "thumbnails"});
        a1.setPossibleValues(new String[]{"image"});
        Vector<Argument> out = new Vector<Argument>();
        out.add(a1);

        Argument a2 = new Argument("Local Buffer the raster data", Argument.BOOLEAN, false, "buffer");
        out.add(a2);

        return out;
    }

    public void setCurrent(int i) {
    }

    public void setMaximum(int size) {
    }

    public void setMessage(String string) {
    }

    public void setIndeterminate(boolean value) {
    }

    public void setDone(boolean value) {
    }

    public File listFiles(File file, String filter) {
        File[] entries = file.listFiles();

        for (File listfile : entries) {
            if (listfile.isDirectory()) {
                File filteredfile = null;
                filteredfile = listFiles(listfile, filter);
                if (filteredfile != null) {
                    return filteredfile;
                }
            } else {
                String listfilename = listfile.getName();
                String[] strings = filter.split("\\*");
                int found = 0;
                int index = 0;
                for (String string : strings) {
                    if (index == 0) {
                        if (listfilename.startsWith(string)) {
                            found++;
                            listfilename = listfilename.substring(string.length());
                        } else {
                            break;
                        }
                    } else {
                        if (index == strings.length) {
                            if (listfilename.endsWith(string)) {
                                found++;
                            } else {
                                break;
                            }
                        } else {
                            if (listfile.getName().contains(string)) {
                                found++;
                                listfilename = listfilename.substring(listfilename.indexOf(string) + string.length());
                            } else {
                                break;
                            }
                        }
                    }
                    index++;
                }
                if (found == strings.length) {
                    return listfile;
                }
            }
        }
        return null;
    }
}
