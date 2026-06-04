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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.*;

public class CampusLandmarksActivity extends AppCompatActivity {
    private String studentId;
    private Map<String, List<JSONObject>> groupedData = new HashMap<>();
    private List<String> currentItems = new ArrayList<>();
    private List<JSONObject> currentLandmarks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_landmarks);
        studentId = getIntent().getStringExtra("USER_STUDENT_ID");

        findViewById(R.id.tv_back).setOnClickListener(v -> finish());
        fetchData();}
    private void fetchData() {
        new OkHttpClient().newCall(new Request.Builder().url("http://10.0.2.2:8080/api/markers/all").get().build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONArray data = new JSONObject(response.body().string()).getJSONArray("data");
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        String cat = obj.optString("category", "其他");
                        if (!groupedData.containsKey(cat)) groupedData.put(cat, new ArrayList<>());
                        groupedData.get(cat).add(obj);
                    }
                    runOnUiThread(() -> updateUI());
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }
    private void updateUI() {
        List<String> categories = new ArrayList<>(groupedData.keySet());
        ListView lvCat = findViewById(R.id.lv_categories);
        ListView lvItems = findViewById(R.id.lv_landmark_items);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(
                this,
                R.layout.item_landmark_category,
                android.R.id.text1,
                categories
        ) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.widget.TextView tv = (android.widget.TextView) super.getView(position, convertView, parent);
                tv.setText(categories.get(position));
                tv.setTextColor(android.graphics.Color.parseColor("#3A86FF"));
                tv.setTextSize(15f);
                return tv;
            }
        };
        lvCat.setAdapter(catAdapter);
        lvCat.setOnItemClickListener((p, v, position, id) -> {
            String selectedCat = categories.get(position);
            currentLandmarks = groupedData.get(selectedCat);
            currentItems.clear();
            if (currentLandmarks != null) {
                for (JSONObject item : currentLandmarks) {
                    currentItems.add(item.optString("name"));
                }
            }
            ArrayAdapter<String> itemAdapter = new ArrayAdapter<String>(
                    this,
                    R.layout.item_modern_list,
                    android.R.id.text1,
                    currentItems
            ) {
                @Override
                public android.view.View getView(int pos, android.view.View convertView, android.view.ViewGroup parent) {
                    android.widget.TextView tv = (android.widget.TextView) super.getView(pos, convertView, parent);
                    tv.setText("" + currentItems.get(pos) + "\n点击查看详情、收藏或导航");
                    tv.setTextColor(android.graphics.Color.parseColor("#172033"));
                    tv.setTextSize(15f);
                    return tv;
                }
            };
            lvItems.setAdapter(itemAdapter);
        });
        lvItems.setOnItemClickListener((p, v, pos, id) -> {
            JSONObject landmark = currentLandmarks.get(pos);
            new AlertDialog.Builder(this)
                    .setTitle("" + landmark.optString("name"))
                    .setMessage(landmark.optString("snippet"))
                    .setPositiveButton("去这里", (d, w) -> {
                        Intent intent = new Intent();
                        intent.putExtra("NAV_LAT", landmark.optDouble("latitude", 0));
                        intent.putExtra("NAV_LNG", landmark.optDouble("longitude", 0));
                        setResult(RESULT_OK, intent);
                        finish();
                    })
                    .setNeutralButton("收藏", (d, w) -> addFavorite(landmark.optInt("id")))
                    .setNegativeButton("关闭", null)
                    .show();
        });
        if (!categories.isEmpty()) {
            lvCat.performItemClick(
                    lvCat.getAdapter().getView(0, null, lvCat),
                    0,
                    lvCat.getAdapter().getItemId(0)
            );
        }
    }
    private void addFavorite(int markerId) {
        JSONObject json = new JSONObject();
        try {
            json.put("studentId", studentId);
            json.put("markerId", String.valueOf(markerId));
        } catch (Exception e) {}

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());
        new OkHttpClient().newCall(new Request.Builder().url("http://10.0.2.2:8080/api/favorites/add").post(body).build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                final String resStr = response.body().string();
                runOnUiThread(() -> {
                    try {
                        Toast.makeText(CampusLandmarksActivity.this, new JSONObject(resStr).getString("msg"), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {}
                });
            }
        });
    }
}
