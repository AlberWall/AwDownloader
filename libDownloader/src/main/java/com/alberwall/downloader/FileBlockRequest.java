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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class FileBlockRequest implements Comparable<FileBlockRequest> {
    public final DownloadRequest rawRequest;
    public final FileBlock fileBlock;

    private volatile long downloadedBytes = 0;
    private final transient AtomicInteger retryTimes = new AtomicInteger(0);
    private InternalListener listener;
    private transient TaskRunnable runner;

    private volatile boolean markDelivered = false;

    public FileBlockRequest(@NonNull DownloadRequest req, @NonNull FileBlock block) {
        this(req, block, 0);
    }

    public FileBlockRequest(@NonNull DownloadRequest req, @NonNull FileBlock block, long curDownloadedBytes) {
        rawRequest = req;
        fileBlock = block;
        downloadedBytes = curDownloadedBytes;

        listener = req.getInnerListener();
    }

    void setListener(InternalListener l) {
        listener = l;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    void setDownloadedBytes(long bytes) {
        if (bytes != downloadedBytes) {
            downloadedBytes = bytes;

            listener.onProgress(this, downloadedBytes, rawRequest.totalLength);
        }
    }

    boolean isFileBlockCompleted() {
        return fileBlock.blockSize() <= downloadedBytes;
    }

    void finished() {
        if (markDelivered) return;

        markDelivered = true;
        listener.onCompleted(this);
    }

    void failed(Exception e) {
        if (markDelivered) return;

        markDelivered = !canRetry(e);
        listener.onFailed(this, e);
    }

    void cancel(boolean notify) {
        if (!notify) {
            markDelivered = true;
        }

        TaskRunnable r = runner;
        if (null != r) {
            r.cancel();
        }
    }

    TaskRunnable getRunner() {
        return runner;
    }

    void setRunner(TaskRunnable r) {
        runner = r;
    }

    boolean canRetry(Exception e) {
        return e instanceof IOException &&
                retryTimes.get() < rawRequest.maxRetryTimes;
    }

    void incrementRetry() {
        retryTimes.incrementAndGet();
    }

    @Override
    public int compareTo(@NonNull FileBlockRequest o) {
        int cmp = rawRequest.compareTo(o.rawRequest);
        if (0 == cmp) {
            return o.fileBlock.blockIndex - fileBlock.blockIndex;
        }
        return cmp;
    }

    @Override
    public String toString() {
        return "{" +
                "bk=" + fileBlock +
                ", dl=" + downloadedBytes +
                '}';
    }

    interface InternalListener {
        void onProgress(FileBlockRequest blockRequest, long downloadedBytes, long totalBytes);

        void onCompleted(FileBlockRequest blockRequest);

        void onFailed(FileBlockRequest blockRequest, Exception error);
    }
}
