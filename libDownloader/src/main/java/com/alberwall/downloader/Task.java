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

import com.alberwall.downloader.exceptions.CancelException;
import com.alberwall.downloader.exceptions.RequestException;
import com.alberwall.downloader.exceptions.ResponseException;

import java.io.IOException;

public abstract class Task<Request extends Comparable<Request>> {

    protected final Request mReq;
    protected final AwDownloader mDownloader;

    public Task(Request req, AwDownloader dl) {
        mReq = req;
        mDownloader = dl;
    }

    public abstract void execute()
            throws RequestException, ResponseException, IOException, CancelException;


    public abstract void cancel();

    public abstract boolean isCancelled();

    public abstract void failed(Exception e);

    public abstract String getTaskName();

    public Request getRequest() {
        return mReq;
    }
}
