package com.ding.campusguide;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        EditText etStudentId = findViewById(R.id.et_reg_student_id);
        EditText etNickname = findViewById(R.id.et_reg_nickname);
        EditText etPassword = findViewById(R.id.et_reg_password);
        Button btnRegister = findViewById(R.id.btn_do_register);
        btnRegister.setOnClickListener(v -> {
            String studentId = etStudentId.getText().toString().trim();
            String nickname = etNickname.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (studentId.isEmpty() || nickname.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请将信息填写完整！", Toast.LENGTH_SHORT).show();
                return;
            }

            doRegister(studentId, nickname, password);
        });
    }

    private void doRegister(String studentId, String nickname, String password) {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        JSONObject json = new JSONObject();
        try {
            json.put("studentId", studentId);
            json.put("nickname", nickname);
            json.put("password", password);
        } catch (Exception e) {
            e.printStackTrace();
        }

        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json; charset=utf-8"),
                json.toString()
        );

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("http://10.0.2.2:8080/api/user/register")
                .post(body)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "网络连接失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                String res = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject resJson = new JSONObject(res);
                        int code = resJson.getInt("code");
                        String msg = resJson.getString("msg");

                        if (code == 200) {
                            Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_LONG).show();
                            finish();
                        } else {

                            Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }
}
