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

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.alberwall.downloader.ftp.FtpParameters;
import com.alberwall.downloader.ftp.FtpSource;
import com.alberwall.downloader.http.HttpParameters;
import com.alberwall.downloader.http.HttpSource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class DownloadRequest implements Comparable<DownloadRequest> {

    public static final int STATE_NEW = 0;
    public static final int STATE_PREPARE_QUEUE = 1;
    public static final int STATE_PREPARE = 2;
    public static final int STATE_DOWNLOAD_QUEUE = 3;
    public static final int STATE_DOWNLOADING = 4;
    public static final int STATE_COMPLETED = 5;
    public static final int STATE_CANCELLED = 6;
    public static final int STATE_FAILED = 7;

    @IntDef({
            STATE_NEW,
            STATE_PREPARE_QUEUE,
            STATE_PREPARE,
            STATE_DOWNLOAD_QUEUE,
            STATE_DOWNLOADING,
            STATE_COMPLETED,
            STATE_CANCELLED,
            STATE_FAILED,
    })
    public @interface DownloadState {
    }

    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_MEDIUM = 2;
    public static final int PRIORITY_HIGH = 3;

    @IntDef({
            PRIORITY_LOW,
            PRIORITY_MEDIUM,
            PRIORITY_HIGH,
    })
    public @interface Priority {
    }

    public long id;

    //TODO:support multiple urls, and source include different Protocol(ftp and http, and so on)
    public final Source[] source;
    public final File downloadFile;
    @Priority
    public final int priority;
    public Object tag;

    public final int maxRetryTimes;

    public transient OnDownloadListener downloadListener;
    public transient OnDownloadBlockListener blockDownloadListener;

    public final List<FileBlockRequest> blockRequests;

    public File tmpFile;

    public volatile long blockSize;
    public volatile long totalLength;

    private volatile transient Integer cachedHash;

    private transient FileBlockRequest.InternalListener innerListener;
    private transient TaskRunnable runner;

    @DownloadState
    private volatile int state;
    private volatile boolean markDelivered = false;
    private volatile boolean cleanIfCancelled = false;
    private final AwDownloader downloader;

    private DownloadRequest(Builder builder) {
        state = STATE_NEW;
        downloader = builder.downloader;

        source = new Source[builder.source.size()];
        builder.source.toArray(source);
        downloadFile = new File(builder.dirPath, builder.fileName);
        priority = builder.priority;
        tag = builder.tag;

        maxRetryTimes = builder.maxRetryTimes;
        blockRequests = new ArrayList<>();

        downloadListener = builder.downloadListener;
        blockDownloadListener = builder.blockDownloadListener;

        id = System.currentTimeMillis();
        tmpFile = new File(builder.dirPath, builder.fileName + AwDownloader.TEMP_FILE_EXTENSION);
        innerListener = new FileBlockRequest.InternalListener() {

            @Override
            public void onProgress(FileBlockRequest req, long downloadedBytes, long totalBytes) {
                if (null == downloader) return;
                long totalDownloaded = computeTotalDownloadedBytes();
                downloader.dispatcher().deliverProgress(DownloadRequest.this, totalDownloaded);
                downloader.dispatcher().deliverProgress(DownloadRequest.this, req.fileBlock, downloadedBytes);
            }

            @Override
            public void onCompleted(FileBlockRequest req) {
                if (null == downloader) return;
                downloader.dispatcher().deliverCompleted(DownloadRequest.this, req.fileBlock);

                if (isAllBlockCompleted()) {
                    downloader.dispatcher().finished(DownloadRequest.this);
                }
            }

            @Override
            public void onFailed(FileBlockRequest req, Exception error) {
                if (req.canRetry(error)) {
                    req.incrementRetry();
                    downloader.dispatcher().enqueue(req);
                } else {
                    for (FileBlockRequest blockReq : blockRequests) {
                        if (!blockReq.equals(req)) {
                            blockReq.cancel(false);
                        }
                    }

                    downloader.dispatcher().deliverFailed(req.rawRequest, req.fileBlock, error);
                    downloader.dispatcher().failed(DownloadRequest.this, error);
                }

            }
        };
    }

    public DownloadRequest(AwDownloader d,
                           long reqId,
                           @Priority int pri,
                           String downloadPath, String downloadFileName,
                           long blockSz, long totalLen, int maxRetry,
                           Source... theSource) {
        state = STATE_NEW;

        downloader = d;
        id = reqId;
        priority = pri;
        blockRequests = new ArrayList<>();
        source = theSource;
        downloadFile = new File(downloadPath, downloadFileName);
        tmpFile = new File(downloadPath, downloadFileName + AwDownloader.TEMP_FILE_EXTENSION);

        maxRetryTimes = maxRetry;

        blockSize = blockSz;
        totalLength = totalLen;
    }

    public void pause() {
        //TODO:

    }

    public void enqueue() {
        downloader.dispatcher().enqueue(this);
    }

    public synchronized void cancel(boolean cleanTmpFile) {
        cleanIfCancelled = cleanTmpFile;
        setState(STATE_CANCELLED);
        if (null != runner) {
            runner.cancel();
        }

        for (FileBlockRequest req : blockRequests) {
            req.cancel(true);
        }
    }

    public boolean isCancelled() {
        return STATE_CANCELLED == state;
    }

    public synchronized void setFileBlockRequests(List<FileBlockRequest> fileBlockRequests) {
        blockRequests.clear();
        blockRequests.addAll(fileBlockRequests);
    }

    public TaskRunnable getRunner() {
        return runner;
    }

    void setRunner(TaskRunnable r) {
        runner = r;
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(Object t) {
        tag = t;
    }

    synchronized void setState(@DownloadState int s) {
        if (state != s) {
            state = s;
        }
    }

    void setCompleted() {
        synchronized (this) {
            if (state == STATE_COMPLETED) return;
            setState(STATE_COMPLETED);
        }

        downloader.database().removeDownloadRequest(id);
        downloader.database().removeBlockInfo(id);
        Utils.renameFile(tmpFile, downloadFile);

        if (markDelivered) return;
        markDelivered = true;
        downloader.dispatcher().deliverCompleted(this);
    }

    void setFailed(Exception e) {
        synchronized (this) {
            if (state == STATE_FAILED) return;
            setState(STATE_FAILED);
        }

        if (cleanIfCancelled) {
            downloader.database().removeDownloadRequest(id);
            downloader.database().removeBlockInfo(id);
            Utils.deleteFile(tmpFile);
        }

        if (markDelivered) return;
        markDelivered = true;
        downloader.dispatcher().deliverFailed(this, e);
    }

    public int getState() {
        return state;
    }

    FileBlockRequest.InternalListener getInnerListener() {
        return innerListener;
    }

    public Source getSourceBy(String url) {
        if (null == url) return null;
        for (Source s : source) {
            if (url.equals(s.url)) {
                return s;
            }
        }
        return null;
    }

    @NonNull
    public String[] urls() {
        String[] urls = new String[source.length];
        for (int i = 0; i < source.length; i++) {
            urls[i] = source[i].url;
        }
        return urls;
    }

    @Override
    public int compareTo(@NonNull DownloadRequest o) {
        if (priority != o.priority) {
            return priority - o.priority;
        }

        return id - o.id > 0 ? 1 : -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadRequest that = (DownloadRequest) o;

        String[] url1 = urls();

        Arrays.sort(url1);
        String[] url2 = that.urls();
        Arrays.sort(url2);
        return Arrays.equals(url1, url2);
    }

    @Override
    public int hashCode() {
        if (null == cachedHash) {
            cachedHash = hashUrls(urls());
        }
        return cachedHash;
    }

    private static int hashUrls(String[] urlArray) {
        Arrays.sort(urlArray);

        StringBuilder urls = new StringBuilder();
        for (String url : urlArray) {
            urls.append(url);
        }
        return urls.toString().hashCode();
    }

    private long computeTotalDownloadedBytes() {
        long downloadedBytes = 0;
        for (FileBlockRequest req : blockRequests) {
            downloadedBytes += req.getDownloadedBytes();
        }
        return downloadedBytes;
    }

    private boolean isAllBlockCompleted() {
        for (FileBlockRequest req : blockRequests) {
            if (!req.isFileBlockCompleted()) {
                return false;
            }
        }
        return true;
    }

    public static class Builder {
        private final AwDownloader downloader;
        private List<Source> source = new ArrayList<>();

        private String dirPath;
        private String fileName;
        private Object tag;
        @Priority
        private int priority = PRIORITY_MEDIUM;

        private int maxRetryTimes = -1;

        private OnDownloadListener downloadListener;
        private OnDownloadBlockListener blockDownloadListener;

        Builder(AwDownloader d) {
            downloader = d;
        }

        public Builder dirPath(String dirPath) {
            if (null == dirPath || dirPath.isEmpty())
                throw new IllegalArgumentException("Null download path");
            this.dirPath = dirPath;
            return this;
        }

        public Builder fileName(String fileName) {
            if (null == fileName || fileName.isEmpty())
                throw new IllegalArgumentException("Null download fileName");
            this.fileName = fileName;
            return this;
        }

        public Builder http(String url) {
            if (null == url || url.isEmpty())
                throw new IllegalArgumentException("Null download url");
            HttpSource s = new HttpSource(
                    url,
                    new HttpParameters.Builder()
                            .connectTimeout(downloader.connectTimeout)
                            .readTimeout(downloader.readTimeout)
                            .build());
            source.add(s);
            return this;
        }

        public Builder http(String url, int connectTimeout, int readTimeout) {
            if (null == url || url.isEmpty())
                throw new IllegalArgumentException("Null download url");
            HttpSource s = new HttpSource(
                    url,
                    new HttpParameters.Builder()
                            .connectTimeout(connectTimeout)
                            .readTimeout(readTimeout)
                            .build());
            source.add(s);
            return this;
        }

        public Builder ftp(String url, String user, String pwd) {
            if (null == url || url.isEmpty())
                throw new IllegalArgumentException("Null download url");
            FtpSource s = new FtpSource(
                    url,
                    new FtpParameters.Builder()
                            .connectTimeout(downloader.connectTimeout)
                            .readTimeout(downloader.readTimeout)
                            .user(user)
                            .password(pwd)
                            .build());
            source.add(s);
            return this;
        }

        public Builder ftp(String url, String user, String pwd, int connectTimeout, int readTimeout) {
            if (null == url || url.isEmpty())
                throw new IllegalArgumentException("Null download url");
            FtpSource s = new FtpSource(
                    url,
                    new FtpParameters.Builder()
                            .connectTimeout(connectTimeout)
                            .readTimeout(readTimeout)
                            .user(user)
                            .password(pwd)
                            .build());
            source.add(s);
            return this;
        }

        public Builder addSource(Source s) {
            source.add(s);
            return this;
        }

//        public Builder source(Source... source) {
//            if (null == source)
//                throw new IllegalArgumentException("Null download file source");
//            if (source.length < 1)
//                throw new UnsupportedOperationException("Multiple url is unsupported NOW, but will be soon!");
//            return this;
//        }

        public Builder setTag(Object tag) {
            this.tag = tag;
            return this;
        }

        public Builder maxRetryTimes(int times) {
            if (times < 0)
                throw new IllegalArgumentException("Invalid retry times: " + times);
            maxRetryTimes = times;
            return this;
        }

        public Builder setPriority(@Priority int pri) {
            priority = pri;
            return this;
        }

        public Builder setDownloadListener(OnDownloadListener l) {
            downloadListener = l;
            return this;
        }

        public Builder setBlockDownloadListener(OnDownloadBlockListener l) {
            blockDownloadListener = l;
            return this;
        }

        public DownloadRequest build() {
            if (maxRetryTimes < 0) {
                maxRetryTimes = downloader.maxRetryTimes;
            }

            return new DownloadRequest(this);
        }
    }
}
