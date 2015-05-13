import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.VideoInputFrameGrabber;

import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_calib3d.*;



public class FaceManager {
	
	/*
	 * Designed to manage faces in the user's field of view
	 * Handles face persistence 
	 * Passes an array of faces to FaceDetect
	 * This array holds all face instances (including multiples of the same face) that should be
	 * used to determine new content positions
	 * */
	
	//persistance threshold
	int N = 10;

	//Holds faces
	ArrayList<Face> faceHolder = new ArrayList<Face>();
	
	public FaceManager(){
	}//end Constructor
	
	//takes in list of new faces, removes old faces from existing array, and adds new ones 
	public void newFaces(CvSeq inputFaceArray, CvSeq secondInputFaceArray){
		
		//increment through existing array, decrement all face persistences by 1
		for(int i=0; i< faceHolder.size();i++){
			Face temp = (Face) faceHolder.get(i);
			temp.persistence--;
			
			if(temp.persistence==0){
				//remove any faces with a persistance of 0 or less
				faceHolder.remove(i);
				i--; //TODO if an element is deleted, decrement i by one?? check this
			}//end if
		}//end decrementation/removal loop

		//add new faces to faceHolder with a peristance of N
		for(int i=0; i< inputFaceArray.total();i++){
			//take face size into account
				CvRect r = new CvRect(cvGetSeqElem(inputFaceArray, i));
				if(r.width()>20){
					Face temp = new Face(r.x(), r.y(), r.width(), r.height(), N);
					faceHolder.add(temp);
				}
		}//end addition of new faces
		 
		//add second set of new faces to faceHolder with a peristance of N
		if(secondInputFaceArray!=null){
			for(int i=0; i< secondInputFaceArray.total();i++){
				CvRect r = new CvRect(cvGetSeqElem(inputFaceArray, i));
				Face temp = new Face(r.x(), r.y(), r.width(), r.height(), N);
				faceHolder.add(temp);
			}//end addition of new faces
		}//end if
		 
		
	}//end inputFaces
	
	public void removeAllFaces(){
		faceHolder = new ArrayList<Face>();
	}
	
	//returns all faces (held in faceHolder array)
	public ArrayList<Face> getFaces(){
		
		//returned faces will have a persistance greater than 0. 
		return faceHolder;
		
	}//end getfaces

}//end class


