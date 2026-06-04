package com.ding.campusguide;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
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

public class LostFoundActivity extends AppCompatActivity {
    private String currentStudentId;
    private ListView lvLostFound;
    private List<JSONObject> dataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lost_found);
        currentStudentId = getIntent().getStringExtra("USER_STUDENT_ID");

        findViewById(R.id.tv_back).setOnClickListener(v -> finish());
        lvLostFound = findViewById(R.id.lv_lost_found);

        findViewById(R.id.btn_publish).setOnClickListener(v -> showPublishDialog());

        lvLostFound.setOnItemClickListener((parent, view, position, id) -> {
            try {
                JSONObject item = dataList.get(position);
                boolean isOwner = item.getString("student_id").equals(currentStudentId);

                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle((item.getString("type").equals("lost") ? "【寻物】" : "【招领】") + item.getString("title"))
                        .setMessage("详情：" + item.getString("content") + "\n联系方式：" + item.getString("contact"))
                        .setPositiveButton("关闭", null);

                if (isOwner) {
                    int postId = item.getInt("id");
                    builder.setNegativeButton("删除此帖", (dialog, which) -> deletePost(postId));
                }
                builder.show();
            } catch (Exception e) { e.printStackTrace(); }
        });
        fetchData();
    }

    private void showPublishDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText etTitle = new EditText(this);
        etTitle.setHint("标题，例如：丢失蓝色雨伞");
        etTitle.setTextColor(Color.BLACK);
        etTitle.setHintTextColor(Color.GRAY);
        etTitle.setBackgroundResource(R.drawable.bg_input_modern);
        etTitle.setPadding(28, 16, 28, 16);
        etTitle.setSingleLine(true);

        EditText etContent = new EditText(this);
        etContent.setHint("详细描述，例如：地点、时间、物品特征");
        etContent.setTextColor(Color.BLACK);
        etContent.setHintTextColor(Color.GRAY);
        etContent.setBackgroundResource(R.drawable.bg_input_modern);
        etContent.setPadding(28, 16, 28, 16);
        etContent.setMinLines(3);

        EditText etContact = new EditText(this);
        etContact.setHint("联系方式");
        etContact.setTextColor(Color.BLACK);
        etContact.setHintTextColor(Color.GRAY);
        etContact.setBackgroundResource(R.drawable.bg_input_modern);
        etContact.setPadding(28, 16, 28, 16);
        etContact.setSingleLine(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 12, 0, 12);

        layout.addView(etTitle, lp);
        layout.addView(etContent, lp);
        layout.addView(etContact, lp);

        new AlertDialog.Builder(this)
                .setTitle("发布失物招领")
                .setView(layout)
                .setPositiveButton("发布", (d, w) -> submitPost(
                        "lost",
                        etTitle.getText().toString(),
                        etContent.getText().toString(),
                        etContact.getText().toString()
                ))
                .setNegativeButton("取消", null)
                .show();
    }


    private void submitPost(String type, String title, String content, String contact) {
        JSONObject json = new JSONObject();
        try {
            json.put("studentId", currentStudentId); json.put("type", type);
            json.put("title", title); json.put("content", content); json.put("contact", contact);
        } catch (Exception e) {}

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());
        new OkHttpClient().newCall(new Request.Builder().url("http://10.0.2.2:8080/api/lostfound").post(body).build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> { Toast.makeText(LostFoundActivity.this, "发布成功", Toast.LENGTH_SHORT).show(); fetchData(); });
            }
        });
    }

    private void deletePost(int postId) {
        new OkHttpClient().newCall(new Request.Builder().url("http://10.0.2.2:8080/api/lostfound/" + postId + "?studentId=" + currentStudentId).delete().build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException { runOnUiThread(() -> fetchData()); }
        });
    }

    private void fetchData() {
        new OkHttpClient().newCall(new Request.Builder().url("http://10.0.2.2:8080/api/lostfound").get().build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONArray array = new JSONObject(response.body().string()).getJSONArray("data");
                    List<String> displayList = new ArrayList<>();
                    dataList.clear();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        dataList.add(obj);
                        displayList.add(
                                (obj.getString("type").equals("lost") ? "寻物启事" : "招领启事")
                                        + "\n"
                                        + obj.getString("title")
                                        + "\n点击查看详情与联系方式"
                        );
                    }

                    runOnUiThread(() -> {

                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                LostFoundActivity.this,
                                R.layout.item_lost_found,
                                displayList
                        );
                        lvLostFound.setAdapter(adapter);
                    });} catch (Exception e) { e.printStackTrace(); }
            }
        });
    }
}