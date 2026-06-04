package com.ding.campusguide;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etStudentId, etNickname, etNewPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etStudentId = findViewById(R.id.et_reset_student_id);
        etNickname = findViewById(R.id.et_reset_nickname);
        etNewPassword = findViewById(R.id.et_reset_new_password);
    }

    public void onResetClick(View view) {
        String studentId = etStudentId.getText().toString().trim();
        String nickname = etNickname.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (studentId.isEmpty() || nickname.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "请将信息填写完整！", Toast.LENGTH_SHORT).show();
            return;
        }


        Toast.makeText(this, "正在提交请求，请稍候...", Toast.LENGTH_SHORT).show();
        doResetPassword(studentId, nickname, newPassword);
    }

    private void doResetPassword(String studentId, String nickname, String newPassword) {

        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();

        JSONObject json = new JSONObject();
        try {
            json.put("studentId", studentId);
            json.put("nickname", nickname);
            json.put("newPassword", newPassword);
        } catch (Exception e) {
            e.printStackTrace();
        }

        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json; charset=utf-8"),
                json.toString()
        );

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("http://10.0.2.2:8080/api/user/resetPassword")
                .post(body)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ForgotPasswordActivity.this, "网络连接失败，请检查后端是否开启", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                final String res = response.body().string();

                runOnUiThread(() -> {
                    try {
                        JSONObject resJson = new JSONObject(res);
                        int code = resJson.getInt("code");
                        String msg = resJson.getString("msg");

                        if (code == 200) {
                            Toast.makeText(ForgotPasswordActivity.this, "密码修改成功！", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();

                        Toast.makeText(ForgotPasswordActivity.this, "解析失败，后端返回: " + res, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
