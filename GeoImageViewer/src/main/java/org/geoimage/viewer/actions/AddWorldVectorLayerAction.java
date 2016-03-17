package org.geoimage.viewer.actions;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geoimage.def.SarImageReader;
import org.geoimage.viewer.core.GeometryCollection;
import org.geoimage.viewer.core.SumoPlatform;
import org.geoimage.viewer.core.factory.FactoryLayer;
import org.geoimage.viewer.core.gui.manager.LayerManager;
import org.geoimage.viewer.core.io.SimpleShapefile;
import org.geoimage.viewer.core.layers.image.ImageLayer;
import org.geoimage.viewer.core.layers.visualization.GenericLayer;
import org.geoimage.viewer.core.layers.visualization.vectors.MaskVectorLayer;
import org.geoimage.viewer.widget.dialog.ActionDialog.Argument;
import org.jrc.sumo.configuration.PlatformConfiguration;

import com.vividsolutions.jts.geom.Polygon;

/**
 *
 * @author thoorfr.
 * this class is called when you want to load a coast line for an active image. The land mask is based on the GSHHS shapefile which is situated on /org/geoimage/viewer/core/resources/shapefile/.
 *
 */
public class AddWorldVectorLayerAction extends SumoAbstractAction {
	private static Logger logger=LogManager.getLogger(AddWorldVectorLayerAction.class);

    public AddWorldVectorLayerAction() {
    	super("world","Import/Land mask");
    }
    public AddWorldVectorLayerAction(String name,String path) {
    	super(name,path);
    }

    public String getDescription() {
        return " Add a land mask layer";
    }

    public boolean execute() {
        done = false;
        String message= "Importing land mask from GSHHS shapefile...";
    	super.notifyEvent(new SumoActionEvent(SumoActionEvent.STARTACTION, message, -1));

        new Thread(new Runnable() {
            public void run() {
                try {
                	ImageLayer  l=LayerManager.getIstanceManager().getCurrentImageLayer();
                	if(l!=null){
                        try {
                        	File shape=new File(SumoPlatform.getApplication().getConfiguration().getDefaultLandMask());
                        	Polygon imageP=((SarImageReader)l.getImageReader()).getBbox(PlatformConfiguration.getConfigurationInstance().getLandMaskMargin(0));
                            GeometryCollection gl = SimpleShapefile.createIntersectedLayer(shape,imageP,((SarImageReader)l.getImageReader()).getGeoTransform());

                            int t=MaskVectorLayer.COASTLINE_MASK;
                            //if(paramsAction.get("data_type").equalsIgnoreCase("ice"))
                            //	t=MaskVectorLayer.ICE_MASK;
                    		GenericLayer lay=FactoryLayer.createMaskLayer(gl,t);


                            LayerManager.addLayerInThread(lay);
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                	}
                } catch (Exception e) {
                }
                notifyEvent(new SumoActionEvent(SumoActionEvent.ENDACTION,"",-1));
            }
        }).start();
        return true;
    }
/*
    public void addLayerInThread(final GeometricLayer layer, final ImageLayer il) {
        if(layer != null)
        {
            new Thread(new Runnable() {

                public void run() {
                    Platform.getLayerManager().addLayer(new EditGeometryVectorLayer(Platform.getCurrentImageLayer(),layer.getName(), layer.getGeometryType(), layer));
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
        return "adding world layer...";
    }

    public List<Argument> getArgumentTypes() {
        return null;
    }

    public void setCurrent(int i) {
    }

    public void setMaximum(int size) {
    }

    public void setIndeterminate(boolean value) {
    }

    public void setDone(boolean value) {
    }
}
