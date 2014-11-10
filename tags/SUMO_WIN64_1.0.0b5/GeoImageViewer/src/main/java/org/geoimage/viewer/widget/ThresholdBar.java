/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoimage.viewer.widget;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.fenggui.ObservableLabelWidget;
import org.fenggui.Slider;
import org.fenggui.border.PlainBorder;
import org.fenggui.event.ISliderMovedListener;
import org.fenggui.event.SliderMovedEvent;
import org.fenggui.layout.RowLayout;
import org.fenggui.render.Binding;
import org.fenggui.render.Graphics;
import org.fenggui.render.ITexture;
import org.fenggui.util.Color;
import org.geoimage.viewer.core.Platform;
import org.geoimage.viewer.core.api.IThreshable;

/**
 *
 * @author thoorfr
 */
public class ThresholdBar extends TransparentWidget {

    private final Slider slider;
    private int[] histogram = null;
    private final IThreshable layer;
    private static String GOOGLE_CHART_API="Google Chart format";
    private static String NUM_HISTOGRAM_CLASSES="Number of Histogram Classes";

    public ThresholdBar(IThreshable layer) {
        super("Threshold");
        Platform.getPreferences().insertIfNotExistRow(GOOGLE_CHART_API,"http://chart.apis.google.com/chart?chf=c,lg,45,CCCCCC,0,999999,0.75|bg,s,CCCCCC&chm=B,76A4FB,0,0,0&chxt=x&chtt=Histogram&chs=300x200&cht=lc&chco=0077CC");
        Platform.getPreferences().insertIfNotExistRow(NUM_HISTOGRAM_CLASSES,"20");

        slider = new Slider(true);
        slider.setVisible(true);
        setLayoutManager(new RowLayout(false));
        this.layer=layer;

        
        
        addWidget(slider);
        slider.getAppearance().add(new PlainBorder(new Color(200, 200, 200, 50)));
    }

    public void addListener(ISliderMovedListener listener) {
        slider.addSliderMovedListener(listener);
    }

    public void removeListener(ISliderMovedListener listener) {
        slider.removeSliderMovedListener(listener);
    }

    @Override
    public void paint(Graphics g) {
        slider.getSliderButton().setVisible(!isTransparent());
        super.paint(g);

    }

    public static ThresholdBar createThresholdBar(final IThreshable layer) {
        ThresholdBar bar = new ThresholdBar(layer);
        final double min = layer.getMinimumThresh()-0.01;
        final double max = layer.getMaximumThresh()+0.01;
        int classes=Integer.parseInt(Platform.getPreferences().readRow(NUM_HISTOGRAM_CLASSES));
        bar.setHistogram(layer.getHistogram(classes));
        bar.slider.setValue((layer.getThresh()-min)/(max-min));
        bar.addListener(new ISliderMovedListener() {

            public void sliderMoved(SliderMovedEvent sliderMovedEvent) {
                layer.setThresh(min + (max - min) * sliderMovedEvent.getPosition());
                Platform.refresh();
            }
        });
        return bar;
    }

    /**
     * @return the histogram
     */
    public int[] getHistogram() {
        return histogram;
    }

    /**
     * @param histogram the histogram to set
     */
    public void setHistogram(int[] histogram) {
        this.histogram = histogram;
        BufferedImage buf = null;
        try {
            System.out.println(Platform.getPreferences().readRow(GOOGLE_CHART_API)+"&chxr=0,"+layer.getMinimumThresh()+","+layer.getMaximumThresh()+"&chd=t:"+getValues(histogram));
            buf = ImageIO.read(new URL(Platform.getPreferences().readRow(GOOGLE_CHART_API)+"&chxr=0,"+layer.getMinimumThresh()+","+layer.getMaximumThresh()+"&chd=t:"+getValues(histogram)));
        } catch (IOException ex) {
            Logger.getLogger(ThresholdBar.class.getName()).log(Level.SEVERE, null, ex);
        }
        addWidget(new ImageWidget(buf));
    }

    private String getValues(int[] array) {
        String out="";
        for(int d:array){
            out+=d+",";
        }
        return out.substring(0, out.length()-1);
    }

    class ImageWidget extends ObservableLabelWidget {

        private ITexture texture = null;
        protected Color backgroundColor = null;
        private Point imagePosition;
        private BufferedImage bufferedImage = null;

        public ImageWidget(BufferedImage image) {
            setSize(image.getWidth(), image.getHeight());
            bufferedImage = image;

            this.setExpandable(false);
            this.setShrinkable(false);
        }

        @Override
        public void paint(org.fenggui.render.Graphics g) {
            super.paint(g);
            if (texture == null) {
                if (bufferedImage != null) {
                    texture = Binding.getInstance().getTexture(bufferedImage);
                }
            }
            if (texture != null) {
                g.setColor(new Color(255, 255, 255, transparency));
                imagePosition = new Point(0, this.getHeight() - texture.getImageHeight());
                g.drawImage(texture, imagePosition.x, imagePosition.y);
            }
        }
    }
}



