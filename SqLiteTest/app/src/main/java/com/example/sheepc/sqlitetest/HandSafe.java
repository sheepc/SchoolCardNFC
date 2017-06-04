package com.example.sheepc.sqlitetest;

/**
 * Created by sheepc on 2017/5/9.
 */

import android.content.Context;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import Jama.Matrix;
/*
三维手势身份认证的类
 流程： 手势模版
        手势数据平滑
        手势数据归一化
        手势数据截取
        匹配判定
*/

public class HandSafe{
    private int modelNumer;             //模版个数，在模版注册算法中使用
    private int smoothPeriod;           //简单平滑的周期，在数据平滑算法中使用
    private double subLimite;           //截取门限，在手势数据截取算法中使用
    private double judgeLimite;         //判定门限，在判定算法中是使用
    //private double updateLimite;        //更新门限，小于该门限，更新模版
    private Matrix[] rawHandDataMatrixArry; //原始手势数据矩阵组
    private Matrix[] smoothHandDataMatrixArry;//平滑手势数据矩阵组
    private Matrix[] normalHandDataMatrixArry;//归一化手势数据矩阵组
    private Matrix[] subHandDataMatrixArry;    //截取的手势数据矩阵组
    private double modeDistance;        //模版距离均值
    private int updateModelIndex;       //更新模版数组的元素下标



    //HandSafe类的构造函数，传入参数：模版个数，简单平滑周期，手势数据截取门限，判定门限
    public HandSafe(int modelNumer, int smoothPeriod, double subLimite, double judgeLimite){

        this.modelNumer = modelNumer;
        this.smoothPeriod = smoothPeriod;
        this.subLimite = subLimite;
        this.judgeLimite = judgeLimite;
        //this.updateLimite = updateLimite;
        this.rawHandDataMatrixArry = new Matrix[modelNumer];
        this.smoothHandDataMatrixArry = new Matrix[modelNumer];
        this.normalHandDataMatrixArry = new Matrix[modelNumer];
        this.subHandDataMatrixArry = new Matrix[modelNumer];
        this.updateModelIndex = -1;

        createDir(Environment.getExternalStorageDirectory().getPath()+"/毕业设计");
    }

    //注册模版
    public Boolean ModelRegister(Matrix[] rawHandDataMatrixArry){
        boolean success = false;

        if(modelNumer == rawHandDataMatrixArry.length && rawHandDataMatrixArry.length != 0){

            //对模版手势数据进行处理
            for (int i = 0; i < rawHandDataMatrixArry.length; i++)
            {
                this.rawHandDataMatrixArry[i] = rawHandDataMatrixArry[i].copy();
                //平滑手势数据
                this.smoothHandDataMatrixArry[i] = smooth(this.rawHandDataMatrixArry[i]);
                //手势数据归一化
                this.normalHandDataMatrixArry[i] = normalation(this.smoothHandDataMatrixArry[i]);
                //手势数据截取
                this.subHandDataMatrixArry[i] = sub(this.normalHandDataMatrixArry[i]);
            }

            //计算模版手势数据的平均距离
            this.modeDistance = this.getModeDistance(this.subHandDataMatrixArry);

            for(int i = 0; i < modelNumer; i++)
            {
                saveToFile("raw"+i,this.rawHandDataMatrixArry[i]);
                saveToFile("smooth"+i,this.smoothHandDataMatrixArry[i]);
                saveToFile("normal"+i,this.normalHandDataMatrixArry[i]);
                saveToFile("sub"+i, this.subHandDataMatrixArry[i]);
            }

             success = true;
        }

        return success;
    }

    //对输入的手势数据进行判定
    public boolean judge(Matrix testRawMatrix){

        //对测试数据进行处理
        Matrix testSmoothMatrix = smooth(testRawMatrix);
        Matrix testNormalMatrix = normalation(testSmoothMatrix);
        Matrix testSubMatrix = sub(testNormalMatrix);

        saveToFile("testraw",testRawMatrix);
        saveToFile("testsmooth",testSmoothMatrix);
        saveToFile("testnormal",testNormalMatrix);
        saveToFile("testsub", testSubMatrix);

        boolean success = false;
        double[] distanceArry = new double[modelNumer];
        double sum = 0;
        if(subHandDataMatrixArry.length == modelNumer)
        {
            //计算测试模版和各个注册模版之间三轴距离的均值之和
            for(int i = 0; i < subHandDataMatrixArry.length; i++){
                //计算测试模版和一个注册模版之间三轴距离的均值
                Matrix tm = testSubMatrix.copy();
                Matrix rm = subHandDataMatrixArry[i].copy();
                //第一种实现方式
                double dtwX = DTW(tm.getMatrix(0,tm.getRowDimension()-1,0,0),rm.getMatrix(0,rm.getRowDimension()-1,0,0));
                double dtwY = DTW(tm.getMatrix(0,tm.getRowDimension()-1,1,1),rm.getMatrix(0,rm.getRowDimension()-1,1,1));
                double dtwZ = DTW(tm.getMatrix(0,tm.getRowDimension()-1,2,2),rm.getMatrix(0,rm.getRowDimension()-1,2,2));
                distanceArry[i] = ((dtwX + dtwY + dtwZ)/ 3);
                sum = sum + distanceArry[i];
                //第二种实现方式
                /*distanceArry[i] = DTW(tm,rm);
                sum = sum + distanceArry[i];*/
            }
            double testDistance = sum/modelNumer;
            double judageResult = testDistance/this.modeDistance;
            if(judageResult <= this.judgeLimite){//匹配成功
                saveToFile("testDistance+modeDistance+judageResult",new Matrix(new double[][]{{0},{testDistance},{this.modeDistance},{judageResult}}));
                success = true;
                //模版更新
                updateModel(testRawMatrix,testSmoothMatrix,testNormalMatrix,testSubMatrix,distanceArry);
            }else {
                saveToFile("testDistance+modeDistance+judageResult",new Matrix(new double[][]{{1},{testDistance},{this.modeDistance},{judageResult}}));
            }


        }
        return success;
    }

    //模版更新，在判定算法成功后调用
    //distanceArry中存储着通过测试的手势数据与各个模版之间的距离
    //从distanceArry中找出测试的手势数据与模版之间的最大距离，并用测试手势数据将该模版替换
    private void updateModel(Matrix testRawMatrix,Matrix testSmoothMatrix,Matrix testNormalMatrix,
                             Matrix testSubMatrix, double[] distanceArry){
        //找最大距离的下标
        int maxIndex = 0;
        double maxTmp = distanceArry[0];
        for (int i = 0; i < distanceArry.length; i++){
            if(distanceArry[i] > maxTmp){
                maxIndex = i;
            }
        }
        //更新手势数据模版
        this.rawHandDataMatrixArry[maxIndex] = testRawMatrix.copy();
        this.smoothHandDataMatrixArry[maxIndex] = testSmoothMatrix.copy();
        this.normalHandDataMatrixArry[maxIndex] = testNormalMatrix.copy();
        this.subHandDataMatrixArry[maxIndex] = testSubMatrix.copy();
        //计算模版手势数据的平均距离
        this.modeDistance = this.getModeDistance(this.subHandDataMatrixArry);
        //标明更新的模版下标
        updateModelIndex = maxIndex;



    }

    public Matrix[] getRawHandDataMatrixArry() {
        return rawHandDataMatrixArry;
    }

    public Matrix[] getSmoothHandDataMatrixArry() {
        return smoothHandDataMatrixArry;
    }

    public Matrix[] getNormalHandDataMatrixArry() {
        return normalHandDataMatrixArry;
    }

    public Matrix[] getSubHandDataMatrixArry() {
        return subHandDataMatrixArry;
    }

    public int getUpdateModelIndex() {
        return updateModelIndex;
    }


    //对一次原始手势数据进行平滑
    private Matrix smooth(Matrix rawHandDataMatrix){
        Matrix res = rawHandDataMatrix.copy();
        Matrix m = new Matrix(1,3);
        for(int i = 0; i < this.smoothPeriod; i++)
        {
            m = m.plus(rawHandDataMatrix.getMatrix(i,i,0,2));
        }
        m = m.arrayRightDivide(new Matrix(1,3,this.smoothPeriod));//Mn-1
        res.setMatrix(this.smoothPeriod-1,this.smoothPeriod-1,0,2,m);

        for(int i = this.smoothPeriod; i < rawHandDataMatrix.getRowDimension(); i++)
        {
            Matrix t1 = rawHandDataMatrix.getMatrix(i,i,0,2).minus(rawHandDataMatrix.getMatrix(i-this.smoothPeriod,i-this.smoothPeriod,0,2)).arrayRightDivide(new Matrix(1,3,this.smoothPeriod));
            Matrix t2 = res.getMatrix(i-1,i-1,0,2).plus(t1);
            res.setMatrix(i,i,0,2,t2);
        }
        return res;
    }

    //对一次平滑的手势数据进行归一化
    private Matrix normalation(Matrix smoothHandDataMatrix){
        Matrix smoothM = smoothHandDataMatrix.copy();
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

    //对一次归一化手势进行手势数据截取
    private Matrix sub(Matrix normalHandDataMatrix){
        //寻找平稳序列，先对序列进行一阶差分，并取绝对值
        int row = normalHandDataMatrix.getRowDimension()-1;
        int col = normalHandDataMatrix.getColumnDimension();
        Matrix divM = new Matrix(row,col);
        for (int i = 0; i < row; i++){
            Matrix divLine = normalHandDataMatrix.getMatrix(i+1,i+1,0,2).minus(normalHandDataMatrix.getMatrix(i,i,0,2));
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
            if(divM.get(i,0) > subLimite && divM.get(i,1) > subLimite && divM.get(i, 2) > subLimite){
                ps = i+1;
                //Log.i("ps",Integer.toString(ps));
                //System.out.println("ps:"+Integer.toString(ps));
                break;
            }
        }

        for (int i = row-1; i > ps; i--){
            if(divM.get(i,0) > subLimite && divM.get(i,1) > subLimite && divM.get(i,2) > subLimite){
                pe = i;
                //Log.i("pe",Integer.toString(pe));
                //System.out.println("pe:"+Integer.toString(pe));
                break;
            }
        }
        return normalHandDataMatrix.getMatrix(ps,pe,0,col-1);
    }

    //根据截取的手势数据数组计算距离均值
    private double getModeDistance(Matrix[] subHandDataMatrixArry){
        double w = 0;
        double u = 0;
        int times = 0;
        for(int i = 0; i < subHandDataMatrixArry.length; i++)
        {
            for(int j = 0; j < i; j++){
                Matrix tm = subHandDataMatrixArry[i].copy();
                Matrix rm = subHandDataMatrixArry[j].copy();
                //第一种实现方式
                double dtwX = DTW(tm.getMatrix(0,tm.getRowDimension()-1,0,0),rm.getMatrix(0,rm.getRowDimension()-1,0,0));
                double dtwY = DTW(tm.getMatrix(0,tm.getRowDimension()-1,1,1),rm.getMatrix(0,rm.getRowDimension()-1,1,1));
                double dtwZ = DTW(tm.getMatrix(0,tm.getRowDimension()-1,2,2),rm.getMatrix(0,rm.getRowDimension()-1,2,2));
                w = w + ((dtwX + dtwY + dtwZ)/ 3);
                //第二种实现方式
                /*                double d = DTW(tm,rm);
                w = w + d;*/

                times++;
            }
        }
        u = w / times;
/*        saveToFile("u.txt",Double.toString(u));
        saveToFile("u.txt",Integer.toString(times));*/
        return u;
    }

    //DTW算法，算出两个同维的时间序列的匹配程度，若序列不同维，则返回-1，
    // 否则返回两个序列直接的最小累计平均距离
    public double DTW (Matrix tm, Matrix rm) {
        int n = tm.getRowDimension();
        int tColumnDimension = tm.getColumnDimension();
        int m = rm.getRowDimension();
        int rColumnDimension = rm.getColumnDimension();
        double[][] dm = null;
        if (tColumnDimension != rColumnDimension) {
            return -1;
        } else {
            dm = new double[n][m];
            //算出距离矩阵
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    dm[i][j] = getDistance(tm.getMatrix(i, i, 0, tColumnDimension - 1),
                            rm.getMatrix(j, j, 0, rColumnDimension - 1));
                }

            }

            //求DTW
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
    private double getDistance(Matrix v1, Matrix v2){
        Matrix minusM = v1.minus(v2);
        double distance = Math.sqrt(minusM.times(minusM.transpose()).get(0,0));
        return distance;
    }

    //返回三个数最小的数
    private double getMin(double d1, double d2, double d3){
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

    //保存数据到文件
    public void saveToFile(String fileName, Matrix dataMatrix) {

      FileOutputStream out = null;
        BufferedWriter writer = null;
        boolean append = true;
        try{
            //out = new FileOutputStream(path+fileName);
            //Log.i("path",path+fileName);
            out = new FileOutputStream(Environment.getExternalStorageDirectory().getPath()+"/毕业设计/"+fileName+".txt");
            writer = new BufferedWriter(new OutputStreamWriter(out));
            for(int i = 0; i < dataMatrix.getRowDimension(); i++)
            {
                for(int j = 0; j < dataMatrix.getColumnDimension(); j++)
                {
                    writer.write(Double.toString(dataMatrix.get(i,j)));
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

    public void createDir(String filePath){
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
