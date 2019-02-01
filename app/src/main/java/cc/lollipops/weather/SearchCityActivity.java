package cc.lollipops.weather;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cc.lollipops.weather.db.DbHelper;

public class SearchCityActivity extends AppCompatActivity {

    private ListView listView;
    private List<String> list;
    private ArrayAdapter<String> adapter;

    private SQLiteDatabase db;

    private SearchView searchView;
    private String current_areaid, current_namecn;

    private ImageView back_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_city);

        listView = (ListView) findViewById(R.id.citylist);
        list = new ArrayList<String>();

        query_all_areaid();

        adapter = new ArrayAdapter<String>(SearchCityActivity.this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);
        listView.setTextFilterEnabled(true);

        searchView = (SearchView) findViewById(R.id.searchview);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                if (!TextUtils.isEmpty(newText)) {
                    listView.setFilterText(newText);
                } else {
                    listView.clearTextFilter();
                }
                return false;
            }

        });

        // 点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String s1 = adapter.getItem(position);
                query_areaid(s1);
                Toast.makeText(SearchCityActivity.this, s1, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent();
                intent.setClass(SearchCityActivity.this, WeatherActivity.class);
                startActivity(intent);
                SearchCityActivity.this.finish();
            }
        });

        // 长按事件
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(SearchCityActivity.this, "......", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        // 返回按键
        back_btn = (ImageView) findViewById(R.id.back_btn);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    // 查找全部城市
    public void query_all_areaid() {
        DbHelper helper = new DbHelper(SearchCityActivity.this);
        db = helper.getWritableDatabase();

        Cursor cursor = db.query("areaid", null, "NATIONCN=?", new String[]{"中国"}, null, null, null);
        while (cursor.moveToNext()) {
            String namecnValue = cursor.getString(cursor.getColumnIndex("NAMECN"));
            list.add(namecnValue);
        }
        cursor.close();
        db.close();
    }

    // 查找指定城市,判断是否在current_citylist表中已存在
    public void query_areaid(String name) {
        DbHelper helper = new DbHelper(SearchCityActivity.this);
        db = helper.getWritableDatabase();
        String value_areaid = "";

        Cursor cursor1 = db.query("current_citylist", null, "NAMECN=?", new String[]{name}, null, null, null);
        while (cursor1.moveToNext()) {
            value_areaid = cursor1.getString(cursor1.getColumnIndex("AREAID"));
        }

        if (value_areaid.equals("")) {
            Cursor cursor2 = db.query("areaid", null, "NAMECN=?", new String[]{name}, null, null, null);
            while (cursor2.moveToNext()) {
                current_areaid = cursor2.getString(cursor2.getColumnIndex("AREAID"));
                current_namecn = cursor2.getString(cursor2.getColumnIndex("NAMECN"));
            }
            cursor2.close();
            inseart_areaid();
        } else {
            Toast.makeText(SearchCityActivity.this, "地点已存在", Toast.LENGTH_SHORT).show();
        }
        cursor1.close();
        db.close();
    }

    // 插入指定城市到current_citylist表
    public void inseart_areaid() {
        DbHelper helper = new DbHelper(SearchCityActivity.this);
        db = helper.getWritableDatabase();

        String sql = "INSERT INTO current_citylist (AREAID, NAMECN) VALUES (?, ?);";
        db.execSQL(sql, new String[]{current_areaid, current_namecn});
        db.close();
        Toast.makeText(SearchCityActivity.this, "插入成功", Toast.LENGTH_SHORT).show();
    }

}

