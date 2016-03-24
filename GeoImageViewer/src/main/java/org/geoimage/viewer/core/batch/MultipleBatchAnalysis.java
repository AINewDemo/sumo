package org.geoimage.viewer.core.batch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.geoimage.def.GeoImageReader;
import org.geoimage.def.SarImageReader;
import org.geoimage.factory.GeoImageReaderFactory;
import org.geoimage.viewer.core.GeometryImage;
import org.geoimage.viewer.core.SumoPlatform;
import org.geoimage.viewer.core.analysisproc.AnalysisProcess;
import org.geoimage.viewer.core.api.ilayer.ILayer;
import org.geoimage.viewer.core.api.ilayer.IMask;
import org.geoimage.viewer.core.factory.FactoryLayer;
import org.geoimage.viewer.core.layers.visualization.vectors.ComplexEditVDSVectorLayer;
import org.geoimage.viewer.core.layers.visualization.vectors.MaskVectorLayer;
import org.geoimage.viewer.util.files.ArchiveUtil;
import org.geoimage.viewer.util.files.IceHttpClient;
import org.geoimage.viewer.util.files.SarFileUtil;
import org.jrc.sumo.configuration.PlatformConfiguration;
import org.jrc.sumo.util.Constant;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Polygon;

public class MultipleBatchAnalysis extends AbstractBatchAnalysis{
	private static org.slf4j.Logger logger=LoggerFactory.getLogger(MultipleBatchAnalysis.class);
	private ConfigurationFile confFile;
	private int NTHREDS=4;

	public MultipleBatchAnalysis(AnalysisParams analysisParams,ConfigurationFile conf) {
		super(analysisParams);
		confFile=conf;
		super.setRunVersion(conf.getRunVersion());
		super.setRunVersionNumber(conf.getRunVersionNumber());
	}
	/**
	 *
	 * @return
	 */
	private List<File> readFileList(){
		try{
			List<File>files=new ArrayList<>();
			String fl=confFile.getInputFileList();
			BufferedReader br = new BufferedReader(new FileReader(fl));
			String line = null;
			while ((line = br.readLine()) != null) {
				if(!line.equals("")){
					File manifest=SarFileUtil.findManifest(new File(line));
					files.add(manifest);
				}
			}
			br.close();
			return files;
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			return null;
		}
	}


	/**
	 *
	 */
	public void runAnalysis(){
        //Get the ThreadFactory implementation to use
	    //creating the ThreadPoolExecutor
        ExecutorService executorPool = Executors.newFixedThreadPool(10);//ThreadPoolExecutor(2, NTHREDS, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        final List<Future<AnalysisProcess.Results>> tasks = new ArrayList<Future<AnalysisProcess.Results>>();

			List<File>filesImg=null;
			if(!confFile.getUseFileList())
				filesImg=SarFileUtil.scanFoldersForImages(super.params.pathImg, confFile.getFilterFolder(), false);
			else
				filesImg=readFileList();

			if(filesImg!=null){
				for (File image:filesImg){
					try{
						logger.info("Start analyzing:"+image.getParent());
						AnalysisParams activeParams=params;
						String folderName=image.getParentFile().getName();

						//output folder have the same name of the input folder
						String imagePathFolder=new StringBuilder(params.outputFolder)
										.append(File.separator)
										.append(folderName).toString();


						boolean forceAnalysis=confFile.forceNewAnalysis();

						//check if already analized
						boolean alreadyAnalyzed=checkAlreadyAnalized(imagePathFolder);

						if(!alreadyAnalyzed || forceAnalysis){
							//check for use local configuration file
							if(confFile.useLocalConfigurationFiles()){
								//path to the single image conf file
								String localImageConfFilePath=new StringBuilder(imagePathFolder)
												.append(File.separator)
												.append(Constant.CONF_FILE).toString();

								activeParams=readLocalConfFile(params,localImageConfFilePath);
							}

							//crate the reader
							List<GeoImageReader> readers =  GeoImageReaderFactory.createReaderForName(image.getAbsolutePath(),PlatformConfiguration.getConfigurationInstance().getS1GeolocationAlgorithm());
							for(GeoImageReader r:readers){
								//super.currentReader=r;
								SarImageReader reader=(SarImageReader) r;

								if(confFile.getENL()==0){
									String enl=reader.getENL();
									activeParams.enl=Float.parseFloat(enl);
								}else{
									activeParams.enl=confFile.getENL();
								}

								GeometryImage gl=null;
						    	Polygon imageP=(reader).getBbox(PlatformConfiguration.getConfigurationInstance().getLandMaskMargin(0));

								if(activeParams.shapeFile!=null)
									gl=readShapeFile(params.shapeFile,imageP,reader.getGeoTransform());

								IMask mask = null;
								if(gl!=null){
									mask=FactoryLayer.createMaskLayer("buffered",gl.getGeometryType(),activeParams.buffer,  gl,MaskVectorLayer.COASTLINE_MASK);
								}

								IMask iceMask = null;
								if(confFile.useIceRepositoryShapeFile()){
									File ice=getIceShapeFile(r.getImageDate());
									if(ice!=null){
										activeParams.iceShapeFile=ice.getAbsolutePath();
										GeometryImage glIce=readShapeFile(params.iceShapeFile,imageP,reader.getGeoTransform());
										iceMask=FactoryLayer.createMaskLayer("ice",glIce.getType(),0,glIce,MaskVectorLayer.ICE_MASK);
									}
								}

								AnalysisProcess ap=prepareBatchAnalysis(reader, mask, iceMask, activeParams);
								ap.addProcessListener(this);
								tasks.add(executorPool.submit(ap));


							    //  retrieve and save the result
							    for (Future<AnalysisProcess.Results> future : tasks) {
							      try {
							    	  AnalysisProcess.Results res=(AnalysisProcess.Results)future.get();
							    	  List<ComplexEditVDSVectorLayer>results=res.getLayerResults();
							    	  SarImageReader gr=(SarImageReader)res.getReader();
							    	  String name=(gr).getManifestFile().getParentFile().getName();
									  saveResults(name,gr,results);
							      } catch (InterruptedException e) {
							        e.printStackTrace();
							      } catch (ExecutionException e) {
							        e.printStackTrace();
							      }
							    }

							}
						}
						logger.info("Image processed:"+image.getAbsolutePath());
					}catch(Exception e){
						logger.error("Problem working this image:"+image.getAbsolutePath(),e);
					}finally{
						executorPool.shutdown();
					}
				}//end for loop on the images







			}else{
				logger.error("No file found to analyze");
			}
	}


	/**
	 *
	 * @param imgDate the date of the image. It is used to download the correct shape file  fron the repository
	 * @return the ice shp file
	 */
	private File getIceShapeFile(Date imgDate) {
		File ice=null;
		try{
			String icePatternName=confFile.getIceShapeFileName();
			boolean isRemote=confFile.getIsRemoteRepoIceFile();
			String iceRepoUrl=confFile.getIceRepositoryUrl();
			if(isRemote){
				String tokenName=icePatternName.substring(icePatternName.indexOf("%")+1,icePatternName.lastIndexOf("%"));
				String tokenUrl=iceRepoUrl.substring(iceRepoUrl.indexOf("%")+1,iceRepoUrl.lastIndexOf("%"));

				SimpleDateFormat fd=new SimpleDateFormat(tokenName);
				icePatternName=icePatternName.replace("%"+tokenName+"%",fd.format(imgDate));

				fd.applyPattern(tokenUrl);
				iceRepoUrl=iceRepoUrl.replace("%"+tokenUrl+"%",fd.format(imgDate));

				String completeUrl=iceRepoUrl.concat(File.separator).concat(icePatternName);
				String output=SumoPlatform.getApplication().getCachePath().concat(File.separator).concat(icePatternName);
				ice=new IceHttpClient().download(completeUrl, output);
				if(ArchiveUtil.isArchive(ice)){
					ArchiveUtil.unZip(ice.getAbsolutePath());
					File[] shpfiles=ice.getParentFile().listFiles((java.io.FileFilter) pathname -> FilenameUtils.getExtension(pathname.getName()).equalsIgnoreCase("shp"));
	    			ice=shpfiles[0];
				}

			}else{

			}
		}catch(Exception e){
			logger.error("Ice shape file not loaded:"+e.getMessage());
			ice=null;
		}
		return ice;
	}



	/**
	 *
	 * @param global
	 * @param image
	 * @return
	 */
	private AnalysisParams readLocalConfFile(final AnalysisParams global,String localImageConfFilePath){
		AnalysisParams local=global;

		//check the local file name in the output folder
		try{
			if(!new File(localImageConfFilePath).exists())
				throw new Exception("Local file not found");
			ConfigurationFile localConf=new ConfigurationFile(localImageConfFilePath);

			//if find the local conf then replace the configuration params
			if(!localConf.getShapeFile().isEmpty())
				params.shapeFile=localConf.getShapeFile();
			if(localConf.getBuffer()!=-1)
				params.buffer=localConf.getBuffer();
			if(localConf.getThresholdArray()!=null){
				float[] array=localConf.getThresholdArray();
				for(int i=0;i<array.length;i++){
					float v=array[i];
					if(v!=-1)
						params.thresholdArrayValues[i]=v;
				}
			}
			if(localConf.getENL()!=0){
				params.enl=localConf.getENL();
			}

		}catch(Exception e){
			logger.warn("Local configuration file:"+localImageConfFilePath +" not loaded. ", e.getMessage());
		}
		return local;
	}

	/**
	 *
	 * @param imagePathFolder
	 * @return
	 */
	private boolean checkAlreadyAnalized(String imagePathFolder){
		boolean analyzed=false;

		File f=new File(imagePathFolder);
		if(f.exists()&&f.isDirectory()){
			String[] xmls=f.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.contains("VDS analysis");
				}
			});
			if(xmls.length>0)
				analyzed=true;
		}

		return analyzed;
	}

}
