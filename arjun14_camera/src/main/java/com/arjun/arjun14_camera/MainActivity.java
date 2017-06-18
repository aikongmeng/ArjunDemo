package com.arjun.arjun14_camera;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import youtu.Youtu;
import youtu.common.Config;

import static youtu.common.Config.SECRET_ID;
import static youtu.common.Config.SECRET_KEY;

public class MainActivity extends AppCompatActivity {


    EditText mPath, mName, mType;
    ImageView mPhoto, mPhoto2;

    private Handler handler;
    ProgressDialog dialog;
    private String datapath;
    private Youtu youtu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPath = (EditText) findViewById(R.id.editText);
        mName = (EditText) findViewById(R.id.editText2);
        mType = (EditText) findViewById(R.id.editText3);
        mPhoto = (ImageView) findViewById(R.id.imageView);
        mPhoto2 = (ImageView) findViewById(R.id.imageView2);

        datapath = getFilesDir() + "/tesseract/";
        //make sure training data has been copied
        checkFile(new File(datapath + "tessdata/"));

        handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                dialog.cancel();


            }
        };

        dialog = new ProgressDialog(this);
        dialog.setMessage("识别中");
        youtu = new Youtu(Config.APP_ID, SECRET_ID, SECRET_KEY, Youtu.API_YOUTU_END_POINT);
    }

    public void take(View view) {
        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        String pathStr = mPath.getText().toString();
        String nameStr = mName.getText().toString();
        String typeStr = mType.getText().toString();
        if (!TextUtils.isEmpty(pathStr)) {
            intent.putExtra("path", pathStr);
        }
        if (!TextUtils.isEmpty(nameStr)) {
            intent.putExtra("name", nameStr);
        }
        if (!TextUtils.isEmpty(typeStr)) {
            intent.putExtra("type", typeStr);
        }
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("TAG", "onActivityResult");
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                String path = extras.getString("path");
                String type = extras.getString("type");
                Log.e("arjun", path);
                Toast.makeText(getApplicationContext(), "path:" + path + " type:" + type, Toast.LENGTH_LONG).show();
                File file = new File(path);
                FileInputStream inStream = null;
                try {
                    inStream = new FileInputStream(file);
                    final Bitmap bitmap = BitmapFactory.decodeStream(inStream);
                    mPhoto.setImageBitmap(bitmap);
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();

                    //int x, int y, int width, int height
                    final Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 100, height - 100, width - 200, 60);
                    final Bitmap bitmap3 = Bitmap.createBitmap(bitmap, 195, 350, 400, 60);

                    mPhoto2.setImageBitmap(bitmap2);

                    dialog.show();

                    //youtu 识别
                    new Thread() {
                        @Override
                        public void run() {
                            //SendHttpsRequest
                            JSONObject respose = null;
                            try {
                                respose = youtu.IdcardOcr(bitmap, 0);

                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (KeyManagementException e) {
                                e.printStackTrace();
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            }
                            Log.d("youtu", respose.toString());

                        }
                    }.start();


                    new Thread() {
                        @Override
                        public void run() {
                            String chi_sim = doOcr(bitmap3, "chi_sim");
                            Log.e("result", "2--->" + chi_sim);
                            Message msg = new Message();
                            handler.sendMessage(msg);
                        }
                    }.start();
                    new Thread() {
                        @Override
                        public void run() {
                            String chi_sim = doOcr(bitmap, "chi_sim");
                            Log.e("result", "0--->" + chi_sim);
                            Message msg = new Message();
                            handler.sendMessage(msg);
                        }
                    }.start();
                    new Thread() {
                        @Override
                        public void run() {

                            String chi_sim = doOcr(bitmap2, "chi_sim");
                            Log.e("result", "1--->" + chi_sim);

                            Message msg = new Message();
                            handler.sendMessage(msg);

                        }
                    }.start();

                    inStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public String doOcr(Bitmap bitmap, String language) {


        TessBaseAPI baseApi = new TessBaseAPI();

        baseApi.init(datapath, language);

        // 必须加此行，tess-two要求BMP必须为此配置
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        baseApi.setImage(bitmap);

        String text = baseApi.getUTF8Text();

        baseApi.clear();
        baseApi.end();

        return text;
    }

    /**
     * 获取sd卡的路径
     *
     * @return 路径的字符串
     */
    public static String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();// 获取外存目录
        }
        return sdDir.toString();
    }


    private void checkFile(File dir) {
        //directory does not exist, but we can successfully create it
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles();
        }
        //The directory exists, but there is no data file in it
        if (dir.exists()) {
            String datafilepath = datapath + "/tessdata/chi_sim.traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {
            //location we want the file to be at
            String filepath = datapath + "/tessdata/chi_sim.traineddata";

            //get access to AssetManager
            AssetManager assetManager = getAssets();

            //open byte streams for reading/writing
            InputStream instream = assetManager.open("tessdata/chi_sim.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            //copy the file to the location specified by filepath
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
