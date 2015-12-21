package jrc.it.geolocation.common;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.FastMath;

import jrc.it.geolocation.exception.MathException;

public class MathUtil {

	public static double[] powArray(double a[],double pow){
		double[]  apow=new double[a.length];
		for(int i=0;i<a.length;i++){
			apow[i]=FastMath.pow(a[i], pow);
		}
		return apow;
	}
	
	/**
	 * 
	 * @param val
	 * @param pows
	 * @return
	 */
	public static double[] powValue2Coeffs(double val,int pows[]){
		double[]  apow=new double[pows.length];
		for(int i=0;i<pows.length;i++){
			apow[i]=FastMath.pow(val, pows[i]);
		}
		return apow;
	}
	
	/**
	 * divide each element of vector for val
	 * 
	 * @param vector
	 * @param val
	 * @return
	 */
	public static double[] divVectByVal(double[] vector,double val){
		double[]  r=new double[vector.length];
		for(int i=0;i<vector.length;i++){
			r[i]=vector[i]/val;
		}
		return r;
	}
	
	
	
	 /**
	  *  return the Euclidean norm of this Vector
	  * @return
	  */
    public static double norm(double[] v) {
        return FastMath.sqrt(dot(v,v));
    }
    
    /**
     * return the scalar product 
     * @param v1,v2 should have the same length
     * @return
     */
    public static double dot(double[] v1,double[] v2) {
        double sum = 0.0;
        for (int i = 0; i < v1.length; i++)
            sum = sum + (v1[i] * v2[i]);
        return sum;
    }
    
    /**
     * cross product    (a2b3-a3b2)^i+(a3b1-a1b3)^j+(a1b2-a2b1)^k
     * @param v1,v2 should have the same length
     * @return
     */
    public static double[] crossProd3x3(double[] a,double[] b) {
        double p[] = new double[a.length];
        //for (int i = 0; i < v1.length; i++)
        //    p[i] = v1[i] * v2[i];
        p[0]=a[1]*b[2]-a[2]*b[1];
        p[1]=a[2]*b[0]-a[0]*b[2];
        p[2]=a[0]*b[1]-a[1]*b[0];
        
        return p;
    }
    
    public static double[][] transpose(double[][] m) {
        // returns the transpose of this matrix object
        double[][] t = new double[m[0].length][m.length];
        int i, j;
        for (i = 0; i<m.length; i++) {
          for (j = 0; j<m[0].length; j++) {
        	  t[j][i] = m[i][j];
          }
        }
        return t;
      }
    
    
    /**
     * 
     * @param a
     * @param b
     * @return
     * @throws MathException 
     */
    public static double[][] multiplyMatrix(double[][] m1,double[][] m2) throws MathException{
    	int m1ColLength = m1[0].length; // m1 columns length
        int m2RowLength = m2.length;    // m2 rows length
        if(m1ColLength != m2RowLength) 
        	throw new MathException("Wrong matrix dimension "); // matrix multiplication is not possible
        int mRRowLength = m1.length;    // m result rows length
        int mRColLength = m2[0].length; // m result columns length
        double[][] mResult = new double[mRRowLength][mRColLength];
        for(int i = 0; i < mRRowLength; i++) {         // rows from m1
            for(int j = 0; j < mRColLength; j++) {     // columns from m2
                for(int k = 0; k < m1ColLength; k++) { // columns from m1
                    mResult[i][j] += m1[i][k] * m2[k][j];
                }
            }
        }
        return mResult;
		
	}
	
	/**
	 * Convolution U(m)=Sum( H(m)X(n-m) )    0<=m=<(size(H)-1) n=size(H)
	 * 
	 * This function reproduce the matlab conv function with shape='valid'
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static double[] linearConvolutionMatlabValid(double a[],double b[]){
		//size of the array result without zero padd values
		int sizeResult=a.length+b.length-1;
		int matlabSizeResult=a.length-b.length+1;
		
		b=ArrayUtils.add(b,0);
		
		double[] u=new double[sizeResult];
		int idU=0;
		for(int n=0;n<sizeResult;n++){
			double val=0;

			for(int m=0;m<=n;m++){
				int idx1=m;
				int idx2=n-m;
				
				if(idx2>=0&&idx1>=0&&idx2<b.length-1&&idx1<a.length){
					val=val+a[idx1]*b[idx2];
				}	
			}
			u[idU]=val;
			idU++;
		}
		int diff=(sizeResult-matlabSizeResult);
		int idxStart=diff/2+diff%2;
		int idxEnd=u.length-diff/2;
		u=ArrayUtils.subarray(u, idxStart, idxEnd);
		
		
		return u; 
	}
    
}
