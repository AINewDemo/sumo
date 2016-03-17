/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoimage.viewer.actions;

import java.awt.Color;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.geoimage.def.GeoImageReader;
import org.geoimage.def.SarImageReader;
import org.geoimage.viewer.core.GeometryCollection;
import org.geoimage.viewer.core.SumoPlatform;
import org.geoimage.viewer.core.api.ilayer.ILayer;
import org.geoimage.viewer.core.factory.FactoryLayer;
import org.geoimage.viewer.core.gui.manager.LayerManager;
import org.geoimage.viewer.core.io.GenericCSVIO;
import org.geoimage.viewer.core.io.GmlIO;
import org.geoimage.viewer.core.io.SimpleShapefile;
import org.geoimage.viewer.core.io.SumoXMLWriter;
import org.geoimage.viewer.core.io.SumoXmlIOOld;
import org.geoimage.viewer.core.layers.image.ImageLayer;
import org.geoimage.viewer.core.layers.visualization.GenericLayer;
import org.geoimage.viewer.core.layers.visualization.vectors.MaskVectorLayer;
import org.geoimage.viewer.util.files.ArchiveUtil;
import org.geoimage.viewer.util.files.IceHttpClient;
import org.geoimage.viewer.widget.PostgisSettingsDialog;
import org.geoimage.viewer.widget.dialog.ActionDialog.Argument;
import org.geoimage.viewer.widget.dialog.DatabaseDialog;
import org.jrc.sumo.configuration.PlatformConfiguration;
import org.jrc.sumo.util.Constant;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Polygon;

/**
 *
 * @author thoorfr+ga
 * this class is called whenever you want to open one of the supported vector formats (shp, csv, xml, gml, query). It is opened as a new layer, linked to the active image.
 */
public class AddVectorConsoleAction extends SumoAbstractAction {
	private static org.slf4j.Logger logger=LoggerFactory.getLogger(AddVectorConsoleAction.class);

    private JFileChooser fd;
    private static String lastDirectory;

    private static final String paramFileType="File type";
    private static final String paramType="Type";
    private static final String paramSite="Remote";


    public AddVectorConsoleAction() {
    	super("vector","Import/Vector");

        if(SumoPlatform.getApplication().getConfiguration().getLastVector().equals("")){
        	lastDirectory = java.util.ResourceBundle.getBundle("GeoImageViewer").getString("image_directory");
        }else{
            lastDirectory = new File(SumoPlatform.getApplication().getConfiguration().getLastVector()).getAbsolutePath();
        }
    	fd = new JFileChooser(lastDirectory);

    }

    @Override
    public String getDescription() {
        return " Add a vector layer, using geotools connection.\n" +
                "Use \"add shp SimpleEditVector [file=/home/data/layer.shp]\" to add the layer.shp file to the image\n" +
                "Use \"add postgis SimpleEditVector [host=myhost.org dbname=database user=user password=pwd table=mytable]\" to add a postgis table to the image\n";
    }

    @Override
    public boolean execute() {

        if (super.paramsAction.size() < 1) {
            errorWindow("Wrong arguments for add vector action\n" + getDescription());
            done = true;
            return true;
        }
        done = false;
        try {
        	String message = "Adding Vector. Please wait...";
        	super.notifyEvent(new SumoActionEvent(SumoActionEvent.STARTACTION, message, -1));


        	GenericLayer lay=null;
        	File shp=null;
        	Color shpColor=Color.GREEN;
            if (getParamValue(paramFileType).equals("shp")) {
                int t=MaskVectorLayer.COASTLINE_MASK;
                if(getParamValue(paramType).equalsIgnoreCase("ice")){
                	try{
	                	t=MaskVectorLayer.ICE_MASK;
	                	shpColor=Color.WHITE;
	                	if(getParamValue(paramSite).equals("true")){
	                		GeoImageReader reader=SumoPlatform.getApplication().getCurrentImageReader();
	                		String cachePath=SumoPlatform.getApplication().getCachePath();
	                		File cache=new File(cachePath+File.separator+System.currentTimeMillis());
	                		if(!cache.exists())
	                			cache.mkdirs();
	                		File f=new IceHttpClient().downloadFromNoaa(reader.getImageDate(),cache.getAbsolutePath());

	                		if(f.getName().endsWith("zip")||f.getName().endsWith("gz")){
	                			ArchiveUtil.unZip(f.getAbsolutePath());
	                			File[] shpfiles=f.getParentFile().listFiles((java.io.FileFilter) pathname -> FilenameUtils.getExtension(pathname.getName()).equalsIgnoreCase("shp"));
	                			shp=shpfiles[0];
	                		}
	                	}else{
	                		shp=selectFile(new String[]{"shp","SHP"});
	                	}
                	}catch(Exception e){
                		shp=null;
                	}
                }else{
             		shp=selectFile(new String[]{"shp","SHP"});
            	}
                if(shp!=null){
                	GeometryCollection gl=loadShapeFile(shp,"");
                	lay=FactoryLayer.createMaskLayer(gl,t);

                	lay.setColor(shpColor);
                    lay.setWidth(5);
                }

            } else if (getParamValue(paramFileType).equals("postgis")) {
                addPostgis();

            } else if (getParamValue(paramFileType).equals("csv")) {
            	GeometryCollection positions=loadGenericCSV();
                lay=FactoryLayer.createComplexLayer(positions);
                done=LayerManager.addLayerInThread(lay);

            } else if (getParamValue(paramFileType).equals("sumo XML")) {
            /*    int t=MaskVectorLayer.COASTLINE_MASK;
                if(args[1].equalsIgnoreCase("ice"))
                	t=MaskVectorLayer.ICE_MASK;

            	GeometricLayer positions=loadSumoXML(args);
        		lay=FactoryLayer.createMaskLayer(positions,t);*/
            	File xml=selectFile(new String[]{"xml","XML"});
            	SumoXMLWriter xmlWR=new SumoXMLWriter(xml);
            	xmlWR.read();
            } else if (getParamValue(paramFileType).equals("gml")) {
            	GeometryCollection positions=loadGml();
                GenericLayer gl=FactoryLayer.createComplexLayer(positions);
                done=LayerManager.addLayerInThread(gl);

            } else if (getParamValue(paramFileType).equals("query")) {
                addQuery();
            }
            if(lay!=null)
            	done=LayerManager.addLayerInThread(lay);

        	super.notifyEvent(new SumoActionEvent(SumoActionEvent.ENDACTION, "", -1));

        } catch (Exception e) {
        	super.notifyEvent(new SumoActionEvent(SumoActionEvent.ACTION_ERROR, "Error importing layer", -1));
            errorWindow("Problem with import of vector data\n");
            done = true;
            return false;
        }
        return true;
    }

    /**
     *
     * @param args
     */
    private GeometryCollection loadGenericCSV() {
    	ImageLayer l=LayerManager.getIstanceManager().getCurrentImageLayer();
    	GeometryCollection positions=null;
    	if(l!=null){
    		try {
		    	GenericCSVIO csv=null;

	            File f=selectFile(new String[]{"csv","CSV"});
	            csv=new GenericCSVIO(f);

	            if(csv!=null){
	                positions = csv.readLayer();
	                if (positions.getProjection() != null) {
	                    positions = GeometryCollection.createImageProjectedLayer(positions, ((ImageLayer) l).getImageReader().getGeoTransform(), positions.getProjection());
	                }
	            }
	        } catch (Exception ex) {
	        	logger.error(ex.getMessage(), ex);
	        }
    	}
    	return positions;
    }

    private void addPostgis() {
        Map <String,String> config = null;
        String layer = "";
        if (paramsAction.size() == 1) {
            PostgisSettingsDialog ps = new PostgisSettingsDialog(null, true);
            ps.setVisible(true);
            if (!ps.isOk()) {
                done = true;
            }
            layer = ps.getTable();
            config = ps.getConfig();

        } else if (paramsAction.size() == 6) {
            config = new HashMap<String,String>();
            config.put("dbtype", "postgis");
            config.put("schema", "public");
            config.put("port", "5432");

            config.put("host", getParamValue("host").replace("host=", ""));
            config.put("dbname", getParamValue("dbname").replace("dbname=", ""));
            config.put("user", getParamValue("user").replace("user=", ""));
            config.put("password", getParamValue("password").replace("password=", ""));
            layer = getParamValue("table").replace("table=", "");
        }
        for (ILayer l : SumoPlatform.getApplication().getLayerManager().getLayers().keySet()) {
            if (l instanceof ImageLayer && l.isActive()) {
                try {
                    DatabaseDialog dialog = new DatabaseDialog(null, false);
                    Connection conn = DriverManager.getConnection("jdbc:postgresql://" + config.get("host").toString()
                            + ":" + config.get("port") + "/" + config.get("dbname"), config.get("user"), config.get("password"));
                    dialog.setConnection(conn);
                    dialog.setImageLayer((ImageLayer) l, getParamValue("dbname"));
                    dialog.setVisible(true);
                    done = true;
                    return;
                } catch (Exception ex) {
                    done = true;
                    logger.error(ex.getMessage(), ex);
                }
            }
        }
    }

    private void addQuery() {
        try {
            for (ILayer l : SumoPlatform.getApplication().getLayerManager().getLayers().keySet()) {
                if (l instanceof ImageLayer && l.isActive()) {
                    DatabaseDialog dialog = new DatabaseDialog(null, true);
                    Connection conn = DriverManager.getConnection("jdbc:h2:~/.sumo/VectorData;AUTO_SERVER=TRUE", "sa", "");
                    dialog.setConnection(conn);

                    //TODO:replace the args[1] with the correct param name
                    //dialog.setImageLayer((ImageLayer) l, args[1]);
                    dialog.setVisible(true);
                    done = true;
                }
            }
        } catch (SQLException ex) {
        	logger.error(ex.getMessage(), ex);
        }
    }

    /**
     *
     * @return
     */
    private File selectFile(String[] extsFilter){
    	File file=null;
    	FileFilter f =null;
    	if(extsFilter!=null & extsFilter.length>0){
	    	f = new FileNameExtensionFilter("Vector files",extsFilter);
	        fd.setFileFilter(f);
    	}


        int returnVal = fd.showOpenDialog(null);
        if(f!=null)
        	fd.removeChoosableFileFilter(f);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                lastDirectory = fd.getSelectedFile().getParent();
                SumoPlatform.getApplication().getConfiguration().updateConfiguration(Constant.PREF_LASTVECTOR, lastDirectory);
                file = fd.getSelectedFile();
            } catch (Exception ex) {
            	logger.error(ex.getMessage(), ex);
            }
        }
        return file;
    }



    /**
     *
     * @param args
     */
    private GeometryCollection loadShapeFile(File file,String name){//String[] args) {
        GeometryCollection gl=null;

        ImageLayer imgLayer=LayerManager.getIstanceManager().getCurrentImageLayer();
        if(imgLayer!=null){
        	try {
        		Polygon imageP=((SarImageReader)imgLayer.getImageReader()).getBbox(PlatformConfiguration.getConfigurationInstance().getLandMaskMargin(0));
        		long start=System.currentTimeMillis();
                 gl = SimpleShapefile.createIntersectedLayer(file,imageP,((SarImageReader)imgLayer.getImageReader()).getGeoTransform());
                long end=System.currentTimeMillis();
                System.out.println("Shapefile loaded in:"+(end-start));
                // if 5 args, set a specific name
                if (name!=null&&!name.equals("")) {
                    if (gl != null) {
                        gl.setName(name);
                    }
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        return gl;
    }

    /**
     *
     * @param args
     */
    private void addSimpleCSV(String[] args) {
    	try {
    		GeometryCollection positions=null;
    		GenericCSVIO csv=null;
	        if (args.length == 2) {
	            String file=args[1].split("=")[1];

           		csv=new GenericCSVIO(file);
	        } else {
	            int returnVal = fd.showOpenDialog(null);
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                    lastDirectory = fd.getSelectedFile().getParent();
	                    csv=new GenericCSVIO(fd.getSelectedFile());//,l.getImageReader().getGeoTransform());
	            }
	        }
            if(csv!=null){
                positions = csv.readLayer();
                if (positions.getProjection() != null) {
                	ImageLayer l=LayerManager.getIstanceManager().getCurrentImageLayer();
                	positions = GeometryCollection.createImageProjectedLayer(positions, l.getImageReader().getGeoTransform(), positions.getProjection());

                }
            }
	        GenericLayer gl=FactoryLayer.createComplexLayer(positions);
            done=LayerManager.addLayerInThread(gl);
    	} catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }
    }

    /**
     *
     * @param args
     */
    private GeometryCollection loadSumoXML(String[] args) {
    	GeometryCollection positions =null;
    	File sumoXml=null;
    	try{
	        if (args.length == 2) {
	        	sumoXml=new File(args[2].split("=")[3]);
	        } else {
	            int returnVal = fd.showOpenDialog(null);
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                    lastDirectory = fd.getSelectedFile().getParent();
	                    sumoXml=fd.getSelectedFile();
	            }
	        }
            ImageLayer l=LayerManager.getIstanceManager().getCurrentImageLayer();
	        SumoXmlIOOld old=new SumoXmlIOOld(sumoXml);
            positions = old.readLayer();
            if (positions.getProjection() != null) {
            	positions = GeometryCollection.createImageProjectedLayer(positions, ((ImageLayer) l).getImageReader().getGeoTransform(), positions.getProjection());
            }

    	} catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    	return positions;
    }

    /**
     *
     * @param args
     */
    private GeometryCollection loadGml() {
    	GeometryCollection positions =null;
    	try{
	        int returnVal = fd.showOpenDialog(null);
	        if (returnVal == JFileChooser.APPROVE_OPTION) {
	                lastDirectory = fd.getSelectedFile().getParent();
	                ImageLayer l=LayerManager.getIstanceManager().getCurrentImageLayer();

            		GmlIO gmlIO=new GmlIO(fd.getSelectedFile(),(SarImageReader)l.getImageReader());
                    positions = gmlIO.readLayer();
                    if (positions.getProjection() != null) {
                    	positions = GeometryCollection.createImageProjectedLayer(positions, ((ImageLayer) l).getImageReader().getGeoTransform(), positions.getProjection());
                    }
	        }
	    } catch (Exception ex) {
	        logger.error(ex.getMessage(), ex);
	        return null;
	    }
    	return positions;
    }


    @Override
    public List<Argument> getArgumentTypes() {
    	List<Argument> out = new ArrayList<Argument>();
        Argument a1 = new Argument(paramFileType, Argument.STRING, false, "image","Type");
        a1.setPossibleValues(new String[]{"csv", "shp", "gml", "sumo XML", "postgis", "query"});

        Argument a2 = new Argument(paramType, Argument.STRING, false, "coastline","Mask type");
        a2.setPossibleValues(new String[]{"coastline", "ice","other"});

        Argument a3= new Argument(paramSite,Argument.BOOLEAN,true,false,"Load from remote site");
        a3.setCondition(a2,"ice");


        out.add(a1);
        out.add(a2);
        out.add(a3);

        return out;
    }


}
