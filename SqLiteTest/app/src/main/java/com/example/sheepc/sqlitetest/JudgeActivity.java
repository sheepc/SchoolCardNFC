package com.example.sheepc.sqlitetest;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;

import Jama.Matrix;

public class JudgeActivity extends AppCompatActivity {
    private Dao dao;
    private HandSafe handSafe;
    private ArrayList<double[]> testList;
    private SensorManager sensorManager;
    private Sensor sensor;
    private TextView tip2;
    private SensorEventListener listener  = new SensorEventListener() {

        @Override
        //读取加速度传感器数据，并将传感器数据插入RawData_i表
        public void onSensorChanged(SensorEvent event) {
            testList.add(new double[]{event.values[0],event.values[1],event.values[2]});
            tip2.setText("加速度值("+event.values[0]+","+event.values[1]+","+event.values[2]+")");

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_judge);
        setTitle("判定");

        //初始化数据库连接
        dao = new Dao(this,"JiaShuDu.db",null,3);

        testList = new ArrayList<double[]>();

        //获得原始手势数据
        Matrix[] rawDataMatrixArry = new Matrix[5];
        for (int i = 1; i <= 5; i++){
            rawDataMatrixArry[i-1] = new Matrix(dao.selecttRawData_2(i));
        }

        //初始化HandSafe
        //HandSafe类的构造函数，传入参数：模版个数，简单平滑周期，手势数据截取门限，判定门限
        int modelNumer = 5;
        int smoothPeriod = 4;
        double subLimite = 0.05;
        double judgeLimite = 1.18  ; //自己在0.7~0.9 他人在1.3~,不要在1以内，否则或产生收敛现象。
        handSafe = new HandSafe(modelNumer,smoothPeriod,subLimite,judgeLimite);
        handSafe.ModelRegister(rawDataMatrixArry);//注册模版
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //tip
        tip2 = (TextView)findViewById(R.id.tip2);

        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    //注册传感器管理器，设定采样率

                    sensorManager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_GAME); //Normal和UI都行
                    Toast.makeText(JudgeActivity.this,"开始捕捉数据",Toast.LENGTH_SHORT).show();
                }else{
                    //解绑传感器管理器
                    sensorManager.unregisterListener(listener);
                    Toast.makeText(JudgeActivity.this,"停止捕捉数据",Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button judgeButton = (Button)findViewById(R.id.judgeButton);
        judgeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Matrix testHandMatrix = new Matrix(testList.size(), 3);
                for (int i = 0; i < testList.size(); i++){
                    for(int j = 0; j < 3; j++){
                        testHandMatrix.set(i,j,testList.get(i)[j]);
                    }
                }
                testList.clear();
                if(handSafe.judge(testHandMatrix)){
                    Matrix updateRawMatrix = handSafe.getRawHandDataMatrixArry()[handSafe.getUpdateModelIndex()].copy();
                    dao.updateRawTable(updateRawMatrix, handSafe.getUpdateModelIndex());
                    
                    Toast.makeText(JudgeActivity.this,"认证成功",Toast.LENGTH_SHORT).show();
                }else{

                    Toast.makeText(JudgeActivity.this,"认证失败",Toast.LENGTH_SHORT).show();
                }
            }
        });


    }


}

