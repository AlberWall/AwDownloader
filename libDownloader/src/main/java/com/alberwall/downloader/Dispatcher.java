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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class Dispatcher {
    private final AwDownloader mDownloader;
    @NonNull
    private final Deque<DownloadRequest> mRequests = new ArrayDeque<>();
    @NonNull
    private final ExecutorService mPrepareExecutorService;
    @NonNull
    private final ExecutorService mDownloadExecutorService;

    Dispatcher(AwDownloader downloader) {
        mDownloader = downloader;

        mPrepareExecutorService = new ThreadPoolExecutor(
                1,
                1,
                0, TimeUnit.SECONDS,
                new PriorityBlockingQueue<Runnable>());
        mDownloadExecutorService = new ThreadPoolExecutor(
                2,
                downloader.threadCount,
                10, TimeUnit.SECONDS,
                new PriorityBlockingQueue<Runnable>());
    }

    @UiThread
    synchronized void enqueue(DownloadRequest req) {
        if (!mRequests.contains(req)) {
            mRequests.add(req);
            TaskRunnable task = new TaskRunnable(new PrepareTask(req, mDownloader));
            req.setRunner(task);
            req.setState(DownloadRequest.STATE_PREPARE_QUEUE);
            prepareExecutorService().execute(task);
        }
    }

    synchronized void cancel(@NonNull String url) {
        DownloadRequest req = findRequestBy(url);
        if (null != req) {
            req.cancel(false);
        }
    }

    synchronized void cancelAll() {
        for (DownloadRequest req : mRequests) {
            req.cancel(false);
        }
    }

    synchronized void clean(String url) {
        DownloadRequest req = findRequestBy(url);
        if (null != req) {
            req.cancel(true);
        }
    }

    synchronized void shutdown() {
        prepareExecutorService().shutdown();
        downloadExecutorService().shutdown();
    }

    void enqueue(FileBlockRequest request) {
        TaskRunnable task = new TaskRunnable(new FileBlockDownloadTask(request, mDownloader));
        request.setRunner(task);
        downloadExecutorService().execute(task);
    }

    void finished(@NonNull DownloadRequest mReq) {
        mReq.setCompleted();
        synchronized (this) {
            mRequests.remove(mReq);
        }
    }

    void failed(@NonNull DownloadRequest mReq, Exception e) {
        mReq.setFailed(e);
        synchronized (this) {
            mRequests.remove(mReq);
        }
    }

    void deliverProgress(DownloadRequest req, long downloadedBytes) {
        OnDownloadListener l = req.downloadListener;
        if (null != l) {
            l.onProgress(downloadedBytes, req.totalLength);
        }
    }

    void deliverProgress(DownloadRequest req, FileBlock block, long downloadedBytes) {
        OnDownloadBlockListener l = req.blockDownloadListener;
        if (null != l) {
            l.onProgress(block, downloadedBytes, req.totalLength);
        }
    }

    void deliverCompleted(DownloadRequest req) {
        OnDownloadListener l = req.downloadListener;
        if (null != l) {
            l.onCompleted();
        }
    }

    void deliverCompleted(DownloadRequest req, FileBlock block) {
        OnDownloadBlockListener l = req.blockDownloadListener;
        if (null != l) {
            l.onCompleted(block);
        }
    }

    void deliverFailed(DownloadRequest req, Exception error) {
        OnDownloadListener l = req.downloadListener;
        if (null != l) {
            l.onFailed(error);
        }
    }

    void deliverFailed(DownloadRequest req, FileBlock fileBlock, Exception error) {
        OnDownloadBlockListener l = req.blockDownloadListener;
        if (null != l) {
            l.onFailed(fileBlock, error);
        }
    }

    private ExecutorService prepareExecutorService() {
        return mPrepareExecutorService;
    }

    private ExecutorService downloadExecutorService() {
        return mDownloadExecutorService;
    }

    private DownloadRequest findRequestBy(String url) {
        if (null == url) return null;
        for (DownloadRequest req : mRequests) {
            if (null != req.getSourceBy(url)) {
                return req;
            }
        }
        return null;
    }
}
