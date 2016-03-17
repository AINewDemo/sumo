/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoimage.viewer.actions;

import java.io.File;
import java.util.List;
import java.util.Vector;

import org.geoimage.impl.GeotiffWriter;
import org.geoimage.viewer.core.SumoPlatform;
import org.geoimage.viewer.core.gui.manager.LayerManager;
import org.geoimage.viewer.widget.dialog.ActionDialog.Argument;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thoorfr
 */
public class ExportGeotiffAction extends SumoAbstractAction{
	private static org.slf4j.Logger logger=LoggerFactory.getLogger(ExportGeotiffAction.class);

	public ExportGeotiffAction(){
		super("exportGeotiff","Tools/Export Geotiff");
	}

    public String getDescription() {
        return "simple export of raster data in geotiff";
    }


    public boolean execute() {
    	String val=paramsAction.values().iterator().next();
        final File f = new File(val);
        new Thread(new Runnable() {
            public void run() {
                try {
                	notifyEvent(new SumoActionEvent(SumoActionEvent.STARTACTION,"Exporting file...",-1));
                    f.createNewFile();
                    GeotiffWriter.create(LayerManager.getIstanceManager().getCurrentImageLayer().getImageReader(), 0,f.getAbsolutePath());
                    notifyEvent(new SumoActionEvent(SumoActionEvent.ENDACTION,"Exporting file...",-1));
                } catch (Exception ex) {
                	logger.error(ex.getLocalizedMessage(),ex);
                }
            }
        }).start();
        return true;
    }

    public List<Argument> getArgumentTypes() {
        Argument arg1 = new Argument("File path", Argument.FILE, false, null,"File path");
        Vector<Argument> out = new Vector<Argument>();
        out.add(arg1);
        return out;
    }


}
