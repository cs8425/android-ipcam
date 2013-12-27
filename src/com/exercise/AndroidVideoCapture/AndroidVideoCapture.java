package com.exercise.AndroidVideoCapture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.body.FileBody;
import com.koushikdutta.async.http.body.JSONObjectBody;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.StreamBody;
import com.koushikdutta.async.http.body.StringBody;
import com.koushikdutta.async.http.callback.HttpConnectCallback;
import com.koushikdutta.async.http.socketio.Acknowledge;
import com.koushikdutta.async.http.socketio.StringCallback;

public class AndroidVideoCapture extends Activity{
	
	private Camera myCamera;
    private MyCameraSurfaceView myCameraSurfaceView;
    //private MediaRecorder mediaRecorder;

	Button pollButton;
	Button chButton;
	Button takeButton;
	Button netButton;
	Button autonetButton;
	SurfaceHolder surfaceHolder;
	boolean record;
	boolean poll;
	boolean Pictureing;
	boolean autoing;
	int camid = 0;
	String server_url = "";

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        record = false;
        poll = false;
        Pictureing = false;
        autoing = false;
        
        setContentView(R.layout.main);
        
        pollButton = (Button)findViewById(R.id.mybutton);
        pollButton.setOnClickListener(pollButtonOnClickListener);
        
        chButton = (Button)findViewById(R.id.chcambtn);
        
        //camid = Camera.getNumberOfCameras()-1;
        chButton.setText(""+camid);
        chButton.setOnClickListener(chButtonOnClickListener);
        
        takeButton = (Button)findViewById(R.id.takebutton);
        takeButton.setOnClickListener(taButtonOnClickListener);
        
        netButton = (Button)findViewById(R.id.socket);
        netButton.setOnClickListener(netButtonOnClickListener);
        
        autonetButton = (Button)findViewById(R.id.auto);
        autonetButton.setOnClickListener(autoButtonOnClickListener);
        
        //Get Camera for preview
        myCamera = getCameraInstance();
        if(myCamera == null){
        	Toast.makeText(AndroidVideoCapture.this, 
        			"Fail to get Camera", 
        			Toast.LENGTH_LONG).show();
        	finish();
        }

        myCameraSurfaceView = new MyCameraSurfaceView(this, myCamera);
        FrameLayout myCameraPreview = (FrameLayout)findViewById(R.id.videoview);
        myCameraPreview.addView(myCameraSurfaceView);
        
    }
    
    Button.OnClickListener pollButtonOnClickListener = new Button.OnClickListener(){
		@Override
		public void onClick(View v) {
			if(poll){
				poll = false;
				pollButton.setText("long-poll");
			}else{
				poll = true;
				pollButton.setText("STOP");
				longpoll();
				try{
		    		Parameters parameters = myCamera.getParameters();
		    		parameters.set("jpeg-quality", 60);
		    		parameters.setPictureFormat(PixelFormat.JPEG);
		    		List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
		    		Size size = sizes.get(0);
		    		parameters.setPictureSize(size.width, size.height);
		    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
		    		myCamera.setParameters(parameters);
				}
				catch(Exception ex){
					ex.printStackTrace();
				}
				myCamera.setPreviewCallback(previewCallback);
				Toast.makeText(AndroidVideoCapture.this, "long-polling start",Toast.LENGTH_LONG).show();
			}

		}};
    Button.OnClickListener autoButtonOnClickListener = new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				if(autoing){
					autonetButton.setText("auto Take(net)");
					autoing = false;
				}else{
					autonetButton.setText("STOP");
					autoing = true;
					try{
			    		Parameters parameters = myCamera.getParameters();
			    		parameters.set("jpeg-quality", 60);
			    		parameters.setPictureFormat(PixelFormat.JPEG);
			    		List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
			    		Size size = sizes.get(0);
			    		parameters.setPictureSize(size.width, size.height);
			    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
			    		myCamera.setParameters(parameters);
					}
					catch(Exception e){
						e.printStackTrace();
					}
					myCamera.setPreviewCallback(previewCallback);
				}
	}};

    Button.OnClickListener chButtonOnClickListener = new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				camid++;
				if(camid == Camera.getNumberOfCameras()) camid = 0;
				chButton.setText("" + camid);
				releaseCamera();
				myCamera = getCameraInstance();
				if(myCamera == null){
					Toast.makeText(AndroidVideoCapture.this, 
		        			"Fail to get Camera", 
		        			Toast.LENGTH_LONG).show();
		        	finish();
				}
				try {
					myCamera.setPreviewDisplay(myCameraSurfaceView.getHolder());
		            myCamera.startPreview();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	};
    Button.OnClickListener taButtonOnClickListener = new Button.OnClickListener(){
    	@Override
        public void onClick(View v) {
    		if(!Pictureing){
	    		Pictureing = true;
	            myCamera.takePicture(null, null, mPicture);
            }
        }
	};
	
	private PictureCallback mPicture = new PictureCallback() {
	    @Override
	    public void onPictureTaken(byte[] data, Camera camera) {
		    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	        File pictureFile = new File("/sdcard/"+timeStamp+".jpg");
	        if (pictureFile == null){
	        	Toast.makeText(AndroidVideoCapture.this, "Error creating media file, check storage permissions", 
        			Toast.LENGTH_LONG).show();
	            return;
	        }
	        try {
	            FileOutputStream fos = new FileOutputStream(pictureFile);
	            fos.write(data);
	            fos.close();
	            myCamera.startPreview();
	            Pictureing = false;
	        } catch (FileNotFoundException e) {
	        	Toast.makeText(AndroidVideoCapture.this, "File not found: " + e.getMessage(),Toast.LENGTH_LONG).show();
	        } catch (IOException e) {
	        	Toast.makeText(AndroidVideoCapture.this, "Error accessing file: " + e.getMessage(),Toast.LENGTH_LONG).show();
	        }
	    }
	};
    Button.OnClickListener netButtonOnClickListener = new Button.OnClickListener(){
    	@Override
        public void onClick(View v) {
    		if(!Pictureing){
	    		Pictureing = true;
				try{
		    		Parameters parameters = myCamera.getParameters();
		    		parameters.set("jpeg-quality", 60);
		    		parameters.setPictureFormat(PixelFormat.JPEG);
		    		List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
		    		Size size = sizes.get(0);
		    		parameters.setPictureSize(size.width, size.height);
		    		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
		    		myCamera.setParameters(parameters);
				}
				catch(Exception e){
					e.printStackTrace();
				}
	    		myCamera.takePicture(null, null, netPicture);
    		}
        }
	};
	private PictureCallback netPicture = new PictureCallback() {
	    @Override
	    public void onPictureTaken(byte[] data, Camera camera) {
	    	try {
	            AsyncHttpPost post = new AsyncHttpPost("http://134.208.0.206:5900/data");
	    		//AsyncHttpPost post = new AsyncHttpPost("http://192.168.1.96:2000/data");
	            ByteBody ok = new ByteBody(data);
	            post.setBody(ok);
	            AsyncHttpClient.getDefaultInstance().execute(post, new HttpConnectCallback() {
					@Override
					public void onConnectCompleted(Exception e,
							AsyncHttpResponse response) {
						//Toast.makeText(AndroidVideoCapture.this, "send.",Toast.LENGTH_LONG).show();
					}
	            });
	        }
	        catch (Exception ex) {
	            ex.printStackTrace();
	        }
	    	myCamera.startPreview();
	    	Pictureing = false;
	    }
	};
	
	Camera.PreviewCallback previewCallback = new Camera.PreviewCallback()  
    {
            public void onPreviewFrame(byte[] data, Camera camera)  
            {
            	if(autoing || record){
                    try 
                    {
                    	Camera.Parameters parameters = camera.getParameters();
                        Size size = parameters.getPreviewSize();
                        YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                        Rect rectangle = new Rect();
                        rectangle.bottom = size.height;
                        rectangle.top = 0;
                        rectangle.left = 0;
                        rectangle.right = size.width;
                        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
                        image.compressToJpeg(rectangle, 60, out2);

        	            AsyncHttpPost post = new AsyncHttpPost("http://134.208.0.206:5900/data");
        	            ByteBody ok = new ByteBody(out2.toByteArray());
        	            post.setBody(ok);
        	            AsyncHttpClient.getDefaultInstance().execute(post, new HttpConnectCallback() {
        					@Override
        					public void onConnectCompleted(Exception e,
        							AsyncHttpResponse response) {
        						//Toast.makeText(AndroidVideoCapture.this, "send.",Toast.LENGTH_LONG).show();
        					}
        	            });
                    }
                    catch(Exception ex)
                    {
                    	ex.printStackTrace();
                    }
            	}
            }
    }; 
	private void longpoll(){
		if(poll){
	        AsyncHttpClient.getDefaultInstance().getString("http://134.208.0.206:5900/poll", new AsyncHttpClient.StringCallback(){
				@Override
				public void onCompleted(Exception e, AsyncHttpResponse response, String result) {
					if (e != null) {
			            e.printStackTrace();
			            Toast.makeText(AndroidVideoCapture.this, "err",Toast.LENGTH_LONG).show();
				        longpoll();
			            return;
			        }
			        System.out.println("I got a string: " + result);
					if(result.equals("ON")){
						record = true;
					}else{
						if(result.equals("OFF")){
							record = false;
						}
					}
					Toast.makeText(AndroidVideoCapture.this, result,Toast.LENGTH_LONG).show();
			        longpoll();
				}
	        });
		}
	}
    private Camera getCameraInstance(){
        Camera c = null;
        try {
            //c = Camera.open(Camera.getNumberOfCameras()-1); // attempt to get a Camera instance
        	c = Camera.open(camid);
        	//c.setPreviewCallback(previewCallback);
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
	}
	
    // not work
	/*private boolean prepareMediaRecorder(){
	    myCamera = getCameraInstance();
	    mediaRecorder = new MediaRecorder();

	    myCamera.unlock();
	    mediaRecorder.setCamera(myCamera);

	    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
	    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

	    //mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
	    //mediaRecorder.setProfile(CamcorderProfile.get(1, CamcorderProfile.QUALITY_HIGH));
	    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
	    //mediaRecorder.setVideoSize(720, 480);
	    //mediaRecorder.setVideoFrameRate(20);

	    mediaRecorder.setOutputFile("/sdcard/myvideo.mp4");
        //mediaRecorder.setMaxDuration(60000); // Set max duration 60 sec.
        //mediaRecorder.setMaxFileSize(5000000); // Set max file size 5M

	    mediaRecorder.setPreviewDisplay(myCameraSurfaceView.getHolder().getSurface());
	    myCamera.release();
	    try {
	        mediaRecorder.prepare();
	    } catch (IllegalStateException e) {
	        releaseMediaRecorder();
	        return false;
	    } catch (IOException e) {
	        releaseMediaRecorder();
	        return false;
	    }
	    return true;

	}*/
	
    @Override
    protected void onPause() {
        super.onPause();
        //releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
        autoing = false;
    }

    /*private void releaseMediaRecorder(){
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            myCamera.lock();           // lock camera for later use
        }
    }*/

    private void releaseCamera(){
        if (myCamera != null){
        	myCamera.stopPreview();
            myCamera.release();        // release the camera for other applications
            myCamera = null;
        }
    }
	
	public class MyCameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback{

		private SurfaceHolder mHolder;
	    private Camera mCamera;
		
		public MyCameraSurfaceView(Context context, Camera camera) {
	        super(context);
	        mCamera = camera;

	        // Install a SurfaceHolder.Callback so we get notified when the
	        // underlying surface is created and destroyed.
	        mHolder = getHolder();
	        mHolder.addCallback(this);
	        // deprecated setting, but required on Android versions prior to 3.0
	        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	    }

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int weight,
				int height) {
	        // If your preview can change or rotate, take care of those events here.
	        // Make sure to stop the preview before resizing or reformatting it.

	        if (mHolder.getSurface() == null){
	          // preview surface does not exist
	          return;
	        }

	        // stop preview before making changes
	        try {
	            mCamera.stopPreview();
	        } catch (Exception e){
	          // ignore: tried to stop a non-existent preview
	        }

	        // make any resize, rotate or reformatting changes here

	        // start preview with new settings
	        try {
	            mCamera.setPreviewDisplay(mHolder);
	            //mCamera.setPreviewCallback(previewCallback);
	            mCamera.startPreview();

	        } catch (Exception e){
	        }
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			// The Surface has been created, now tell the camera where to draw the preview.
	        try {
	            mCamera.setPreviewDisplay(holder);
	            //mCamera.setPreviewCallback(previewCallback);
	            mCamera.startPreview();
	        } catch (IOException e) {
	        }
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			
		}
	}
}
