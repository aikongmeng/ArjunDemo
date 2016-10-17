package com.example.arjun02;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.SyncStateContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptor;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;

import static com.example.arjun02.R.id.mapView;
import static com.example.arjun02.R.id.textView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Arjun";
    private static final int MEDIA_TYPE_IMAGE = 0x001;
    private static final int CAPTURE_IMAGE_REQUEST_CODE = 0x100;
    private Button button;
    private static String path;
    private TextView txt;
    private float lat, lng;
    private MapView mMapView = null;
    private AMap aMap;
    private MarkerOptions markerOption;
    private Button btnLocation;
    //声明mLocationOption对象
    public AMapLocationClientOption mLocationOption = null;
    private AMapLocationClient mlocationClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView(savedInstanceState);
        initLocation();
    }

    /**
     * 需要进行检测的权限数组
     */
    protected String[] needPermissions = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA
    };

    /**
     * 判断是否需要检测，防止不停的弹框
     */
    private boolean isNeedCheck = true;
    private static final int PERMISSON_REQUESTCODE = 0;

    /**
     * 检测是否说有的权限都已经授权
     * @param grantResults
     * @return
     * @since 2.5.0
     *
     */
    private boolean verifyPermissions(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 显示提示信息
     *
     * @since 2.5.0
     *
     */
    private void showMissingPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("权限不足");
        builder.setMessage("请打开权限");

        // 拒绝, 退出应用
        builder.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });

        builder.setPositiveButton("去设置",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startAppSettings();
                    }
                });

        builder.setCancelable(false);

        builder.show();
    }

    /**
     *  启动应用的设置
     *
     * @since 2.5.0
     *
     */
    private void startAppSettings() {
        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            this.finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     *
     * @since 2.5.0
     *
     */
    private void checkPermissions(String... permissions) {
        List<String> needRequestPermissonList = findDeniedPermissions(permissions);
        if (null != needRequestPermissonList
                && needRequestPermissonList.size() > 0) {
            ActivityCompat.requestPermissions(this,
                    needRequestPermissonList.toArray(
                            new String[needRequestPermissonList.size()]),
                    PERMISSON_REQUESTCODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] paramArrayOfInt) {
        if (requestCode == PERMISSON_REQUESTCODE) {
            if (!verifyPermissions(paramArrayOfInt)) {
                showMissingPermissionDialog();
                isNeedCheck = false;
            }
        }
    }

    /**
     * 获取权限集中需要申请权限的列表
     *
     * @param permissions
     * @return
     * @since 2.5.0
     *
     */
    private List<String> findDeniedPermissions(String[] permissions) {
        List<String> needRequestPermissonList = new ArrayList<String>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this,
                    perm) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.shouldShowRequestPermissionRationale(
                    this, perm)) {
                needRequestPermissonList.add(perm);
            }
        }
        return needRequestPermissonList;
    }

    private void initView(Bundle savedInstanceState) {
        button = (Button) findViewById(R.id.button);
        btnLocation = (Button) findViewById(R.id.btn_location);
        txt = (TextView) findViewById(R.id.textView);

        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.mapView);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，实现地图生命周期管理
        mMapView.onCreate(savedInstanceState);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePic();
            }
        });

        btnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeGPS();
            }
        });
    }

    /**
     * 定位
     */
    private void takeGPS() {
        ////启动定位
        mlocationClient.startLocation();
    }

    android.os.Handler handler = new android.os.Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String info = String.valueOf(msg.obj);
            txt.setText(info);
        }
    };

    private void initLocation() {
        mlocationClient = new AMapLocationClient(this);
        ////初始化定位参数
        mLocationOption = new AMapLocationClientOption();

////设置定位监听
        mlocationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation amapLocation) {
                StringBuilder stringBuilder = new StringBuilder();
                if (amapLocation != null) {
                    if (amapLocation.getErrorCode() == 0) {
                        //定位成功回调信息，设置相关消息

                        stringBuilder.append("\n类型：" + amapLocation.getLocationType());

                        switch (amapLocation.getLocationType()) {
                            case AMapLocation.LOCATION_TYPE_GPS:
                                stringBuilder.append("\n类型：GPS");break;
                            case AMapLocation.LOCATION_TYPE_WIFI:
                                stringBuilder.append("\n类型：WIFI");break;
                            case AMapLocation.LOCATION_TYPE_CELL:
                                stringBuilder.append("\n类型：CELL");break;
                            case AMapLocation.LOCATION_TYPE_FIX_CACHE:
                                stringBuilder.append("\n类型：FIX_CACHE");break;

                        }


                        //获取当前定位结果来源，如网络定位结果，详见定位类型表
                        stringBuilder.append("\n纬度：" + amapLocation.getLatitude());//获取纬度
                        stringBuilder.append("\n经度：" + amapLocation.getLongitude());//获取经度
                        stringBuilder.append("\n精度：" + amapLocation.getAccuracy());//获取精度信息
                        stringBuilder.append("\n地址：" + amapLocation.getAddress());//获取地址信息
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date date = new Date(amapLocation.getTime());
                        stringBuilder.append("\n" + "时间：").append(df.format(date));//定位时间

                        if (aMap == null) {
                            aMap = mMapView.getMap();
                        }
                        aMap.clear();
                        LatLng latLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
                        final Marker marker = aMap.addMarker(new MarkerOptions().
                                position(latLng).
                                title("详细地址：").
                                snippet(amapLocation.getAddress()));

                    } else {
                        //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                        Log.e("AmapError", "location Error, ErrCode:"
                                + amapLocation.getErrorCode() + ", errInfo:"
                                + amapLocation.getErrorInfo());
                        stringBuilder.append("定位错误：" + amapLocation.getErrorInfo());
                    }


                } else {
                    stringBuilder.append("定位失败");
                }
                Message msg = Message.obtain();
                msg.obj = stringBuilder.toString();
                handler.sendMessage(msg);
            }
        });

        ////设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);

////设置定位间隔,单位毫秒,默认为2000ms
//        mLocationOption.setInterval(2000);
        ////设置定位参数
        mlocationClient.setLocationOption(mLocationOption);
//        /设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
    }

    /*
    拍照
     */
    private void takePic() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri uri = Uri.fromFile(getOutputMediaFile(MEDIA_TYPE_IMAGE));
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, CAPTURE_IMAGE_REQUEST_CODE);
        mlocationClient.stopLocation();
    }

    private static File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "arjun_app");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "目录创建失败");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = null;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        }
        path = mediaFile.getPath();
        return mediaFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, path, Toast.LENGTH_LONG).show();
                setPicInfo();

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "capture image canceled.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Image capture failed.",
                        Toast.LENGTH_LONG).show();
            }
        }

    }

    private void setPicInfo() {
        try {
            StringBuffer stringBuffer  = new StringBuffer();
            ExifInterface exifInterface = new ExifInterface(path);
            stringBuffer.append("拍摄时间："+exifInterface.getAttribute(ExifInterface.TAG_DATETIME));
            stringBuffer.append("设备型号："+exifInterface.getAttribute(ExifInterface.TAG_MAKE));// 设备品牌
            stringBuffer.append("TAG_MODEL："+exifInterface.getAttribute(ExifInterface.TAG_MODEL)); // 设备型号
            stringBuffer.append("LATITUDE："+ exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
            stringBuffer.append("LONGITUDE："+ exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
            stringBuffer.append("LATITUDE_REF："+exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF));
            stringBuffer.append("LONGITUDE_REF："+ exifInterface.getAttribute
                    (ExifInterface.TAG_GPS_LONGITUDE_REF));


            String latValue = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String lngValue = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);

            String latRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            String lngRef = exifInterface.getAttribute
                    (ExifInterface.TAG_GPS_LONGITUDE_REF);
//            if (latValue != null && latRef != null && lngValue != null && lngRef != null) {
                try {
                    lat = convertRationalLatLonToFloat(latValue, latRef);
                    lng = convertRationalLatLonToFloat(lngValue, lngRef);


                    LatLng latLng = new LatLng(lat, lng);
                    aMap = mMapView.getMap();
                    aMap.clear();
                    final Marker marker = aMap.addMarker(new MarkerOptions().
                            position(latLng).
                            title("北京").
                            snippet("自己的位置自己的位置自己的位置"));


                    markerOption = new MarkerOptions();
                    markerOption.position(latLng);
                    markerOption.title("自己的位置").snippet("自己的位置自己的位置自己的位置自己的位置");

                    markerOption.draggable(true);

//                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
//                    BitmapFactory.Options options = new BitmapFactory.Options();
//                    options.inJustDecodeBounds = true;
//                    Bitmap bitmap = BitmapFactory.decodeFile(path,options);
//                    int height = options.outHeight * 200 / options.outWidth;
//                    options.outWidth = 200;
//                    options.outHeight = 200;

                    BitmapDescriptor var1 = BitmapDescriptorFactory.fromBitmap(decodeFile(path, 100));
                    markerOption.icon(var1);
//                    markerOption.icon( BitmapDescriptorFactory.fromBitmap());
                    // 将Marker设置为贴地显示，可以双指下拉看效果
                    markerOption.setGps(true);
                    marker.setIcon(var1);


                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            txt.setText(" "+stringBuffer.toString());

//            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static Bitmap decodeFile(String pathName, int reqWidth) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);
// 源图片的宽度
        final int width = options.outWidth;
        // 调用上面定义的方法计算inSampleSize值（inSampleSize值为图片压缩比例）
        options.inSampleSize = calculateInSampleSize(options, reqWidth);
        /**
         * 第二轮解析，负责具体压缩
         */
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth) {
        // 源图片的宽度
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (width > reqWidth) {
            // 计算出实际宽度和目标宽度的比率
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = widthRatio;
        }
        return inSampleSize;
    }

    private static float convertRationalLatLonToFloat(
            String rationalString, String ref) {

        String[] parts = rationalString.split(",");

        String[] pair;
        pair = parts[0].split("/");
        double degrees = Double.parseDouble(pair[0].trim())
                / Double.parseDouble(pair[1].trim());

        pair = parts[1].split("/");
        double minutes = Double.parseDouble(pair[0].trim())
                / Double.parseDouble(pair[1].trim());

        pair = parts[2].split("/");
        double seconds = Double.parseDouble(pair[0].trim())
                / Double.parseDouble(pair[1].trim());

        double result = degrees + (minutes / 60.0) + (seconds / 3600.0);
        if ((ref.equals("S") || ref.equals("W"))) {
            return (float) -result;
        }
        return (float) result;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，实现地图生命周期管理
        mMapView.onResume();
        if(isNeedCheck){
            checkPermissions(needPermissions);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，实现地图生命周期管理
        mMapView.onSaveInstanceState(outState);
    }
}
