/*
 * Calculates a running average of the last "length" values
 * Used for smoothing text movements
 * */
public class InputAverager {

	int[] avgArray = new int[1000];
	double[] avgArray2 = new double[1000];
	double[] rateArray = new double[4];
	
	public InputAverager(){
		
	}
	
	public int[] getArray(){
		return avgArray;
	}
	public double[] getArrayD(){
		return avgArray2;
	}
	
	public double[] getArrayR(){
		return rateArray;
	}
	
	public int getPrevious(){
	
		return avgArray[0];
		
	}
	
	public double returnAvgD(int length){//returns current average

		double total = 0;

		for(int i = 0; i < length; i++){

			total = total + avgArray2[i];
		}

		return ((total/(double)length));
	}
	
	public double nextD(double input, int length){
		
		for(int i = length; i >= 0; i--){

			avgArray2[i+1] = avgArray2[i];
		}
		//input is the next item to be averaged
		avgArray2[0]=input;
		//length is the number of items to average
		
		double total = 0;
		
		for(int i = 0; i < length; i++){

			total = total + avgArray2[i];
		}
		
		return ((total/(double)length));
	}
	
	public int next(int input, int length){
		
		for(int i = length; i >= 0; i--){

			avgArray[i+1] = avgArray[i];
		}
		//input is the next item to be averaged
		avgArray[0]=input;
		//length is the number of items to average
		
		int total = 0;
		
		for(int i = 0; i < length; i++){

			total = total + avgArray[i];
		}
		
		return ((int) Math.ceil((double)(total/length)));
	}
	
	//returns whether or not the acceleration of the next element is valid based on accelerationThresh
	public boolean rateLimiterNext(double input, double accelerationThresh){
		//input = number to be rate limited
		//length = number of elements in array to compare
		//thresh = acceleration threshold 
		
		//shift array and add input
		for(int i = 2; i >= 0; i--){

			rateArray[i+1] = rateArray[i];
		}
		
		//input is the next item to be averaged
		rateArray[0]=input;
		
		// | x0 | x1 | x2 | x3 |  (taken from array)
		//   | v0 | v1 | v2 |  (calculated)
		//      | a0 | a1 |   (calculated)
		
		double v0 = Math.abs(rateArray[0]-rateArray[1]);
		double v1 = Math.abs(rateArray[1]-rateArray[2]);
		double v2 = Math.abs(rateArray[2]-rateArray[3]);
		
		double a0 = Math.abs(v0 - v1);
		double a1 = Math.abs(v1 - v2);
		

		
		//if acceleration > accelerationThresh, limit, predict next spot based on current velocity/acceleration
		
		//if 
		if(a0<=accelerationThresh){
			System.out.println(a0+"");
			return true;
		}
		else{

//			System.out.println("Positions    : "+"|"+rateArray[0]+"|"+rateArray[1]+"|"+rateArray[2]+"|"+rateArray[3]+"false");
//			System.out.println("Velocities   : "+"   |"+v0+"|"+v1+"|"+v2+"|");
//			System.out.println("Accelerations: "+"      |"+a0+"|"+a1);
//			System.out.println(a0+"false");
			return false;
		}
	}
	

public static void main(String[] args) {
	
	InputAverager test = new InputAverager();
//	System.out.println(test.nextD(-0.08726654946804047, 10));
//	System.out.println(test.nextD(-0.08726654946804047, 10));
//	System.out.println(test.nextD(-0.08726654946804047, 10));
//	for(int i = 0; i < 10; i++){
//
//		System.out.println(test.getArrayD()[i]);
//		
//	}
//	System.out.println(test.nextD(-0.08726654946804047, 10));
//	System.out.println(test.nextD(-0.08726654946804047, 10));
//	System.out.println(test.nextD(-0.08726654946804047, 10));
//	System.out.println(test.nextD(-0.08726654946804047, 10));
//	System.out.println(test.nextD(-0.08726654946804047, 10));
//	System.out.println(test.nextD(-0.08726654946804047, 10));
//	System.out.println(test.nextD(-0.08726654946804047, 10));
	
	System.out.println(test.rateLimiterNext(1, 1));
	System.out.println(test.rateLimiterNext(5, 1));
	System.out.println(test.rateLimiterNext(10, 1));
	System.out.println(test.rateLimiterNext(21, 1));
	
	for(int i = 0; i < 4; i++){

		System.out.println(test.getArrayR()[i]);
		
	}
	
}



}//end class
