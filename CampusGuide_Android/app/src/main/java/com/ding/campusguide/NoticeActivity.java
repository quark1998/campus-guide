package com.ding.campusguide;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NoticeActivity extends AppCompatActivity {

    private ListView lvNotices;
    private List<JSONObject> noticeList = new ArrayList<>();
    private NoticeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);


        View btnBack = findViewById(R.id.tv_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        lvNotices = findViewById(R.id.lv_notices);


        lvNotices.setBackgroundColor(Color.parseColor("#F5F5F5"));

        lvNotices.setDivider(null);

        lvNotices.setPadding(0, dpToPx(8), 0, dpToPx(20));
        lvNotices.setClipToPadding(false);

        adapter = new NoticeAdapter();
        lvNotices.setAdapter(adapter);

        fetchNotices();
    }

    private void fetchNotices() {
        new OkHttpClient().newCall(new Request.Builder().url("http://10.0.2.2:8080/api/notices").get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> Toast.makeText(NoticeActivity.this, "网络异常，获取通知失败", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            String res = response.body().string();
                            JSONArray array = new JSONObject(res).getJSONArray("data");
                            noticeList.clear();
                            for (int i = 0; i < array.length(); i++) {
                                noticeList.add(array.getJSONObject(i));
                            }
                            runOnUiThread(() -> adapter.notifyDataSetChanged());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }


    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }


    private class NoticeAdapter extends BaseAdapter {
        @Override public int getCount() { return noticeList.size(); }
        @Override public Object getItem(int position) { return noticeList.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Context context = NoticeActivity.this;
            LinearLayout root;
            TextView tvTitle, tvTime, tvContent;

            if (convertView == null) {

                root = new LinearLayout(context);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));


                LinearLayout card = new LinearLayout(context);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));


                GradientDrawable drawable = new GradientDrawable();
                drawable.setColor(Color.WHITE);
                drawable.setCornerRadius(dpToPx(10));

                drawable.setStroke(dpToPx(1), Color.parseColor("#DCDFE6"));
                card.setBackground(drawable);


                tvTitle = new TextView(context);
                tvTitle.setTextColor(Color.parseColor("#333333"));
                tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                tvTitle.getPaint().setFakeBoldText(true);


                tvTime = new TextView(context);
                tvTime.setTextColor(Color.parseColor("#999999"));
                tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                tvTime.setPadding(0, dpToPx(6), 0, 0);


                View divider = new View(context);
                divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
                LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
                divParams.setMargins(0, dpToPx(12), 0, dpToPx(12));
                divider.setLayoutParams(divParams);


                tvContent = new TextView(context);
                tvContent.setTextColor(Color.parseColor("#666666"));
                tvContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                tvContent.setLineSpacing(dpToPx(4), 1.0f);


                card.addView(tvTitle);
                card.addView(tvTime);
                card.addView(divider);
                card.addView(tvContent);
                root.addView(card);


                root.setTag(new Object[]{tvTitle, tvTime, tvContent});
            } else {
                root = (LinearLayout) convertView;
                Object[] tags = (Object[]) root.getTag();
                tvTitle = (TextView) tags[0];
                tvTime = (TextView) tags[1];
                tvContent = (TextView) tags[2];
            }


            JSONObject notice = noticeList.get(position);
            tvTitle.setText("📢 " + notice.optString("title"));
            tvTime.setText("发布时间: " + notice.optString("create_time", "刚刚"));
            tvContent.setText(notice.optString("content"));

            return root;
        }
    }
}
