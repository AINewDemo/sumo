/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jrc.sumo.core.api;

import java.util.HashMap;
import java.util.List;

import org.jrc.sumo.core.api.layer.ILayer;

/**
 * Interface that codes the behavior of a Layer Manager which consists of a set of layers.
 * @author thoorfr
 */
public interface ILayerManager  {

    public void addLayer(ILayer layer);
    public void removeLayer(ILayer layer);
    public HashMap<ILayer,List<ILayer>> getLayers();
    public List<ILayer> getAllLayers();

}
