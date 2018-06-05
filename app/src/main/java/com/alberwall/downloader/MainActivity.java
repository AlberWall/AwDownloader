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

package com.alberwall.downloader;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.alberwall.download.android.DbDownload;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private String TAG = "AwDownloader";

    private AwDownloader mDownloader;
    private DbDownload mdb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDownloader();
    }

    @Override
    public void onDetachedFromWindow() {
        mdb.cleanCache();
    }

    private void initDownloader() {
        if (null == mdb) {
            mdb = new DbDownload(this);
        }

        if (null == mDownloader) {
            mDownloader = new AwDownloader.Builder().setDownloadDatabase(mdb).build();
            mDownloader.init();
        }
    }

    boolean[] flag = new boolean[]{false, false, false, false, false, false};
    static String[] urls = new String[]{
            "http://www.periodicooficial.oaxaca.gob.mx/files/2011/05/EXT02-2011-05-19.pdf",
            "https://powercam.wondershare.cc/files/vlogit/mall/sticker/download/ios/Explosions.zip",
            "https://powercam.wondershare.cc/files/vlogit/mall/filter/download/ios/BW.zip",
            "http://p.gdown.baidu.com/733e9b836a7a2d200ce06d4a03e479dcda55adb18ec3252658c7bf16039f618fd146f4eb9d6f27529488f08edc991735413da3e487a326256adcab24e67a265b421312a9225d450885468cb685eec8e52a17bea85ba32c2623b66b0ac2e2920ab9ada19bf28e02a45b166975a4c3d60a3369965d0ecf55c90339a66d12fa72ac69e3156d7abb0a071538926fee90cd00679905df08bc6a7852529194dea17b5b7d4388f1bf35b988cbe8e6cc31046bd0f5a4c7612064e7de8168576bde25cb164823a96abac18fbc8d1cd691de40e917333fd08f750967104903d830bb40cb66dee0d029b983fc01",
            "https://codeload.github.com/MindorksOpenSource/PRDownloader/zip/master",
            "http://www.lingala.net/zip4j/downloads/zip4j_src_1.3.2.zip",
    };

    static String[] name = new String[]{
            "java_concurrency.pdf",
            "Explosions_ios.zip",
            "BW_ios.zip",
            "baiduyind.apk",
            "prdownload.zip",
            "zip4j_src_1.3.2.zip"
    };

    public static final String DIR =
            new File(Environment.getExternalStorageDirectory(), "test").getAbsolutePath();

    public void onClickView(View view) {
        switch (view.getId()) {
            case R.id.button1:
                actionDownload(0);
                break;
            case R.id.button2:
                actionDownload(1);
                break;
            case R.id.button3:
                actionDownload(2);
                break;
            case R.id.button4:
                actionDownload(3);
                break;
            case R.id.button5:
                actionDownload(4);
                break;
            case R.id.button6:
                actionDownload(5);
                break;
            case R.id.read:
                readDb();
                break;
            case R.id.shutdown:
                shutdown();
                break;
        }
    }

    private void actionDownload(final int i) {
        initDownloader();
        if (flag[i]) {
            mDownloader.cancelDownload(urls[i]);
        } else {
            DownloadRequest req = mDownloader.newRequestBuilder()
                    .dirPath(DIR)
                    .fileName(name[i])
                    .http(urls[i])
                    .setBlockDownloadListener(new OnDownloadBlockListener() {
                        @Override
                        public void onProgress(FileBlock block, long downloadedBytes, long totalBytes) {
                            Log.v(TAG, "download progress: " + downloadedBytes + " of " + totalBytes + " with " + block);
                        }

                        @Override
                        public void onCompleted(FileBlock block) {
                            Log.i(TAG, "completed with " + block);
                        }

                        @Override
                        public void onFailed(FileBlock block, Exception error) {
                            Log.e(TAG, "err: " + error + ", cause: " + error.getCause() + " with " + block);
                            Log.e(TAG, "error: " + Log.getStackTraceString(error) + " with " + block);
                        }
                    })
                    .setDownloadListener(new OnDownloadListener() {
                        @Override
                        public void onProgress(long downloadedBytes, long totalBytes) {
                            Log.d(TAG, "download: " + downloadedBytes + " of " + totalBytes + "\n" + urls[i]);
                        }

                        @Override
                        public void onCompleted() {
                            flag[i] = false;
                            Log.i(TAG, "completed" + "\n" + urls[i]);
                        }

                        @Override
                        public void onFailed(Exception error) {
                            Log.e(TAG, "err: " + error + ", cause: " + error.getCause() + "\n" + urls[i]);
                            flag[i] = false;
                            Log.e(TAG, "error: " + Log.getStackTraceString(error) + "\n" + urls[i]);

                        }
                    })
                    .build();

            mDownloader.startDownload(req);
        }

        flag[i] = !flag[i];
    }

    private void readDb() {
        List<DownloadRequest> reqs = mDownloader.database().getAllRequests();
        Log.d(TAG, "reqs: " + reqs);
    }

    private void shutdown() {
        if (null == mDownloader) return;
        mDownloader.shutdown();
        mDownloader = null;
    }
}
