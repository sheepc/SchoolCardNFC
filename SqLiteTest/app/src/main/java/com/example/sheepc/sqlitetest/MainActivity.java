package com.example.sheepc.sqlitetest;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import Jama.Matrix;

public class MainActivity extends AppCompatActivity {
    private SensorManager sensorManager;
    private Dao dao;
    private TextView tip;
    private ListView results;
    private ArrayList<double[]> list;
    private double[][] valueArry;
    private double[][] dm;
    public static int k = 0;

    private SensorEventListener listener  = new SensorEventListener() {
        @Override
        //读取加速度传感器数据，并将传感器数据插入RawData_i表
        public void onSensorChanged(SensorEvent event) {
            list.add(new double[]{event.values[0],event.values[1],event.values[2]});
            tip.setText("加速度值("+event.values[0]+","+event.values[1]+","+event.values[2]+")");
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("注册模版");
        tip = (TextView) findViewById(R.id.tip);

        list = new ArrayList<double[]>();

        //初始化数据库
        dao = new Dao(this,"JiaShuDu.db",null,3);

        final ToggleButton captureButton = (ToggleButton) findViewById(R.id.captureButton);
        captureButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){

                    if(k == 5)
                    {
                        captureButton.setChecked(false);
                        Toast.makeText(MainActivity.this,"已完成"+k+"次捕获,请点击测试按钮",Toast.LENGTH_SHORT).show();

                    }else{
                        ++k;
                        //注册传感器管理器，设定采样率
                        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
                        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                        sensorManager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_GAME); //Normal和UI都行

                        //Log.i("cap",Integer.toString(k));
                        Toast.makeText(MainActivity.this,"开始第"+k+"次捕获",Toast.LENGTH_SHORT).show();
                    }

                }else{
                        //解绑传感器管理器
                        sensorManager.unregisterListener(listener);

                        dao.insertRawData_2(list,k);
                        list.clear();
                        Toast.makeText(MainActivity.this,"停止第"+k+"次捕获",Toast.LENGTH_SHORT).show();
                }

            }
        });


        //删除按钮
        Button delBtn = (Button)findViewById(R.id.delBtn);
        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //drop数据库中所有的表
                dao.onUpgrade(dao.getWritableDatabase(),2,3);
                list.clear();
                k = 0;
                Toast.makeText(MainActivity.this,"删除所有数据",Toast.LENGTH_SHORT).show();
            }
        });

        //测试按钮
        Button testBtn = (Button) findViewById(R.id.testBtn);
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //跳转到测试活动
                Intent itent = new Intent(MainActivity.this, JudgeActivity.class);
                startActivity(itent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    //保存数据到文件
    public void saveToFile(String fileName, ArrayList<double[]> list) {

        FileOutputStream out = null;
        BufferedWriter writer = null;
        boolean append = true;
        try{
            out = new FileOutputStream(Environment.getExternalStorageDirectory().getPath()+"/毕业设计/"+fileName+".txt");
            writer = new BufferedWriter(new OutputStreamWriter(out));
            for(int i = 0; i < list.size(); i++)
            {
                for(int j = 0; j < 3; j++)
                {
                    writer.write(Double.toString(list.get(i)[j]));
                    writer.write(" ");
                }
                writer.newLine();
            }


        }catch (IOException e){
            e.printStackTrace();
        }finally {
            if (writer != null){
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
