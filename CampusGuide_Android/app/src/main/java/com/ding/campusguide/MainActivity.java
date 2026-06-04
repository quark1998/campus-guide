package com.ding.campusguide;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMapUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements AMapLocationListener {
    private LinearLayout navGuide, navMine;
    private androidx.cardview.widget.CardView navHome;
    private TextView tvGuide, tvMine, tvTrackDistance, tvTrackGoText;
    private androidx.cardview.widget.CardView cardTrackGo;
    private androidx.activity.result.ActivityResultLauncher<Intent> navLauncher;
    private ScrollView pageContainer, pageHomeContainer;
    private View searchCard, trackPanel, locationCard;
    private com.amap.api.maps.TextureMapView mMapView = null;
    private AMap aMap;
    private androidx.activity.result.ActivityResultLauncher<Intent> historyLauncher;
    private boolean isTracking = false;
    private List<LatLng> trackPoints = new ArrayList<>();
    private float totalDistance = 0f;
    private Polyline trackPolyline;
    private android.widget.ListPopupWindow searchDropdown;
    private List<JSONObject> currentSearchResults = new ArrayList<>();
    private Marker currentClickedMarker;
    private Polyline currentRouteLine;

    private String currentStudentId;
    private String currentAvatarUrl;
    private android.os.Handler bannerHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable bannerRunnable;
    private AMapLocationClient mLocationClient;
    private List<LatLng> currentRoutePoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.content.Context context = getApplicationContext();
        MapsInitializer.updatePrivacyShow(context, true, true);
        MapsInitializer.updatePrivacyAgree(context, true);
        AMapLocationClient.updatePrivacyShow(context, true, true);
        AMapLocationClient.updatePrivacyAgree(context, true);
        setContentView(R.layout.activity_main);


        Intent intent = getIntent();
        String nickname = intent.getStringExtra("USER_NICKNAME");
        currentStudentId = intent.getStringExtra("USER_STUDENT_ID");
        currentAvatarUrl = intent.getStringExtra("USER_AVATAR");

        TextView tvUserNickname = findViewById(R.id.tv_user_nickname);
        TextView tvUserStudentId = findViewById(R.id.tv_user_student_id);
        android.widget.ImageView ivUserAvatar = findViewById(R.id.iv_user_avatar);

        if (nickname != null && tvUserNickname != null) tvUserNickname.setText(nickname);
        if (currentStudentId != null && tvUserStudentId != null) tvUserStudentId.setText("学号: " + currentStudentId);

        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty() && ivUserAvatar != null) {
            com.bumptech.glide.Glide.with(this).load(currentAvatarUrl).circleCrop().into(ivUserAvatar);
        }

        mMapView = findViewById(R.id.map_view);
        mMapView.onCreate(savedInstanceState);
        if (aMap == null) aMap = mMapView.getMap();
        com.amap.api.maps.model.LatLng jstuCampus = new com.amap.api.maps.model.LatLng(31.765, 119.923);
        aMap.moveCamera(com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(jstuCampus, 16f));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            showBlueDot();
        }

        historyLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String pointsJson = result.getData().getStringExtra("REPLAY_POINTS");
                        if (pointsJson != null) replayTrack(pointsJson);
                    }
                });

        initBottomNav();
        initTrackLogic();
        fetchMarkersFromServer();
        initSearchLogic();

        aMap.setOnMyLocationChangeListener(new AMap.OnMyLocationChangeListener() {
            boolean isFirstLocate = true;
            @Override
            public void onMyLocationChange(Location location) {
                if (location != null && isFirstLocate) {
                    aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 17f));
                    isFirstLocate = false;
                }
            }
        });

        aMap.setOnMarkerClickListener(marker -> {
            currentClickedMarker = marker;
            showBottomSheetDialog(marker);
            return true;
        });

        aMap.setOnMapClickListener(latLng -> {
            if (currentClickedMarker != null) currentClickedMarker.hideInfoWindow();
            if (currentRouteLine != null) {
                currentRouteLine.remove();
                currentRouteLine = null;
            }
        });

        try {
            mLocationClient = new AMapLocationClient(getApplicationContext());
            mLocationClient.setLocationListener(this);
            AMapLocationClientOption option = new AMapLocationClientOption();
            option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            option.setInterval(2000);
            option.setSensorEnable(true);
            option.setLocationCacheEnable(false);
            mLocationClient.setLocationOption(option);
            mLocationClient.startLocation();
        } catch (Exception e) { e.printStackTrace(); }

        navLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        double lat = result.getData().getDoubleExtra("NAV_LAT", 0);
                        double lng = result.getData().getDoubleExtra("NAV_LNG", 0);
                        if (lat != 0 && lng != 0) {
                            switchPage(0);
                            requestCustomRouteTo(lat, lng);
                        }
                    }
                });
        refreshSystemConfig();
    }

    private void showBlueDot() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
        myLocationStyle.interval(2000);
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true);
        UiSettings uiSettings = aMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showBlueDot();
        }
    }

    private void initBottomNav() {
        navGuide = findViewById(R.id.nav_guide);
        navMine = findViewById(R.id.nav_mine);
        navHome = findViewById(R.id.nav_home);
        tvGuide = findViewById(R.id.tv_guide);
        tvMine = findViewById(R.id.tv_mine);
        pageContainer = findViewById(R.id.page_container);
        pageHomeContainer = findViewById(R.id.page_home_container);
        searchCard = findViewById(R.id.search_card);
        trackPanel = findViewById(R.id.track_panel);
        locationCard = findViewById(R.id.location_card);

        locationCard.setOnClickListener(v -> {
            if (aMap != null && aMap.getMyLocation() != null) {
                Location loc = aMap.getMyLocation();
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 17f));
            }
        });

        navGuide.setOnClickListener(v -> switchPage(0));
        navHome.setOnClickListener(v -> switchPage(1));
        navMine.setOnClickListener(v -> switchPage(2));

        View btnHomeNav = findViewById(R.id.btn_home_nav);
        if(btnHomeNav != null) btnHomeNav.setOnClickListener(v -> switchPage(0));

        View btnHomeLandmarks = findViewById(R.id.btn_home_landmarks);
        if (btnHomeLandmarks != null) {
            btnHomeLandmarks.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CampusLandmarksActivity.class);
                intent.putExtra("USER_STUDENT_ID", currentStudentId);
                navLauncher.launch(intent);
            });
        }

        View btnHomeLost = findViewById(R.id.btn_home_lost);
        if (btnHomeLost != null) {
            btnHomeLost.setOnClickListener(v -> {
                Intent intentLost = new Intent(MainActivity.this, LostFoundActivity.class);
                intentLost.putExtra("USER_STUDENT_ID", currentStudentId);
                startActivity(intentLost);
            });
        }

        View btnHomeNotice = findViewById(R.id.btn_home_notice);
        if (btnHomeNotice != null) {
            btnHomeNotice.setOnClickListener(v -> {
                Intent intentNotice = new Intent(MainActivity.this, NoticeActivity.class);
                intentNotice.putExtra("USER_STUDENT_ID", currentStudentId);
                startActivity(intentNotice);
            });
        }

        View btnMyHistory = findViewById(R.id.btn_my_history);
        if (btnMyHistory != null) {
            btnMyHistory.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, TrackHistoryActivity.class);
                intent.putExtra("USER_STUDENT_ID", currentStudentId);
                historyLauncher.launch(intent);
            });
        }

        switchPage(1);

        View btnEditProfile = findViewById(R.id.btn_edit_profile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intentEdit = new Intent(MainActivity.this, EditProfileActivity.class);
                intentEdit.putExtra("USER_STUDENT_ID", currentStudentId);
                intentEdit.putExtra("USER_AVATAR", currentAvatarUrl);
                startActivity(intentEdit);
            });
        }

        View btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "已退出登录", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            });
        }

        View btnMyFavorites = findViewById(R.id.btn_my_favorites);
        if (btnMyFavorites != null) {
            btnMyFavorites.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, MyFavoritesActivity.class);
                intent.putExtra("USER_STUDENT_ID", currentStudentId);
                navLauncher.launch(intent);
            });
        }
    }

    private void switchPage(int index) {
        tvGuide.setTextColor(Color.parseColor("#888888"));
        tvMine.setTextColor(Color.parseColor("#888888"));
        navHome.setCardBackgroundColor(Color.parseColor("#CCCCCC"));
        if (index == 0) {
            tvGuide.setTextColor(Color.parseColor("#3385FF"));
            mMapView.setVisibility(View.VISIBLE);
            searchCard.setVisibility(View.VISIBLE);
            trackPanel.setVisibility(View.VISIBLE);
            locationCard.setVisibility(View.VISIBLE);
            pageHomeContainer.setVisibility(View.GONE);
            pageContainer.setVisibility(View.GONE);
        } else if (index == 1) {
            navHome.setCardBackgroundColor(Color.parseColor("#3385FF"));
            mMapView.setVisibility(View.GONE);
            searchCard.setVisibility(View.GONE);
            trackPanel.setVisibility(View.GONE);
            locationCard.setVisibility(View.GONE);
            pageHomeContainer.setVisibility(View.VISIBLE);
            pageContainer.setVisibility(View.GONE);
        } else if (index == 2) {
            tvMine.setTextColor(Color.parseColor("#3385FF"));
            mMapView.setVisibility(View.GONE);
            searchCard.setVisibility(View.GONE);
            trackPanel.setVisibility(View.GONE);
            locationCard.setVisibility(View.GONE);
            pageHomeContainer.setVisibility(View.GONE);
            pageContainer.setVisibility(View.VISIBLE);
        }
    }

    private void initTrackLogic() {
        tvTrackDistance = findViewById(R.id.tv_track_distance);
        tvTrackGoText = findViewById(R.id.tv_track_go_text);
        cardTrackGo = findViewById(R.id.card_track_go);
        cardTrackGo.setOnClickListener(v -> {
            if (!isTracking) {
                isTracking = true;
                tvTrackGoText.setText("STOP");
                cardTrackGo.setCardBackgroundColor(Color.parseColor("#FF4C4C"));
                trackPoints.clear();
                totalDistance = 0f;
                tvTrackDistance.setText("0.00 km");
                if (trackPolyline != null) trackPolyline.remove();
            } else {
                isTracking = false;
                tvTrackGoText.setText("GO");
                cardTrackGo.setCardBackgroundColor(Color.parseColor("#3385FF"));
                if (trackPolyline != null) trackPolyline.remove();
                if (totalDistance > 0) uploadTrackData(totalDistance);
            }
        });
    }

    private void uploadTrackData(float distance) {
        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        try {
            json.put("studentId", currentStudentId);
            json.put("distance", distance);
            JSONArray pointsArray = new JSONArray();
            for (LatLng point : trackPoints) {
                JSONObject p = new JSONObject();
                p.put("lat", point.latitude);
                p.put("lng", point.longitude);
                pointsArray.put(p);
            }
            json.put("points", pointsArray.toString());
        } catch (Exception e) { e.printStackTrace(); }
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json.toString());
        Request request = new Request.Builder().url("http://10.0.2.2:8080/api/track/upload").post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "轨迹已保存", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void initSearchLogic() {
        EditText etSearch = findViewById(R.id.et_search);
        searchDropdown = new android.widget.ListPopupWindow(this);
        searchDropdown.setAnchorView(findViewById(R.id.search_card));
        searchDropdown.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_search_popup));
        searchDropdown.setModal(false);
        searchDropdown.setVerticalOffset(10);
        searchDropdown.setOnItemClickListener((parent, view, position, id) -> {
            searchDropdown.dismiss();
            try {
                JSONObject selectedData = currentSearchResults.get(position);
                etSearch.setText(selectedData.getString("name"));
                etSearch.clearFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                handleSearchResult(selectedData);
            } catch (Exception e) { e.printStackTrace(); }
        });
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String keyword = etSearch.getText().toString().trim();
            if (!keyword.isEmpty()) {
                executeSearch(keyword);
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
            }
            return true;
        });
    }

    private void executeSearch(String keyword) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("http://10.0.2.2:8080/api/markers/search?keyword=" + keyword).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "搜索失败", Toast.LENGTH_SHORT).show());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                final String res = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonObject = new JSONObject(res);
                        if (jsonObject.getInt("code") == 200) {
                            JSONArray data = jsonObject.getJSONArray("data");
                            if (data.length() == 0) {
                                Toast.makeText(MainActivity.this, "未找到相关地点", Toast.LENGTH_SHORT).show();
                                if (searchDropdown.isShowing()) searchDropdown.dismiss();
                                return;
                            }
                            currentSearchResults.clear();
                            List<String> displayList = new ArrayList<>();
                            for (int i = 0; i < data.length(); i++) {
                                currentSearchResults.add(data.getJSONObject(i));
                                displayList.add(data.getJSONObject(i).getString("name") + " (" + data.getJSONObject(i).optString("category", "未分类") + ")");
                            }
                            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(
                                    MainActivity.this,
                                    R.layout.item_search_result,
                                    android.R.id.text1,
                                    displayList
                            ) {
                                @androidx.annotation.NonNull
                                @Override
                                public android.view.View getView(int position, android.view.View convertView, @androidx.annotation.NonNull android.view.ViewGroup parent) {
                                    android.widget.TextView tv = (android.widget.TextView) super.getView(position, convertView, parent);
                                    tv.setText("🔍 " + displayList.get(position));
                                    tv.setTextColor(android.graphics.Color.parseColor("#172033"));
                                    tv.setTextSize(15f);
                                    return tv;
                                }

                            };
                            searchDropdown.setAdapter(adapter);
                            searchDropdown.show();
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                });
            }
        });
    }

    private void handleSearchResult(JSONObject data) throws JSONException {
        LatLng target = new LatLng(data.getDouble("latitude"), data.getDouble("longitude"));
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 18f));
        Marker marker = aMap.addMarker(new MarkerOptions().position(target).title(data.getString("name")).snippet(data.getString("snippet")));
        showBottomSheetDialog(marker);
    }

    private void showBottomSheetDialog(Marker marker) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet, null);
        dialog.setContentView(view);
        View parent = (View) view.getParent();
        com.google.android.material.bottomsheet.BottomSheetBehavior behavior =
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(parent);
        ((TextView) view.findViewById(R.id.tv_sheet_title)).setText(marker.getTitle());
        ((TextView) view.findViewById(R.id.tv_sheet_snippet)).setText(marker.getSnippet());
        TextView tvDistance = view.findViewById(R.id.tv_sheet_distance);
        if (aMap != null && aMap.getMyLocation() != null) {
            float dist = calculateDistanceMeters(
                    aMap.getMyLocation().getLatitude(), aMap.getMyLocation().getLongitude(),
                    marker.getPosition().latitude, marker.getPosition().longitude);
            tvDistance.setText(dist < 1000 ? "直线距离 " + (int)dist + " 米" : String.format("直线距离 %.1f 公里", dist / 1000f));
        }
        view.findViewById(R.id.btn_sheet_nav).setOnClickListener(v -> {
            dialog.dismiss();
            requestCustomRouteTo(marker.getPosition().latitude, marker.getPosition().longitude);
        });
        dialog.show();
    }

    private void fetchMarkersFromServer() {
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).build();
        client.newCall(new Request.Builder().url("http://10.0.2.2:8080/api/markers").get().build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                final String res = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONArray data = new JSONObject(res).optJSONArray("data");
                        if (data != null) {
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject item = data.getJSONObject(i);
                                aMap.addMarker(new MarkerOptions().position(new LatLng(item.optDouble("latitude"), item.optDouble("longitude"))).title(item.optString("name")).snippet(item.optString("snippet")));
                            }
                        }
                    } catch (Exception e) {}
                });
            }
        });
    }

    private void replayTrack(String pointsJson) {
        try {
            JSONArray array = new JSONArray(pointsJson);
            List<LatLng> points = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) points.add(new LatLng(array.getJSONObject(i).getDouble("lat"), array.getJSONObject(i).getDouble("lng")));
            switchPage(0);
            if (currentRouteLine != null) currentRouteLine.remove();
            currentRouteLine = aMap.addPolyline(
                    new PolylineOptions()
                            .addAll(points)
                            .width(18)
                            .color(Color.parseColor("#3A86FF"))
                            .lineJoinType(PolylineOptions.LineJoinType.LineJoinRound)
            );
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng p : points) builder.include(p);
            aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
        } catch (Exception e) {}
    }

    @Override
    public void onLocationChanged(AMapLocation loc) {
        if (loc != null && loc.getErrorCode() == 0 && isTracking) {
            if (loc.getAccuracy() > 30.0f) return;
            LatLng rawLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());


            LatLng snappedLatLng = rawLatLng;
            if (!currentRoutePoints.isEmpty()) {
                snappedLatLng = MapMatchUtil.snapToRoute(rawLatLng, currentRoutePoints);
            }

            if (!trackPoints.isEmpty()) {
                float dist = calculateDistanceMeters(trackPoints.get(trackPoints.size() - 1), snappedLatLng);
                if (dist > 2.0f && loc.getSpeed() <= 10.0f) {
                    totalDistance += dist;
                    runOnUiThread(() -> tvTrackDistance.setText(String.format("%.2f km", totalDistance / 1000f)));
                    trackPoints.add(snappedLatLng);
                    runOnUiThread(() -> {
                        if (trackPolyline == null) {
                            trackPolyline = aMap.addPolyline(new PolylineOptions()
                                    .addAll(trackPoints).width(18).color(Color.parseColor("#30D5C8"))
                            );
                        } else {
                            trackPolyline.setPoints(trackPoints);
                        }
                    });
                }
            } else {
                trackPoints.add(snappedLatLng);
            }
        }
    }

    private void refreshSystemConfig() {
        new OkHttpClient().newCall(new Request.Builder().url("http://10.0.2.2:8080/api/config/all").get().build()).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject configs = new JSONObject(response.body().string()).getJSONObject("data");
                    runOnUiThread(() -> {
                        TextView tvIntro = findViewById(R.id.tv_home_intro);
                        if (tvIntro != null) tvIntro.setText(configs.optString("school_intro", "暂无简介"));
                        TextView tvName = findViewById(R.id.tv_home_school_name);
                        if(tvName != null) tvName.setText(configs.optString("school_name", "XX大学"));
                        TextView tvSlogan = findViewById(R.id.tv_home_slogan);
                        if(tvSlogan != null) tvSlogan.setText(configs.optString("school_slogan", "博学笃志"));
                        TextView tvTags = findViewById(R.id.tv_home_tags);
                        if(tvTags != null) tvTags.setText(configs.optString("school_tags", "公办·本科"));
                        TextView marquee = findViewById(R.id.tv_home_marquee);
                        if(marquee != null) {
                            marquee.setText(configs.optString("school_marquee", "欢迎使用校园导览 APP"));
                            marquee.setSelected(true);
                        }
                        try {
                            JSONObject details = new JSONObject(configs.optString("school_details"));
                            TextView tvYear = findViewById(R.id.tv_detail_year);
                            if(tvYear != null) tvYear.setText("建校: " + details.optString("found_year"));
                            TextView tvType = findViewById(R.id.tv_detail_type);
                            if(tvType != null) tvType.setText("类型: " + details.optString("type"));
                            TextView tvCat = findViewById(R.id.tv_detail_cat);
                            if(tvCat != null) tvCat.setText("分类: " + details.optString("category"));
                            TextView tvLoc = findViewById(R.id.tv_detail_loc);
                            if(tvLoc != null) tvLoc.setText("位置: " + details.optString("location"));
                        } catch (Exception e) {}

                        String logoUrl = configs.optString("school_logo");
                        android.widget.ImageView ivLogo = findViewById(R.id.iv_school_logo);
                        if (ivLogo != null && !logoUrl.isEmpty()) {
                            com.bumptech.glide.Glide.with(MainActivity.this).load(logoUrl).into(ivLogo);
                        }

                        try {
                            JSONArray banners = new JSONArray(configs.optString("banner_images"));
                            List<String> imageUrls = new ArrayList<>();
                            for (int i = 0; i < banners.length(); i++) imageUrls.add(banners.getString(i));
                            androidx.viewpager2.widget.ViewPager2 viewPager = findViewById(R.id.vp_home_banner);
                            if (viewPager != null) {
                                viewPager.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
                                    @androidx.annotation.NonNull @Override
                                    public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
                                        android.widget.ImageView iv = new android.widget.ImageView(MainActivity.this);
                                        iv.setLayoutParams(new android.view.ViewGroup.LayoutParams(-1, -1));
                                        iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                                        return new androidx.recyclerview.widget.RecyclerView.ViewHolder(iv) {};
                                    }
                                    @Override public void onBindViewHolder(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
                                        com.bumptech.glide.Glide.with(MainActivity.this)
                                                .load(imageUrls.get(position))
                                                .into((android.widget.ImageView)holder.itemView);
                                    }
                                    @Override public int getItemCount() { return imageUrls.size(); }
                                });
                                if (bannerRunnable != null) {
                                    bannerHandler.removeCallbacks(bannerRunnable);
                                }
                                bannerRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        if (viewPager.getAdapter() != null && viewPager.getAdapter().getItemCount() > 0) {
                                            int currentItem = viewPager.getCurrentItem();
                                            int nextItem = (currentItem + 1) % viewPager.getAdapter().getItemCount();
                                            viewPager.setCurrentItem(nextItem, true);
                                        }
                                        bannerHandler.postDelayed(this, 3000);
                                    }
                                };
                                bannerHandler.postDelayed(bannerRunnable, 3000);
                                viewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                                    @Override
                                    public void onPageScrollStateChanged(int state) {
                                        if (state == androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING) {
                                            bannerHandler.removeCallbacks(bannerRunnable);
                                        } else if (state == androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE) {
                                            bannerHandler.removeCallbacks(bannerRunnable);
                                            bannerHandler.postDelayed(bannerRunnable, 3000);
                                        }
                                    }
                                });
                            }
                        } catch (Exception e) {}

                        try {
                            JSONObject weather = new JSONObject(configs.optString("weather_json", "{}"));
                            TextView tvTemp = findViewById(R.id.tv_home_temp);
                            if (tvTemp != null) tvTemp.setText(weather.optString("temp", "25°C"));
                            TextView tvWeatherDesc = findViewById(R.id.tv_home_weather_desc);
                            if (tvWeatherDesc != null) tvWeatherDesc.setText(weather.optString("desc", "晴") + " | " + weather.optString("wind", "微风"));
                        } catch (Exception e) {}
                    });
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call call, IOException e) {}
        });
    }

    @Override protected void onResume() { super.onResume(); mMapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mMapView.onPause(); }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        if (bannerHandler != null && bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }
    }
    @Override protected void onSaveInstanceState(Bundle outState) { super.onSaveInstanceState(outState); mMapView.onSaveInstanceState(outState); }

    private void requestCustomRouteTo(double targetLat, double targetLng) {
        if (aMap == null || aMap.getMyLocation() == null) {
            Toast.makeText(this, "正在定位中，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        Location loc = aMap.getMyLocation();
        String url = "http://10.0.2.2:8080/api/nav/routeByLocation"
                + "?startLat=" + loc.getLatitude()
                + "&startLng=" + loc.getLongitude()
                + "&endLat=" + targetLat
                + "&endLng=" + targetLng
                + "&algorithm=astar";
        new OkHttpClient().newCall(new Request.Builder().url(url).get().build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                final String resStr = response.body().string();
                runOnUiThread(() -> drawCustomRouteFromResponse(resStr));
            }
        });
    }

    private void drawCustomRouteFromResponse(String resStr) {
        try {
            JSONObject jsonObject = new JSONObject(resStr);
            if (jsonObject.optInt("code") != 200) {
                Toast.makeText(this, jsonObject.optString("msg", "路径规划失败"), Toast.LENGTH_SHORT).show();
                return;
            }
            JSONArray pathData = jsonObject.getJSONArray("data");
            if (pathData.length() == 0) {
                Toast.makeText(this, "未能找到有效路径", Toast.LENGTH_SHORT).show();
                return;
            }
            List<LatLng> points = new ArrayList<>();
            for (int i = 0; i < pathData.length(); i++) {
                JSONObject point = pathData.getJSONObject(i);
                points.add(new LatLng(point.getDouble("lat"), point.getDouble("lng")));
            }
            currentRoutePoints.clear();
            currentRoutePoints.addAll(points);
            if (currentRouteLine != null) currentRouteLine.remove();
            currentRouteLine = aMap.addPolyline(new PolylineOptions()
                    .addAll(points)
                    .width(18)
                    .color(Color.parseColor("#3A86FF"))
                    .lineJoinType(PolylineOptions.LineJoinType.LineJoinRound));

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng p : points) builder.include(p);
            aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
            double distance = jsonObject.optDouble("distance", estimatePathDistance(points));
            int minutes = Math.max(1, (int) Math.ceil(distance / 80.0));
            TextView tvRouteSummary = findViewById(R.id.tv_route_summary);
            View routePanel = findViewById(R.id.route_result_panel);
            View btnRouteClose = findViewById(R.id.btn_route_close);

            if (tvRouteSummary != null) {
                tvRouteSummary.setText("路线全长 " + formatDistance(distance) + "，步行约 " + minutes + " 分钟");
            }

            if (routePanel != null) {
                routePanel.setVisibility(View.VISIBLE);
            }

            if (btnRouteClose != null) {
                btnRouteClose.setOnClickListener(v -> {
                    if (currentRouteLine != null) {
                        currentRouteLine.remove();
                        currentRouteLine = null;
                    }
                    currentRoutePoints.clear();
                    routePanel.setVisibility(View.GONE);
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "路径解析异常", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatDistance(double distance) {
        return distance >= 1000 ? String.format("%.1f 公里", distance / 1000.0) : ((int) distance) + " 米";
    }

    private double estimatePathDistance(List<LatLng> points) {
        double sum = 0;
        for (int i = 1; i < points.size(); i++) {
            sum += calculateDistanceMeters(points.get(i - 1), points.get(i));
        }
        return sum;
    }

    private float calculateDistanceMeters(LatLng a, LatLng b) {
        return calculateDistanceMeters(a.latitude, a.longitude, b.latitude, b.longitude);
    }

    private float calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return (float) (2 * earthRadius * Math.asin(Math.sqrt(h)));
    }
}