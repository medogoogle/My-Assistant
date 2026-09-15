package com.myassistant.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class Backup {
    private static final String[] TABLES={"accounts","debts","trades","tasks","transactions","investments","recurring_transactions"};

    public static void export(Context ctx, DatabaseHelper db, Uri uri) throws Exception {
        JSONObject root=new JSONObject();
        root.put("app","myassistant");
        root.put("version",1);
        for(String table:TABLES){
            JSONArray arr=new JSONArray();
            Cursor c=db.getReadableDatabase().rawQuery("SELECT * FROM "+table,null);
            String[] cols=c.getColumnNames();
            while(c.moveToNext()){
                JSONObject row=new JSONObject();
                for(int i=0;i<cols.length;i++){
                    switch(c.getType(i)){
                        case Cursor.FIELD_TYPE_INTEGER: row.put(cols[i], c.getLong(i)); break;
                        case Cursor.FIELD_TYPE_FLOAT: row.put(cols[i], c.getDouble(i)); break;
                        case Cursor.FIELD_TYPE_NULL: row.put(cols[i], JSONObject.NULL); break;
                        default: row.put(cols[i], c.getString(i));
                    }
                }
                arr.put(row);
            }
            c.close();
            root.put(table, arr);
        }
        OutputStream os=ctx.getContentResolver().openOutputStream(uri);
        if(os==null) throw new IOException("تعذر فتح الملف للكتابة");
        os.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        os.flush(); os.close();
    }

    public static void restore(Context ctx, DatabaseHelper db, Uri uri) throws Exception {
        InputStream is=ctx.getContentResolver().openInputStream(uri);
        if(is==null) throw new IOException("تعذر فتح الملف للقراءة");
        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        byte[] buf=new byte[4096]; int n;
        while((n=is.read(buf))!=-1) bos.write(buf,0,n);
        is.close();
        JSONObject root=new JSONObject(bos.toString("UTF-8"));
        SQLiteDatabase wdb=db.getWritableDatabase();
        wdb.beginTransaction();
        try{
            for(String table:TABLES){
                if(!root.has(table)) continue;
                wdb.delete(table,null,null);
                JSONArray arr=root.getJSONArray(table);
                for(int i=0;i<arr.length();i++){
                    JSONObject row=arr.getJSONObject(i);
                    ContentValues cv=new ContentValues();
                    Iterator<String> keys=row.keys();
                    while(keys.hasNext()){
                        String k=keys.next();
                        Object v=row.get(k);
                        if(v==JSONObject.NULL) cv.putNull(k);
                        else if(v instanceof Integer) cv.put(k,(Integer)v);
                        else if(v instanceof Long) cv.put(k,(Long)v);
                        else if(v instanceof Double) cv.put(k,(Double)v);
                        else cv.put(k, v.toString());
                    }
                    wdb.insert(table,null,cv);
                }
            }
            wdb.setTransactionSuccessful();
        } finally {
            wdb.endTransaction();
        }
    }
}
