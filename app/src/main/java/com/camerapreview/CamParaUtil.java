package com.camerapreview;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;

public class CamParaUtil {
	private static final String TAG = "CamParaUtil";
	private static CamParaUtil myCamPara = null;
	private CamParaUtil(){

	}
	public static CamParaUtil getInstance(){
		if(myCamPara == null){
			myCamPara = new CamParaUtil();
			return myCamPara;
		}
		else{
			return myCamPara;
		}
	}



	/**printSupportPreviewSize
	 * @param params
	 */
	public  void printSupportPreviewSize(Camera.Parameters params){
		List<Size> previewSizes = params.getSupportedPreviewSizes();
		for(int i=0; i< previewSizes.size(); i++){
			Size size = previewSizes.get(i);
			Log.i(TAG, "previewSizes:width = "+size.width+" height = "+size.height);
		}

	}

	/**printSupportPictureSize
	 * @param params
	 */
	public  void printSupportPictureSize(Camera.Parameters params){
		List<Size> pictureSizes = params.getSupportedPictureSizes();
		for(int i=0; i< pictureSizes.size(); i++){
			Size size = pictureSizes.get(i);
			Log.i(TAG, "pictureSizes:width = "+ size.width+" height = " + size.height);
		}
	}
	/**printSupportFocusMode
	 * @param params
	 */
	public void printSupportFocusMode(Camera.Parameters params){
		List<String> focusModes = params.getSupportedFocusModes();
		for(String mode : focusModes){
			Log.i(TAG, "focusModes--" + mode);
		}
	}

	public void printSupportPictureFormat(Camera.Parameters parameters) {
		List<Integer> formats = parameters.getSupportedPictureFormats();
		for(Integer mode : formats){
			Log.i(TAG, "PictureFormat--" + mode);
		}
	}
	public void printSupportPreviewFormat(Camera.Parameters parameters) {
		List<Integer> formats = parameters.getSupportedPreviewFormats();
		for(Integer mode : formats){
			Log.i(TAG, "PreviewFormat--" + mode);
		}
	}
}
