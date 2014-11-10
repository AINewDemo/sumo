/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.geoimage.viewer.actions;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.geoimage.viewer.core.api.IConsoleAction;

/**
 *
 * @author leforth
 */
public abstract class ConsoleAction implements IConsoleAction {

    public void errorWindow(String message)
    {
        final String errorMessage = message;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

}
