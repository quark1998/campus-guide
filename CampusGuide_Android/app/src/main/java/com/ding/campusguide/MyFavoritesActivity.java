package com.ding.campusguide;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.*;

public class MyFavoritesActivity extends AppCompatActivity {
    private String studentId;
    private ListView lvFavorites;
    private List<JSONObject> favList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_favorites);
        studentId = getIntent().getStringExtra("USER_STUDENT_ID");

        findViewById(R.id.tv_back).setOnClickListener(v -> finish());
        lvFavorites = findViewById(R.id.lv_favorites);


        lvFavorites.setOnItemClickListener((parent, view, position, id) -> {
            try {
                JSONObject marker = favList.get(position);
                new AlertDialog.Builder(this)
                        .setTitle("📍 " + marker.getString("name"))
                        .setMessage("分类：" + marker.optString("category", "默认") + "\n\n" + marker.getString("snippet"))
                        .setPositiveButton("去这里导航", (dialog, which) -> {
                            Intent intent = new Intent();
                            intent.putExtra("NAV_LAT", marker.optDouble("latitude"));
                            intent.putExtra("NAV_LNG", marker.optDouble("longitude"));
                            setResult(RESULT_OK, intent);
                            finish();
                        })

                        .setNeutralButton("取消收藏", (dialog, which) -> cancelFavorite(marker.optString("id")))
                        .setNegativeButton("关闭", null)
                        .show();
            } catch (Exception e) { e.printStackTrace(); }
        });

        fetchFavorites();
    }

    private void fetchFavorites() {
        new OkHttpClient().newCall(new Request.Builder()
                        .url("http://10.0.2.2:8080/api/favorites?studentId=" + studentId).get().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> Toast.makeText(MyFavoritesActivity.this, "网络异常", Toast.LENGTH_SHORT).show());
                    }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        try {
                            JSONArray array = new JSONObject(response.body().string()).getJSONArray("data");
                            List<String> displayList = new ArrayList<>();
                            favList.clear();
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject obj = array.getJSONObject(i);
                                favList.add(obj);
                                displayList.add("⭐ " + obj.getString("name") + "\n所属分类: " + obj.optString("category", "暂无"));
                            }
                            runOnUiThread(() -> {
                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                        MyFavoritesActivity.this,
                                        R.layout.item_modern_list,
                                        android.R.id.text1,
                                        displayList
                                ) {
                                    @Override
                                    public android.view.View getView(int pos, android.view.View convertView, android.view.ViewGroup parent) {
                                        android.widget.TextView tv = (android.widget.TextView) super.getView(pos, convertView, parent);
                                        tv.setText(displayList.get(pos) + "\n点击查看详情 / 导航 / 取消收藏");
                                        tv.setTextColor(android.graphics.Color.parseColor("#172033"));
                                        tv.setTextSize(15f);
                                        return tv;
                                    }
                                };

                                lvFavorites.setAdapter(adapter);
                            });
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                });
    }

    private void cancelFavorite(String markerId) {
        JSONObject json = new JSONObject();
        try {
            json.put("studentId", studentId);
            json.put("markerId", markerId);
        } catch (Exception e) {}

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());

        new OkHttpClient().newCall(new Request.Builder().url("http://10.0.2.2:8080/api/favorites/delete").post(body).build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    Toast.makeText(MyFavoritesActivity.this, "已取消收藏", Toast.LENGTH_SHORT).show();
                    fetchFavorites();
                });
            }
        });
    }
}
