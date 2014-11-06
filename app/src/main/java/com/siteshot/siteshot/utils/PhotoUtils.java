package com.siteshot.siteshot.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.siteshot.siteshot.models.UserPhoto;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by Andrew Clissold, Rachel Glomski, Jon Wong on 10/6/14.
 * Class that contains utilities for uploading photos to parse
 */
public class PhotoUtils {

    private static final String TAG = PhotoUtils.class.getName();
    private static PhotoUtils mInstance = null;

    private List<UserPhoto> mUserPhotos;
    private List<UserPhoto> mClusterPhotos;
    private String mCurrentPhotoPath;

    private PhotoUtils() {}

    public static PhotoUtils getInstance() {
        if (mInstance == null) {
            mInstance = new PhotoUtils();
        }
        return mInstance;
    }

    public void downloadUserPhotos() {
        ParseQuery<UserPhoto> query = UserPhoto.getQuery();
        // TODO: make this an ordered query
        query.findInBackground(new FindCallback<UserPhoto>() {
            @Override
            public void done(List<UserPhoto> resultUserPhotos, ParseException e) {
                if (e == null) {
                    mUserPhotos = resultUserPhotos;
                } else {
                    Log.e(TAG, "error retrieving user photos:");
                    e.printStackTrace();
                }
            }
        });
    }

    public void downloadClusterPhotos(String[] cluster) throws ParseException {
        List<String> test = Arrays.asList(cluster);
        ParseQuery<UserPhoto> clusterQuery = UserPhoto.getQuery();
        clusterQuery = clusterQuery.whereContainedIn("objectId", test);
        // TODO: make this an ordered query
        mClusterPhotos = clusterQuery.find();

/*            @Override
            public void done(List<UserPhoto> resultUserPhotos, ParseException e) {
                if (e == null) {
                    mClusterPhotos = resultUserPhotos;
                } else {
                    Log.e(TAG, "error retrieving user photos:");
                    e.printStackTrace();
                }
            }
        });*/
    }

    public List<UserPhoto> getUserPhotos() {
        return mUserPhotos;
    }

    public List<UserPhoto> getClusterPhotos(String[] cluster) throws ParseException {
        downloadClusterPhotos(cluster);
        return mClusterPhotos;
    }

    public List<UserPhoto> updateUserPhotos() {
        downloadUserPhotos();
        return mUserPhotos;
    }

    public String getCurrentPhotoPath() {
        return mCurrentPhotoPath;
    }

    public Bitmap uploadPhoto(byte[] data, ParseGeoPoint geoPoint, String description,
                              boolean rotateFlag, SaveCallback callback) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        return uploadPhoto(bitmap, geoPoint, description, rotateFlag, callback);
    }

    /**
     * Convert an image taken by embedded camera to a bitmap and then upload the bitmap to
     * Parse
     *
     * @param data the image taken from embedded camera
     * @param rotateFlag flag to denote rotation correction
     *
     * @return       the image converted to bitmap uploaded to Parse
     *
     */
    public Bitmap uploadPhoto(byte[] data, ParseGeoPoint geoPoint, boolean rotateFlag) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        return uploadPhoto(bitmap, geoPoint, null, rotateFlag);
    }

    /**
     * Upload an image Parse attributed to the current user
     *
     * @param bitmap the image to upload to Parse
     * @param rotateFlag flag to denote rotation correction
     *
     * @return       the same bitmap, rotated if necessary
     *
     */
    public Bitmap uploadPhoto(Bitmap bitmap, ParseGeoPoint geoPoint, String description,
                              boolean rotateFlag) {
        // Prepare the image data.
        Bitmap rotatedBitmap = rotate(bitmap, rotateFlag);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] data = stream.toByteArray();
        ParseFile file = new ParseFile("photo.jpg", data);

        // Get the current user to pre-unlock the photo.
        String username = ParseUser.getCurrentUser().getUsername();
        ArrayList<String> unlocked = new ArrayList<String>();
        unlocked.add(username);

        // Create and save the ParseObject.
        ParseObject object = ParseObject.create("UserPhoto");
        object.put("photo", file);
        object.put("location", geoPoint);
        object.put("unlocked", unlocked);
        if (description != null) { object.put("description", description); }
        object.saveInBackground();

        return rotatedBitmap;
    }

    // Overloaded version of the above for when a callback is necessary.
    public Bitmap uploadPhoto(Bitmap bitmap, ParseGeoPoint geoPoint, String description,
                              boolean rotateFlag, SaveCallback callback) {
        // Prepare the image data.
        Bitmap rotatedBitmap = rotate(bitmap, rotateFlag);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] data = stream.toByteArray();
        ParseFile file = new ParseFile("photo.jpg", data);

        // Get the current user to pre-unlock the photo.
        String username = ParseUser.getCurrentUser().getUsername();
        ArrayList<String> unlocked = new ArrayList<String>();
        unlocked.add(username);

        // Create and save the ParseObject.
        ParseObject object = ParseObject.create("UserPhoto");
        object.put("photo", file);
        object.put("location", geoPoint);
        object.put("description", description);
        object.put("unlocked", unlocked);
        object.saveInBackground(callback);

        return rotatedBitmap;
    }

    /**
     * Upload an image to be used as user profile icon to Parse
     *
     * @param bitmap the image to upload to Parse
     *
     * @return       the same bitmap, rotated if necessary
     *
     */
    public Bitmap uploadProfilePhoto(Bitmap bitmap, boolean profileRotate) {
        Bitmap rotatedBitmap = rotate(bitmap, profileRotate);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] data = stream.toByteArray();
        ParseFile file = new ParseFile("userIcon.jpg", data);
        ParseUser user = ParseUser.getCurrentUser();
        user.put("icon", file);
        user.saveInBackground();

        return rotatedBitmap;
    }

    public Bitmap rotatePreview(Bitmap bitmap, boolean shouldRotate){
        return rotate (bitmap, shouldRotate);
    }

    // method to rotate the bitmap for upload if necessary
    private Bitmap rotate(Bitmap bitmap, boolean shouldRotate) {
        boolean rotateFlag = shouldRotate;
        int iconOrientation;
        /* portrait upload by forcing 90 degree rotation of the bitmap when uploading from the
         * embedded camera
        */
        if (rotateFlag) {
            iconOrientation = 6;
        }
        else {
            iconOrientation = 1;
        }

        /* if the upload is from native camera rotate the result using the headers added by the
         * native camera. if upload is from embedded camera force 90 degree rotation
        */
        if (rotateFlag == false) {
            try {
                ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
                iconOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
            }
        }
        Matrix matrix = new Matrix();

        switch (iconOrientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * Creates the photo file on disk, ready to be written to. After calling this method,
     * getCurrentPhotoPath will return the absolute path of the file.
     *
     * @return             a new photo file
     * @throws IOException
     */
    public File createPhotoFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
