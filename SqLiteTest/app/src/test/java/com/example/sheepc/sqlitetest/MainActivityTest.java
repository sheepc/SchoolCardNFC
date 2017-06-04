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
    public void testFRR() throws Exception {
        String[] name = {"羊远灿", "张尚斌","张劲松","闫玉亮","张亚浩","杨友钊"};
        for (int i = 0; i < name.length; i++){

            Matrix[] mode = new Matrix[5];
            for(int j = 1; j <= 5; j++){
                String fileName = "D:\\桌面文件\\桌面\\毕业设计\\数据\\25\\"+name[i]+"\\Raw_"+j+".txt";
                mode[j-1] = readFile(fileName);
            }

            int modelNumer = 5;
            int smoothPeriod = 4;
            double subLimite = 0.05;
            double judgeLimite = 1.3  ; //自己在0.7~0.9 他人在1.3~,不要在1以内，否则或产生收敛现象。
            HandSafe handSafe = new HandSafe(modelNumer,smoothPeriod,subLimite,judgeLimite);

            handSafe.ModelRegister(mode);

            Matrix test;
            int k = 0;
            for (int j = 6; j <= 25; j++){
                String fileName = "D:\\桌面文件\\桌面\\毕业设计\\数据\\25\\"+name[i]+"\\Raw_"+j+".txt";
                //System.out.println(fileName);
                test = readFile(fileName);
                if(!handSafe.judge(test)){
                    k++;
                }
            }
            System.out.println(name[i]+"-"+k);
        }
    }

    @Test
    public void testFAR() {
        String[] name = {"羊远灿", "张尚斌", "张劲松", "闫玉亮", "张亚浩", "杨友钊"};
        for (int i = 0; i < name.length; i++) {

            //读取模版数据
            Matrix[] mode = new Matrix[5];
            for (int j = 1; j <= 5; j++) {
                String modeFileName = "D:\\桌面文件\\桌面\\毕业设计\\数据\\25\\" + name[i] + "\\Raw_" + j + ".txt";
                mode[j - 1] = readFile(modeFileName);
            }
            int modelNumer = 5;
            int smoothPeriod = 4;
            double subLimite = 0.05;
            double judgeLimite = 1.2; //自己在0.7~0.9 他人在1.3~,不要在1以内，否则或产生收敛现象。
            HandSafe handSafe = new HandSafe(modelNumer,smoothPeriod,subLimite,judgeLimite);

            handSafe.ModelRegister(mode);
            int times = 0;
            for (int j = 0; j < name.length; j++){
                if (j == i)
                {
                    continue;
                }else{
                    for (int k = 1; k <= 25; k++)
                    {
                        String testFileName = "D:\\桌面文件\\桌面\\毕业设计\\数据\\25\\" + name[j] + "\\Raw_" + k + ".txt";
                        Matrix testM = readFile(testFileName);
                        if(handSafe.judge(testM)){
                            times++;
                        }
                    }
                }
            }
            System.out.println(name[i]+"-"+times);

        }

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
                    String[] line = lineTxt.split(" ",3);
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

