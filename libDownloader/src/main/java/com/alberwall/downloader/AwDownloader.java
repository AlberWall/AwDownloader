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

import android.support.annotation.NonNull;
import android.support.annotation.UiThread;

import com.alberwall.downloader.db.DownloadDatabase;
import com.alberwall.downloader.db.NoDatabase;

@SuppressWarnings("WeakerAccess")
public class AwDownloader {
    private final Dispatcher mDispatcher;
    private final DownloadDatabase mdb;

    public final int connectTimeout;
    public final int readTimeout;
    public final int maxRetryTimes;
    public final int bufferSize;

    public final int threadCount;
    public final int nThreadsOfRequest;
    public final int maxTaskCount;
    public final int minFileBlockSize;
    public final int maxFileBlockCnt;

    public final boolean enableLogFile;
    public final int logLevel;
    public final String logPath;

    public static AwDownloader createDefault() {
        return new Builder().build();
    }

    private AwDownloader(Builder builder) {
        enableLogFile = builder.enableLogFile;
        logLevel = builder.logLevel;
        logPath = builder.logPath;

        connectTimeout = builder.connectTimeout;
        readTimeout = builder.readTimeout;
        maxRetryTimes = builder.maxRetryTimes;
        bufferSize = builder.bufferSize;
        minFileBlockSize = builder.minFileBlockSize;
        maxFileBlockCnt = builder.maxFileBlockCnt;

        threadCount = builder.threadCount;
        nThreadsOfRequest = builder.nThreadsOfRequest;
        maxTaskCount = builder.maxTaskCount;

        mDispatcher = new Dispatcher(this);
        mdb = builder.db;
    }

    @UiThread
    public void init() {
        mdb.init(this);
    }

    @UiThread
    public DownloadRequest.Builder newRequestBuilder() {
        return new DownloadRequest.Builder(this);
    }

    @UiThread
    public void startDownload(DownloadRequest req) {
        dispatcher().enqueue(req);
    }

    @UiThread
    public void cancelDownload(@NonNull DownloadRequest req) {
        req.cancel(false);
    }

    @UiThread
    public void cancelDownload(@NonNull String url) {
        dispatcher().cancel(url);
    }

    @UiThread
    public void cleanDownload(@NonNull String url) {
        dispatcher().clean(url);
    }

    @UiThread
    public void cleanDownload(@NonNull DownloadRequest req) {
        req.cancel(true);
    }

    @UiThread
    public void cancleAll() {
        dispatcher().cancelAll();
    }

    @UiThread
    public void shutdown() {
        dispatcher().cancelAll();
        dispatcher().shutdown();
    }

    Dispatcher dispatcher() {
        return mDispatcher;
    }

    public DownloadDatabase database() {
        return mdb;
    }


    public static class Builder {
        public DownloadDatabase db;

        private int connectTimeout = TIMEOUT_CONNECT;
        private int readTimeout = TIMEOUT_READ;
        private int maxRetryTimes = MAX_RETRY_TIMES;
        private int bufferSize = BUFFER_SIZE;

        private int threadCount = MAX_THREAD_COUNT;
        private int nThreadsOfRequest = MAX_THREAD_COUNT_PER_REQUEST;
        private int maxTaskCount = MAX_TASK_COUNT;

        private boolean enableLogFile = false;
        private int logLevel = LOG_LEVEL_INFO;
        private String logPath = null;
        private int minFileBlockSize = MIN_FILE_BLOCK_SIZE;
        private int maxFileBlockCnt = MAX_FILE_BLOCK_CNT;

        public Builder() {
        }

        public AwDownloader build() {
            if (null == db) {
                db = new NoDatabase();
            }

            if (nThreadsOfRequest > threadCount)
                throw new IllegalArgumentException("Thread count is less than nThreadsOfRequest");

            return new AwDownloader(this);
        }

        public Builder setDownloadDatabase(DownloadDatabase db) {
            this.db = db;
            return this;
        }

        public Builder connectTimeout(int timeout) {
            if (timeout < 0)
                throw new IllegalArgumentException("Invalid connect timeout: " + timeout);
            connectTimeout = timeout;
            return this;
        }

        public Builder readTimeout(int timeout) {
            if (timeout < 0)
                throw new IllegalArgumentException("Invalid read timeout: " + timeout);
            readTimeout = timeout;
            return this;
        }

        public Builder maxRetryTimes(int times) {
            if (times < 0)
                throw new IllegalArgumentException("Invalid retry times: " + times);
            maxRetryTimes = times;
            return this;
        }

        public Builder bufferSize(int buffer) {
            if (buffer < 0)
                throw new IllegalArgumentException("Invalid buffer: " + buffer);
            bufferSize = buffer;
            return this;
        }

        public Builder threadCount(int cnt) {
            if (cnt < 1)
                throw new IllegalArgumentException("Invalid thread count: " + cnt);
            threadCount = cnt;
            return this;
        }

        public void nThreadsOfRequest(int cnt) {
            if (cnt <= 0)
                throw new IllegalArgumentException("Invalid max thread count per request: " + cnt);
            nThreadsOfRequest = cnt;
        }

        public Builder maxTaskCount(int cnt) {
            if (cnt < 0)
                throw new IllegalArgumentException("Invalid max task count: " + cnt);
            maxTaskCount = cnt;
            return this;
        }

        public Builder minFileBlockSize(int blockSize) {
            minFileBlockSize = blockSize;
            return this;
        }

        public void maxFileBlockCnt(int maxFileBlockCnt) {
            this.maxFileBlockCnt = maxFileBlockCnt;
        }

        public Builder enableLogFile(String path) {
            enableLogFile = true;
            logPath = path;
            return this;
        }

        public Builder logLevel(int level) {
            logLevel = level;
            return this;
        }
    }

    public static final int TIMEOUT_CONNECT = 8_000; // ms
    public static final int TIMEOUT_READ = 8_000;
    public static final int MAX_RETRY_TIMES = 2;
    public static final int BUFFER_SIZE = 8192;
    public static final int MIN_FILE_BLOCK_SIZE = 16 * 1024 * 1024;
    public static final int MAX_FILE_BLOCK_CNT = 16;

    public static final int MAX_THREAD_COUNT = 4;
    public static final int MAX_THREAD_COUNT_PER_REQUEST = 2;
    public static final int MAX_TASK_COUNT = 10000;

    public static final int LOG_LEVEL_DEBUG = 1;
    public static final int LOG_LEVEL_INFO = 2;

    public static final String TEMP_FILE_EXTENSION = ".tmp";
}
