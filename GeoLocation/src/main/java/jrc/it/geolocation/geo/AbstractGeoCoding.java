package jrc.it.geolocation.geo;

import java.util.Arrays;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder.In;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.FastMath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jrc.it.geolocation.common.GeoUtils;
import jrc.it.geolocation.common.MathUtil;
import jrc.it.geolocation.exception.GeoLocationException;
import jrc.it.geolocation.exception.MathException;
import jrc.it.geolocation.interpolation.OrbitInterpolation;
import jrc.it.geolocation.metadata.AbstractMetadata;
import jrc.it.geolocation.metadata.IMetadata;
import jrc.it.geolocation.metadata.impl.S1Metadata;

public abstract class AbstractGeoCoding implements GeoCoding {
	protected OrbitInterpolation orbitInterpolation=null;
	protected AbstractMetadata meta=null;
	protected List<double[]> statepVecInterp=null;
	protected List<double[]> statevVecInterp=null;
	protected IMetadata.CoordinateConversion[] coordConv=null;
	
	
	private static Logger logger=LogManager.getLogger(AbstractGeoCoding.class);

	/**
	 * 
	 * @param metaFile
	 * @throws MathException
	 * @throws JAXBException 
	 */
	public AbstractGeoCoding(AbstractMetadata meta) throws MathException, JAXBException{
		orbitInterpolation=new OrbitInterpolation();
		this.meta=meta;
	}
	
	

	/**
	 * 
	 * @param lon
	 * @param lat
	 * @return
	 */
	public double getSlantRange(final double lon,final double lat){
		double[] pXYZ =GeoUtils.convertFromGeoToEarthCentred(lat, lon);
		Double dd2[]=(Double[])orbitInterpolation.getTimeStampInterp().toArray(new Double[0]);
		double[] results=findZeroDoppler(statepVecInterp, pXYZ, ArrayUtils.toPrimitive(dd2));
		return results[1];
	}	
	
	
	
	/**
	 *@see In matlab: reverse geolocation
	 *
	 *@result r[o]=p r[1]=l
	 */
	@Override
	public double[] pixelFromGeo(final double lon,final double lat)throws GeoLocationException{
		double[] resultReverse=new double[2];
		
		double[] pXYZ =GeoUtils.convertFromGeoToEarthCentred(lat, lon);
		
		Double dd2[]=(Double[])orbitInterpolation.getTimeStampInterp().toArray(new Double[0]);
		double[] results=findZeroDoppler(statepVecInterp, pXYZ, ArrayUtils.toPrimitive(dd2));

		double zeroDopplerTime=results[0];
		double srdist=results[1];
		
		//Convert zero Doppler azimuth time to image line number
		
		//Modifiche di Carlos del //20150703 pL = ((zeroDopplerTime - zeroDopplerTimeFirstLineSecondsRef) / (zeroDopplerTimeLastLineSecondsRef - zeroDopplerTimeFirstLineSecondsRef)) * (meta.nLines - 1);
		double lNominatore=(zeroDopplerTime - orbitInterpolation.getZeroDopplerTimeFirstRef());
		double lDenom=(orbitInterpolation.getZeroDopplerTimeLastRef() - orbitInterpolation.getZeroDopplerTimeFirstRef()) ;
		double l = (lNominatore/lDenom )* (meta.getNlines() - 1);
		
		//******************* this part is only Sentinel 1 'SLC 'IW' and 'EW'
		    // Need to take the bursts into account
		/*if(meta.getMode().equalsIgnoreCase("IW")||meta.getMode().equalsIgnoreCase("EW")){
			double timeRef = meta.getOrbitStatePosVelox().get(0).timeStampInitSeconds - orbitInterpolation.getSecondsDiffFromRefTime()[0];
			double[] az0TimBSeconds = meta.az0TimBSeconds - timeRef;
		    double azLTimBSeconds = az0TimBSeconds + meta.getLinesPerBurst()*meta.getAzimuthTimeInterval();
		    
		    int idx=0;
		    do{
		    	idx++;
		    }while(az0TimBSeconds<zeroDopplerTime&&azLTimBSeconds > zeroDopplerTime);
			    
		    
		    idx = find(az0TimBSeconds < zeroDopplerTime & azLTimBSeconds > zeroDopplerTime);
		    l = meta.linesPerBurst * (idx-1) + (zeroDopplerTime - az0TimBSeconds(idx))/meta.azimuthTimeInterval;
		
		}	*/
		//******************* End part only for Sentinel 1 'SLC 'IW' and 'EW'
		
		
		//************************************Sentinel 1 non SLC a********************//
		// Interpolate the GR to SR coefficients at the zero Doppler time.  This is only needed in non-SLC with multiple GR to SR polynomials

		if (meta.getCoordinateConversion()!=null&&meta.getCoordinateConversion().length>0){
			
			
			//TODO check if the time ref is correct and if the 2 values are calculated in the correct way
				  							//getSecondsDiffFromRefTime is timeStampInitSecondsRef in Matlab
			double timeRef = meta.getOrbitStatePosVelox().get(0).timeStampInitSeconds - orbitInterpolation.getSecondsDiffFromRefTime()[0];
			
			double[]groundToSlantRangePolyTimesSeconds=new double[coordConv.length];
			int index=0;
			for(IMetadata.CoordinateConversion cc:coordConv){
				groundToSlantRangePolyTimesSeconds[index] = cc.groundToSlantRangePolyTimesSeconds - timeRef;
				index++;
			}
			
		    int idx=0;
		    if(zeroDopplerTime < groundToSlantRangePolyTimesSeconds[0]){
		        idx = 0;
			}else if (zeroDopplerTime > groundToSlantRangePolyTimesSeconds[groundToSlantRangePolyTimesSeconds.length-1]){
		        idx = groundToSlantRangePolyTimesSeconds.length - 2;
			}else{
				for(idx=groundToSlantRangePolyTimesSeconds.length-1;idx>0;idx--){
					if(groundToSlantRangePolyTimesSeconds[idx] < zeroDopplerTime)
						break;
				}
				if(idx==groundToSlantRangePolyTimesSeconds.length-1){
					idx=idx-1;
				}
				if(idx==groundToSlantRangePolyTimesSeconds.length){
					idx=idx-2;
				}
		    }
		    
		    double factor1 = (groundToSlantRangePolyTimesSeconds[idx+1] - zeroDopplerTime) / (groundToSlantRangePolyTimesSeconds[idx+1] - groundToSlantRangePolyTimesSeconds[idx]);
		    double factor2 = (zeroDopplerTime - groundToSlantRangePolyTimesSeconds[idx]) / (groundToSlantRangePolyTimesSeconds[idx+1] - groundToSlantRangePolyTimesSeconds[idx]);

		    double[]groundToSlantRangeCoefficientsInterp=new double[coordConv[idx].groundToSlantRangeCoefficients.length];
	    	for(int idCoeff=0;idCoeff<groundToSlantRangeCoefficientsInterp.length;idCoeff++){
	    		groundToSlantRangeCoefficientsInterp[idCoeff]=factor1*coordConv[idx].groundToSlantRangeCoefficients[idCoeff]+factor2*coordConv[idx+1].groundToSlantRangeCoefficients[idCoeff];
	    	}
	    	
		    double[]slantToGroundRangeCoefficientsInterp=new double[coordConv[idx].slantToGroundRangeCoefficients.length];
	    	for(int idCoeff=0;idCoeff<slantToGroundRangeCoefficientsInterp.length;idCoeff++){
	    		slantToGroundRangeCoefficientsInterp[idCoeff]=factor1*coordConv[idx].slantToGroundRangeCoefficients[idCoeff]+factor2*coordConv[idx+1].slantToGroundRangeCoefficients[idCoeff];
	    	}

	    	//TODO ADDING THIS PART FOR SLC 
			// Convert the slant range into the distance D, from the near-range edge of the scene.
	    	/*	if strncmp(meta.productType,'SLC',3) || strncmp(meta.productType,'SCS',3) || strncmp(meta.productType,'SSC',3)
			    D = SRdist - meta.slantRangeNearEdge;
			else*/
		
	       double newD=0;
           // Using the groundToSlantRangeCoefficients
           double oldD = 0;
           int nCoeffs = groundToSlantRangeCoefficientsInterp.length;
           
          //initialize the Arrays with the coefficients
           int[] numCoeffs =new int[nCoeffs];
           int[] numExps =new int[nCoeffs];
           int[] denomCoeffs =new int[nCoeffs];
           int[] denomExps =new int[nCoeffs];
           
           for(int i=0;i<nCoeffs;i++){
        	   numCoeffs[i]=i-1;
        	   numExps[i]=i;
        	   
        	   denomCoeffs[i]=i;
        	   denomExps[i]=i-1;
           }
           denomExps[0]=0;
           
           double deltaD=0;
           do{
        	 //calcolo dExp e denommExp come prodotti vettoriali => il risultato e'  uno scalare
        	   double numCoeffsXGround=0;//[]=new double[nCoeffs];
        	   double denomCoeffsXGround=0;//[]=new double[nCoeffs];
        	   double scalNum=0;
        	   double scalDen=0;
        	   
        	   for(int i=0;i<groundToSlantRangeCoefficientsInterp.length;i++){
        		   double dExp=FastMath.pow(oldD,numExps[i]);
        		   double denomExp=FastMath.pow(oldD,denomExps[i]);
        		   
        		   numCoeffsXGround=groundToSlantRangeCoefficientsInterp[i]*numCoeffs[i];
        		   denomCoeffsXGround=groundToSlantRangeCoefficientsInterp[i]*denomCoeffs[i];
        		   
             	  //calcolo lo scalare risultante dal prodotto di 2 matrici (1xn)x(nx1) 
        		   scalNum=scalNum+dExp*numCoeffsXGround;
        		   scalDen=scalDen+denomExp*denomCoeffsXGround;
        	   }
        	   
        	   double num=srdist+scalNum;
        	   newD = num/scalDen;
               
               deltaD = FastMath.abs(newD-oldD);
               oldD=newD;
               
           }while(deltaD > 1);
		           
			// Convert slant range distance to image pixel (column)
			double p=0;
			//newD=37575.05;
			if (meta.isPixelTimeOrderingAscending()){
			    p = (newD + meta.getGroundRangeOrigin())/meta.getSamplePixelSpacing();
			 }else{
			    p = meta.getNumberOfSamplesPerLine() - 1 - (newD + meta.getGroundRangeOrigin())/meta.getSamplePixelSpacing();
			}
			resultReverse[0]=p;
			resultReverse[1]=l;
			logger.debug("line:"+l+"  "+"col:"+p);
		}
		return resultReverse;
	}
	
	/**
	 *@see In Matlab forwardgeolocation
	 *@return   r[0]=lon r[1]=lat
	 */
	@Override
	public double[] geoFromPixel(final double l, final double p) throws GeoLocationException{
		try{
			double[] results=new double[2];
			
			double lon=0;
			double lat=0;
			//Convert the line number into zero Doppler time
			/*if strcmp(meta.ImTyp,'TX') && ~strncmp(meta.productType,'SSC',3) && strcmp(meta.pass,'ASCENDING') //This is to make it compatible with SUMO
			    l = meta.nLines - l;
			end*/ 
	
			double t0=0;
			int idxStartT0=0;
			//TODO blocco da inserire solo per immagini complesse
			if(meta.getType().equalsIgnoreCase("S1") && meta.getProductType().equalsIgnoreCase("SLC") && (meta.getMode().equalsIgnoreCase("IW") ||meta.getMode().equalsIgnoreCase("EW"))){
			    // Need to take the bursts into account 
				double timeRef = meta.getOrbitStatePosVelox().get(0).timeStampInitSeconds - orbitInterpolation.getSecondsDiffFromRefTime()[0];
			    
				//TODO COMPLETE THIS PART FOR THE EW
			  /*  double az0TimBSeconds = meta.az0TimBSeconds - timeRef;
			    double azLTimBSeconds = az0TimBSeconds + meta.linesPerBurst*meta.azimuthTimeInterval;
			    
			    double idxBurst = ceil(l / meta.linesPerBurst) - 1;
			    double lBurst = l - idxBurst * meta.linesPerBurst;
			    
			    t0 = az0TimBSeconds(idxBurst+1) + lBurst * meta.azimuthTimeInterval;
			    
			    for(idxStartT0=0;idxStartT0<orbitInterpolation.getTimeStampInterp().length;idxStartT0++){
			    	if(orbitInterpolation.getTimeStampInterp()[idxStartT0]>=t0){
			    		break;
			    	}
			    }     */
			
			}else{
			 //modifiche di Carlos del 20150703 t0 = (zeroDopplerTimeFirstLineSecondsRef * (meta.nLines-1-l) + zeroDopplerTimeLastLineSecondsRef * l) / (meta.nLines-1);				
				double t01=(orbitInterpolation.getZeroDopplerTimeFirstRef()) * (meta.getNlines()-1-l);
				double t02=orbitInterpolation.getZeroDopplerTimeLastRef()*l;
				t0 =  (t01+t02) / (meta.getNlines()-1); //In seconds
				
				for(idxStartT0=0;idxStartT0<orbitInterpolation.getTimeStampInterp().size();idxStartT0++){
			    	if(orbitInterpolation.getTimeStampInterp().get(idxStartT0)>=t0){
			    		break;
			    	}
			    }
			    if(idxStartT0==statepVecInterp.size())//MATLAB if isempty(idx_t0) idx_t0 = length(timeStampInterpSecondsRef); end  %20150608
			    	idxStartT0--;
			}
			//Using the orbit propagation model, find the sensor position p(t0) and sensor velocity v(t0) at zero Doppler time
			
			final double[] pT0 = statepVecInterp.get(idxStartT0);
			final double[] vT0 = statevVecInterp.get(idxStartT0);
	
	
			double distance=0;  //D
			// Convert the pixel number into a distance from the near-range edge of the image
			if(meta.isPixelTimeOrderingAscending()){
			    distance = p * meta.getSamplePixelSpacing() - meta.getGroundRangeOrigin();
			}else{
			    distance = (meta.getNumberOfSamplesPerLine() - 1 - p) * meta.getSamplePixelSpacing() - meta.getGroundRangeOrigin();
			}
	
			// Interpolate the GR to SR coefficients at the zero Doppler time.  This is only needed in non-SLC with multiple GR to SR polynomials
			if (coordConv!=null){
			    double zeroDopplerTime = t0;
			    double timeRef = meta.getOrbitStatePosVelox().get(0).timeStampInitSeconds - orbitInterpolation.getSecondsDiffFromRefTime()[0];
			    
			    double[]groundToSlantRangePolyTimesSeconds=new double[coordConv.length];
				int index=0;
				for(S1Metadata.CoordinateConversion cc:coordConv){
					groundToSlantRangePolyTimesSeconds[index] = cc.groundToSlantRangePolyTimesSeconds - timeRef;
					index++;
				}
				
				int idx=0;
			    if(zeroDopplerTime < groundToSlantRangePolyTimesSeconds[0]){
			        idx = 1;
				}else if (zeroDopplerTime > groundToSlantRangePolyTimesSeconds[groundToSlantRangePolyTimesSeconds.length-1]){
			        idx = groundToSlantRangePolyTimesSeconds.length - 2;
				}else{
					for(idx=groundToSlantRangePolyTimesSeconds.length-1;idx>0;idx--){
						if(groundToSlantRangePolyTimesSeconds[idx] < zeroDopplerTime)
							break;
					}
			    }
			    double factor1 = (groundToSlantRangePolyTimesSeconds[idx+1] - zeroDopplerTime) / (groundToSlantRangePolyTimesSeconds[idx+1] - groundToSlantRangePolyTimesSeconds[idx]);
			    double factor2 = (zeroDopplerTime - groundToSlantRangePolyTimesSeconds[idx]) / (groundToSlantRangePolyTimesSeconds[idx+1] - groundToSlantRangePolyTimesSeconds[idx]);
	
			    double[]groundToSlantRangeCoefficientsInterp=ArrayUtils.clone(coordConv[idx].groundToSlantRangeCoefficients);
			    double[]groundToSlantRangeCoefficientsInterp2=coordConv[idx+1].groundToSlantRangeCoefficients;
		    	for(int idCoeff=0;idCoeff<groundToSlantRangeCoefficientsInterp.length;idCoeff++){
		    		groundToSlantRangeCoefficientsInterp[idCoeff]=factor1*groundToSlantRangeCoefficientsInterp[idCoeff]+factor2*groundToSlantRangeCoefficientsInterp2[idCoeff];
		    	}
		    	
	
			    double[]slantToGroundRangeCoefficientsInterp=ArrayUtils.clone(coordConv[idx].slantToGroundRangeCoefficients);
			    double[]slantToGroundRangeCoefficientsInterp2=coordConv[idx+1].slantToGroundRangeCoefficients;
		    	for(int idCoeff=0;idCoeff<slantToGroundRangeCoefficientsInterp.length;idCoeff++){
		    		slantToGroundRangeCoefficientsInterp[idCoeff]=factor1*slantToGroundRangeCoefficientsInterp[idCoeff]+factor2*slantToGroundRangeCoefficientsInterp2[idCoeff];
		    	}
		        
				// Convert the distance from the near-range edge of the image into zero Doppler slant range
				/*TODO ADDING THIS PART FOR COMPLEX IMAGE
					//(strncmp(meta.productType,'SLC',3) || strncmp(meta.productType,'SCS',3) || strncmp(meta.productType,'SSC',3)){
				    //SRdist = D + meta.slantRangeNearEdge;
				}else{*/
				int nCoeffs = groundToSlantRangeCoefficientsInterp.length;
				int[] vExps =new int[nCoeffs];
				//create the Array with the coefficients
				for(int i=0;i<nCoeffs;i++){
			     	   vExps[i]=i;
			    }
			    double tmpD = distance - meta.getGroundRangeOrigin();
			    double[] pows=MathUtil.powValue2Coeffs(tmpD,vExps);
			    double sRdist =MathUtil.dot(groundToSlantRangeCoefficientsInterp, pows); // This polynomial transforms from slant range (in metres) to ground range (in metres)       
			    logger.debug("sRdist:"+sRdist);
	
			    //norma for pt0 vector
			    double normPt0=MathUtil.norm(pT0);
			    
				// Find the tangent of the angle ? between the zero Doppler plane and the vertical direction, which is the same as the ratio between the radial and tangential components of the sensor velocity
				double vRadial = MathUtil.dot(vT0, pT0) / normPt0;
				double vTangential = FastMath.sqrt(FastMath.pow(MathUtil.norm(vT0),2) - FastMath.pow(vRadial,2));
				double tanPsi = vRadial/vTangential;

	
				// Define a satellite coordinate system centred on the sensor position, where the Z axis points towards the Earth centre, the X axis points along the tangential component of the sensor velocity, and the Y axis completes the right-handed coordinate system
				double[] zsUnit =MathUtil.divVectByVal(pT0,-normPt0);
				double[] vTmp =MathUtil.crossProd3x3(zsUnit,vT0);// cross(zs_unit,v_t0);
				double[] ysUnit = MathUtil.divVectByVal(vTmp, MathUtil.norm(vTmp));
				double[] xsUnit = MathUtil.crossProd3x3(ysUnit,zsUnit);
	
				
				double pH=0;
				double rEarth = normPt0/FastMath.sqrt((pT0[0]*pT0[0] + pT0[1]*pT0[1])/GeoUtils.semiMajorAxis2 + (pT0[2]*pT0[2])/GeoUtils.semiMinorAxis2);
						
				for(int iidx=0; iidx<2;iidx++){ //First iteration with a default pH, second with a more accurate pH
					// Find the approximate Earth radius at a point directly below the sensor position.
					rEarth = rEarth + pH;
		
					//iteration
					double rEarthOld = rEarth;
					double rEarthChange = 100000;
					
					double[]q=new double[3];
					for(;rEarthChange > 0.1;){
						
					    // Form a triangle whose sides are the distance from the centre of the earth to the sensor, the slant range and the local earth radius and using the cosine law find the intersection point between the slant range vector and the Earth surface in the satellite coordinate system
					    double Rz = (normPt0*normPt0 + sRdist*sRdist - rEarth*rEarth) / (2*normPt0);
					    double Rx = Rz * tanPsi;
					    double Ry = FastMath.sqrt(sRdist*sRdist - Rz*Rz - Rx*Rx);
					    
					    if (!meta.getAntennaPointing().equalsIgnoreCase("Right")){
					        Ry = -Ry;
					    }
		
					    // Transform the coordinates of the intersection point from satellite to target ECR coordinates
					    double[][] sRvect = new double[][]{{Rx},{Ry},{Rz-normPt0}};
					    
					    double[][] qbig = new double[][]{xsUnit,ysUnit,zsUnit};
					    //RealMatrix m=MatrixUtils.createRealMatrix(qbig);
					    //qbig=m.transpose().getData();
					    qbig=MathUtil.transpose(qbig);
					    
					    double[][] qmat =MathUtil.multiplyMatrix(qbig,sRvect);
					    q[0]=qmat[0][0]; //x
					    q[1]=qmat[1][0]; //y
					    q[2]=qmat[2][0]; //z
		
					    // Find the Earth radius at a point directly below the intersection point (this approximation becomes more and more precise as the intersection point becomes closer to the surface)
					    rEarth = MathUtil.norm(q) / FastMath.sqrt((q[0]*q[0]+q[1]*q[1])/GeoUtils.semiMajorAxis2 + (q[2]*q[2]/GeoUtils.semiMinorAxis2)) + pH;
					    
					    rEarthChange = FastMath.abs(rEarth-rEarthOld);
					    rEarthOld = rEarth;
					}
	
					// Convert the target ECR coordinates (x,y,z) into geographic coordinates (lat,lon,h) based on the WGS84 ellipsoid Earth model
					lon = FastMath.atan2(q[1],q[0]) * 180/FastMath.PI;
					double lattmp = FastMath.asin(q[2]/FastMath.sqrt(q[0]*q[0]+q[1]*q[1]+q[2]*q[2])); //x^2+y^2+z^2
					lat=FastMath.atan(FastMath.tan(lattmp) * FastMath.pow((GeoUtils.semiMajorAxis+pH),2)/FastMath.pow((GeoUtils.semiMinorAxis+pH),2)) * 180/FastMath.PI;
					
					pH = GeoUtils.getGeoidH(lon,lat);
				}
			}
			logger.debug("lat:"+lat+ "  lon:"+lon);
			results[0]=lon;
			results[1]=lat;
			return results;
		}catch(MathException me){
			throw new GeoLocationException(GeoLocationException.MSG_CONV_FROM_PIXEL_TO_GEO + "  " +me.getMessage());
		}	
		
	}
	
	
	 

	/**
	 * 
	 * @param statepVecInterp
	 * @param pXYZ
	 * @param timeStampInterp
	 * @return double array with 2 elements [0]=zeroDopplerTimeSmooth  [1]=sRdistSmooth
	 */
	public double[] findZeroDoppler(final List<double[]> statepVecInterp,double[] pXYZ,double[] timeStampInterp){
		try{
			int iOptFactor = 10;// to improve speed
		    int nPointsAroundMin = 100;//100;//50;
		    int nWindowLength = 7;//5;
		    double nWindowLengthSize = FastMath.floor(nWindowLength/2);
		    
		    int size=statepVecInterp.size();

		    ///**********Optimization************    
		    int sizeFactor=(int)Math.ceil((new Double(size*1.0/iOptFactor)));
		    double [][]diffSubsample = new double[sizeFactor][statepVecInterp.get(0).length];
		    double []vDistSubsample = new double[sizeFactor];
		    
		    for(int i=0;i<sizeFactor;i++){
		    	int pos=i*iOptFactor;
		    	diffSubsample[i][0]=(statepVecInterp.get(pos)[0]-pXYZ[0])*(statepVecInterp.get(pos)[0]-pXYZ[0]);
		    	diffSubsample[i][1]=(statepVecInterp.get(pos)[1]-pXYZ[1])*(statepVecInterp.get(pos)[1]-pXYZ[1]);
		    	diffSubsample[i][2]=(statepVecInterp.get(pos)[2]-pXYZ[2])*(statepVecInterp.get(pos)[2]-pXYZ[2]);
		    	vDistSubsample[i]=diffSubsample[i][0]+diffSubsample[i][1]+diffSubsample[i][2];
		    }
		    
		    
		    int idxMinSubsample=0;
		    double distMinSubsample=vDistSubsample[0];
		    for(int i=1;i<vDistSubsample.length;i++){
		    	if(distMinSubsample>vDistSubsample[i]){
		    		distMinSubsample=vDistSubsample[i];
		    		idxMinSubsample=i;
		    	}
		    }
		  //  double idxMinold = idxMinSubsample * iOptFactor;
		    double idxMin =  (idxMinSubsample ) * iOptFactor ; //Carlos to improve speed
		    
		    double[] vDist=new double[size];
		    for(int i=0;i<vDistSubsample.length;i++){
		    	for(int j=0;j<iOptFactor&&((i*iOptFactor)+j)<vDist.length;j++){
		    		vDist[(i*iOptFactor)+j]=vDistSubsample[i];
		    	}			
		    }
		    //Arrays.fill(vDist,iOptFactor*vDistSubsample.length,size,vDistSubsample[vDistSubsample.length-1]);
		///**********End Optimization************
		    
	   ////
	   // vDist is spiky, especially if a high order Hermite polynomial has been used (ie degree 12 or higher) (maybe because of precision issues??) -> smooth it with
	   // a moving average. This is done to limit the maximum potential error in zero Doppler azimuth time (ie pixel line coordinate).
	   // The smoothing may reduce the zero Doppler distance by a very small amount (millimetres), so it will not alter the pixel column coordinate
		   
		    double iDistWInit = idxMin-nPointsAroundMin;
		    if( iDistWInit <= 0) 
		    	iDistWInit = 1; 
		    double iDistWEnd = idxMin+nPointsAroundMin+1;
		    if (iDistWEnd > vDist.length)
		    	iDistWEnd = vDist.length;

		    // Refine the distance calculation around the minimum found with optimisation
		    int start=((Double)iDistWInit).intValue();
		    double[] vDistOptimization=new double[((Double)iDistWEnd).intValue()-start];
		    for(int i=start;i<iDistWEnd;i++){
		    	double v1=(statepVecInterp.get(i)[0]-pXYZ[0])*(statepVecInterp.get(i)[0]-pXYZ[0]);
		    	double v2=(statepVecInterp.get(i)[1]-pXYZ[1])*(statepVecInterp.get(i)[1]-pXYZ[1]);
		    	double v3=(statepVecInterp.get(i)[2]-pXYZ[2])*(statepVecInterp.get(i)[2]-pXYZ[2]);
		    	vDistOptimization[i-start]=v1+v2+v3;
		    }
		    //matlab code w = ones(1,nWindowLength) / nWindowLength;
		    double w[]=new double[nWindowLength];
		    Arrays.fill(w,1.0/nWindowLength);

		    
		    
		    ///TODO: problema con distMinSmooth diverso da Matlab!!! Probabilmente va controllata la convoluzione
		    double[] vdistSmooth=MathUtil.linearConvolutionMatlabValid(vDistOptimization, w);//MathArrays.convolve(vDistOptimization, w);
		    double distMinSmooth=vdistSmooth[0];
		    int idxMinSmoothW=0;
		    for(int i=0;i<vdistSmooth.length;i++){
		    	if(distMinSmooth>vdistSmooth[i]){
		    		distMinSmooth=vdistSmooth[i];
		    		idxMinSmoothW=i;
		    	}
		    }
		    int idxMinSmooth=new Double(idxMinSmoothW+iDistWInit+nWindowLengthSize+1).intValue();
		    
		    double res[]={timeStampInterp[idxMinSmooth],FastMath.sqrt(distMinSmooth)};
		    return res; 
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			throw e;
		}    
	}
	
	
	
	
	
}
