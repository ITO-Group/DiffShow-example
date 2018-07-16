package com.example.diffshow;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.support.v4.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.widget.TextView;
import android.view.KeyEvent;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


public class MainActivity extends Activity implements SensorEventListener {
    public final String TAG = "READ_DIFF_JAVA";
    public final int MY_PERMISSIONS_REQUEST_WRITE_CONTACTS = 1;
    //TextView tv;
    int num_pixel = 32 * 18;
    short diffData[] = new short[num_pixel];
    int amountSave = 120 * 120;
    short saveData[] = new short[num_pixel * amountSave];
    //byte[][] saveDataByte = new byte[amountSave][num_pixel*5];
    //String[] saveDataString = new String[amountSave];
    //String timeData[] = new String[amountSave];
    //byte[][] timeDataByte = new byte[amountSave][];
    Long[] timeDataLong = new Long[amountSave];
    String[] sensorData = new String[amountSave];
    int countSave = 0;

    CapacityView capacityView;
    int screenWidth;
    int screenHeight;

    FileOutputStream fileout;
    DataOutputStream dataout;
    String filename = "/sdcard/getear.txt";
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private SensorManager sensorManager;
    private Sensor sensor_rotation;
    private Sensor sensor_accelerometer;
    private Sensor sensor_gyo;
    private boolean isrecording;
    private int times_save = 0;
    private String username = "yanyukang";
    private String[] gesturenames = {"坐姿","站姿","走动","侧卧","仰卧"};
    private String[] tasknames = {"单手拇指", "单手食指", "双手拇指","正常接听", "贴近脸颊", "左右耳切换"};
    private String[] filenames = {"sit_singleThumb", "sit_singleIndex", "sit_doubleThumb", "sit_earNormal", "sit_earCheeck", "sit_earSwitch", "stand_singleThumb", "stand_singleIndex", "stand_doubleThumb", "stand_earNormal", "stand_earCheeck", "stand_earSwitch", "walk_singleThumb", "walk_singleIndex", "walk_doubleThumb", "walk_earNormal", "walk_earCheeck", "walk_earSwitch", "lieside_singleThumb", "lieside_singleIndex", "lieside_doubleThumb", "lieside_earNormal", "lieside_earCheeck", "lieside_earSwitch", "lieplan_singleThumb", "lieplan_singleIndex", "lieplan_doubleThumb", "lieplan_earNormal", "lieplan_earCheeck", "lieplan_earSwitch", "finish"};
    private float[] gravity = {0,0,0};
    private float[] linear_acceleration = {0,0,0};
    private short[] cleanData = new short[6*8]; // ******** need to rewrite

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @SuppressLint("HandlerLeak")
    private Handler fileHandler;

    {
        fileHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                //writeIntoFile(saveData,countSave);
                Log.d("record", "Finish Write");
            }
        };
    }

    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            capacityView.invalidate();

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        getPermissions();
        iniateSensors();

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(false);
        mediaPlayer = MediaPlayer.create(this, R.raw.audio);
        isrecording = false;

        capacityView = findViewById(R.id.capacityView);
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(size);
        //getRealSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        capacityView.screenHeight = screenHeight;
        capacityView.screenWidth = screenWidth;
        capacityView.diffData = diffData;
        capacityView.task_name = tasknames[0];
        capacityView.gesture_name = gesturenames[0];

        readDiffStart();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        readDiffStop();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /*
        int action = event.getActionMasked();
        if(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN){
            Log.d(TAG,"------  Down Point Id:"+event.getPointerId(event.getActionIndex()));
        }
        for(int i = 0; i<event.getPointerCount();++i){
            Log.d(TAG,"------  Id:"+event.getPointerId(i)+"  x:"+event.getX(i)+"  y:"+event.getY(i));
        }
        */
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                Log.d("volume key", "up");

                if (isrecording == true) {
                    if (mediaPlayer.isPlaying() == true) {
                        mediaPlayer.pause();
                    }
                    isrecording = false;
                    //fileHandler.obtainMessage(0).sendToTarget();

                    //writeIntoFile();
                    new Thread(new Runnable() {
                        public void run() {
                            //FileOutputStream fileout = new FileOutputStream(new File("/sdcard/number" + Integer.toString(countSave) + ".txt"));
                            //dataout = new DataOutputStream(fileout);
                            String filename = "/sdcard/" + filenames[times_save] + "_" + username + ".txt";
                            String sensorname = "/sdcard/sensor_" + filenames[times_save] + "_" + username + ".txt";
                            times_save += 1;
                            if(times_save != filenames.length - 1) {
                                int gesture_num = times_save % gesturenames.length;
                                int task_num = times_save / gesturenames.length;
                                capacityView.task_name = tasknames[task_num];
                                capacityView.gesture_name = gesturenames[gesture_num];

                            }
                            else {
                                capacityView.task_name = "结束啦";
                                capacityView.gesture_name = "";
                            }
                            capacityView.istapping = false;
                            capacityView.isrecording = false;
                            capacityView.resetCapacity();
                            capacityView.invalidate();
                            Log.d("filename", filename);
                            writeIntoFile(saveData, timeDataLong, countSave, filename);
                            writeSensors(sensorData,countSave,sensorname);
                            if (times_save == filenames.length - 1) {
                                capacityView.isrecording = true;
                                capacityView.invalidate();
                            }

                            //writeIntoFile(saveData, timeData, countSave, filename);
                            //writeIntoFile(saveDataString,timeData,countSave,filename);
                        }
                    }).start();


                } else {
                    isrecording = true;
                    countSave = 0;
                    saveData = new short[amountSave * num_pixel];
                    timeDataLong = new Long[amountSave];
                    if (mediaPlayer.isPlaying() == false) {
                        mediaPlayer.start();
                        mediaPlayer.setLooping(true);
                    }
                    capacityView.isrecording = true;
                    if (times_save < filenames.length/2) // tapping but not ear
                    {
                        capacityView.istapping = true;
                    }
                    //capacityView.state_name = statena mes[1];
                    capacityView.invalidate();
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                Log.d("volume key", "down");
                isrecording = false;
                if (mediaPlayer.isPlaying() == true) {
                    mediaPlayer.pause();
                }
                capacityView.isrecording = false;
                capacityView.istapping = false;
                capacityView.resetCapacity();
                capacityView.invalidate();
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
    public void writeSensors(String[] sensorData,int countSave, String filename){
        try {
            FileOutputStream fileout = new FileOutputStream(new File(filename));
            for (int i = 0; i < countSave; i++) {
                fileout.write(sensorData[i].getBytes());
            }
            fileout.close();
            Log.d("record", "Write into files " + filename);
        }catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void writeIntoFile(short[] saveData, Long[] timeDataLong, int countSave, String filename) {
        try {
            //Log.d("record","Write into files");
            FileOutputStream fileout = new FileOutputStream(new File(filename));
            //Log.d("test", Integer.toString(countSave));
            for (int i = 0; i < countSave; i++) {
                //Log.d("test","in while");
                //fileout.write(timeData[i].getBytes());
                //fileout.write('\n');
                /*
                for (int j = 0; j < num_pixel/2; j++) {
                    //long value = saveData[i*num_pixel+4*j]*1000000000000L+saveData[i*num_pixel+4*j+1]*100000000L+saveData[4*j+2]*10000L+saveData[4*j+3];
                    //byte[] bytes = ByteBuffer.allocate(8).putLong(value).array();
                    //int value = saveData[i*num_pixel+2*j]*10000+saveData[i*num_pixel+2*j+1];
                    //byte[] bytes = ByteBuffer.allocate(4).putInt(value).array();
                    //fileout.write(bytes);
                    //fileout.write(saveData[i*num_pixel+2*j]);
                    //fileout.write(saveData[i*num_pixel+2*j+1]);
                    //fileout.write(Short.toString(saveData[i*num_pixel+j]).getBytes());
                    //fileout.write(' ');
                }
                */
                byte[] timebyte = ByteBuffer.allocate(8).putLong(timeDataLong[i]).array();
                fileout.write(timebyte);
                //fileout.write(timeData[i].getBytes());
                for (int j = 0; j < num_pixel / 64; j++) {
                    byte[] bytes = new byte[128];
                    for (int k = 0; k < 16; k++) {
                        //int value = saveData[i*num_pixel+j*8+k*2]*10000+saveData[i*num_pixel+j*8+k*2+1];
                        //byte[] newbytes = ByteBuffer.allocate(4).putInt(value).array();
                        long value = saveData[i * num_pixel + j * 64 + k * 4] * 1000000000000L + saveData[i * num_pixel + j * 64 + k * 4 + 1] * 100000000L + saveData[i * num_pixel + j * 64 + k * 4 + 2] * 10000L + saveData[i * num_pixel + j * 64 + k * 4 + 3];
                        byte[] newbytes = ByteBuffer.allocate(8).putLong(value).array();
                        System.arraycopy(newbytes, 0, bytes, k * 8, 8);
                    }
                    fileout.write(bytes);
                    //long value = saveData[i*num_pixel+4*j]*1000000000000L+saveData[i*num_pixel+4*j+1]*100000000L+saveData[4*j+2]*10000L+saveData[4*j+3];
                    //byte[] newbytes = ByteBuffer.allocate(8).putLong(value).array();

                    //dataout.writeShort(saveData[i*num_pixel+j]);
                }


                //fileout.write(saveDataString[i].getBytes());
            }
            fileout.close();
            Log.d("record", "Write into files " + filename);
            //fileout.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getAccelerometer(SensorEvent event) {
        final float alpha = 0.8f;
                // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];
    }



    private void iniateSensors()
    {
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        sensor_accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

    }
    private void getPermissions()
    {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,Manifest.permission.MODIFY_AUDIO_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission", "Write Fail");
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {


            } else {

                // No explanation needed, we can request the permission.
                Log.d("Permission", "Request Write");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MODIFY_AUDIO_SETTINGS},
                        MY_PERMISSIONS_REQUEST_WRITE_CONTACTS);

            }
        }
        else
        {
            Log.d("Permission", "Write Success");
        }
    }

    public void processDiff(short[] data){
        int max_val = 0;
        int min_val = 10000;
        for(int i = 0;i<data.length;i++){
            if(max_val<data[i]) max_val = data[i];
            if(min_val>data[i]) min_val = data[i];
        }
        Log.d("para",Integer.toString(max_val)+","+Integer.toString(min_val));
//        Log.d("time",Long.toString(time)); // for test time

        // not update the capacity data
        capacityView.diffData =  data;

        if(isrecording == false){
            return;
        }
        if(countSave >= amountSave)
        {
            Log.d("save","too many items");
            return;
        }

        long time=System.currentTimeMillis();
        //saveDataString[countSave] = "";
        //timeData[countSave] = "\n" + Long.toString(time) + " " + Short.toString(data[num_pixel]) + "\n";
        timeDataLong[countSave] = time*10+data[num_pixel];
        sensorData[countSave] = Float.toString(linear_acceleration[0]) + " " + Float.toString(linear_acceleration[0]) + " " +Float.toString(linear_acceleration[0]) + "\n";
        //timeDataByte[countSave] = (Long.toString(time) + " " + Short.toString(data[num_pixel]) + "\n").getBytes();
        for(int i = 0;i < num_pixel;i++) {
            saveData[countSave * num_pixel + i] = data[i];
            //byte[] bytes = ByteBuffer.allocate(2).putShort(data[i]).array();
            //saveDataString[countSave] += Short.toString(data[i]) + " ";

            //Log.d("test","here");
            //Log.d("test",Integer.toString(bytes.length));
            //saveDataByte[countSave*num_pixel*2+i*2] = bytes[0];
            //saveDataByte[countSave*num_pixel*2+i*2+1] = bytes[1];
        }
        //saveDataString[countSave] += "\n";
        countSave ++;


        // not update the capacity view
        myHandler.obtainMessage(0).sendToTarget();
//        Log.d(TAG,"processDiff touchNum :"+ data.touchNum);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void readDiffStart();
    public native void readDiffStop();
}
