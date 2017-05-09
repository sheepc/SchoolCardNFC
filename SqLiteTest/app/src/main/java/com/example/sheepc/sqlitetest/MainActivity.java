package com.example.sheepc.sqlitetest;

import android.content.Context;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
    private ArrayList<String> list;
    private double[][] valueArry;
    private double[][] dm;
    public static int k;
    private String path = "/data/data/com.example.sheepc.sqlitetest/files/";
    private SensorEventListener listener  = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(/*(Math.abs(event.values[0])>15) || (Math.abs(event.values[1])>15) || (Math.abs(event.values[2])>15)*/true)
            {
               // Toast.makeText(MainActivity.this, "加速度为("+event.values[0]+","+event.values[1]+","+event.values[2]+")", Toast.LENGTH_SHORT).show();
                tip.setText("加速度值("+event.values[0]+","+event.values[1]+","+event.values[2]+")");
                dao.insertRawData(event.values,k);
                saveToFile("RawData"+k+".txt", event.values[0]+","+event.values[1]+","+event.values[2]);
               // Log.i("insert",Integer.toString(k));
                //Log.i("cap","捕捉到数据");
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        k = 0;
        tip = (TextView) findViewById(R.id.tip);
        results = (ListView) findViewById(R.id.results);
        //list = new ArrayList<String>();
        //初始化数据库
        dao = new Dao(this,"JiaShuDu.db",null,3);

        //开始捕捉按钮
        Button captureBtn = (Button) findViewById(R.id.captureBtn);
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ++k;
                if(k<=5)
                {
                    sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
                    Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    sensorManager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_UI); //Normal和UI都行

                    //Log.i("cap",Integer.toString(k));
                    Toast.makeText(MainActivity.this,"开始第"+k+"次捕获",Toast.LENGTH_SHORT).show();
                }else{
                    k = 5;
                    Toast.makeText(MainActivity.this,"已完成"+k+"次捕获",Toast.LENGTH_SHORT).show();
                }

            }
        });
        //停止捕捉按钮，并处理数据
        Button stopBtn = (Button) findViewById(R.id.stopBtn);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorManager.unregisterListener(listener);
                list = dao.selectRawData(k);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, list);
                results.setAdapter(adapter);
                Toast.makeText(MainActivity.this,"停止第"+k+"次捕获",Toast.LENGTH_SHORT).show();
                //处理数据
                Matrix t = new Matrix(dao.selecttRawData_2(k));     //获取初始数据
                Matrix smoothM = smooth(t,4);                       //平滑
                Matrix normalM = normalation(smoothM);              //归一化
                dao.insertNormalData(normalM.getArray(),k);         //保存归一化的数据
                Matrix cutM = cut(normalM,0.05);                    //截取手势数据
                //保存数据到文件
                for (int i = 0; i < smoothM.getRowDimension(); i++)
                {
                    saveToFile("SmoothData"+k+".txt", smoothM.getArray()[i][0] + "," + smoothM.getArray()[i][1] + "," + smoothM.getArray()[i][2] );
                }

                for (int i = 0; i < normalM.getRowDimension(); i++)
                {
                    saveToFile("NormalData"+k+".txt", normalM.getArray()[i][0] + "," + normalM.getArray()[i][1] + "," + normalM.getArray()[i][2] );
                }

                for (int i = 0; i < cutM.getRowDimension(); i++)
                {
                    saveToFile("CutData"+k+".txt",cutM.getArray()[i][0] + "," + cutM.getArray()[i][1] + "," + cutM.getArray()[i][2]);
                }
                Toast.makeText(MainActivity.this,"第"+k+"次测试完成",Toast.LENGTH_SHORT).show();
            }
        });


        //删除按钮
        Button delBtn = (Button)findViewById(R.id.delBtn);
        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dao.onUpgrade(dao.getWritableDatabase(),2,3);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, new ArrayList<String>());
                results.setAdapter(adapter);
                k = 0;
                Toast.makeText(MainActivity.this,"删除所有数据",Toast.LENGTH_SHORT).show();
            }
        });

        //测试按钮
        Button testBtn = (Button) findViewById(R.id.testBtn);
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    //手势数据平滑处理,n为移动平均的项数
    public Matrix smooth(Matrix rawData,int n){
        Matrix res = rawData.copy();
        Matrix m = new Matrix(1,3);
        for(int i = 0; i < n; i++)
        {
            m = m.plus(rawData.getMatrix(i,i,0,2));
        }
        m = m.arrayRightDivide(new Matrix(1,3,n));//Mn-1
        res.setMatrix(n-1,n-1,0,2,m);

        for(int i = n; i < rawData.getRowDimension(); i++)
        {
            Matrix t1 = rawData.getMatrix(i,i,0,2).minus(rawData.getMatrix(i-n,i-n,0,2)).arrayRightDivide(new Matrix(1,3,n));
            Matrix t2 = res.getMatrix(i-1,i-1,0,2).plus(t1);
            res.setMatrix(i,i,0,2,t2);
        }
        return res;
    }

    //将n*m的矩阵归一化，并返回归一化后的n*m矩阵
    public Matrix normalation(Matrix smoothData){
        Matrix smoothM = smoothData.copy();
        int rowDimension = smoothM.getRowDimension();
        int columnDimension = smoothM.getColumnDimension();
        Matrix rightM = new Matrix(1,rowDimension,1);
        Matrix avgerM = rightM.times(smoothM).arrayRightDivide(new Matrix(1,columnDimension,rowDimension));//xyz三轴的平均值

        double[][] valueD = smoothM.getArray();
        double[][] avgerD = avgerM.getArray();
        double[][] standardD = new double[1][columnDimension];
        for (int j = 0; j < columnDimension; j++)
        {
            for (int i = 0; i < rowDimension; i++)
            {
                standardD[0][j] += Math.pow(valueD[i][j] - avgerD[0][j],2);
            }
            standardD[0][j] = Math.sqrt(standardD[0][j]/rowDimension);
        }

        for (int j = 0; j < columnDimension; j++)
        {
            for (int i = 0; i < rowDimension; i++)
            {
                valueD[i][j] = (valueD[i][j] - avgerD[0][j])/standardD[0][j];
            }
        }

        return new Matrix(valueD);
    }

    //手势数据截取
    public Matrix cut(Matrix normalData,double e){
        //寻找平稳序列，先对序列进行一阶差分，并取绝对值
        int row = normalData.getRowDimension()-1;
        int col = normalData.getColumnDimension();
        Matrix divM = new Matrix(row,col);
        for (int i = 0; i < row; i++){
            Matrix divLine = normalData.getMatrix(i+1,i+1,0,2).minus(normalData.getMatrix(i,i,0,2));
            divM.setMatrix(i,i,0,2,divLine);
            for (int j = 0; j < col; j++)
            {
                //取绝对值
                divM.set(i,j,Math.abs(divM.get(i,j)));
            }
        }

        //对差分绝对值矩阵进行截取
        int ps = 0;
        int pe = 0;
        //寻找ps
        for(int i = 0; i < row; i++)
        {
             if(divM.get(i,0) > e && divM.get(i,1) > e && divM.get(i, 2) > e){
                 ps = i+1;
                 //Log.i("ps",Integer.toString(ps));
                 System.out.println("ps:"+Integer.toString(ps));
                 break;
             }
        }

        for (int i = row-1; i > ps; i--){
            if(divM.get(i,0) > e && divM.get(i,1) > e && divM.get(i,2) > e){
                pe = i;
                //Log.i("pe",Integer.toString(pe));
                System.out.println("pe:"+Integer.toString(pe));
                break;
            }
        }
        return normalData.getMatrix(ps,pe,0,col-1);
        /*        Matrix limte = getLimte(normalData, e);
        //ps,找起始点
        Matrix t = normalData.getMatrix(0,0,0,2);
        int ps = 0;
        for(int i = 0; i < normalData.getRowDimension(); i++)
        {
            if(vetorLarger(normalData.getMatrix(i,i,0,2),limte))
            {
                t = normalData.getMatrix(i,i,0,2);
                ps = i;
                break;
            }
        }
        System.out.println("ps: "+ps);
        t.print(1,3);

        //pe,找结束点
        t = normalData.getMatrix(0,0,0,2);
        int pe = 0;
        for (int i = ps; i < normalData.getRowDimension(); i++)
        {
            if( vetorSmaller(normalData.getMatrix(i,i,0,2),limte) ){
                t = normalData.getMatrix(i,i,0,2);
                pe = i;
            }
        }
        System.out.println("pe: "+pe);
        t.print(1,3);

        return normalData.getMatrix(ps,pe,0,2);*/
    }


    public double getU(Matrix[] cutDataArry){
        double w = 0;
        double u = 0;
        int times = 0;
        for(int i = 1; i <= cutDataArry.length; i++)
        {
            for(int j = 1; j < i; j++){
                Matrix tm = cutDataArry[i].copy();
                Matrix rm = cutDataArry[j].copy();
                double dtwX = aDTW(tm.getMatrix(0,tm.getRowDimension()-1,0,0),rm.getMatrix(0,rm.getRowDimension()-1,0,0));
                double dtwY = aDTW(tm.getMatrix(0,tm.getRowDimension()-1,1,1),rm.getMatrix(0,rm.getRowDimension()-1,1,1));
                double dtwZ = aDTW(tm.getMatrix(0,tm.getRowDimension()-1,2,2),rm.getMatrix(0,rm.getRowDimension()-1,2,2));
                w = w + ((dtwX + dtwY + dtwZ)/ 3);
                saveToFile("w.txt",Double.toString((dtwX + dtwY + dtwZ)/ 3));
                times++;
            }
        }
        u = w / times;
        saveToFile("u.txt",Double.toString(u));
        saveToFile("u.txt",Integer.toString(times));
        return u;
    }

    //判断v1>v2
    public boolean vetorLarger(Matrix v1, Matrix v2){
        if(v1.getColumnDimension() != v2.getColumnDimension() || v1.getRowDimension() != v2.getColumnDimension()){
            return false;
        }

        for(int i = 0; i < v1.getColumnDimension(); i++)
        {
            if(v1.get(0,i) <= v2.get(0,i)){
                return false;
            }
        }
        return true;
    }

    //判断v1<v2
    public boolean vetorSmaller(Matrix v1, Matrix v2){
        if(v1.getColumnDimension() != v2.getColumnDimension() || v1.getRowDimension() != v2.getColumnDimension()){
            return false;
        }

        for(int i = 0; i < v1.getColumnDimension(); i++)
        {
            if(v1.get(0,i) >= v2.get(0,i)){
                return false;
            }
        }
        return true;
    }

    //序列对齐，插值，零值补偿
    public Matrix alignMent(){
        return null;
    }


    //ADTW算法，算出两个同维的时间序列的匹配程度，若序列不同维，则返回-1，否则返回两个序列直接的最小累计平均距离
    public double aDTW (Matrix tm, Matrix rm) {
        int n = tm.getRowDimension();
        int tColumnDimension = tm.getColumnDimension();
        int m = rm.getRowDimension();
        int rColumnDimension = rm.getColumnDimension();
        if (tColumnDimension != rColumnDimension) {
            return -1;
        } else {
            dm = new double[n][m];
            //算出距离矩阵
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    dm[i][j] = getDistance(tm.getMatrix(i, i, 0, tColumnDimension - 1), rm.getMatrix(j, j, 0, rColumnDimension - 1));
                    //System.out.print(dm[i][j]);
                    //System.out.print("\t");
                }
                //System.out.println();
            }
/*            int n = 5;
            int m = 6;
            double[][] dm = new double[][]{{1,2,3,4,5,6},{1,2,3,4,5,6},{1,2,3,4,5,6},{1,2,3,4,5,6},{1,2,3,4,5,6}};*/
            //求ADTW
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    double tmp = dm[i][j];
                    if (i == 0)
                        dm[i][j] = tmp;
                    if (i > 0) {
                        if (j == 0)
                            dm[i][j] = tmp + dm[i - 1][j];
                        else if (j == 1)
                            dm[i][j] = tmp + Math.min(dm[i - 1][j - 1], dm[i - 1][j]);
                        else
                            dm[i][j] = tmp + getMin(dm[i - 1][j], dm[i - 1][j - 1], dm[i - 1][j - 2]);
                    }
                }
            }
            return dm[n - 1][m - 1];
        }
    }


    //算出两个同维向量的欧式距离
    public double getDistance(Matrix v1, Matrix v2){
        Matrix minusM = v1.minus(v2);
        double distance = Math.sqrt(minusM.times(minusM.transpose()).get(0,0));
        return distance;
    }

    public double getD(int n, int m) {
        if (n <= 0 || m <= 0) {
            return 0;
        } else {
            double dist = getDist(n, m);
            //System.out.println(n + ","+ m + "\t\t" + dist);
            return dist;
        }
    }

    public double getDist(int n, int m)
    {
        if(n == 1 && m == 1)
        {
            return dm[n-1][m-1];
        }else if(n <= 0 || m <= 0){
            return  0;
        }else{
            if(n == 0)
            {
                //Log.w("zero",Integer.toString(n)+ ',' +Integer.toString(m));
            }
            return ( dm[n-1][m-1]+getMin(getD(n-1,m), getD(n-1, m-1), getD(n-1, m-2)) )/n;
        }
    }

    public double getMin(double d1, double d2, double d3){
        if(d1 > d2)
        {
            if(d2 > d3){
                return d3;
            }else{
                return d2;
            }
        }else{
            if(d1 > d3){
                return d3;
            }else{
                return d1;
            }
        }
    }

    public void saveToFile(String fileName, String data) {

        FileOutputStream out = null;
        BufferedWriter writer = null;
        boolean append = true;
        try{
            //out = new FileOutputStream(path+fileName);
            //Log.i("path",path+fileName);
            out = openFileOutput(fileName, Context.MODE_APPEND);
            writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.write(data);
            writer.newLine();
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

/*    public double getDistance(double[] seqa, double[] seqb) {
        double distance = 0;
        int lena = seqa.length;
        int lenb = seqb.length;
        double[][] c = new double[lena][lenb];
        for (int i = 0; i < lena; i++) {
            for (int j = 0; j < lenb; j++) {
                c[i][j] = 1;
            }
        }
        for (int i = 0; i < lena; i++) {
            for (int j = 0; j < lenb; j++) {
                double tmp = (seqa[i] - seqb[j]) * (seqa[i] - seqb[j]);
                if (j == 0 && i == 0)
                    c[i][j] = tmp;
                else if (j > 0)
                    c[i][j] = c[i][j - 1] + tmp;
                if (i > 0) {
                    if (j == 0)
                        c[i][j] = tmp + c[i - 1][j];
                    else
                        c[i][j] = tmp + getMin(c[i][j - 1], c[i - 1][j - 1], c[i - 1][j]);
                }
            }
        }
        distance = c[lena - 1][lenb - 1];
        return distance;
    }*/

}

