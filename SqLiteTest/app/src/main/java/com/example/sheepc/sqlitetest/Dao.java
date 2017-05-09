package com.example.sheepc.sqlitetest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sheepc on 2017/2/28.
 */

public class Dao extends SQLiteOpenHelper {
    private Context mContext;
    private int version;
    public Dao(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
        super(context, name, factory, version);
        mContext = context;
        this.version = version;
    };

    @Override
    //创建数据表
    public void onCreate(SQLiteDatabase db) {
        for(int k = 1; k <= 5; k++)
        {
            String RawDataTableName = "RawData_" + k;
            String SmoothDataTableName = "SmoothData_" + k;
            String NormalDataTableName = "NormalData_" + k;
            String CutDataTableName = "CutData_" + k;

            db.execSQL("create table "+RawDataTableName+"("
                    + "id integer primary key autoincrement, "
                    + "x double, "
                    + "y double, "
                    + "z double "
                    + ")");

            db.execSQL("create table "+SmoothDataTableName+"("
                    + "id integer primary key autoincrement, "
                    + "x double, "
                    + "y double, "
                    + "z double "
                    + ")");

            db.execSQL("create table "+NormalDataTableName+"("
                    + "id integer primary key autoincrement, "
                    + "x double, "
                    + "y double, "
                    + "z double "
                    + ")");

            db.execSQL("create table "+CutDataTableName+"("
                    + "id integer primary key autoincrement, "
                    + "x double, "
                    + "y double, "
                    + "z double "
                    + ")");

        }
        //Toast.makeText(mContext, "Create Succeeded", Toast.LENGTH_SHORT).show();
    }

    @Override
    //卸载数据表
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for(int k = 1; k <= 5; k++)
        {
            String RawDataTableName = "RawData_" + k;
            String SmoothDataTableName = "SmoothData_" + k;
            String NormalDataTableName = "NormalData_" + k;
            String CutDataTableName = "CutData_" + k;
            db.execSQL("drop table "+ RawDataTableName);
            db.execSQL("drop table "+ SmoothDataTableName);
            db.execSQL("drop table "+ NormalDataTableName);
            db.execSQL("drop table "+ CutDataTableName);
        }
        onCreate(db);
    }

    //插入原始数据
    public void insertRawData(float[] values,int k){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("x",values[0]);
        contentValues.put("y",values[1]);
        contentValues.put("z",values[2]);
        String RawDataTableName = "RawData_" + k;

        db.insert(RawDataTableName,null,contentValues);
    }

    //插入归一化后的数据
    public void insertNormalData(double[][] values,int k){
        SQLiteDatabase db = this.getWritableDatabase();

        for (int i = 0; i < values.length; i++ ) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("x", values[i][0]);
            contentValues.put("y", values[i][1]);
            contentValues.put("z", values[i][2]);
            String NormalDataTableName = "NormalData_" + k;
            db.insert(NormalDataTableName,null,contentValues);
        }


    }

    //返回原始数据的列表
    public ArrayList<String> selectRawData(int k) {
        String RawDataTableName = "RawData_" + k;
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList list = new ArrayList<String>();
        Cursor cursor = db.query(RawDataTableName, null, null, null, null, null, null);
        if (cursor.moveToFirst()){
            do{
                int id = cursor.getInt(cursor.getColumnIndex("id"));
                double x = cursor.getDouble(cursor.getColumnIndex("x"));
                double y = cursor.getDouble(cursor.getColumnIndex("y"));
                double z = cursor.getDouble(cursor.getColumnIndex("z"));
                list.add(id + " " + x + "," + y + "," + z);
            }while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    //返回原始数据的数组
    public double[][] selecttRawData_2(int k) {
        String RawDataTableName = "RawData_" + k;
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList list = new ArrayList<Float[]>();
        double[][] valueArry = null;
        Cursor cursor = db.query(RawDataTableName, null, null, null, null, null, null);
        if (cursor.moveToFirst()){
            do{
                double x = cursor.getDouble(cursor.getColumnIndex("x"));
                double y = cursor.getDouble(cursor.getColumnIndex("y"));
                double z = cursor.getDouble(cursor.getColumnIndex("z"));
                list.add(new double[]{x,y,z});
            }while (cursor.moveToNext());

            int size = list.size();
            valueArry = (double[][]) list.toArray(new double[size][3]);    //获得数据的二维数组
        }
        cursor.close();
        return valueArry;
    }

    //返回归一化数据的数组
    public double[][] selecttNormalData(int k) {
        String NormalDataTableName = "NormalData_" + k;
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList list = new ArrayList<Float[]>();
        double[][] valueArry = null;
        Cursor cursor = db.query(NormalDataTableName, null, null, null, null, null, null);
        if (cursor.moveToFirst()){
            do{
                double x = cursor.getDouble(cursor.getColumnIndex("x"));
                double y = cursor.getDouble(cursor.getColumnIndex("y"));
                double z = cursor.getDouble(cursor.getColumnIndex("z"));
                list.add(new double[]{x,y,z});
            }while (cursor.moveToNext());

            int size = list.size();
            valueArry = (double[][]) list.toArray(new double[size][3]);    //获得数据的二维数组
        }
        cursor.close();
        return valueArry;
    }
}
