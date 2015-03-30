/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoimage.viewer.core;


import org.geoimage.viewer.core.api.GeoContext;
import org.geoimage.viewer.core.gui.manager.LayerManager;
import org.geoimage.viewer.core.layers.ConsoleLayer;
import org.geoimage.viewer.core.layers.image.Cache;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thoorfr
 */
public class GeoImageBatchMode {
	private static org.slf4j.Logger logger=LoggerFactory.getLogger(GeoImageBatchMode.class);

    private static GeoImageBatchMode instance;
    private LayerManager lm;
    private ConsoleLayer cl;
    private String info = null;
    private static String[] args;
    private boolean stop=false;

    

    public GeoImageBatchMode() {
        instance = this;
        lm = LayerManager.getIstanceManager();
        //lm.setName("Layers");
        //lm.setIsRadio(true);
        cl = new ConsoleLayer(null);
        lm.addLayer(cl);
        new Thread(new Runnable() {
            private GeoContext gc=new GeoContext(null);
            public void run() {
                gc.setDirty(false);
                while(!stop){
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                    	logger.error(ex.getMessage(),ex);
                    }
                    gc.setDirty(false);
                    lm.render(gc);
                }
            }
        }).start();
        if (args != null) {
            initLayers();
        }
        stop=true;
    }

    private void initLayers() {
        for (int i = 0; i < args.length; i++) {
            String c = args[i];
            if (c.equals("-f")) {
                if (i + 1 < args.length) {
                    try {
                        cl.runScript(args[i + 1]);
                    } catch (Exception ex) {
                    	logger.error(ex.getMessage(),ex);
                    }
                    i++;
                }

            }
        }
    }

    public void setInfo(String info) {
        if(info!=null && info.equals(this.info)){
            return;
        }
        this.info = info;
        logger.info(info);
    }

    public void setInfo(String information, final long timeout) {
        setInfo(information);
    }

    public LayerManager getLayerManager() {
        return lm;
    }

    public ConsoleLayer getConsole() {
        return cl;
    }
    
    public static void main(String[] args) {
        GeoImageBatchMode.args = args;
        new GeoImageBatchMode();
    }

    static GeoImageBatchMode getInstance() {
        return instance;
    }
}
