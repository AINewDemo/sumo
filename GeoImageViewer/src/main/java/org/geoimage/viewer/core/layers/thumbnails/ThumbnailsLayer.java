/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoimage.viewer.core.layers.thumbnails;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLBase;

import org.geoimage.exception.GeoTransformException;
import org.geoimage.opengl.OpenGLContext;
import org.geoimage.viewer.core.Platform;
import org.geoimage.viewer.core.api.ILayer;
import org.geoimage.viewer.core.layers.GenericLayer;
import org.geoimage.viewer.core.layers.GeometricLayer;
import org.geoimage.viewer.core.layers.visualization.vectors.EditGeometryVectorLayer;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 *
 * @author thoorfr
 */
public class ThumbnailsLayer extends GenericLayer {

    private String id;
    public ThumbnailsManager tmanager;
    private BufferedImage overview;
    //private int[] size;
    private float contrast = 1;
    private float brightness = 0;
    private RescaleOp rescale = new RescaleOp(contrast, brightness, null);
    private ThumbnailsImageReader thumbReader;
    private float scale;

    public ThumbnailsLayer(ILayer parent, GeometricLayer glayer, String projection, String idColumnName, ThumbnailsManager tmanager) throws GeoTransformException {
    	super(parent,"",null,glayer);
        thumbReader = new ThumbnailsImageReader(tmanager);
        if (projection == null) {
            this.glayer = glayer;
        } else {
            this.glayer = GeometricLayer.createImageProjectedLayer(glayer, thumbReader.getTransformation(), projection);
        }
        this.id = idColumnName;
        this.tmanager = tmanager;
        this.overview = this.tmanager.getOverview();
        this.scale=thumbReader.getWidth()/(1f*thumbReader.getHeight())*(overview.getHeight()/(1f*overview.getWidth()));
        //this.size = this.tmanager.getImageSize();

        Platform.getLayerManager().addLayer(new EditGeometryVectorLayer(this,"analysis", this.glayer.getGeometryType(), this.glayer));
        setName("Thumbnails Image");
        super.init(parent);
    }

    @Override
    public void render(OpenGLContext context) {
        GL gl = context.getGL();
        gl.getGL2().glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
        BufferedImage temp=new BufferedImage(overview.getWidth(), overview.getHeight(), overview.getType());
        this.overview.copyData(temp.getRaster());

        BufferedImage buffer=rescale.filter(temp, temp);
        Texture texture = AWTTextureIO.newTexture(((GLBase)gl).getGLProfile(), buffer, true);
        //Texture texture = TextureIO.newTexture(rescale.filter(temp, temp), false);

        float tempscale=this.scale*context.getHeight()/context.getWidth();
        if(tempscale<1){
            bindTexture(gl, texture, 0, tempscale, 0, 1);
        }
        else{
            bindTexture(gl, texture, 0, 1, 0, tempscale);
        }
        texture.disable(gl);
        context.setX(0);
        context.setY(0);
        context.setZoom(Math.max(thumbReader.getWidth() / context.getWidth(),thumbReader.getHeight() / context.getHeight()));
        Platform.getLayerManager().render(context);
    }

    public BufferedImage get(Point position) {
        GeometryFactory gf = new GeometryFactory();
        com.vividsolutions.jts.geom.Point p = gf.createPoint(new Coordinate(position.x, position.y));
        for (Geometry temp : glayer.getGeometries()) {
            if (temp.isWithinDistance(p, 5 * thumbReader.getWidth() / 512f)) {
                BufferedImage image=tmanager.get(glayer.getAttributes(temp).get(id).toString());
                return rescale.filter(image,image);
            }
        }
        return null;
    }

    private void bindTexture(GL gl, Texture texture, float xmin, float xmax, float ymin, float ymax) {
        texture.enable(gl);
        texture.bind(gl);
        TextureCoords coords = texture.getImageTexCoords();
        gl.getGL2().glBegin(GL2.GL_QUADS);
        gl.getGL2().glTexCoord2f(coords.left(), coords.top());
        gl.getGL2().glVertex2f(xmin, 1 - ymin);
        gl.getGL2().glTexCoord2f(coords.right(), coords.top());
        gl.getGL2().glVertex2f(xmax, 1 - ymin);
        gl.getGL2().glTexCoord2f(coords.right(), coords.bottom());
        gl.getGL2().glVertex2f(xmax, 1 - ymax);
        gl.getGL2().glTexCoord2f(coords.left(), coords.bottom());
        gl.getGL2().glVertex2f(xmin, 1 - ymax);
        gl.getGL2().glEnd();
        texture.disable(gl);

    }

    @Override
    public String getDescription() {
        return "Thumbnails viewer";
    }

    public void setContrast(float value) {
        this.contrast = value;
        rescale = new RescaleOp(contrast, brightness, null);
    }

    public void setBrightness(float value) {
        this.brightness = value;
        rescale = new RescaleOp(contrast, brightness, null);
    }

    public float getContrast() {
        return contrast;
    }

    public float getBrightness() {
        return brightness;
    }

    public void setBand(int val) {
    }

    public int getNumberOfBands() {
        return 1;
    }

    public int getBand() {
        return 0;
    }

    public ThumbnailsImageReader getImageReader() {
        return this.thumbReader;
    }

    public void setMaximumCut(float value) {
    }

    public float getMaximumCut() {
        return 1;
    }

    public void level(int levelIncrease) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

	@Override
	public void dispose() {
	}

}
