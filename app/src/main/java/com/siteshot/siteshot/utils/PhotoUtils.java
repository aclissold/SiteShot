package com.siteshot.siteshot.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by aclissold on 10/6/14.
 */
public class PhotoUtils {

    private static final String TAG = PhotoUtils.class.getName();

    private String mCurrentPhotoPath;

    public String getCurrentPhotoPath() {
        return mCurrentPhotoPath;
    }

    public Bitmap uploadPhoto(Bitmap bitmap) {
        Bitmap rotatedBitmap = rotate(bitmap);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] data = stream.toByteArray();
        ParseFile file = new ParseFile("photo.jpg", data);
        ParseObject object = new ParseObject("UserPhoto");
        object.put("photo", file);
        object.saveInBackground();

        return rotatedBitmap;
    }

    /**
     *
     * @param bitmap the image to upload to Parse
     *
     * @return       the same bitmap, rotated if necessary
     */
    public Bitmap uploadProfilePhoto(Bitmap bitmap) {
        Bitmap rotatedBitmap = rotate(bitmap);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] data = stream.toByteArray();
        ParseFile file = new ParseFile("userIcon.jpg", data);
        ParseUser user = ParseUser.getCurrentUser();
        user.put("icon", file);
        user.saveInBackground();

        return rotatedBitmap;
    }

    private Bitmap rotate(Bitmap bitmap) {
        int iconOrientation = 1;

        try {
            ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
            iconOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
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
