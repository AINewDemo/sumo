/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoimage.viewer.core.layers.image;

import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.geoimage.def.GeoImageReader;
import org.geoimage.def.SarImageReader;
import org.geoimage.impl.TiledBufferedImage;
import org.geoimage.impl.cosmo.AbstractCosmoSkymedImage;
import org.geoimage.opengl.OpenGLContext;
import org.geoimage.viewer.core.SumoPlatform;
import org.geoimage.viewer.core.api.ilayer.ILayer;
import org.jrc.sumo.util.Constant;
import org.slf4j.LoggerFactory;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

/**
 *
 * @author thoorfr
 */
public class ImageLayer implements ILayer  {
	protected String name = "";
	protected boolean active = true;
	protected boolean isRadio = false;
	protected String type;
	protected ILayer parent=null;
	
	class TileInfo{
		String idTile;
		String path;
		BufferedImage pngImage;
		
		public TileInfo(String id,String absPath,BufferedImage img) {
			idTile=id;
			path=absPath;
			pngImage=img;
		}
	}

	
	
	class ServiceTile implements  Callable<TileInfo> {
		private File tileFile;
		private int level;
		private int xTile;
		private int yTile;
		private String idTile;

		public ServiceTile(File initfile,String idTile, int level, int xTile, int yTile){
			super();
			this.tileFile=initfile;
			this.xTile=xTile;
			this.yTile=yTile;
			this.level=level;
			this.idTile=idTile;
		}


        public TileInfo call() {

            GeoImageReader gir2 = activeGir;
         /*   String next=new StringBuilder().append(level)
            		.append(" ").append(getBandFolder(activeBand))
            		.append(" ").append(xTile)
            		.append(" ").append(yTile).toString();*/
            if (gir2 == null) {
                return new TileInfo(idTile, tileFile.getAbsolutePath(), null);
            }
            try {
            	int x=xTile * (1 << level) * Constant.TILE_SIZE_IMG_LAYER - xpadding;
            	int y=yTile * (1 << level) * Constant.TILE_SIZE_IMG_LAYER - ypadding;
            	float zoom=(1 << level);
                final BufferedImage out = createImage(gir2, x,y, Constant.TILE_SIZE_IMG_LAYER, Constant.TILE_SIZE_IMG_LAYER, zoom);

                ImageIO.write(out, "png", tileFile);
                return new TileInfo(idTile, tileFile.getAbsolutePath(), out);
            } catch (Exception ex) {
                logger.error(ex.getMessage(),ex);
            }finally{
            	//imagePool.release(gir2);
            }
            return new TileInfo(idTile, tileFile.getAbsolutePath(), null);
        }
        /**
        *
        * @param gir
        * @param x
        * @param y
        * @param width
        * @param height
        * @param zoom
        * @return
        */
       private BufferedImage createImage(GeoImageReader gir, int x, int y, int width, int height, float zoom) {
           BufferedImage bufferedImage = new BufferedImage(width, height, gir.getType(true));

           int[] nat;
           WritableRaster raster = bufferedImage.getRaster();

           // Put the pixels on the raster.
           nat = gir.readAndDecimateTile(x, y,
           		(int) (width * zoom),
           		(int) (height * zoom),
           		width, height,((SarImageReader)gir).getWidth(),
           		((SarImageReader)gir).getHeight() ,true,activeBand);

           raster.setPixels(0, 0, width, height, nat);
           return bufferedImage;
       }
    }




	private static org.slf4j.Logger logger=LoggerFactory.getLogger(ImageLayer.class);

    private GeoImageReader activeGir;
    private HashMap<String, Float> contrast = new HashMap<String, Float>();
    private float brightness = 0;
    private int activeBand;
    private int xpadding;
    private int ypadding;
    private TextureCacheManager tcm;
    private List<String> submitedTiles;

    private List<Future<TileInfo>> futures = new ArrayList<Future<TileInfo>>();
    private int mylevel = -1;
    private RescaleOp rescale = new RescaleOp(1f, brightness, null);
    private boolean disposed = false;
    private int maxCut = 1;
    private int increaseLevel = 0;
    private int currentSize = 0;
    private int curlevel;
    private int levels;
    private boolean torescale = false;
    private int maxlevels;

    private int realTileSizeX=0;
    private int realTileSizeY=0;
    private int horizontalTilesImage=0;
    private int verticalTilesImage=0;

    private ThreadPoolExecutor poolExcutorService;

    private int arrayReadTilesOrder[][]=null ;
    private int maxnumberoftiles = 7;

	ImageReader pngReader=null;
	int nThreads=1;
	OpenGLContext context;
	

    /**
     *
     * @param gir
     */
    public ImageLayer(GeoImageReader gir) {
    	Iterator<ImageReader> iReader=ImageIO.getImageReadersByFormatName("png");
		pngReader=(ImageReader)iReader.next();

        this.activeGir = gir;
        setName(gir);
        activeBand = 0;
        
        int nThreads=Runtime.getRuntime().availableProcessors();
        poolExcutorService = new ThreadPoolExecutor(1,nThreads,100, TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>());//,new ThreadPoolExecutor.DiscardOldestPolicy());

        submitedTiles = new ArrayList<String>();

        
        levels = (int) (Math.sqrt(Math.max(gir.getWidth() / Constant.TILE_SIZE_DOUBLE, gir.getHeight() / Constant.TILE_SIZE_DOUBLE))) + 1;
        maxlevels = (int) (Math.sqrt(Math.max(gir.getWidth() / (Constant.TILE_SIZE_DOUBLE*2), gir.getHeight() / (Constant.TILE_SIZE_DOUBLE*2)))) +1;  //massimo livello di zoom
        curlevel = levels;

        //TODO Understand the meaning of this values for padding
        xpadding = (((1 << levels) << 8) - gir.getWidth()) / 2;  // is equal to(int)((Math.pow(2,levels+8)- gir.getWidth())/2);
        ypadding = (((1 << levels) << 8) - gir.getHeight()) / 2; //			   (int)((Math.pow(2,levels+8)- gir.getHeight())/2);

        String strmaxBuffer = SumoPlatform.getApplication().getConfiguration().getMaxTileBuffer();
        int maxBuffer = Integer.parseInt(strmaxBuffer);
        tcm = new TextureCacheManager(maxBuffer);
        
        setInitialContrast();
        maxnumberoftiles = SumoPlatform.getApplication().getConfiguration().getMaxNumOfTiles();
        createMatrixTileOrder();

        int tileSize = (int)(Constant.TILESIZE / gir.getPixelsize()[0]);
        if(tileSize < Constant.TILESIZEPIXELS) tileSize = Constant.TILESIZEPIXELS;

        this.horizontalTilesImage = gir.getWidth() / tileSize;
        this.verticalTilesImage= gir.getHeight()/ tileSize;

     // the real size of tiles
        this.realTileSizeX = gir.getWidth() / horizontalTilesImage;
        this.realTileSizeY = gir.getHeight() / verticalTilesImage;
    }

   
	/**
     * Create the matrix that define the order in which the tiles will be read
     */
    private void createMatrixTileOrder(){
    	arrayReadTilesOrder = new int[maxnumberoftiles][maxnumberoftiles];//contain the weight for reading tile sequence, 0 most important
        for (int i = 0; i <= maxnumberoftiles / 2; i++) {
            int k = maxnumberoftiles - i - 1;
            for (int j = 0; j < maxnumberoftiles / 2; j++) {
            	arrayReadTilesOrder[i][j] = k--;
            }
            for (int j = maxnumberoftiles / 2; j < maxnumberoftiles; j++) {
            	arrayReadTilesOrder[i][j] = k++;
            }
        }
        for (int i = maxnumberoftiles / 2 + 1; i < maxnumberoftiles; i++) {
            int k = i;
            for (int j = 0; j < maxnumberoftiles / 2; j++) {
            	arrayReadTilesOrder[i][j] = k--;
            }
            for (int j = maxnumberoftiles / 2; j < maxnumberoftiles; j++) {
            	arrayReadTilesOrder[i][j] = k++;
            }
        }
    }


    /**
     *
     * @param gl
     */
    private void updateFutures(GL gl) {
        List<Future<TileInfo>> remove1 = new ArrayList<Future<TileInfo>>();
        for (Future<TileInfo> f : futures) {
            if (f.isDone() || f.isCancelled()) {
                remove1.add(f);
                try {
                	TileInfo o = f.get();
                    submitedTiles.remove(o.idTile); //o[0]=file o[1]=id o[2]=buffered image 
                    if (o.pngImage!=null) {
                        tcm.add(o.idTile, AWTTextureIO.newTexture(gl.getGLProfile(),o.pngImage, false));
                    }
                } catch (Exception ex) {
                	logger.error(ex.getMessage(),ex);
                }
            }
        }
        futures.removeAll(remove1);
    }

    @Override
    /**
     * displays the tiles on screen
     */
    public void render(Object glContext) {
    	context=(OpenGLContext)glContext;
    	Cache cacheInstance=CacheManager.getCacheInstance(activeGir.getDisplayName(activeBand));
    	
    	if(activeGir!=null){
    		String description=activeGir instanceof TiledBufferedImage?((TiledBufferedImage)activeGir).getDescription()+File.separator:"";
    		
	        if (torescale) {
	            torescale = false;
	            tcm.clear();
	        }
	        GL gl = context.getGL();

	        updateFutures(gl);

	        float zoom = context.getZoom();
	        int width = context.getWidth();
	        int height = context.getHeight();
	        int x = context.getX();
	        int y = context.getY();

	        int xx = (int) (x + xpadding);
	        int yy = (int) (y + ypadding);

	        //max tiles to compute time by time
	        int max = maxnumberoftiles; //for the computation of the array is important to keep this number odd
	        gl.getGL2().glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
		        if (zoom >= 1) {
		            curlevel = (int) Math.sqrt(zoom + 1);
		            //cycle to avoid the black area when zooming in/out and tiles not in memory. Stop when lll=max zoom level
		            for (int lll = maxlevels; (lll > curlevel - 1); lll--) {
		            	if (lll > maxlevels) {
		                    break;
		                }
		            	//modificato tramite action dalla console layer
		                lll += increaseLevel;

		                if (lll < 0) {
		                    continue;
		                }
		                if (this.mylevel != curlevel) {
		                    this.mylevel = curlevel;
		                    poolExcutorService.shutdown();
		                    poolExcutorService = new ThreadPoolExecutor(1,nThreads,100, TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>());// Executors.newFixedThreadPool(poolSize);
		                }

		                int w0 = xx / ((1 << lll) << 8);//xx/(int)Math.pow(2,lll+8);
		                int h0 = yy / ((1 << lll) << 8);


		                final String initfile = new StringBuffer(File.separator)
		                		.append((int) lll )
		                		.append(File.separator)
		                		.append(description)
		                		.toString();
		                

		                //AG loads the different tiles, starting from the center (k=0)
		                for (int k = 0; k < max; k++) {//loop on priority

		                    for (int j = 0; j < max; j++) {
		                        if (j + h0 < 0) {
		                            continue;
		                        }
		                        for (int i = 0; i < max; i++) {
		                            if (i + w0 < 0) {
		                                continue;
		                            }
		                            if (arrayReadTilesOrder[i][j] == k) {
		                                //start reading tiles in center of the image and go through the borders
		                                float ymin = (float) (((j + h0) * Constant.TILE_SIZE_DOUBLE * (1 << lll) - yy) / (height * zoom));
		                                float ymax = (float) (((j + h0 + 1) * Constant.TILE_SIZE_DOUBLE * (1 << lll) - yy) / (height * zoom));
		                                float xmin = (float) (((i + w0) * Constant.TILE_SIZE_DOUBLE * (1 << lll) - xx) / (1d * width * zoom));
		                                float xmax = (float) (((i + w0 + 1) * Constant.TILE_SIZE_DOUBLE * (1 << lll) - xx) / (1d * width * zoom));

		                                //check if the tile is in or out, if is not visible then is not loaded
		                                if (ymin > 1 || ymax < 0) {
		                                    continue;
		                                }
		                                if (xmin > 1 || xmax < 0) {
		                                    continue;
		                                }
		    		                    
		                                String relativePath = buildPathString(initfile, (i + w0),(j + h0));
		                                File absolutePath=cacheInstance.newFile(relativePath);
		                                String idTile=buildTileId(lll, (i + w0),(j + h0));

		                              //checked if the tile is already in memory or in cache, otherwise required it
		    		                    Texture t=tcm.getTexture(idTile);
		    		                    if (t==null) {
		    		                    	//check in cache
		    		                    	BufferedImage tile=tryFileCache(gl, absolutePath,idTile);
		    		                    	if (tile!=null) {
		    		                        	t = AWTTextureIO.newTexture(gl.getGLProfile(),tile, false);
		    		                        	tcm.add(idTile, t);
		    		                        }
		    		                    }
		    		                    if(t!=null){
	                    	                bindTexture(gl, t, xmin, xmax, ymin, ymax);
		    		                    }else if ((curlevel == 0 && lll == 0)||(curlevel == lll)) {
                                            addTileToQueue(absolutePath,idTile,lll,i,j);
                                        } 
		                            }
		                        }
		                    }
		                }
		            }
		        } else if (zoom > 0) {

		            curlevel = 0;//max zoom
		            int w0 = xx / Constant.TILE_SIZE_IMG_LAYER;
		            int h0 = yy / Constant.TILE_SIZE_IMG_LAYER;

		            final String initfile = new StringBuilder(File.separator)
		            						.append("0").append(File.separator)
		            						.append(description).append(File.separator).toString();
		            
		            for (int j = 0; j < max; j++) {
		                if (j + h0 < 0) {
		                    continue;
		                }
		                for (int i = 0; i < max; i++) {
		                    if (i + w0 < 0) {
		                        continue;
		                    }
		                    //start reading tiles in center of the image and go through the borders
		                    float ymin = (float) (((j + h0) * Constant.OVERVIEW_SIZE_DOUBLE - yy) / (height * zoom));
		                    float ymax = (float) (((j + h0 + 1) * Constant.OVERVIEW_SIZE_DOUBLE - yy) / (height * zoom));
		                    float xmin = (float) (((i + w0) * Constant.OVERVIEW_SIZE_DOUBLE - xx) / (1d * width * zoom));
		                    float xmax = (float) (((i + w0 + 1) * Constant.OVERVIEW_SIZE_DOUBLE - xx) / (1d * width * zoom));

		                    //check if the tile is in or out, if is not visible then is not loaded
		                    if (ymin > 1 || ymax < 0) {
		                        continue;
		                    }
		                    if (xmin > 1 || xmax < 0) {
		                        continue;
		                    }
		                    
		                    String relativePath = buildPathString(initfile, (i + w0),(j + h0));
                            File absolutePath=cacheInstance.newFile(relativePath);
                            String idTile=buildTileId(0, (i + w0),(j + h0));

		                    //checked if the tile is already in memory or in cache, otherwise required it
		                    Texture t=tcm.getTexture(idTile);
		                    if (t==null) {
		                    	//check in cache
		                    	BufferedImage tile=tryFileCache(gl,absolutePath,idTile);
		                    	if (tile!=null) {
		                        	t = AWTTextureIO.newTexture(gl.getGLProfile(),tile, false);
		                        	tcm.add(idTile,t);
		                        }
		                    }
		                    if(t!=null){
		                    	bindTexture(gl, t, xmin, xmax, ymin, ymax);
		                    }else{
		                    	addTileToQueue(absolutePath,idTile,0,i,j);
		                    }	
		                }
		            }
		        }
	        displayDownloading(futures.size());
	        SumoPlatform.getApplication().refresh();
	        if (this.disposed) {
	            disposeSync();
	        }
        }
    }
    
    /**
     * 
     * @param level
     * @param xtile
     * @param ytile
     * @return
     */
    public String buildTileId(int level,int xtile,int ytile){
    	String tileId=new StringBuilder("").append(level)
    			.append(" ").append(activeBand)
    			.append(" ").append(xtile).append(" ").append(ytile).toString();
    	
    	return tileId;
    }
    
    /**
     * 
     * @param initfile
     * @param xtile
     * @param ytile
     * @return
     */
    private String buildPathString(String initfile,int xtile,int ytile){
    	return new StringBuilder(initfile).append(getBandFolder(activeBand))
        		.append("/")
        		.append(xtile)
        		.append("_")
        		.append(ytile)
        		.append(".png").toString();
    }

    private void displayDownloading(int size) {
        if (currentSize != size) {
            if (size == 0) {
            	SumoPlatform.getApplication().setMessageInfo("");
            } else {
            	SumoPlatform.getApplication().setMessageInfo(new StringBuilder("loading ").append(size).toString());
            }
            currentSize = size;
        }
    }

    private void setInitialContrast() {
        // very rough calculation of a possible suitable contrast value
        int[] data = activeGir.readTile(activeGir.getWidth() / 2 - 100, activeGir.getHeight() / 2 - 100, 200, 200,activeBand);
        float average = 0;
        for (int i = 0; i < data.length; i++) {
            average = average + data[i];
        }

        average = average / data.length;
        int factor=8 * activeGir.getNumberOfBytes();
        //if the factor is >16 the contrast will be too high
        factor=(factor<16)?factor:16;

        float contrastLevel=(1 << ((factor))) / 5 / average;
        if(contrastLevel==0)
        	contrastLevel=100;
        if(contrastLevel>255)
        	contrastLevel=128;
        setContrast(contrastLevel);
    }




    /**
     * search for tiles in the file cache
     * @param gl
     * @param file
     * @param level
     * @param i
     * @param j
     * @param xmin
     * @param xmax
     * @param ymin
     * @param ymax
     * @return
     */
    private BufferedImage tryFileCache(GL gl, File absoluteFile,String idTile) {
    	Cache cacheInstance=CacheManager.getCacheInstance(activeGir.getDisplayName(activeBand));
    	BufferedImage temp=null;
    	
        if (cacheInstance.contains(absoluteFile.getAbsolutePath()) & !submitedTiles.contains(idTile)) {
            	try {
            		try {
            			temp = ImageIO.read(absoluteFile);
            		} catch (Exception ex) {
            			try {
            			    Thread.sleep(200);
            			} catch(InterruptedException e) {
            			    Thread.currentThread().interrupt();
            			}
            			temp = ImageIO.read(absoluteFile);
            		}
            	} catch (Exception ex) {
            		logger.warn("Problem reading tile:"+absoluteFile.getAbsolutePath()+":   "+ex.getMessage());
            		return null;
                }	finally{
            		pngReader.dispose();
                }

                if (temp!=null&&temp.getColorModel().getNumComponents() == 1) {
                    temp = rescale.filter(temp, rescale.createCompatibleDestImage(temp, temp.getColorModel()));
                }
        }
        return temp;
    }


    /**
     *
     * @param gl
     * @param texture
     * @param xmin
     * @param xmax
     * @param ymin
     * @param ymax
     */
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

   

    private String getBandFolder(int band) {
        StringBuilder out = new StringBuilder();
        out.append(band);
        return out.toString();
    }

    /**
     *
     * @param initfile file tile
     * @param level	   zoom level
     * @param i
     * @param j
     */
    public void addTileToQueue(final File fileTile,String idTile,int level,int i, int j) {
        if (!submitedTiles.contains(idTile)) {
            submitedTiles.add(idTile);
            futures.add(0, poolExcutorService.submit(new ServiceTile(fileTile,idTile, level, i, j)));
        }
    }

    public void setContrast(float value) {
        contrast.put(createBandsString(activeBand), value);
        torescale = true;
        rescale = new RescaleOp(value, brightness, null);
    }

    private String createBandsString(int b) {
        StringBuilder out = new StringBuilder();
        out.append(b).append(",");
        return out.toString();
    }

    public void setBrightness(float value) {
        this.brightness = value;
        torescale = true;
        rescale = new RescaleOp(getContrast(), brightness, null);
    }

    public float getContrast() {
        return contrast.get(createBandsString(activeBand)) == null ? 1 : contrast.get(createBandsString(activeBand));
    }

    public float getBrightness() {
        return brightness;
    }
    
    /**
     * 
     * @param val
     */
    public void setActiveBand(int val) {
        if (futures.size() > 0) {
            return;
        }
        this.activeBand = val;
        if (contrast.get(createBandsString(activeBand)) == null) {
            setInitialContrast();
        } else {
            rescale = new RescaleOp(contrast.get(createBandsString(activeBand)), brightness, null);
        }
    }
   
    @Override
    public void dispose() {
        disposed = true;
        if(poolExcutorService!=null){
        	poolExcutorService.shutdownNow();
        	poolExcutorService = null;
        }
        if(activeGir!=null){
        	activeGir.dispose();
        	activeGir = null;
        }
        if(tcm!=null){
        	tcm.clear();
        	tcm = null;
        }
        if(submitedTiles!=null){
        	submitedTiles.clear();
        	submitedTiles = null;
        }
       /* if(imagePool!=null){
        	imagePool.dispose();
        	imagePool = null;
        }*/
        SumoPlatform.getApplication().getLayerManager().removeLayer(this);
    }

    private void disposeSync() {
        poolExcutorService.shutdownNow();
        poolExcutorService = null;
        activeGir.dispose();
        activeGir = null;
        tcm.clear();
        tcm = null;
        submitedTiles.clear();
        submitedTiles = null;
      //  imagePool.dispose();
      //  imagePool = null;
        SumoPlatform.getApplication().getLayerManager().removeLayer(this);
    }


    public void setName(GeoImageReader gir){
    	if(gir.getDisplayName(activeBand)!=null&&!gir.getDisplayName(activeBand).equals(""))
        	setName(gir.getDisplayName(activeBand));
        else{
        	String temp = gir.getFilesList()[0].replace("\\", "/");
        	String name=temp.substring(temp.lastIndexOf("/") + 1);
        	//TODO: change this for the cosmoskymed images
        	if(gir instanceof AbstractCosmoSkymedImage && ((AbstractCosmoSkymedImage)gir).getInternalImage()!=null){
        		name=name+"_"+((AbstractCosmoSkymedImage)gir).getInternalImage();
        	}
        	setName(name);
        }
    }
    

    public int getNumberOfBands() {
        return activeGir.getNBand();
    }

    public int getActiveBand() {
        return activeBand;
    }

    
    public GeoImageReader getImageReader() {
        return activeGir;
    }

    public void setMaximumCut(float value) {
        maxCut = (int) value;
    }

    public float getMaximumCut() {
        return maxCut;
    }

    public void level(int levelIncrease) {
        this.increaseLevel = levelIncrease;
    }

    public boolean isActive() {
        return active;
    }

	public void setActive(boolean active) {
        this.active=active;
    }

	public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public boolean isRadio() {
        return isRadio;
    }

    public void setIsRadio(boolean radio) {
        isRadio = radio;
    }

    public void init(ILayer parent) {
		this.parent = parent;
	}


	public ILayer getParent() {
		return parent;
	}

	public void setParent(ILayer parent) {
		this.parent = parent;
	}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getDescription() {
        if (activeGir.getDescription() != null&&!activeGir.getDescription().equals("")) {
            return activeGir.getDescription();
        } else {
            return activeGir.getDisplayName(activeBand);
        }
    }

    public int getRealTileSizeX() {
		return realTileSizeX;
	}

	public void setRealTileSizeX(int realTileSizeX) {
		this.realTileSizeX = realTileSizeX;
	}

	public int getRealTileSizeY() {
		return realTileSizeY;
	}

	public void setRealTileSizeY(int realTileSizeY) {
		this.realTileSizeY = realTileSizeY;
	}

	public int getHorizontalTilesImage() {
		return horizontalTilesImage;
	}

	public void setHorizontalTilesImage(int horizontalTilesImage) {
		this.horizontalTilesImage = horizontalTilesImage;
	}

	public int getVerticalTilesImage() {
		return verticalTilesImage;
	}

	public void setVerticalTilesImage(int verticalTilesImage) {
		this.verticalTilesImage = verticalTilesImage;
	}

}
