/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoimage.viewer.core.io;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoimage.analysis.VDSSchema;
import org.geoimage.def.GeoImageReader;
import org.geoimage.def.GeoMetadata;
import org.geoimage.def.GeoTransform;
import org.geoimage.viewer.core.api.Attributes;
import org.geoimage.viewer.core.api.GeometricLayer;
import org.geoimage.viewer.core.api.VDSFields;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 *
 * @author thoorfr
 */
public class GmlIO extends AbstractVectorIO {
    public static String CONFIG_FILE = "file";

    public GmlIO(){
    }
    
    public GeometricLayer read(GeoImageReader reader) {
        try {
            GeometricLayer layer = new GeometricLayer(GeometricLayer.POINT);
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new File((String) config.get(CONFIG_FILE)));

            GeometryFactory gf = new GeometryFactory();
            String[] schema = VDSFields.getSchema();
            String[] types = VDSFields.getTypes();
            Element root = doc.getRootElement();
                       
            if (root != null) {

                layer.setProjection("EPSG:4326");
                layer.setName(new File((String) config.get(CONFIG_FILE)).getName());
                for (Object obj : root.getChildren()) {
                    if (obj instanceof Element) {
                        Element feature = (Element) obj;
                                                                     
                        if (feature.getName().equals("featureMember")) {
                            Namespace vd=Namespace.getNamespace("vd", "http://cweb.ksat.no/cweb/schema/vessel");
                            Namespace gml=Namespace.getNamespace("gml", "http://www.opengis.net/gml");
                            Element vessel = feature.getChild("feature",vd).getChild("vessel",vd);
                            Attributes atts = Attributes.createAttributes(schema, types);
                            String point[] = vessel.getChild("Point",gml).getChild("pos",gml).getText().split(" ");
                            double lon = Double.parseDouble(point[0]);                            
                            double lat = Double.parseDouble(point[1]);
                            Geometry geom = gf.createPoint(new Coordinate(lat, lon));
                            layer.put(geom, atts);
                            try {
                                atts.set(VDSSchema.ID, Double.parseDouble(feature.getChild("feature",vd).getChild("name",gml).getValue()));
                                atts.set(VDSSchema.ESTIMATED_LENGTH, Double.parseDouble(vessel.getChild("length",vd).getValue()));
                                atts.set(VDSSchema.ESTIMATED_WIDTH, Double.parseDouble(vessel.getChild("beam",vd).getValue()));
                                atts.set(VDSSchema.ESTIMATED_HEADING, Double.parseDouble(vessel.getChild("heading",vd).getValue()));
                            } catch (Exception e) {
                                //e.printStackTrace();
                            }
                        }
                    }
                }
            }
            return layer;
        } catch (Exception ex) {
            Logger.getLogger(SumoXmlIOOld.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void save(GeometricLayer glayer, String projection,GeoImageReader gir) {
    	GeoTransform transform=gir.getGeoTransform();
    	
        Namespace gml=Namespace.getNamespace("gml", "http://www.opengis.net/gml");
        Namespace vd=Namespace.getNamespace("vd", "http://cweb.ksat.no/cweb/schema/vessel");
        Namespace sat=Namespace.getNamespace("sat", "http://cweb.ksat.no/cweb/schema/satellite");
        Namespace xsi=Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        Namespace xmls=Namespace.getNamespace("http://www.w3.org/1999/xlink");
        //Namespace schemaLocation=Namespace.getNamespace("schemaLocation", "http://cweb.ksat.no/cweb/schema/vessel http://cweb.ksat.no/cweb/schema/vessel/vd.xsd");
        Attribute schemaLocation=new Attribute("schemaLocation", "http://cweb.ksat.no/cweb/schema/vessel http://cweb.ksat.no/cweb/schema/vessel/vd.xsd",xsi);

        Element root = new Element("featureCollection", vd);
        root.addNamespaceDeclaration(xmls);
        root.addNamespaceDeclaration(gml);
        root.addNamespaceDeclaration(sat);
        root.addNamespaceDeclaration(xsi);
        root.setAttribute(schemaLocation);
        int c = 1;
        for (Geometry geom : glayer.getGeometries()) {
            Attributes att = glayer.getAttributes(geom);
            String[] atts = att.getSchema();
            Element featureMember = new Element("featureMember", gml);
            double[] posA = transform.getGeoFromPixel(geom.getCoordinate().x, geom.getCoordinate().y, "EPSG:4326");
            Element feature = new Element("feature", vd);
            Element name = new Element("name", gml);
            name.addContent(c+"");
            c++;
            Element vessel = new Element("vessel", vd);
            Element point = new Element("Point", gml);
            point.setAttribute("srsName","EPSG:4326" );
            Element pos = new Element("pos", gml);
            pos.addContent(posA[1]+" "+posA[0]);//lat, lon
            point.addContent(pos);
            Element vType = new Element("vesselType", vd);
            vType.addContent("ship");
            Element length = new Element("length", vd);
            Element width = new Element("beam", vd);
            Element heading = new Element("heading", vd);
            for (int i = 0; i < atts.length; i++) {
                if(atts[i].compareTo(VDSSchema.ESTIMATED_LENGTH)==0) length.addContent(att.get(atts[i]).toString());
                if(atts[i].compareTo(VDSSchema.ESTIMATED_WIDTH)==0) width.addContent(att.get(atts[i]).toString());
                if(atts[i].compareTo(VDSSchema.ESTIMATED_HEADING)==0) heading.addContent(att.get(atts[i]).toString());
            }
            vessel.addContent(point);
            vessel.addContent(vType);
            vessel.addContent(length);
            vessel.addContent(width);
            vessel.addContent(heading);
            feature.addContent(name);
            feature.addContent(vessel);
            featureMember.addContent(feature);
            root.addContent(featureMember);
        }

        Element source = new Element("source", sat);
        Element prdID = new Element("productID", sat);
        Element satellite = new Element("satellite", sat);
        satellite.setText("" + gir.getMetadata(GeoMetadata.SATELLITE));
        source.addContent(satellite);
        Element direction = new Element("direction", sat);
        direction.setText("" + gir.getMetadata(GeoMetadata.ORBIT_DIRECTION));
        source.addContent(direction);
        Element resolutionRange = new Element("resolution", sat);
        resolutionRange.setText("" + gir.getMetadata(gir.RANGE_SPACING));
        source.addContent(resolutionRange);
        Element time = new Element("startTime", sat);
        String date = "" +gir.getMetadata(GeoMetadata.TIMESTAMP_START);
        date = date.replace(" ", "T");
        date = date+"Z";
        time.setText(date);
        source.addContent(time);
        time = new Element("stopTime", sat);
        date = "" +gir.getMetadata(GeoMetadata.TIMESTAMP_STOP);
        date = date.replace(" ", "T");
        date = date+"Z";
        time.setText(date);
        source.addContent(time);
        Element coorners = new Element("cornerPoint", sat);
        coorners.setAttribute("srsName", "EPSG:4326");
        double[] topLeft = gir.getGeoTransform().getGeoFromPixel(0, 0, "EPSG:4326");
        double[] topRight = gir.getGeoTransform().getGeoFromPixel(gir.getWidth(), 0, "EPSG:4326");
        double[] bottomLeft = gir.getGeoTransform().getGeoFromPixel(0, gir.getHeight(), "EPSG:4326");
        double[] bottomRight = gir.getGeoTransform().getGeoFromPixel(gir.getWidth(), gir.getHeight(), "EPSG:4326");
        Element pos = new Element("pos", gml);
        pos.addContent(topLeft[1] +" "+topLeft[0]);
        coorners.addContent(pos);
        source.addContent(coorners);
        coorners = new Element("cornerPoint", sat);
        pos = new Element("pos", gml);
        pos.addContent(topRight[1] +" "+topRight[0]);
        coorners.addContent(pos);
        source.addContent(coorners);
        coorners = new Element("cornerPoint", sat);
        pos = new Element("pos", gml);
        pos.addContent(bottomLeft[1] +" "+bottomLeft[0]);
        coorners.addContent(pos);
        source.addContent(coorners);
        coorners = new Element("cornerPoint", sat);
        pos = new Element("pos", gml);
        pos.addContent(bottomRight[1] +" "+bottomRight[0]);
        coorners.addContent(pos);
        source.addContent(coorners);
        root.addContent(source);
        Element vendor = new Element("dataSource", vd);
        vendor.addContent("JRC-SUMO");
        root.addContent(vendor);

        org.jdom.Document doc = new org.jdom.Document(root);
        XMLOutputter serializer = new XMLOutputter();
        serializer.setFormat(Format.getPrettyFormat());
        // System.out.println(serializer.outputString(doc));
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter((String) config.get(CONFIG_FILE)));
            out.write(serializer.outputString(doc));
            out.close();
        } catch (IOException e) {
        }
    }


    
}
