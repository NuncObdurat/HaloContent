import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_highgui;
import org.bytedeco.javacpp.opencv_highgui.CvCapture;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.VideoInputFrameGrabber;

import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_calib3d.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;


public class HaloContent implements Runnable{

	/*
	 * Main class for HaloContent
	 * Holds the FaceManager and ContentManager objects
	 * Gets camera input and runs the face detection algorithm
	 * Renders content based on the output of ContentManager
	 * */

	//face classifier
	CvHaarClassifierCascade faceDetector;


	//return of the web cam image for re-display (not necessary when rendering to optical see-through)
	BufferedImage originalImage = null;
	
	//holds the image accessed from opencv
	IplImage img2 = null;	
	
	//define classifiers 
	static CvHaarClassifierCascade cascade = new
			CvHaarClassifierCascade(cvLoad("haarcascade_frontalface_alt.xml"));
	static CvHaarClassifierCascade cascade2 = new
			CvHaarClassifierCascade(cvLoad("haarcascade_mcs_eyepair_big.xml"));
	
	//initial number of content blocks
	int contentBlocks = 4;
	
	//variable that holds the number of faces detected for a frame
	int total_Faces = 5;

	//for drawing the final bufferedimage to the frame
	JFrame frame = new JFrame();
	JPanel imageHolder = new JPanel();
	
	//Holds currently detected face objects
	FaceManager faces = new FaceManager();

	//temp holder for writing images to file
	BufferedImage tempCloneBuff = null;

	//temp variables for holding positions and dimensions of detected faces
	int rx = 0;
	int ry = 0;
	int rw = 0;
	int rh = 0;

	//Array positions (centers of text)
	//TODO These are currently defined manually, but should be defined programatically
	int[] L1 = {320, 240};
	int[] L2 = {213, 240, 426, 240};
	int[] L3 = {160, 240, 320, 240, 480, 240};
	int[] L4 = {128, 320, 256, 160, 426, 160, 512, 320};
	int[] L5 = {128, 320, 160, 160, 320, 160, 480, 160, 512, 320};
	int[] L6 = {128, 320, 128, 160, 256, 160, 384, 160, 512, 160, 512, 320};
	int[] L7 = {128, 320, 128, 160, 256, 160, 384, 160, 512, 160, 512, 320, 320, 320};

	//# of frames to average, more = smoother
	int dampener = 10; 

	//Object that averages the last n items of input
	InputAverager L1ax = new InputAverager();
	InputAverager L2ax = new InputAverager();
	InputAverager L3ax = new InputAverager();
	InputAverager L4ax = new InputAverager();
	InputAverager L5ax = new InputAverager();
	InputAverager L6ax = new InputAverager();
	InputAverager L7ax = new InputAverager();

	InputAverager L1ay = new InputAverager();
	InputAverager L2ay = new InputAverager();
	InputAverager L3ay = new InputAverager();
	InputAverager L4ay = new InputAverager();
	InputAverager L5ay = new InputAverager();
	InputAverager L6ay = new InputAverager();
	InputAverager L7ay = new InputAverager();

	//holds updates to the positions of each object 
	static Object[] arrayPositions = new Object[7];


	public HaloContent(){
		
		//initialize detector
		
		//use eyes (more robust, but more false positives)
		//		faceDetector = new CvHaarClassifierCascade(cvLoad("haarcascade_eye.xml"));
		
		//use faces
		faceDetector = new CvHaarClassifierCascade(cvLoad("haarcascade_frontalface_alt_tree.xml"));

		//initialize frame for redisplay
		frame.setSize(640, 480);
		frame.setVisible(true);
		frame.getContentPane().add(imageHolder);
		
		
		//keylistener allowing user to add/remove content via arrow keys
		frame.addKeyListener(new KeyListener() {


			@Override
			public void keyPressed(KeyEvent arg0) {

				//add a block if up is pressed
				if(arg0.getKeyCode()==38){
					if(contentBlocks<7){
						contentBlocks = contentBlocks+1;
					}
				}

				//remove a block if down is pressed
				if(arg0.getKeyCode()==40){
					if(contentBlocks>1){
						contentBlocks = contentBlocks-1;
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent arg0) {

			}

			@Override
			public void keyTyped(KeyEvent arg0) {

			}
		});



		//Add block layouts to an accessible array
		arrayPositions[0] = L1;
		arrayPositions[1] = L2;
		arrayPositions[2] = L3;
		arrayPositions[3] = L4;
		arrayPositions[4] = L5;
		arrayPositions[5] = L6;
		arrayPositions[6] = L7;

	}//end constructor

	/*
	 * Does face detection for a single frame and loads any faces found into face array
	 */
	public void detect(IplImage src){

		//initialize storage for the object detector
		CvMemStorage storage = CvMemStorage.create();
		CvSeq sign = cvHaarDetectObjects(src, faceDetector, storage, 1.3, 3, CV_HAAR_DO_CANNY_PRUNING);

		cvClearMemStorage(storage);

		//pass face array info stored in CvSeq to FaceManager here for processing (once per frame)
		//faces object should then be up-to-date with the latest persistent faces
		faces.newFaces(sign, null);

		//list the number of faces detected
		total_Faces = sign.total();	

		for(int i = 0; i < total_Faces; i++){
			CvRect r = new CvRect(cvGetSeqElem(sign, i));
			
			//load face dimensions into faces array
			rx = r.x();
			ry = r.y();
			rw = r.width();
			rh = r.height();

		}

	}	

	//runs the actual Halo Content movement algorithm and draws updated elements on the input image
	public BufferedImage haloContent(IplImage img2){
		detect(img2);

		originalImage = img2.getBufferedImage();
		

		Graphics g = originalImage.getGraphics();

		Graphics2D g2d=(Graphics2D)g;

		g2d.setFont(new Font( "SansSerif", Font.BOLD, 16 ));
		g2d.setColor(Color.green);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setStroke( new BasicStroke( 5.0f ) );


		//FaceManager working as of 9/5/2014
		for(int i = 0; i<faces.getFaces().size();i++){
			Face temp = (Face) faces.getFaces().get(i);
			
			//for debug just draw an x over all faces returned by FaceManager
			//			g2d.drawString("X", temp.x+temp.width/2, temp.y+temp.height/2);        		
		}

		// 7 default layouts (1-7 blocks of text)
		// if ContentManager doesn't return new positions, display all as normal;
		//create an extra copy of locations for comparision in the case of multiple faces
		//only the farthest content blocks from this whole iteration should be rendered in the end

		int[] temp = (int[])arrayPositions[contentBlocks-1];
		int[] tempLocations = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

		for(int t = 0;t<contentBlocks;t++){
			//fill tempLocations with respective array from arrayPositions
			tempLocations[2*t] = temp[2*t];
			tempLocations[2*t+1] = temp[2*t+1];
		}

		for(int j = 0; j<faces.getFaces().size();j++){
			Face faceForProcessing = (Face) faces.getFaces().get(j);

			for(int i = 0; i<contentBlocks; i++){

				//starting coordinates of content
				int x1 = temp[2*i];
				int y1 = temp[2*i+1];

				//centers of content blocks
				int contentCenterx = x1;
				int contentCentery = y1;

				//location of origin (bottom center)
				int originx = 320;
				int originy = 400;

				//center of face
				int faceCenterx = faceForProcessing.x+faceForProcessing.width/2;
				int faceCentery = faceForProcessing.y+faceForProcessing.height/2;

				//calculate angle of travel here (based on lower center), should be layout independent
				float slope = 0;
				try{
					//this slope is relative to a point on the bottom center of the screen @ 400,320
					slope = -(float)(y1-originy)/(float)(x1-originx);
				}catch(ArithmeticException div0){
				}

				//calculate distance of travel based on spring/force/gap threshold 
				//	        				sp("squarey: "+((ry+rh/2))+" squarex:"+((rx+rw/2)));//+" rh:"+rh+" x1:"+x1+" rx:"+rx+" rw:"+rw);

				//distance between original x,y of text block and head (working as of 9/4/2014)
				int distance = (int) Math.sqrt(Math.pow(y1-(faceCentery),2)+Math.pow(x1-(faceCenterx),2));

				//distance between center of head and line representing trajectory of content block
				//formula: http://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
				//a = 1, b = -slope : from( y = mx + b -> b = 0 : y - mx = 0 )
				int x0 = -(faceCenterx - originx);
				int y0 = faceCentery - originy;

				//this calculation is correct
				int contentToOriginDistance = (int) Math.sqrt(
						Math.pow(contentCenterx-originx, 2)+Math.pow(contentCentery-originy, 2));


				//this calculation is correct
				int faceToOriginDistance = (int) Math.sqrt(
						Math.pow(faceCenterx-originx,2)+Math.pow(faceCentery-originy,2));


				float a = (contentCentery - originy);
				float b = (contentCenterx - originx);

				int distanceLine = (int) (Math.abs(a*x0+b*y0)/
						Math.sqrt(Math.pow(a,2)+Math.pow(b,2)));

				//				int xinter = (int) (b*(b*x0-a*y0)/(Math.pow(a,2)+Math.pow(b,2)))+originx;
				//				int yinter = (int) (a*(-b*x0+a*y0)/(Math.pow(a,2)+Math.pow(b,2)))+originy;


				//if distance less than N pix, move text @ slope angle by N-distance pix
				//This is effectively D_min in the paper
				int N = 230;

				int dx = x1-(rx+rw/2);
				int dy = y1-(ry+rh/2);
				int dTotal = 0;

				if(distance<N){
					dx = N-distance;
					dy = (int)(dx*slope);
				}

				int x2 = x1+dx;
				int y2 = y1+dy;

				//if distance of face to Content trajectory line is less than N &&
				//   distance from face to origin is greater than distance from content to origin minus ????? -> 
				// 		add (N minus distanceLine +  distance from origin to face) -> new x2, y2 of content 
				if (distanceLine<N && faceToOriginDistance > contentToOriginDistance - N && x1 >= 320){
					dTotal = faceToOriginDistance + (N-distanceLine); //TODO need to account for head size if possible (in facemgr)
					float theta = (float) Math.atan(slope);

					x2 = (int) (dTotal*Math.cos(theta))+originx;
					y2 = -(int) (dTotal*Math.sin(theta))+originy;

				}
				else if (distanceLine<N && faceToOriginDistance > contentToOriginDistance - N && x1 < 320){
					dTotal = faceToOriginDistance + (N-distanceLine); 
					float theta = (float) Math.atan(slope)+(float)Math.PI;

					x2 = (int) (dTotal*Math.cos(theta))+originx;
					y2 = -(int) (dTotal*Math.sin(theta))+originy;

				}

				else{
					x2 = x1;
					y2 = y1;
				}

				if(total_Faces==0){
					x2 = x1;
					y2 = y1;
				}

				//accounts for multiple faces (takes distance to nearest face for every face) 
				int currentDistToOrigin = (int) Math.sqrt(
						Math.pow(x2-originx, 2)+Math.pow(y2-originy, 2));
				int storedDistToOrigin = (int) Math.sqrt(
						Math.pow(tempLocations[2*i]-originx, 2)+Math.pow(tempLocations[2*i+1]-originy, 2));
				
				if(currentDistToOrigin>storedDistToOrigin){
					//if distance to new face greater than stored face, set new x,y values for content
					tempLocations[2*i] = x2;
					tempLocations[2*i+1] = y2;
				}
				
				
				//for debugging 	        					
				
				
				//renderBlock(g2d, "News "+ i , tempLocations[2*i], tempLocations[2*i+1], 100, 100);

				//for debug, draw origin, face center, original content center, and lines from origin to centers
				//Origin
				//g2d.drawString("O", originx, originy);
				//Face centers
//				g2d.setFont(new Font("courier", Color.green.getRGB(), 30));
//				g2d.drawString("O", 200, 200);
//				g2d.setFont(new Font("courier", Color.green.getRGB(), 50));
//				
//				g2d.drawString("O", 290, 200);
				//g2d.drawString("FtoO: "+faceToOriginDistance+ "  FtoL: "+distanceLine, faceCenterx, faceCentery);

				//Original content center
				//g2d.drawString("OrigC"+contentToOriginDistance, x1, y1);
				//line from origin to face
				//g2d.drawLine(originx,originy,faceCenterx,faceCentery);
				//line from origin to content
				//g2d.drawLine(originx,originy,x2, y2);

			}//end increment through all content elements

		}//end increment through all faces

		//TODO implement application priority?
		
		//averages last "averagingLength" positions to smooth out movement
		for(int i = 0; i<contentBlocks; i++){

			int x = tempLocations[2*i];
			int y = tempLocations[2*i+1];

			int averagingLength = 6;

			if(i==0){
				x = L1ax.next(tempLocations[2*i], averagingLength); 
				y = L1ay.next(tempLocations[2*i+1], averagingLength); 
			}
			if(i==1){
				x = L2ax.next(tempLocations[2*i], averagingLength); 
				y = L2ay.next(tempLocations[2*i+1], averagingLength); 
			}
			if(i==2){
				x = L3ax.next(tempLocations[2*i], averagingLength); 
				y = L3ay.next(tempLocations[2*i+1], averagingLength); 
			}
			if(i==3){
				x = L4ax.next(tempLocations[2*i], averagingLength); 
				y = L4ay.next(tempLocations[2*i+1], averagingLength); 
			}
			if(i==4){
				x = L5ax.next(tempLocations[2*i], averagingLength); 
				y = L5ay.next(tempLocations[2*i+1], averagingLength); 
			}
			if(i==5){
				x = L6ax.next(tempLocations[2*i], averagingLength); 
				y = L6ay.next(tempLocations[2*i+1], averagingLength); 
			}

			if(i==6){
				x = L7ax.next(tempLocations[2*i], averagingLength); 
				y = L7ay.next(tempLocations[2*i+1], averagingLength); 
			}
			//			x = temp[2*i];
			//			y = temp[2*i+1];

			//draw the block on the image (with some text)
			renderBlock(g2d, "News "+ i , x, y, 120, 100);

		}//end increment through all content elements

		return originalImage;
	}

	//loop for grabbing frames from camera
	@Override
	public void run() {

		//initialize a frame grabber
		CvCapture grabber = opencv_highgui.cvCreateCameraCapture(2);
		
		//set resolutions if necessary, current layouts are for 640x480
//		System.out.println(opencv_highgui.cvSetCaptureProperty(grabber, opencv_highgui.CV_CAP_PROP_FRAME_WIDTH,800));
//		System.out.println(opencv_highgui.cvSetCaptureProperty(grabber, opencv_highgui.CV_CAP_PROP_FRAME_HEIGHT,600));
		
		
		try {

			//start the grabber
			opencv_highgui.cvQueryFrame(grabber);
			
			//the loop
			while (true) {

				img2 = opencv_highgui.cvQueryFrame(grabber);
				
				if (img2 != null) {

					//run halo content on the grabbed image and show it on the canvas
					imageHolder.getGraphics().drawImage((Image)haloContent(img2), 0, 0, null);

				}

			}
		} catch (Exception e) {sp("error foo: "+e);}

	}//end Run 

	/*
	 * Renders a single block of text based on a string, x,y starting value, and width and height of the block
	 * */
	public void renderBlock(Graphics2D graphicsIn, String text, int x, int y, int width, int height){
		graphicsIn.drawString(text, x+5+15-width/2, y+20+35-height/2);
		graphicsIn.drawRect(x-width/2, y-height/2, width, height);
	}
	

	//a shorter way to printline
	public static void sp(String s){
		System.out.println(s);
	}

	public static void main(String[] args) {
		HaloContent l = new HaloContent();
		Thread th = new Thread(l); th.start();
		sp("Welcome to HaloContent.  Press up to add a block of content and down to remove one.");

	}//end main

}//end class
