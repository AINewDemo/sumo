/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoimage.viewer.core.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.geoimage.def.GeoImageReader;
import org.geoimage.def.GeoTransform;
import org.geoimage.exception.GeoTransformException;
import org.geoimage.viewer.core.Platform;
import org.geoimage.viewer.core.api.Attributes;
import org.geoimage.viewer.core.api.IImageLayer;
import org.geoimage.viewer.core.layers.GeometricLayer;
import org.geoimage.viewer.core.layers.vectors.MaskVectorLayer;
import org.geoimage.viewer.util.PolygonOp;
import org.geoimage.viewer.widget.SelectParametersJDialog;
import org.h2.tools.Csv;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTWriter;

/**
 *
 * @author leforth based on gabban
 *
 *         this class manages the csv files. It uses a connection with the
 *         embedded H2 database for storing csv information, in a temporary
 *         table (TEMPCSV). It takes as input the latitude and longitude columns
 *         of the csv and the date column (optional).
 *
 */
public class GenericCSVIO extends AbstractVectorIO{
	private static org.slf4j.Logger logger = LoggerFactory
			.getLogger(GenericCSVIO.class);
	private File csvFile = null;
	private GeoTransform transform=null;
	private GeometricLayer glayer=null;

	public GenericCSVIO(File file,GeoTransform transform) {
		csvFile = file;
		this.transform=transform;
	}

	public GenericCSVIO(String filePath,GeoTransform transform) {
		csvFile = new File(filePath);
		this.transform=transform;
	}
	
	public void read() {
		glayer=readLayer();
	}
	
	/*public GeometricLayer readLayer() {
		GeometricLayer out = null;
		try {
			Connection conn = DriverManager.getConnection("jdbc:h2:~/.sumo/AIS;AUTO_SERVER=TRUE", "sa", "");
			Statement stat = conn.createStatement();
			stat.execute("DROP TABLE TEMPCSV IF EXISTS");
			String sql = null;

			ResultSet rs = Csv.getInstance().read(csvFile.getName(), null, null);
			ResultSetMetaData meta = rs.getMetaData();

			/* check if it is a sumo csv, based on Fix work
			if (meta.getColumnName(1).equals("type=point")) {
				GenericCSVIO csv = new GenericCSVIO(csvFile.getAbsolutePath());
				GeometricLayer gl = csv.read(reader);
				out = GeometricLayer.createImageProjectedLayer(gl,reader.getGeoTransform(), "EPSG:4326");

				return out;
			}*/
/*
			SelectParametersJDialog ff2 = new SelectParametersJDialog(meta);
			ff2.setVisible(true);
			sql = "create table tempcsv as select * from csvread('"	+ csvFile.getName() + "')";
			stat.execute(sql);

			String latName = ff2.getLatField();
			String lonName = ff2.getLonField();
			String dateName = "";
			if (ff2.getTimeField() != null) {
				dateName = ff2.getTimeField();
			}

			sql = "select * from tempcsv";
			stat.execute(sql);
			GeometricLayer gl = new GeometricLayer(MaskVectorLayer.POINT);

			gl.setName(csvFile.getName());
			//addCSVGeom("tempcsv", gl, latName, lonName, dateName,transform, reader.getWidth(),reader.getHeight());
			out = GeometricLayer.createImageProjectedLayer(gl,transform, "EPSG:4326");

			rs.close();
			stat.close();
			conn.close();
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}

		return out;
	}*/
	
	 public GeometricLayer readLayer() {
	        RandomAccessFile fss = null;
	        GeometricLayer out=null;
	        try {
	            fss = new RandomAccessFile(csvFile, "r");
	            String line = null;
	            String[] temp = fss.readLine().split(",");
	            String type = temp[0].split("=")[1];
	            out = new GeometricLayer(type);
	            out.setName(csvFile.getName().substring(0, csvFile.getPath().lastIndexOf(".")));
	            if (temp.length == 2) {
	                out.setProjection(temp[1].split("=")[1]);
	            }
	            line = fss.readLine();
	            String[] attributes = null;
	            if (line.equals("")) {
	                attributes = new String[]{};
	            } else {
	                attributes = line.split(",");
	            }
	            line = fss.readLine();
	            String[] types = null;
	            if (line.equals("")) {
	                types = new String[]{};
	            } else {
	                types = line.split(",");
	            }
	            while ((line = fss.readLine()) != null) {
	                Attributes atts = Attributes.createAttributes(attributes, types);
	                String[] val = line.split(",");
	                if (val.length != attributes.length + 1) {
	                    continue;
	                }
	                Geometry geom = parse(val[0]);
	                for (int i = 1; i < val.length; i++) {
	                    if (types[i - 1].equals("Date")) {
	                        try {
	                            atts.set(attributes[i - 1], Timestamp.valueOf(val[i]));
	                        } catch (Exception e) {
	                            atts.set(attributes[i - 1], null);
	                        }
	                    } else {
	                        if (types[i - 1].equals("Double")) {
	                            try {
	                                atts.set(attributes[i - 1], Double.valueOf(val[i]));
	                            } catch (Exception e) {
	                                atts.set(attributes[i - 1], Double.NaN);
	                            }
	                        } else {
	                            if (types[i - 1].equals("Integer")) {
	                                try {
	                                    atts.set(attributes[i - 1], Integer.valueOf(val[i]));
	                                } catch (Exception e) {
	                                    atts.set(attributes[i - 1], 0);
	                                }
	                            } else {
	                                atts.set(attributes[i - 1], val[i]);
	                            }

	                        }
	                    }
	                }
	                out.put(geom, atts);
	            }
	        } catch (Exception ex) {
	        	logger.error(ex.getMessage(), ex);
	        } finally {
	            try {
	                fss.close();
	            } catch (IOException ex) {
	            	logger.error(ex.getMessage(), ex);
	            }
	        }
	        return out;
	    }

	/**
     * 
     */
	public void save(File output,String projection,GeoTransform transform) {
		export(output,glayer,projection,transform);
	}
	public static void export(File output,GeometricLayer glayer,String projection,GeoTransform transform) {
		FileWriter fis=null;
		try {
			fis = new FileWriter(output);
			if (projection == null) {
				fis.write("x,y," + glayer.getSchema(',') + "\n");
			} else {
				fis.write("lat,lon," + glayer.getSchema(',') + "\n");
			}
			String[] schema = glayer.getSchema();
			for (Geometry geom : glayer.getGeometries()) {
				Coordinate[] pos = geom.getCoordinates();
				for (int i = 0; i < pos.length; i++) {
					if (projection == null) {
						fis.write(pos[i].x + "," + pos[i].y);
					} else {
						double[] temp;
						try {
							temp = transform.getGeoFromPixel(pos[i].x, pos[i].y);
							fis.write(temp[1] + "," + temp[0]);
						} catch (GeoTransformException e) {
							logger.warn("Can't write coordinates:"+ e.getMessage());
						}

					}
					if (schema.length != 0) {
						fis.write(",");
					}
					
					Attributes atts = glayer.getAttributes(geom);
					for (int ii = 0; ii < schema.length; ii++) {
						String s = "";
						if (atts.get(schema[ii]) != null) {
							s = "\""+atts.get(schema[ii]).toString()+"\"";
						}
						fis.write(s + (ii == schema.length - 1 ? "" : ","));
					}
					fis.write("\n");
				}
			}
			
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		}finally{
			try {
				fis.flush();
				fis.close();
			} catch (IOException e) {
			}
		}

	}

	public static void createSimpleCSV(GeometricLayer glayer, String file) throws Exception {
        FileWriter fw = new FileWriter(file);
        fw.append("geom," + glayer.getSchema(',') + "\n");
        WKTWriter wkt = new WKTWriter();
        for (Geometry geom : glayer.getGeometries()) {
            fw.append("\"" + wkt.writeFormatted(geom) + "\",");
            Attributes att = glayer.getAttributes(geom);
            if (att == null || att.getSchema().length==0) {
                fw.append("\n");
                continue;
            }
            for (int i = 0; i < att.getSchema().length; i++) {
                String key = att.getSchema()[i];
                fw.append(att.get(key) + "");
                if (i < att.getSchema().length - 1) {
                    fw.append(",");
                } else {
                    fw.append("\n");
                }
            }
        }
        fw.flush();
        fw.close();
    }
	
	
	
	private Geometry parse(String string) {
		if (string.endsWith(";")) {
			string = string.substring(0, string.length() - 1);
		}
		GeometryFactory gf = new GeometryFactory();
		String[] pos = string.split(";");
		if (pos.length == 1) {
			String[] point = pos[0].split(" ");
			return gf.createPoint(new Coordinate(Double.parseDouble(point[0]),
					Double.parseDouble(point[1])));
		} else {
			Vector<Coordinate> coords = new Vector<Coordinate>();
			for (String point : pos) {
				String[] cc = point.split(" ");
				coords.add(new Coordinate(Double.parseDouble(cc[0]), Double
						.parseDouble(cc[1])));
			}
			return gf.createPolygon(gf.createLinearRing(coords
					.toArray(new Coordinate[coords.size()])), null);
		}
	}

	private void addGenericCSV(String[] args) {
		try {

			if (args.length > 0) {
				String csvfilename = args[0];
				Connection conn = DriverManager.getConnection(
						"jdbc:h2:~/.sumo/AIS;AUTO_SERVER=TRUE", "sa", "");
				Statement stat = conn.createStatement();
				stat.execute("DROP TABLE TEMPCSV IF EXISTS");
				String sql = null;


				ResultSet rs = Csv.getInstance().read(args[0], null, null);
				ResultSetMetaData meta = rs.getMetaData();
				SelectParametersJDialog ff2 = new SelectParametersJDialog(meta);
				ff2.setVisible(true);
				sql = "create table tempcsv as select * from csvread('"	+ csvfilename + "')";
				stat.execute(sql);

				String latName = ff2.getLatField();
				String lonName = ff2.getLonField();
				String dateName = ff2.getTimeField();

				sql = "select * from tempcsv";
				stat.execute(sql);
				GeometricLayer gl = new GeometricLayer(MaskVectorLayer.POINT);
				gl.setName(csvFile.getName());
				IImageLayer l = Platform.getCurrentImageLayer();
				if (l != null) {
					GeoImageReader reader = l.getImageReader();
					addCSVGeom("tempcsv", gl, latName, lonName, dateName,
							reader.getGeoTransform(), reader.getWidth(),
							reader.getHeight());
					gl = GeometricLayer.createImageProjectedLayer(gl,
							((IImageLayer) l).getImageReader()
									.getGeoTransform(), "EPSG:4326");
				}
				rs.close();
				stat.close();
				conn.close();

			}

		} catch (SQLException | GeoTransformException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	public void addCSVGeom(String tableName, GeometricLayer gl, String lat,String lon, String date, GeoTransform gt, int width, int height)
			throws SQLException {
		Connection conn = DriverManager.getConnection(
				"jdbc:h2:~/.sumo/AIS;AUTO_SERVER=TRUE", "sa", "");
		Statement stat = conn.createStatement();
		String sql = "select * from " + tableName;
		ResultSet rset = stat.executeQuery(sql);
		ResultSetMetaData rsetM = rset.getMetaData();
		int numcols = rsetM.getColumnCount();
		String[] attributes = new String[numcols];
		String[] types = new String[numcols];
		for (int i = 0; i < numcols; i++) {
			attributes[i] = rsetM.getColumnName(i + 1);
			int type = rsetM.getColumnType(i + 1);
			switch (type) {
			case 12:
				types[i] = "String";
				break;
			case 93:
				types[i] = "Date";
				break;
			case 7:
				types[i] = "Double";
				break;
			}
			if (attributes[i].equals(date)) {
				types[i] = "Date";
			}

		}
		Attributes atts = Attributes.createAttributes(attributes, types);
		GeometryFactory gf = new GeometryFactory();
		Geometry geom = null;

		try {
			double[] x0;
			double[] x1;
			double[] x2;
			double[] x3;
			int margin = Integer.parseInt(java.util.ResourceBundle.getBundle(
					"GeoImageViewer").getString("SimpleShapeFileIO.margin"));
			x0 = gt.getGeoFromPixel(-margin, -margin);
			x2 = gt.getGeoFromPixel(margin + width, margin + height);
			x3 = gt.getGeoFromPixel(margin + width, -margin);
			x1 = gt.getGeoFromPixel(-margin, margin + height);

			Polygon imageP = PolygonOp.createPolygon(x0, x1, x2, x3, x0);

			int count = 0;
			while (rset.next()) {
				if (count == 0) {
					count++;
					for (int i = 0; i < numcols; i++) {
						try {
							Double.parseDouble(rset.getString(i + 1));
							types[i] = "Double";
						} catch (Exception e) {

						}
					}
				}
				atts = Attributes.createAttributes(attributes, types);
				for (int i = 0; i < numcols; i++) {
					if (attributes[i].equals(date)) {
						atts.set(attributes[i],
								Timestamp.valueOf(rset.getString(i + 1)));
					} else {
						if (types[i].equals("Double")) {
							atts.set(attributes[i],
									new Double(rset.getString(i + 1)));
						} else {
							atts.set(attributes[i], rset.getString(i + 1));
						}
					}
				}
				geom = gf.createPoint(new Coordinate(Double.parseDouble(rset
						.getString(lon)), Double.parseDouble(rset
						.getString(lat))));
				if (imageP.contains(geom)) {
					gl.put(geom, atts);
				}

			}
		} catch (Exception e) {
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					JOptionPane.showMessageDialog(null,
							"Problem with date format", "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			});
			System.out.println("Problem with date format");
		}
		stat.close();
		conn.close();
	}

	
	
}
