package com.scaletools.recipecathcer.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.scaletools.recipecathcer.helper.ImageCatcher;
import com.scaletools.recipecathcer.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This fragment was constructed from a code example
 * "code for showing a picture/camera selector that is used in our Android app"
 * WARNING!!! Should be replaced with a proper fragment
 */
public class ChoosingFragment extends Fragment {
    //region ChoosingFragment from email
    private static final int SELECT_PICTURE_REQUEST = 10;
    private static final int CAMERA_REQUEST = 11;
    private final String TAG = "ChoosingFragment";
    private File mImageFile;
    private Context context;
    private ImageCatcher imageCatcher;

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment DrawingFragment.
     */
    public static ChoosingFragment newInstance(ImageCatcher catcher) {
        ChoosingFragment fragment = new ChoosingFragment();
        fragment.setImageCatcher(catcher);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_choosing, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView button = (ImageView) view.findViewById(R.id.blackView);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                showImageChooser();
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    public void clickImageChoose(View v) {
        showImageChooser();
    }

    public void showImageChooser()
    {
        List<Intent> intentList = new ArrayList<>();

        Intent pickIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        addIntentsToList(pickIntent, intentList);

        try
        {
            if (mImageFile == null)
            {
                mImageFile = createImageFile();
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "Could not create image file for capture");
        }

        if (mImageFile != null)
        {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            {
                Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Uri imageUri = Uri.fromFile(mImageFile);
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                takePhotoIntent.putExtra(android.provider.MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                addIntentsToList(takePhotoIntent, intentList);
            }
            else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.READ_CONTACTS)) {

                    Log.d(TAG, "Camera permission not granted");
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_REQUEST);
                    return;

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }

            if (!intentList.isEmpty())
            {
                if (intentList.size() > 1)
                {
                    Intent chooserIntent = Intent.createChooser(intentList.remove(intentList.size() - 1), "Select picture source");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                            intentList.toArray(new Parcelable[intentList.size()]));

                    Log.d(TAG, "Starting image chooser activity");
                    startActivityForResult(chooserIntent, SELECT_PICTURE_REQUEST);
                }
                else
                {
                    Log.d(TAG, "Starting single image intent");
                    startActivityForResult(intentList.get(0), SELECT_PICTURE_REQUEST);
                }
            }
            else
                Log.w(TAG, "No image intents found");
        }
    }

    private void addIntentsToList(Intent intent, List<Intent> intentList)
    {
        List<ResolveInfo> resInfo = getActivity().getPackageManager().queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resInfo)
        {
            String packageName = resolveInfo.activityInfo.packageName;
            Intent targetedIntent = new Intent(intent);
            targetedIntent.setPackage(packageName);
            boolean contains = false;
            for(Intent i : intentList)
            {
                if (i.equals(targetedIntent))
                {
                    contains = true;
                    break;
                }
            }
            if (!contains)
                intentList.add(targetedIntent);
        }
    }

    private File createImageFile() throws IOException
    {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(new Date());
        String imageFileName = timeStamp + '_';
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_PICTURE_REQUEST)
        {
            Uri uri = null;
            if (data != null && (data.getAction() == null || !data.getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE)))
            {
                // User picked an existing file
                uri = data.getData();
            }

            final Uri uriCopy = uri;
            new AsyncTask<Void, Void, Exception>()
            {
                protected Exception doInBackground(Void... unused)
                {
                    RuntimeException exception = null;
                    try
                    {
                        // Rescale the image if it is larger than 1920 px in any dimension
                        final int maxSize = 1920;
                        Bitmap bitmap;
                        if (uriCopy == null)
                        {
                            Log.d(TAG, "Decoding image capture with file " + mImageFile.getPath());
                            bitmap = BitmapFactory.decodeFile(mImageFile.getPath());
                            // We may have to wait a while until the bitmap has finished saving...
                            for (int retries = 0; bitmap == null && retries != 10; ++retries)
                            {
                                try
                                {
                                    Log.d(TAG, "Bitmap is null, sleeping...");
                                    Thread.sleep(200);
                                }
                                catch (InterruptedException e)
                                {
                                }
                                bitmap = BitmapFactory.decodeFile(mImageFile.getPath());
                            }
                            if (bitmap == null)
                            {
                                exception = new RuntimeException("Failed to load the picture from " + mImageFile.getAbsolutePath());
                                Log.e(TAG, exception.getMessage());
                            }
                        }
                        else
                        {
                            bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uriCopy);
                            if (bitmap == null)
                            {
                                exception = new RuntimeException("Failed to load the picture from " + uriCopy);
                                Log.e(TAG, exception.getMessage());
                            }
                        }
                        if (bitmap != null)
                        {
                            Log.d(TAG, String.format("File 1 length: %d Width: %d Height: %d", mImageFile.length(), bitmap.getWidth(), bitmap.getHeight()));
                            if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize)
                            {
                                float scaleFactor = Math.max(bitmap.getWidth() / (float) maxSize, bitmap.getHeight() / (float) maxSize);
                                bitmap = Bitmap.createScaledBitmap(bitmap, Math.round(bitmap.getWidth() / scaleFactor), Math.round(bitmap.getHeight() / scaleFactor), true);
                            }
                            mImageFile.createNewFile();
                            FileOutputStream fos = new FileOutputStream(mImageFile);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                            fos.close();
                            Log.d(TAG, String.format("File 2 length: %d Width: %d Height: %d", mImageFile.length(), bitmap.getWidth(), bitmap.getHeight()));

                        }
                    }
                    catch(Exception e)
                    {
                        exception = new RuntimeException("Failed to scale the picture from " + mImageFile.getAbsolutePath(), e);
                        Log.e(TAG, exception.getMessage());
                    }

                    if (exception != null)
                        removeSavedPhoto();

                    return exception;
                }

                protected void onPostExecute(Exception exception)
                {
                    if (exception != null || mImageFile == null)
                    {
                        Toast.makeText(context, exception.getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        final BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inMutable = true;
                        Bitmap bitmap = BitmapFactory.decodeFile(mImageFile.getAbsolutePath(), options);
                        imageCatcher.catchImage(bitmap);
                    }
                }
            }.execute();
        }
    }

    private void removeSavedPhoto() {

    }

    public void setImageCatcher(ImageCatcher imageCatcher) {
        this.imageCatcher = imageCatcher;
    }
    //endregion
}
