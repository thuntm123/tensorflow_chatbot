package thuntm.uet.drowsywarning;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.Toast;

import com.tzutalin.dlib.Constants;
import com.tzutalin.dlibtest.R;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
public class Instruction extends Activity {
    Button button1, button2;
    DownloadManager downloadManager;
    private String url;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        verifyStoragePermissions(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_instruction);
        final Context context = this;
        button2 = findViewById(R.id.button2);
        button1 = findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CheckForSDCard.isSDCardPresent()){
                    if(new File(Constants.getFaceShapeModelPath()).exists()){
                        Toast.makeText(Instruction.this,"Dữ liệu đã tồn tại, ko cần tải về!",Toast.LENGTH_SHORT).show();
                    }
                    else{
                        if(!new File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()+ File.separator +"shape_predictor_68_face_landmarks.dat.bz2").exists()) {
                            url = "http://dlib.net/files/shape_predictor_68_face_landmarks.dat.bz2";
                            Uri uri = Uri.parse(url);
                            Toast.makeText(Instruction.this, "Đang tải về, xin chờ trong giây lát", Toast.LENGTH_LONG).show();
                            String fileName = URLUtil.guessFileName(url, null, MimeTypeMap.getFileExtensionFromUrl(url));
                            DownloadManager.Request request = new DownloadManager.Request(uri);
                            request.setTitle(fileName);
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                            Long reference = downloadManager.enqueue(request);

                        }
                        else
                        {
                            Toast.makeText(Instruction.this,"Dữ liệu đang được giải nén, xin chờ trong giây lát",Toast.LENGTH_LONG).show();
                        }
                        coppyFile();
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(),
                            "SD Card not found", Toast.LENGTH_LONG).show();
                }
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(new File(Constants.getFaceShapeModelPath()).exists())
                {
                    Intent intent = new Intent(context, MainActivity.class);
                    startActivity(intent);
                }
                else
                {
                    Toast.makeText(Instruction.this,"Bạn chưa có dữ liệu, vui lòng nhấn nút TẢI VỀ",Toast.LENGTH_SHORT).show();
                }

            }
        });
    }
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE

    };
    public static void verifyStoragePermissions(Activity activity) {

        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission2 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
        if (permission2 != PackageManager.PERMISSION_GRANTED||permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
    private void addListenerOnButton() {


        button2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

            }

        });
    }

    public void coppyFile(){
                if (!new File(Constants.getFaceShapeModelPath()).exists()) {
                    InputStream fin = null;
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            fin = Files.newInputStream(Paths.get(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()+ File.separator +"shape_predictor_68_face_landmarks.dat.bz2"));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    BufferedInputStream in = new BufferedInputStream(fin);
                    OutputStream out = null;
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            out = Files.newOutputStream(Paths.get(Constants.getFaceShapeModelPath()));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    BZip2CompressorInputStream bzIn = null;
                    try {
                        bzIn = new BZip2CompressorInputStream(in);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try{
                        final byte[] buffer = new byte[20480];
                        int n = 0;
                        while (-1 != (n = bzIn.read(buffer))) {
                            out.write(buffer, 0, n);
                        }
                        out.close();
                        bzIn.close();
                        Toast.makeText(Instruction.this,"Giải nén xong, bạn có thể BẮT ĐẦU",Toast.LENGTH_LONG).show();
                    }catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (bzIn!= null) {
                                bzIn.close();
                            }
                            if (out != null) {
                                out.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }

    }


