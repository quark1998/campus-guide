package com.ding.campusguide;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.WHITE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        setContentView(R.layout.activity_login);

        EditText etStudentId = findViewById(R.id.et_student_id);
        EditText etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);

        btnLogin.setOnClickListener(v -> {
            String studentId = etStudentId.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (studentId.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "学号或密码不能为空！", Toast.LENGTH_SHORT).show();
                return;
            }
            doLogin(studentId, password);
        });
    }


    public void goToRegister(View view) {
        Toast.makeText(this, "正在跳转注册页...", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, RegisterActivity.class));
    }


    public void goToForgotPassword(View view) {
        startActivity(new Intent(this, ForgotPasswordActivity.class));
    }

    private void doLogin(String studentId, String password) {
        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        try {
            json.put("studentId", studentId);
            json.put("password", password);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                json.toString()
        );

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/api/user/login")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "网络连接失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String res = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonObject = new JSONObject(res);
                        if (jsonObject.getInt("code") == 200) {
                            JSONObject userData = jsonObject.getJSONObject("data");
                            String sId = userData.optString("student_id");
                            String nickname = userData.optString("nickname");
                            String avatar = userData.optString("avatar", "");

                            Toast.makeText(LoginActivity.this, "欢迎回来: " + nickname, Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("USER_NICKNAME", nickname);
                            intent.putExtra("USER_STUDENT_ID", sId);
                            intent.putExtra("USER_AVATAR", avatar);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, jsonObject.optString("msg", "登录失败"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("JSON_ERROR", "解析失败: " + res);
                        Toast.makeText(LoginActivity.this, "数据解析异常，请检查 JSON 结构", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}