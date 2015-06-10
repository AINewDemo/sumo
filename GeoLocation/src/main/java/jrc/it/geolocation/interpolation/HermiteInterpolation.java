package jrc.it.geolocation.interpolation;

import java.util.ArrayList;
import java.util.List;

import jrc.it.geolocation.api.IInterpolation;
import jrc.it.geolocation.common.MathUtil;
import jrc.it.geolocation.exception.MathException;
import jrc.it.geolocation.metadata.S1Metadata;
import jrc.it.geolocation.metadata.IMetadata.OrbitStatePosVelox;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.util.FastMath;


public class HermiteInterpolation {//implements IInterpolation{
	
	/*double [][] interpPpointsOutput=null;
	double [][] interpVpointsOutput=null;
	double [] timeStampInterpSecondsRefOutput=null;
	*/
	public HermiteInterpolation (){

	}
	
	
	/**
	 * @param subTimesDiffRef : valori delle differenze tra i tempi e il timeref per i punti che mi interessano . Li uso per calcolare l'incremento medio (periodo di campionamento)
	 * @throws MathException 
	 * 
	 * 	
	 */
	public static void interpolation(double[] subTimesDiffRef,List<S1Metadata.OrbitStatePosVelox> vpList,
			double timeStampInitSecondsRefPointsInterp[],
			int idxInitTime, int idxEndTime,
			double deltaT,List<double[]>interpPpointsOutput,List<double[]>interpVpointsOutput,List<Double>timeStampInterpSecondsRefOutput) throws MathException {

		int nPoints=subTimesDiffRef.length;
		
//		Interpolate the state vectors, using a Hermite interpolation
//		Based on: Beaulne2005 - A Simple and Precise Approach to Position and Velocity Estimation of Low Earth Orbit Satellites
		double[][] hermiteMatrix=hermiteMatrix(nPoints);
		hermiteMatrix=invertMatrix(hermiteMatrix);

// Calculate the sampling period =periodo di campionamento calcolato con la media tra la differenza in secondi dei "tempi" nelle posizioni che interessano e il tref
		double meanTimeRef=1;//mean(subTimesDiffRef);
		
		
// Interleave position and velocity state vector components
		//m=numero righe n=numero colonne in MATLAB:[m,n]=size(statepVecPoints);
		int m=vpList.size();
		int n=3;

//matrix [2*m,n] elements		
		double[][] state=new double[2*m][n];
		for(int i=0;i<2*m;i+=2){
			OrbitStatePosVelox obj=vpList.get(i/2);
			state[i][0]=obj.px;
			state[i][1]=obj.py;
			state[i][2]=obj.pz;
			state[i+1][0]=obj.vx * meanTimeRef;
			state[i+1][1]=obj.vy * meanTimeRef;
			state[i+1][2]=obj.vz * meanTimeRef;
		}
		
// Scale time variable 
		double[] interTime=new double[timeStampInitSecondsRefPointsInterp.length];
		int index=0;
		for(double v:timeStampInitSecondsRefPointsInterp){
			interTime[index]=v/meanTimeRef;
			index++;		
		}
		
// Create descending powers array pk=(2*m-1):-1:0;
		int powerMax=2*m-1;
		
		double [][] vTmpPos = MathUtil.multiplyMatrix(hermiteMatrix,state);
		double[][] vTmpVel=new double[2*m][n];
		for(int i=0;i<2*m;i++){
			for(int j=0;j<n;j++){
				vTmpVel[i][j]=vTmpPos[i][j]/meanTimeRef;
			}
		}

		//interpPpointsOutput=new double[idxEndTime-idxInitTime+1][];
		//interpVpointsOutput=new double[idxEndTime-idxInitTime+1][];
		//timeStampInterpSecondsRefOutput=new double[idxEndTime-idxInitTime+1];
		
//Loop through desired time points and interpolate
		for(int idx=idxInitTime;idx<=idxEndTime;idx++){
   // Create polynomial powers in time for interpolation
			double[][] ptvec=new double[1][powerMax+1];
			double[][] vtvec=new double[1][powerMax+1];
			for(int i=0;i<=powerMax;i++){
				ptvec[0][i]=FastMath.pow(interTime[idx],powerMax-i);
				vtvec[0][i]=FastMath.pow((interTime[idx]),FastMath.abs(i-1));//Math.pow(i*(interTime[idx]),Math.abs(i-1));
			
			}	
			for(int i=0;i<=powerMax;i++){
				vtvec[0][powerMax-i]=i*vtvec[0][i];
			}
			interpPpointsOutput.set(idx-idxInitTime,MathUtil.multiplyMatrix(ptvec, vTmpPos)[0]);
			interpVpointsOutput.set(idx-idxInitTime,MathUtil.multiplyMatrix(vtvec, vTmpVel)[0]);
			timeStampInterpSecondsRefOutput.set(idx-idxInitTime,timeStampInitSecondsRefPointsInterp[idx]);
		
		}
	}
	
	
	/**
	 * 
	 * @param timeDiff
	 * @return
	 */
	private double mean(double[] timeDiff){
		double mean=0;
		for(int i=0;i<timeDiff.length;i++){
			mean=mean+timeDiff[i];
		}
		mean=mean/timeDiff.length;
		
		return mean;
	}
	
	
	/**
	 * 
	 * @param matrix
	 * @return
	 */
	public static double[][] invertMatrix(double[][] matrix){
		Array2DRowRealMatrix rMatrix=new Array2DRowRealMatrix(matrix);
		RealMatrix inv=MatrixUtils.inverse(rMatrix);
		double[][]invHermite=inv.getData();
		return invHermite;
	}
	
	
	/**
	 * 
	 * @param nPoint
	 * @return
	 */
	public static double[][] hermiteMatrix(int nPoint){
		int np=2*nPoint-1;
		
		List<double[]>matrix=new ArrayList<double[]>();
		
		//loop on rows
		for(int idx=0;idx<=np;idx+=2){
			double row1[]=new double[(np+1)];
			double row2[]=new double[(np+1)];
			
			//loops on columns
			for(int val=np;val>=0;val--){
			  //first two rows contains 1 where we have 0^0
				if(idx==0){
					row1[np-val]=FastMath.pow(0,val);
					row2[np-val]=FastMath.pow(0,FastMath.abs(val-1));
				}
				//
				else{
					int k=idx/2;
					row1[np-val]=FastMath.pow(k,val);
					row2[np-val]=val*FastMath.pow(k,val-1);
				}	
			}
			matrix.add(row1);
			matrix.add(row2);
		}	
		
			

		return  (double[][])matrix.toArray(new double[0][]);
		
	}
	
	/*
	public double[][] getInterpPpointsOutput() {
		return interpPpointsOutput;
	}


	public double[][] getInterpVpointsOutput() {
		return interpVpointsOutput;
	}


	public double[] getTimeStampInterpSecondsRefOutput() {
		return timeStampInterpSecondsRefOutput;
	}*/


	
	
}
