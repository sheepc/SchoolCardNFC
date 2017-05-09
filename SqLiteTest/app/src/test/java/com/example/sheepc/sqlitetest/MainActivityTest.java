package com.example.sheepc.sqlitetest;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import Jama.Matrix;

import static org.junit.Assert.*;

/**
 * Created by sheepc on 2017/4/19.
 */
public class MainActivityTest {
    @Test
    public void test() throws Exception {
//        Matrix t = readFile("D:\\桌面文件\\桌面\\毕业设计\\数据\\羊远灿\\完整-0.05\\NormalData1.txt");
        /*Matrix m = readFile("D:\\桌面文件\\桌面\\毕业设计\\数据\\羊远灿\\1\\NormalData.txt");
        */
        //Matrix t = new Matrix(new double[][]{{1,1,1},{2,2,2},{3,3,3},{4,4,4},{3,3,3},{2,2,2},{2,2,2},{1,1,1}});
/*        MainActivity mainActivity = new MainActivity();
        Matrix cutMatrix = mainActivity.cut(t,-0.05);
        cutMatrix.print(cutMatrix.getRowDimension(),cutMatrix.getColumnDimension());*/
        int times = 0;
        for(int i = 1; i <= 5; i++){
            for(int j = 1; j < i; j++){
                times++;
            }
        }
        System.out.println(Integer.toString(times));
    }

    public Matrix readFile(String filePath){
        File file = new File(filePath);
        try{
            if(file.isFile() && file.exists()) {
                InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
                BufferedReader bufferedReader = new BufferedReader(reader);
                String lineTxt = null;
                ArrayList<double[]> list = new ArrayList<double[]>();
                while ((lineTxt = bufferedReader.readLine()) != null){
                    String[] line = lineTxt.split(",",3);
                    list.add(new double[]{Double.parseDouble(line[0]),Double.parseDouble(line[1]),Double.parseDouble(line[2])});
                }
                double[][] res = new double[list.size()][3];
                for (int i = 0; i < list.size(); i++)
                {
                    double[] t = list.get(i);
                    for (int j = 0; j < t.length; j++)
                    {
                        res[i][j] = t[j];
                    }
                }
                return new Matrix(res);
            }
            else{
                System.out.println("找不到文件");

            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

}

