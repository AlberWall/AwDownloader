/*
 * Copyright (c) 2018. Alberwall Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alberwall.assist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alberwall.downloader.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by guoyong on 2018/8/7.
 */
public class TestActivity extends Activity {

    private TextView mTvTitle;
    private RecyclerView mRvAppList;
    private RecyclerView.Adapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test_app_list);
        initView();
    }

    private void initView() {
        mTvTitle = findViewById(R.id.tv_title);
        mRvAppList = findViewById(R.id.rv_app_list);

        LinearLayoutManager lm = new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false);
        mRvAppList.setLayoutManager(new GridLayoutManager(this, 3));

        mAdapter = new AppListAdapter(this, getAllAppInfo());
        mRvAppList.setAdapter(mAdapter);
    }

    private List<AppInfo> getAllAppInfo() {
        List<AppInfo> allAppInfo = new ArrayList<>();
        List<PackageInfo> packages = getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packages.size(); i++) {
            PackageInfo packageInfo = packages.get(i);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) { //非系统应用
                AppInfo appInfo = new AppInfo(
                        packageInfo.applicationInfo.loadLabel(getPackageManager()).toString(),
                        packageInfo.packageName,
                        packageInfo.versionName,
                        packageInfo.versionCode,
                        packageInfo.applicationInfo.loadIcon(getPackageManager()));
                System.out.println(appInfo.toString());
                allAppInfo.add(appInfo);
            } else { // 系统应用

            }
        }

        return allAppInfo;
    }

    private static class AppInfo {
        public final String appName;
        public final String packageName;
        public final String versionName;
        public final int versionCode;
        public final Drawable drawable;

        private AppInfo(String appName, String packageName, String versionName, int versionCode, Drawable drawable) {
            this.appName = appName;
            this.packageName = packageName;
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.drawable = drawable;
        }
    }

    private class AppListAdapter extends RecyclerView.Adapter<AppViewHolder> {
        private final Context mContext;
        private final LayoutInflater mInflater;

        private final ArrayList<AppInfo> mAppInfo;

        AppListAdapter(Context context, List<AppInfo> allAppInfo) {
            mContext = context;
            mInflater = LayoutInflater.from(context);

            if (null != allAppInfo) {
                mAppInfo = new ArrayList<>(allAppInfo);
            } else {
                mAppInfo = new ArrayList<>();
            }
        }

        void updateAll(List<AppInfo> list) {
            mAppInfo.clear();
            mAppInfo.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.item_test_app_list, parent, false);
            return new AppViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
            holder.updateViews(mAppInfo.get(position));
        }

        @Override
        public int getItemCount() {
            return mAppInfo.size();
        }
    }

    private static class AppViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTvAppName;
        private final ViewGroup mContainer;

        private AppViewHolder(View itemView) {
            super(itemView);

            mTvAppName = itemView.findViewById(R.id.tv_app_name);
            mContainer = itemView.findViewById(R.id.container_app_item);
        }

        public void updateViews(final AppInfo appInfo) {
            mTvAppName.setText(appInfo.appName);
            mContainer.setOnClickListener((v) -> {
                startAppSettingActivity(mContainer.getContext(), appInfo.packageName);
            });
        }
    }

    public static void startAppSettingActivity(Context context, String packageName) {
        Intent mIntent = new Intent();
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 9) {
            mIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            mIntent.setData(Uri.fromParts("package", packageName, null));
        } else if (Build.VERSION.SDK_INT <= 8) {
            mIntent.setAction(Intent.ACTION_VIEW);
            mIntent.setClassName("com.android.settings", "com.android.setting.InstalledAppDetails");
            mIntent.putExtra("com.android.settings.ApplicationPkgName", packageName);
        }
        context.startActivity(mIntent);
    }

}
