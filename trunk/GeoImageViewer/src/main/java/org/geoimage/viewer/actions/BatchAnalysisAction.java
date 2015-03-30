/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.geoimage.viewer.actions;

import java.util.List;

import org.geoimage.viewer.core.api.Argument;
import org.geoimage.viewer.core.api.iactions.AbstractConsoleAction;
import org.geoimage.viewer.core.batch.gui.BatchAnalysisGUI;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thoorfr
 */
public class BatchAnalysisAction extends AbstractConsoleAction{
	private static org.slf4j.Logger logger=LoggerFactory.getLogger(BatchAnalysisAction.class);

    public String getName() {
        return "batch";
    }
    public String getCommand() {
        return "batch";
    }

    public String getDescription() {
        return "use \"catalog\" to access the catalog of datasources";
    }

    public String getPath() {
        return "Tools/Image/Batch Analysis";
    }

    public boolean execute(String[] args) {
        try {
            BatchAnalysisGUI.main(null);
        } catch (Exception ex) {
            logger.error(ex.getLocalizedMessage(),ex);
        }
        return true;
    }

    public List<Argument> getArgumentTypes() {
        return null;
    }

}
