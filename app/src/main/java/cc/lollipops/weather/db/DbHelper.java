package cc.lollipops.weather.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "areaid.db";
    private static final int DB_VERSION = 2;
    private SQLiteDatabase db;

    private String filePath;

    public DbHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);

        filePath = "//data//data//" + context.getPackageName() + "//databases";

        initExternalDB(context);

        db = getWritableDatabase();
    }

    private void initExternalDB(Context context) {
        InputStream ins = null;
        FileOutputStream fos = null;
        try {
            ins = context.getAssets().open("databases/" + DB_NAME);
            File file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
            File dbFile = new File(filePath + "//" + DB_NAME + "//");
            SharedPreferences preferences = context.getSharedPreferences("dbVersion", 0);
            int dbVersion = preferences.getInt("dbVersion", 1);
            if (!dbFile.exists() || DB_VERSION > dbVersion) {
                fos = new FileOutputStream(dbFile);
                byte[] buf = new byte[1024];
                int len = -1;
                while ((len = ins.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                    fos.flush();
                }
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("dbVersion", DB_VERSION);
                editor.commit();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (ins != null) {
                    ins.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            onCreate(db);
        }
    }

}
