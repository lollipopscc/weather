package cc.lollipops.weather;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import cc.lollipops.weather.db.DbHelper;

public class WeatherActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private TextView cityname_text_view, temp_text_view, publish_time_text_view, weather_text_view;        // 城市名,当前温度,更新时间,天气现象
    private ImageView weather_icon_image_view;
    private TextView detail_value_text_view1, detail_value_text_view2, detail_value_text_view3,     // 体感温度,湿度,紫外线级别
            detail_value_text_view4, detail_value_text_view5, detail_value_text_view6;        // 降水量,降水概率,能见度
    private TextView date_text_view1, date_text_view2, date_text_view3,
            date_text_view4, date_text_view5, date_text_view6, date_text_view7;     // 日期
    private ImageView weather_icon_image_view1, weather_icon_image_view2, weather_icon_image_view3,
            weather_icon_image_view4, weather_icon_image_view5, weather_icon_image_view6, weather_icon_image_view7;      // 天气状态图标
    private TextView weather_text_view1, weather_text_view2, weather_text_view3,
            weather_text_view4, weather_text_view5, weather_text_view6, weather_text_view7;      // 天气状态
    private TextView temp_max_text_view1, temp_max_text_view2, temp_max_text_view3,
            temp_max_text_view4, temp_max_text_view5, temp_max_text_view6, temp_max_text_view7;      // 最高温
    private TextView temp_min_text_view1, temp_min_text_view2, temp_min_text_view3,
            temp_min_text_view4, temp_min_text_view5, temp_min_text_view6, temp_min_text_view7;      // 最低

    private ImageView add_city;
    private SQLiteDatabase db;
    private List<String> citynamelist, areaidlist;
    private String current_namecn;
    private String item_namecn, item_areaid;

    public static final int SHOW_RESPONSE = 0;

    // 处理返回的实时天气
    private Handler handler1 = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_RESPONSE:
                    // 获取返回的Json
                    String info = (String) msg.obj;
                    // 在这里进行UI操作，将结果显示到界面上
//                    Toast.makeText(WeatherActivity.this, info, Toast.LENGTH_SHORT).show();
                    parseJSON1(info);
            }
        }
    };

    // 处理返回的7天天气
    private Handler handler2 = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_RESPONSE:
                    // 获取返回的Json
                    String info = (String) msg.obj;
                    // 在这里进行UI操作，将结果显示到界面上
//                    Toast.makeText(WeatherActivity.this, info, Toast.LENGTH_SHORT).show();
                    parseJSON2(info);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                imageView_AddCity();
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        item_current_citylist();

        // 初始化ImageView与TextView
        init_ImageViewandTextView();

        // 添加item
        for (int i = 0; i < citynamelist.size(); i++) {
            String item_areaid = citynamelist.get(i);
            navigationView.getMenu().add(0, 1, 1, item_areaid).setCheckable(true);
        }
        navigationView.setNavigationItemSelectedListener(this);

        // 默认显示item(0)
        cityname_text_view = (TextView) findViewById(R.id.cityname_text_view);
        current_namecn = citynamelist.get(0);
        search_itemcity(current_namecn);    // 按城市名查找id
        cityname_text_view.setText(current_namecn);

        // 发送请求
        sendRequestWithHttpURLConnection1();
        sendRequestWithHttpURLConnection2();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.weather, menu);
        return true;
    }

    // 菜单栏点击事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            Intent intent = new Intent();
            intent.setClass(WeatherActivity.this, WeatherActivity.class);
            startActivity(intent);
            WeatherActivity.this.finish();
            Toast.makeText(WeatherActivity.this, "刷新成功", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.action_delete) {
            if (citynamelist.size() != 1) {
                delete_areaid(current_namecn);
            } else {
                Toast.makeText(WeatherActivity.this, "不可删除!", Toast.LENGTH_SHORT).show();
            }
            Intent intent = new Intent();
            intent.setClass(WeatherActivity.this, WeatherActivity.class);
            startActivity(intent);
            WeatherActivity.this.finish();
        } else if (id == R.id.action_close) {
            WeatherActivity.this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    // 侧边栏点击事件
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        current_namecn = (String) item.getTitle();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        search_itemcity(current_namecn);
        cityname_text_view.setText(current_namecn);
        sendRequestWithHttpURLConnection1();
        sendRequestWithHttpURLConnection2();
//        sendRequestWithHttpURLConnection3();

        return true;
    }

    // 添加按钮的点击事件
    public void imageView_AddCity() {
        add_city = (ImageView) findViewById(R.id.nav_imageView_addcity);
        add_city.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(WeatherActivity.this, SearchCityActivity.class);
                startActivity(intent);
            }
        });
    }

    // 查找出当前所有城市
    public void item_current_citylist() {
        citynamelist = new ArrayList<String>();
        areaidlist = new ArrayList<String>();
        DbHelper helper = new DbHelper(WeatherActivity.this);
        db = helper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM current_citylist WHERE NATIONCN=?", new String[]{"中国"});
        while (cursor.moveToNext()) {
            String namecnValue = cursor.getString(cursor.getColumnIndex("NAMECN"));
            String areaidValue = cursor.getString(cursor.getColumnIndex("AREAID"));
            citynamelist.add(namecnValue);
            areaidlist.add(areaidValue);
        }
        cursor.close();
        db.close();
    }

    // 在current_citylist表中删除指定城市
    public void delete_areaid(String current_namecn) {
        DbHelper helper = new DbHelper(WeatherActivity.this);
        db = helper.getWritableDatabase();

        String sql = "DELETE FROM current_citylist WHERE NAMECN=?;";
        db.execSQL(sql, new String[]{current_namecn});
        db.close();
        Toast.makeText(WeatherActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
    }

    // 查找出item的areaid
    public void search_itemcity(String item) {
        DbHelper helper = new DbHelper(WeatherActivity.this);
        db = helper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM current_citylist WHERE NAMECN=?", new String[]{item});
        while (cursor.moveToNext()) {
            item_namecn = cursor.getString(cursor.getColumnIndex("NAMECN"));
            item_areaid = cursor.getString(cursor.getColumnIndex("AREAID"));
        }
        cursor.close();
        db.close();
    }


    // 请求查找实时天气
    protected void sendRequestWithHttpURLConnection1() {
        new Thread() {
            @Override
            public void run() {
                URL url;
                HttpURLConnection connection = null;
                try {
                    url = new URL(
                            "http://service.envicloud.cn:8082/"
                                    + "v2/weatherlive/"
                                    + "BG9SBGLWB3BZY2MXNTQ3OTG1MJY3NJA5/"
                                    + item_areaid);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    InputStream in = connection.getInputStream();
                    // 下面对获取到的输入流进行读取
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    Message message = new Message();
                    message.what = SHOW_RESPONSE;
                    // 将服务器返回的结果存放到Message中
                    message.obj = response.toString();
                    handler1.sendMessage(message);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }.start();
    }

    // 请求查找7天天气
    protected void sendRequestWithHttpURLConnection2() {
        new Thread() {
            @Override
            public void run() {
                URL url;
                HttpURLConnection connection = null;
                try {
                    url = new URL(
                            "http://service.envicloud.cn:8082/"
                                    + "v2/weatherforecast/"
                                    + "BG9SBGLWB3BZY2MXNTQ3OTG1MJY3NJA5/"
                                    + item_areaid);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    InputStream in = connection.getInputStream();
                    // 下面对获取到的输入流进行读取
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    Message message = new Message();
                    message.what = SHOW_RESPONSE;
                    // 将服务器返回的结果存放到Message中
                    message.obj = response.toString();
                    handler2.sendMessage(message);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }.start();
    }

    // 解析Json1,并进行UI操作,将结果显示到界面上
    public void parseJSON1(String jsonData) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            String rcode = jsonObject.getString("rcode");
            if (rcode.equals("200")) {
                String temperature = jsonObject.getString("temperature");
                String updatetime = jsonObject.getString("updatetime");
                String phenomena = jsonObject.getString("phenomena");
                String feelst = jsonObject.getString("feelst");
                String humidity = jsonObject.getString("humidity");
                String rain = jsonObject.getString("rain");

                weather_icon_image_view = (ImageView) findViewById(R.id.weather_icon_image_view);
                phenomena_icon(weather_icon_image_view, phenomena);  // 设置图标

                temp_text_view = (TextView) findViewById(R.id.temp_text_view);
                publish_time_text_view = (TextView) findViewById(R.id.publish_time_text_view);
                weather_text_view = (TextView) findViewById(R.id.weather_text_view);

                temp_text_view.setText(temperature);
                publish_time_text_view.setText(updatetime);
                weather_text_view.setText(phenomena);

                detail_value_text_view1.setText(feelst + "℃");
                detail_value_text_view2.setText(humidity + "%");
                detail_value_text_view4.setText(rain + "mm");
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 解析Json2,并进行UI操作,将结果显示到界面上
    public void parseJSON2(String jsonData) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);

            JSONArray forecast = jsonObject.getJSONArray("forecast");
            for (int i = 0; i < forecast.length(); i++) {
                JSONObject jObject = forecast.getJSONObject(i);

                String uv = jObject.getString("uv");
                String pop = jObject.getString("pop");
                String vis = jObject.getString("vis");

                String date = jObject.getString("date");

                JSONObject condObject = jObject.getJSONObject("cond");
                String cond_d = condObject.getString("cond_d");

                JSONObject tmpObject = jObject.getJSONObject("tmp");
                String min = tmpObject.getString("min");
                String max = tmpObject.getString("max");

                if (i == 0) {
                    detail_value_text_view3.setText(uv);
                    detail_value_text_view5.setText(pop + "%");
                    detail_value_text_view6.setText(vis + "km");

                    date_text_view1.setText("今天");
                    phenomena_icon(weather_icon_image_view1, cond_d);
                    weather_text_view1.setText(cond_d);
                    temp_max_text_view1.setText(max);
                    temp_min_text_view1.setText(min);
                } else if (i == 1) {
                    date_text_view2.setText("明天");
                    phenomena_icon(weather_icon_image_view2, cond_d);
                    weather_text_view2.setText(cond_d);
                    temp_max_text_view2.setText(max);
                    temp_min_text_view2.setText(min);
                } else if (i == 2) {
                    date_text_view3.setText(date.substring(5));
                    phenomena_icon(weather_icon_image_view3, cond_d);
                    weather_text_view3.setText(cond_d);
                    temp_max_text_view3.setText(max);
                    temp_min_text_view3.setText(min);
                } else if (i == 3) {
                    date_text_view4.setText(date.substring(5));
                    phenomena_icon(weather_icon_image_view4, cond_d);
                    weather_text_view4.setText(cond_d);
                    temp_max_text_view4.setText(max);
                    temp_min_text_view4.setText(min);
                } else if (i == 4) {
                    date_text_view5.setText(date.substring(5));
                    phenomena_icon(weather_icon_image_view5, cond_d);
                    weather_text_view5.setText(cond_d);
                    temp_max_text_view5.setText(max);
                    temp_min_text_view5.setText(min);
                } else if (i == 5) {
                    date_text_view6.setText(date.substring(5));
                    phenomena_icon(weather_icon_image_view6, cond_d);
                    weather_text_view6.setText(cond_d);
                    temp_max_text_view6.setText(max);
                    temp_min_text_view6.setText(min);
                } else if (i == 6) {
                    date_text_view7.setText(date.substring(5));
                    phenomena_icon(weather_icon_image_view7, cond_d);
                    weather_text_view7.setText(cond_d);
                    temp_max_text_view7.setText(max);
                    temp_min_text_view7.setText(min);
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 显示图标
    public void phenomena_icon(ImageView imageView, String phenomena) {
        if (phenomena.equals("晴")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_sunny));
        } else if (phenomena.equals("多云")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_cloudy));
        } else if (phenomena.equals("阴")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_overcast));
        } else if (phenomena.equals("阵雨")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_shower));
        } else if (phenomena.equals("雷阵雨")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_thundershower1));
        } else if (phenomena.equals("雷阵雨伴有冰雹")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_thundershower2));
        } else if (phenomena.equals("雨夹雪")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_sleet));
        } else if (phenomena.equals("小雨")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_lightrain));
        } else if (phenomena.equals("中雨")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_moderaterain));
        } else if (phenomena.equals("冻雨")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_hail));
        } else if (phenomena.equals("大雨")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_heavyrain));
        } else if (phenomena.equals("暴雨")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_storm1));
        } else if (phenomena.equals("大暴雨")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_storm2));
        } else if (phenomena.equals("特大暴雨")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_storm3));
        } else if (phenomena.equals("阵雪")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_snowflurry));
        } else if (phenomena.equals("小雪")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_lightsnow));
        } else if (phenomena.equals("中雪")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_moderatesnow));
        } else if (phenomena.equals("大雪")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_heavysnow));
        } else if (phenomena.equals("暴雪")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_blizzard));
        } else if (phenomena.equals("雾")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_foggy));
        } else if (phenomena.equals("浮尘")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_dust));
        } else if (phenomena.equals("扬沙")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_sand));
        } else if (phenomena.equals("沙尘暴")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_duststorm1));
        } else if (phenomena.equals("强沙尘暴")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_duststorm2));
        } else if (phenomena.equals("霾")) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.weather_haze));
        }
    }

    // 初始化ImageView与TextView
    public void init_ImageViewandTextView() {
        // 详情
        detail_value_text_view1 = (TextView) findViewById(R.id.detail_value_text_view1);
        detail_value_text_view2 = (TextView) findViewById(R.id.detail_value_text_view2);
        detail_value_text_view3 = (TextView) findViewById(R.id.detail_value_text_view3);
        detail_value_text_view4 = (TextView) findViewById(R.id.detail_value_text_view4);
        detail_value_text_view5 = (TextView) findViewById(R.id.detail_value_text_view5);
        detail_value_text_view6 = (TextView) findViewById(R.id.detail_value_text_view6);
        // 预报
        date_text_view1 = (TextView) findViewById(R.id.date_text_view1);
        date_text_view2 = (TextView) findViewById(R.id.date_text_view2);
        date_text_view3 = (TextView) findViewById(R.id.date_text_view3);
        date_text_view4 = (TextView) findViewById(R.id.date_text_view4);
        date_text_view5 = (TextView) findViewById(R.id.date_text_view5);
        date_text_view6 = (TextView) findViewById(R.id.date_text_view6);
        date_text_view7 = (TextView) findViewById(R.id.date_text_view7);
        weather_icon_image_view1 = (ImageView) findViewById(R.id.weather_icon_image_view1);
        weather_icon_image_view2 = (ImageView) findViewById(R.id.weather_icon_image_view2);
        weather_icon_image_view3 = (ImageView) findViewById(R.id.weather_icon_image_view3);
        weather_icon_image_view4 = (ImageView) findViewById(R.id.weather_icon_image_view4);
        weather_icon_image_view5 = (ImageView) findViewById(R.id.weather_icon_image_view5);
        weather_icon_image_view6 = (ImageView) findViewById(R.id.weather_icon_image_view6);
        weather_icon_image_view7 = (ImageView) findViewById(R.id.weather_icon_image_view7);
        weather_text_view1 = (TextView) findViewById(R.id.weather_text_view1);
        weather_text_view2 = (TextView) findViewById(R.id.weather_text_view2);
        weather_text_view3 = (TextView) findViewById(R.id.weather_text_view3);
        weather_text_view4 = (TextView) findViewById(R.id.weather_text_view4);
        weather_text_view5 = (TextView) findViewById(R.id.weather_text_view5);
        weather_text_view6 = (TextView) findViewById(R.id.weather_text_view6);
        weather_text_view7 = (TextView) findViewById(R.id.weather_text_view7);
        temp_max_text_view1 = (TextView) findViewById(R.id.temp_max_text_view1);
        temp_max_text_view2 = (TextView) findViewById(R.id.temp_max_text_view2);
        temp_max_text_view3 = (TextView) findViewById(R.id.temp_max_text_view3);
        temp_max_text_view4 = (TextView) findViewById(R.id.temp_max_text_view4);
        temp_max_text_view5 = (TextView) findViewById(R.id.temp_max_text_view5);
        temp_max_text_view6 = (TextView) findViewById(R.id.temp_max_text_view6);
        temp_max_text_view7 = (TextView) findViewById(R.id.temp_max_text_view7);
        temp_min_text_view1 = (TextView) findViewById(R.id.temp_min_text_view1);
        temp_min_text_view2 = (TextView) findViewById(R.id.temp_min_text_view2);
        temp_min_text_view3 = (TextView) findViewById(R.id.temp_min_text_view3);
        temp_min_text_view4 = (TextView) findViewById(R.id.temp_min_text_view4);
        temp_min_text_view5 = (TextView) findViewById(R.id.temp_min_text_view5);
        temp_min_text_view6 = (TextView) findViewById(R.id.temp_min_text_view6);
        temp_min_text_view7 = (TextView) findViewById(R.id.temp_min_text_view7);
    }

}
