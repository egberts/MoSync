/* Copyright 2013 David Axmark

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.mosync.internal.android;

import static com.mosync.internal.generated.MAAPI_consts.EVENT_TYPE_IMAGE_PICKER;
import static com.mosync.internal.generated.MAAPI_consts.MA_IMAGE_PICKER_EVENT_RETURN_TYPE_IMAGE_HANDLE;
import static com.mosync.internal.generated.MAAPI_consts.MA_IMAGE_PICKER_EVENT_RETURN_TYPE_IMAGE_DATA;
import static com.mosync.internal.generated.MAAPI_consts.MA_IMAGE_PICKER_ITEM_ENCODING_JPEG;
import static com.mosync.internal.generated.MAAPI_consts.MA_IMAGE_PICKER_ITEM_ENCODING_PNG;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.mosync.internal.android.MoSyncThread.ImageCache;
import com.mosync.java.android.MoSync;

/**
 * Helper class for maImagePickerOpen syscall.
 * @author emma tresanszki
 *
 */
public class MoSyncImagePicker {

	// Events for when the Ok or Cancel button are hit.
	final static int PICKER_CANCELED = 0;
	final static int PICKER_READY = 1;

	// The user can choose what kind of data the PICKER_READY event will contain.
	/**
	 * The default type: The event will contain a handle to the image object.
	 * Use this approach if you wish to handle the image object locally on the device.
	 * Note that the image is downsampled due to memory issues on bitmap objects.
	 */
	final static int EVENT_TYPE_IMAGE_HANDLE = MA_IMAGE_PICKER_EVENT_RETURN_TYPE_IMAGE_HANDLE;
	/**
	 * The event will contain a handle to a data object.
	 * Use this approach if you want to handle the raw data on a machine with high-performance.
	 * Note that on Android devices, attempting to create an image object from the data object
	 * (by calling maCreateImageFromData) might cause OutOfMemory exceptions.
	 */
	final static int EVENT_TYPE_DATA_HANDLE = MA_IMAGE_PICKER_EVENT_RETURN_TYPE_IMAGE_DATA;

	//-------------------------- IMPLEMENTATION --------------------------//

	/**
	 * Constructor.
	 * @param thread The MoSync thread.
	 * @param imageTable The bitmap table.
	 */
	public MoSyncImagePicker(MoSyncThread thread,Hashtable<Integer, ImageCache> imageTable, int eventType)
	{
		mMoSyncThread = thread;
		mImageTable = imageTable;
		mEventReturnType = eventType;
	}

	/**
	 * Launch the image picker activity.
	 */
	public void loadGallery()
	{
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        mMoSyncThread.getActivity().startActivityForResult(intent, MoSync.PICK_IMAGE_REQUEST);
	}

	/**
	 * Handle the result after a picture was picked(the control went back to MoSync).
	 * @param intent
	 */
	public static void handleSelectedPicture(Intent data)
	{
		Uri pictureUri = data.getData();

		if (pictureUri == null)
		{
			Log.e("@@MoSync",
					"maImagePickerOpen Error: cannot get image,some error occured.");
			return;
		}
		else
		{
			ContentResolver cr = mMoSyncThread.getActivity()
					.getContentResolver();

			if ( mEventReturnType == EVENT_TYPE_IMAGE_HANDLE )
			{
				try {
					/**
					 * Decoding options used for bitmaps. First get the image
					 * dimensions. Based on the image size perform a scaling.
					 */
					BitmapFactory.Options bfo = new BitmapFactory.Options();
					bfo.inJustDecodeBounds = true;
					bfo.inDither = false;
					bfo.inPreferredConfig = Bitmap.Config.RGB_565;

					BitmapFactory.decodeStream(cr.openInputStream(pictureUri),
							null, bfo);

					// Calculate sample size to keep image under maxFileSize.
					int maxFileSize = 1572864; // in bytes
					int sampleSize = 1;
					long fileSize = 2 * (bfo.outWidth / sampleSize) * (bfo.outHeight / sampleSize);
					while (fileSize > maxFileSize)
					{
						sampleSize++;
						fileSize = 4 * (bfo.outWidth / sampleSize)* (bfo.outHeight / sampleSize);
					}

					/**
					 * Decode to a smaller image to save memory and run faster.
					 * Decode image using calculated sample size.
					 */
					bfo.inSampleSize = sampleSize;
					bfo.inJustDecodeBounds = false;
					Bitmap imageBitmap = BitmapFactory.decodeStream(
							cr.openInputStream(pictureUri), null, bfo);

					if (imageBitmap != null)
					{
						String mimeType = bfo.outMimeType;
						int encodingType = -1;
						if ( mimeType.equalsIgnoreCase("image/jpeg") )
							encodingType = MA_IMAGE_PICKER_ITEM_ENCODING_JPEG;
						else if( mimeType.equalsIgnoreCase("image/png") )
							encodingType = MA_IMAGE_PICKER_ITEM_ENCODING_PNG;

						// Get the handle of the selected item and post event.
						postImagePickerReady(getSelectedImageHandle(imageBitmap), encodingType);
					}
					else
					{
						Log.i("@@MoSync",
								"maImagePickerOpen Error: cannot decode bitmap");
						return;
					}

				} catch (FileNotFoundException e)
				{
					e.printStackTrace();
					Log.i("@@MoSync",
							"maImagePickerOpen Error: cannot find bitmap");
				}
			}
			else if( mEventReturnType == EVENT_TYPE_DATA_HANDLE )
			{
				BitmapFactory.Options bfo = new BitmapFactory.Options();
				bfo.inJustDecodeBounds = false;
				bfo.inDither = false;
				bfo.inPreferredConfig = Bitmap.Config.ARGB_8888;

				try{
					Bitmap imageBitmap = BitmapFactory.decodeStream(
							cr.openInputStream(pictureUri), null, bfo);

					int bytes = imageBitmap.getWidth()*imageBitmap.getHeight()*4;
					// Calculate how many bytes our image consists of. Use a different value than 4 if you don't use 32bit images.

					int imageData = mMoSyncThread.createDataObject(0, readBytes(pictureUri, bytes));
					// Second option would be: ( keep it until final commit)
					/**
					// Create a new buffer
					ByteBuffer buffer = ByteBuffer.allocate(bytes);//400000);
					// Move the byte data to the buffer
					imageBitmap.copyPixelsToBuffer(buffer);
					// Get the underlying array containing the data.
					byte[] array = buffer.array(); */

					String mimeType = bfo.outMimeType;
					int encodingType = -1;
					if ( mimeType.equalsIgnoreCase("image/jpeg") )
						encodingType = MA_IMAGE_PICKER_ITEM_ENCODING_JPEG;
					else if( mimeType.equalsIgnoreCase("image/png") )
						encodingType = MA_IMAGE_PICKER_ITEM_ENCODING_PNG;

					postImagePickerReady(imageData, encodingType);

				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				} catch (IOException e)
				{
//					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}
			}
		}
	}

	/**
	 * Get the array of bytes from an image object.
	 * @param uri The initial uri object.
	 * @return The array of bytes.
	 * @throws IOException
	 */
	public static byte[] readBytes(Uri uri, int maxSize)
		throws IOException
	{
		ContentResolver cr = mMoSyncThread.getActivity().getContentResolver();
        // This dynamically extends to take the bytes we read.
		InputStream inputStream = cr.openInputStream(uri);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // This is storage overwritten on each iteration with bytes.
//        int bufferSize = 1024;
        byte[] buffer = new byte[maxSize];

        // We need to know how may bytes were read to write them to the byteBuffer.
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
          byteBuffer.write(buffer, 0, len);
        }

        // and then we can return the byte array.
        return byteBuffer.toByteArray();
    }

    /**
     * Get the handle of the selected item.
     * Further, post it in a EVENT_TYPE_IMAGE_PICKER event.
     * @return The new handle.
     */
    private static int getSelectedImageHandle(Bitmap imageBitmap)
    {
        // Create handle.
        int dataHandle = mMoSyncThread.nativeCreatePlaceholder();

		mImageTable.put(dataHandle, new ImageCache(null, imageBitmap));

        return dataHandle;
    }

	/**
	 * Handle the result after a picture was picked( the control went back to MoSync).
	 * @param intent
	 */
	public static void handleCancelSelectPicture()
	{
		postImagePickerCanceled();
	}

	/**
	 * Post event to MoSync queue.
	 * @param imageHandle The image handle of the selected image.
	 */
	private static void postImagePickerReady(int imageHandle, int mimeType)
	{
		int[] event = new int[4];
		event[0] = EVENT_TYPE_IMAGE_PICKER;
		event[1] = PICKER_READY;
		// If Cancel is clicked, the handle is -1.
		event[2] = imageHandle;
		event[3] = mimeType;

		mMoSyncThread.postEvent(event);
	}

	/**
	 * Post event to MoSync queue.
	 */
	private static void postImagePickerCanceled()
	{
		int[] event = new int[3];
		event[0] = EVENT_TYPE_IMAGE_PICKER;
		event[1] = PICKER_CANCELED;
		// If Cancel is clicked, the handle is -1.
		event[2] = -1;

		mMoSyncThread.postEvent(event);
	}

    //--------------------------   Members   --------------------------//

	/**
	 * The MoSync thread object.
	 */
    private static MoSyncThread mMoSyncThread;

    /**
     * It has access to the image resource table.
     */
    private static Hashtable<Integer, ImageCache> mImageTable;

    /**
     * The user can choose what kind of data the PICKER_READY event will contain.
     */
    private static int mEventReturnType = EVENT_TYPE_IMAGE_HANDLE;
}