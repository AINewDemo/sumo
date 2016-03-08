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
                    SumoPlatform.setInfo("Exporting file...", -1);
                    f.createNewFile();
                    GeotiffWriter.create(LayerManager.getIstanceManager().getCurrentImageLayer().getImageReader(), 0,f.getAbsolutePath());
                } catch (Exception ex) {
                	logger.error(ex.getLocalizedMessage(),ex);
                }
                SumoPlatform.getApplication().setInfo("");
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

	@Override
	public boolean isIndeterminate() {
		return true;
	}

	@Override
	public int getMaximum() {
		return 0;
	}

	@Override
	public int getCurrent() {
		return 0;
	}



	@Override
	public void setCurrent(int i) {

	}

	@Override
	public void setMaximum(int size) {
		// TODO Auto-generated method stub

	}



	@Override
	public void setIndeterminate(boolean value) {
		// TODO Auto-generated method stub

	}
}
