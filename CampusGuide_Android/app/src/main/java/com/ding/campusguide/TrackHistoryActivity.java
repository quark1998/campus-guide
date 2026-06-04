package com.ding.campusguide;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TrackHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_history);


        findViewById(R.id.tv_back).setOnClickListener(v -> finish());


        String studentId = getIntent().getStringExtra("USER_STUDENT_ID");
        if (studentId != null) {
            fetchHistoryTracks(studentId);
        }
    }

    private void fetchHistoryTracks(String studentId) {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

        String url = "http://10.0.2.2:8080/api/track/history?studentId=" + studentId;

        okhttp3.Request request = new okhttp3.Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(TrackHistoryActivity.this, "网络异常加载失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                final String res = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonObject = new JSONObject(res);
                        JSONArray data = jsonObject.getJSONArray("data");

                        List<String> displayList = new ArrayList<>();
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject item = data.getJSONObject(i);
                            displayList.add("🚶 " + item.getString("createTime") + "\n运动距离：" + String.format("%.2f", item.getDouble("distance")) + " 米");
                        }

                        ListView lvHistory = findViewById(R.id.lv_history_page);
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                TrackHistoryActivity.this,
                                R.layout.item_modern_list,
                                android.R.id.text1,
                                displayList
                        ) {
                            @Override
                            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                                android.widget.TextView tv = (android.widget.TextView) super.getView(position, convertView, parent);
                                tv.setText(displayList.get(position) + "\n点击回放这次轨迹");
                                tv.setTextColor(android.graphics.Color.parseColor("#172033"));
                                tv.setTextSize(15f);
                                return tv;
                            }
                        };
                        lvHistory.setAdapter(adapter);

                        lvHistory.setOnItemClickListener((parent, view, position, id) -> {
                            try {
                                String points = data.getJSONObject(position).getString("points");
                                Intent intent = new Intent();
                                intent.putExtra("REPLAY_POINTS", points);
                                setResult(RESULT_OK, intent);
                                finish();
                            } catch (Exception e) { e.printStackTrace(); }
                        });
                    } catch (Exception e) { e.printStackTrace(); }
                });
            }
        });
    }
}