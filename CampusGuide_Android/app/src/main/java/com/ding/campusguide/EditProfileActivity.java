package com.ding.campusguide;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditProfileActivity extends AppCompatActivity {
    private String studentId;
    private EditText etNickname, etPassword;
    private ImageView ivAvatar;
    private String avatarUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        studentId = getIntent().getStringExtra("USER_STUDENT_ID");
        etNickname = findViewById(R.id.et_edit_nickname);
        etPassword = findViewById(R.id.et_edit_password);
        ivAvatar = findViewById(R.id.iv_edit_avatar);


        String oldAvatar = getIntent().getStringExtra("USER_AVATAR");
        if (oldAvatar != null && !oldAvatar.isEmpty()) {
            avatarUrl = oldAvatar;
            Glide.with(this).load(oldAvatar).circleCrop().into(ivAvatar);
        }

        findViewById(R.id.tv_edit_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save_profile).setOnClickListener(v -> saveProfile());


        ActivityResultLauncher<Intent> pickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();

                        Glide.with(this).load(uri).circleCrop().into(ivAvatar);

                        uploadAvatar(uri);
                    }
                });

        findViewById(R.id.card_edit_avatar).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickerLauncher.launch(intent);
        });
    }

    private void uploadAvatar(Uri uri) {
        File file = new File(getCacheDir(), "temp_avatar.jpg");
        try (InputStream is = getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/api/upload")
                .post(body)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "头像上传失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject res = new JSONObject(response.body().string());
                    if (res.getInt("code") == 200) {
                        avatarUrl = res.getString("data");
                        runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "头像上传成功", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private void saveProfile() {

        String newNickname = etNickname.getText().toString().trim();
        String newPassword = etPassword.getText().toString().trim();

        JSONObject json = new JSONObject();
        try {
            json.put("studentId", studentId);
            json.put("nickname", newNickname);
            json.put("password", newPassword);
            json.put("avatar", avatarUrl);
        } catch (Exception e) { e.printStackTrace(); }

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json.toString());
        Request request = new Request.Builder().url("http://10.0.2.2:8080/api/user/update").post(body).build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String res = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonObject = new JSONObject(res);
                        if (jsonObject.getInt("code") == 200) {
                            Toast.makeText(EditProfileActivity.this, "资料更新成功，请重新登录", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(EditProfileActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(EditProfileActivity.this, jsonObject.getString("msg"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                });
            }
        });
    }
}