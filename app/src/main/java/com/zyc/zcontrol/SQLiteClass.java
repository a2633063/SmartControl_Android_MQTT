package com.zyc.zcontrol;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLiteClass {

    SQLDBHelper dbHelper;

    public SQLiteClass(Context context, String filename) {
        dbHelper = new SQLDBHelper(context, "databases.db", null);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
    }

    public SQLiteClass(Context context) {
        dbHelper = new SQLDBHelper(context, "databases.db", null);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
    }

    //插入 增
    public void Insert(String table, ContentValues cv) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        db.insert(table, null, cv);
        db.close();//关闭数据库
    }

    //删除 删
    public void Delete(String table, String whereClauses, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        //调用delete方法，删除数据
        db.delete(table, whereClauses, whereArgs);
        db.close();
    }

    //查询 查
    public Cursor Query(String table, String[] columns, String selection,
                        String[] selectionArgs, String groupBy, String having,
                        String orderBy) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
        // db.close();

        return cursor;
    }

    //更新 改
    public void Modify(String table, ContentValues cv, String whereClause, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.update(table, cv, whereClause, whereArgs);
        db.close();
    }


    //获取某一项的一个内容
    public String get(String table, String mac, String parameter) {
        String str = null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(table, new String[]{parameter}, "mac=?", new String[]{mac}, null, null, null);
        if (cursor.moveToLast())
            str = cursor.getString(cursor.getColumnIndex(parameter));
        db.close();
        return str;

    }

    class SQLDBHelper extends SQLiteOpenHelper {

        private static final String TAG = "SQLDBHelper";
        public static final int VERSION = 2;

        //必须要有构造函数
        public SQLDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
            super(context, name, factory, VERSION);
        }

        // 当第一次创建数据库的时候，调用该方法
        public void onCreate(SQLiteDatabase db) {
            String sql = "create table device_list(id INTEGER PRIMARY KEY AUTOINCREMENT,name varchar(32) NOT NULL,type int NOT NULL,mac varchar(12) NOT NULL,sort int)";
            //输出创建数据库的日志信息
            Log.i(TAG, "create Database------------->");
            //execSQL函数用于执行SQL语句
            db.execSQL(sql);
        }

        //当更新数据库的时候执行该方法
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //输出更新数据库的日志信息
            Log.i(TAG, "update Database------------->");
            if (oldVersion == 1 && newVersion == 2) {
                db.execSQL("ALTER TABLE device_list ADD COLUMN sort integer;");
            }
        }
    }
}
