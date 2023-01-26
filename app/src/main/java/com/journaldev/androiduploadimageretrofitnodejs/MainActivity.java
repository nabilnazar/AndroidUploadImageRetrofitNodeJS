package com.journaldev.androiduploadimageretrofitnodejs;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    ApiService apiService;
    Uri picUri;
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    private final static int ALL_PERMISSIONS_RESULT = 107;
    private final static int IMAGE_RESULT = 200;
    FloatingActionButton fabCamera, fabUpload;
    Bitmap mBitmap;
    TextView textView,phashview,exifview;
    String exif;
   boolean location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fabCamera = findViewById(R.id.fab);
        fabUpload = findViewById(R.id.fabUpload);
        exifview = findViewById(R.id.exifvalue);
        textView = findViewById(R.id.textView);
        fabCamera.setOnClickListener(this);
        fabUpload.setOnClickListener(this);

        askPermissions();
        initRetrofitClient();


    }

    private void askPermissions() {
        permissions.add(CAMERA);
        permissions.add(WRITE_EXTERNAL_STORAGE);
        permissions.add(READ_EXTERNAL_STORAGE);
        permissionsToRequest = findUnAskedPermissions(permissions);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


            if (permissionsToRequest.size() > 0)
                requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
        }
    }

    private void initRetrofitClient() {

        final OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

        apiService = new Retrofit.Builder().baseUrl("https://c06c-103-194-69-162.ngrok.io").client(client).build().create(ApiService.class);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public Intent getPickImageChooserIntent() throws IOException {

        Uri outputFileUri = getCaptureImageOutputUri();


        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = getPackageManager();


        Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

            }
            allIntents.add(intent);
        }


        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {

            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            allIntents.add(intent);
        }

        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {

            if (intent.getComponent().getClassName().equals("com.android.documentsui.DocumentsActivity")) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);

        Intent chooserIntent = Intent.createChooser(mainIntent, "Select source");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;

    }

    private Uri getCaptureImageOutputUri() throws IOException {
        Uri outputFileUri = null;
        File getImage = getExternalCacheDir();
        if (getImage != null) {
            outputFileUri = Uri.fromFile(new File(getImage.getPath(), "profile.png"));
            InputStream inputStream = getContentResolver().openInputStream(outputFileUri);
            ExifInterface exifInterface = new ExifInterface(inputStream);

            exif = "Exif: ";
            exif += "\nIMAGE_LENGTH: " +
                    exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
            exif += "\nIMAGE_WIDTH: " +
                    exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
            exif += "\n DATETIME: " +
                    exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
            exif += "\n TAG_MAKE: " +
                    exifInterface.getAttribute(ExifInterface.TAG_MAKE);
            exif += "\n TAG_MODEL: " +
                    exifInterface.getAttribute(ExifInterface.TAG_MODEL);
            exif += "\n TAG_ORIENTATION: " +
                    exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
            exif += "\n TAG_WHITE_BALANCE: " +
                    exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
            exif += "\n TAG_FOCAL_LENGTH: " +
                    exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
            exif += "\n TAG_FLASH: " +
                    exifInterface.getAttribute(ExifInterface.TAG_FLASH);
            exif += "\nGPS related:";
            exif += "\n TAG_GPS_DATESTAMP: " +
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_DATESTAMP);
            exif += "\n TAG_GPS_TIMESTAMP: " +
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP);
            exif += "\n TAG_GPS_LATITUDE: " +
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            exif += "\n TAG_GPS_LATITUDE_REF: " +
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            exif += "\n TAG_GPS_LONGITUDE: " +
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            exif += "\n TAG_GPS_LONGITUDE_REF: " +
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
            exif += "\n TAG_GPS_PROCESSING_METHOD: " +
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD);

            inputStream.close();

        }

        return outputFileUri;
    }
                /*
                ExifInterface (InputStream inputStream) added in API level 24
                 */






    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        if (resultCode == Activity.RESULT_OK) {

            ImageView imageView = findViewById(R.id.imageView);

            if (requestCode == IMAGE_RESULT) {


                String filePath = null;
                try {
                    filePath = getImageFilePath(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (filePath != null) {
                    mBitmap = BitmapFactory.decodeFile(filePath);
                    imageView.setImageBitmap(mBitmap);

                    phashview = findViewById(R.id.phash_value);
                    ImagePHash ab;
                    ab = new ImagePHash();
                    phashview.setText(ab.culcPHash(mBitmap));
                    exifview.setText(exif);
                    exifview.setMovementMethod(new ScrollingMovementMethod());






                }
            }

        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private String getImageFromFilePath(Intent data) throws IOException {

        boolean isCamera = data == null || data.getData() == null;
        location = isCamera;


        if (isCamera) return getCaptureImageOutputUri().getPath();

        else return getPathFromURI(data.getData());


    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public String getImageFilePath(Intent data) throws IOException {
        return getImageFromFilePath(data);
    }

    private String getPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Audio.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("pic_uri", picUri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // get the file url
        picUri = savedInstanceState.getParcelable("pic_uri");
    }

    private ArrayList<String> findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList<String> result = new ArrayList<String>();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case ALL_PERMISSIONS_RESULT:
                for (String perms : permissionsToRequest) {
                    if (!hasPermission(perms)) {
                        permissionsRejected.add(perms);
                    }
                }

                if (permissionsRejected.size() > 0) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            requestPermissions(permissionsRejected.toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                        }
                                    });
                            return;
                        }
                    }

                }

                break;
        }

    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void multipartImageUpload() {
        try {
            File filesDir = getApplicationContext().getFilesDir();
            File file = new File(filesDir, "image" + ".png");


            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
            byte[] bitmapdata = bos.toByteArray();

            System.out.println(String.valueOf(file));
            System.out.println();

            ImagePHash ah;
            ah = new ImagePHash();
            String.valueOf(ah.culcPHash(mBitmap));

            //if (location == true) {
                System.out.println(exif);
         //   }
         //   else {exif="NA";}

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();



            RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("upload", file.getName(), reqFile);
            MultipartBody.Part Exiff = MultipartBody.Part.createFormData("Exiff",exif);
            MultipartBody.Part Phash = MultipartBody.Part.createFormData("Phash",String.valueOf(ah.culcPHash(mBitmap)));
            RequestBody name = RequestBody.create(MediaType.parse("text/plain"), "upload");

            Call<ResponseBody> req = apiService.postImage(body, name,Phash,Exiff);

            req.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                    if (response.code() == 200) {
                        textView.setText("Uploaded Successfully!");
                        textView.setTextColor(Color.BLUE);
                    }

                    Toast.makeText(getApplicationContext(), response.code() + " ", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    textView.setText("Uploaded Failed!");
                    textView.setTextColor(Color.RED);
                    Toast.makeText(getApplicationContext(), "Request failed", Toast.LENGTH_SHORT).show();
                    t.printStackTrace();
                }
            });


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab:
                try {
                    startActivityForResult(getPickImageChooserIntent(), IMAGE_RESULT);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.fabUpload:
                if (mBitmap != null)
                    multipartImageUpload();
                else {
                    Toast.makeText(getApplicationContext(), "Bitmap is null. Try again", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

}
