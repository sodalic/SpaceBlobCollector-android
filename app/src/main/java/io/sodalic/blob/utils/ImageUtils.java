package io.sodalic.blob.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.support.media.ExifInterface;
import android.util.Log;
import android.support.annotation.NonNull;

import io.sodalic.blob.BuildConfig;

import static android.os.Environment.DIRECTORY_PICTURES;

/**
 * Class with some helper methods for image processing
 */
public final class ImageUtils {
    private static final String TAG = Utils.getLogTag(ImageUtils.class);

    private ImageUtils() {
    }

    @NonNull
    private static Matrix getImageRotationMatrix(@NonNull File inputImageFile) throws IOException {
        ExifInterface exif = new ExifInterface(inputImageFile.getAbsolutePath());
        int orientationId = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Log.i(TAG, StringUtils.formatEn("image orientationID for '%s' is = %d", inputImageFile, orientationId));
        Matrix matrix = new Matrix();
        switch (orientationId) {
            case ExifInterface.ORIENTATION_NORMAL:
                // do nothing!
                break;
            case ExifInterface.ORIENTATION_UNDEFINED:
                Log.w(TAG, StringUtils.formatEn("Orientation is undefined for '%s'", inputImageFile));
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                throw new IllegalStateException(StringUtils.formatEn("Unexpected image orientation %d", orientationId));
        }
        return matrix;
    }

    private static void rotateImageFile(@NonNull File inputImageFile, @NonNull File rotatedImageFile, @NonNull Matrix matrix) throws IOException {
        Bitmap origBitmap = BitmapFactory.decodeFile(inputImageFile.getAbsolutePath());
        Bitmap rotateBitmap = Bitmap.createBitmap(origBitmap, 0, 0, origBitmap.getWidth(), origBitmap.getHeight(), matrix, false);
        origBitmap.recycle();

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(rotatedImageFile))) {
            rotateBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
        }
        rotateBitmap.recycle();
    }

    /**
     * This method analyzes EXIF of the JPEG image referenced by {@code inputImageFile} and returns a {@link File} object
     * that references an image rotated to the normal orientation. If no transformation was done the {@code inputImageFile}
     * is returned as is. Otherwise a new file is created next to {@code inputImageFile}
     *
     * @param inputImageFile input file with a JPEG image
     * @return The file with the normalized JPEG. Beware, depending on the image rotation this might or might not be the same as {@code inputImageFile}
     * @throws IOException
     */
    @NonNull
    public static File normalizeImageFileRotation(@NonNull File inputImageFile) throws IOException {
        Matrix matrix = getImageRotationMatrix(inputImageFile);
        if (matrix.isIdentity())
            return inputImageFile;

        String inputFileName = inputImageFile.getName();
        int dot_pos = inputFileName.lastIndexOf('.');
        String rotatedFileName = inputFileName.substring(0, dot_pos) + "_rot" + inputFileName.substring(dot_pos);
        File rotatedImageFile = new File(inputImageFile.getParentFile(), rotatedFileName);

        rotateImageFile(inputImageFile, rotatedImageFile, matrix);

        return rotatedImageFile;
    }


    /**
     * @param context      - any Android {@link Context}
     * @param ensureExists - if {@code true}, then the directory will also be created. Note that it means that
     *                     it expects that {@link Manifest.permission#WRITE_EXTERNAL_STORAGE} permission
     *                     has already been granted.
     * @return Returns a {@link File} pointing to a directory to put temporary images such as selfie images saved
     * for further processing.
     * Note that in the {@link BuildConfig#APP_IS_DEV} this is a publicly available folder
     * and otherwise it is a usual application-data folder
     */
    @NonNull
    public static File getTempImageDir(@NonNull Context context, boolean ensureExists) {
        File dir;
        if (BuildConfig.APP_IS_DEV) {
            File baseDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES);
            dir = new File(baseDir, StringUtils.getApplicationName(context));
        } else {
            File baseDir = context.getExternalFilesDir(null);
            dir = new File(baseDir, "face");
        }
        if (ensureExists && !dir.exists() && !dir.mkdirs()) {
            // should never happen in the real life
            Log.e(TAG, StringUtils.formatEn("Failed to make the image directory '%s'", dir));
            throw new RuntimeException(StringUtils.formatEn("Failed to make the image directory '%s'", dir));
        }
        return dir;
    }

    /**
     * The same as {@link #getTempImageDir(Context, boolean)} with {@code ensureExists = true}.
     * It means this call expects that {@link Manifest.permission#WRITE_EXTERNAL_STORAGE} permission
     * has already been granted.
     */
    public static File getTempImageDir(@NonNull Context context) {
        return getTempImageDir(context, true);
    }

}
