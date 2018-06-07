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

package com.alberwall.downloader.http;

import com.alberwall.downloader.DownloadClient;
import com.alberwall.downloader.Source;
import com.alberwall.downloader.exceptions.CancelException;
import com.alberwall.downloader.exceptions.RequestException;
import com.alberwall.downloader.exceptions.ResponseException;
import com.alberwall.downloader.exceptions.TryTooMuchException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.alberwall.downloader.http.HTTP.HEADER_CONTENT_LEN;
import static com.alberwall.downloader.http.HTTP.HEADER_LOCATION;
import static com.alberwall.downloader.http.HTTP.MAX_REDIRECTION;
import static com.alberwall.downloader.http.HTTP.isSuccessful;

public class HttpRealClient implements DownloadClient<HttpParameters> {
    private final Source mSource;
    private URLConnection mConnection;
    private InputStream mInputStream;
    private volatile boolean mCancelled = false;
    private boolean mResumeSupported;

    public HttpRealClient(Source mSource) {
        this.mSource = mSource;
    }

    @Override
    public long queryFileLength(Source<HttpParameters> source)
            throws IOException, CancelException, RequestException, ResponseException {
        realConnect(source, HTTP.HEAD, 0, -1);

        checkCancel("begin redirect url" + " - " + mSource.url);
        redirectIfAny(source, HTTP.HEAD, 0, -1);

        checkResponse();
        return getFileLength();
    }

    @Override
    public void connect(Source<HttpParameters> source, long start, long end)
            throws IOException, CancelException, RequestException {
        realConnect(source, null, start, end);

        checkCancel("begin redirect url" + " - " + mSource.url);
        redirectIfAny(source, null, start, end);

        checkResponse();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        checkConnection();

        mInputStream = mConnection.getInputStream();
        return mInputStream;
    }

    @Override
    public boolean isResumeSupported() {
        return mResumeSupported;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public void close() {
        // should not close input stream here.(okio not thread-safe)
        mCancelled = true;
    }

    private void realConnect(Source<HttpParameters> source, String method, long start, long end)
            throws IOException, RequestException {
        if (start < 0 || (end > 0 && end < start)) {
            throw new RequestException("Invalid start and end: " + start + ", " + end + " - " + mSource.url);
        }

        HttpParameters params = source.parameters;
        String url = (null == params.redirectUrl) ? source.url : params.redirectUrl;

        mConnection = new URL(url).openConnection();
        mConnection.setConnectTimeout(params.connectTimeout);
        mConnection.setReadTimeout(params.readTimeout);

        addHeaders(params.headers);

        if (null != method) {
            ((HttpURLConnection) mConnection).setRequestMethod(method);
        }

        String range;
        if (end <= 0) {
            range = String.format(Locale.US, "bytes=%d-", start);
        } else {
            range = String.format(Locale.US, "bytes=%d-%d", start, end);
        }

        mConnection.setRequestProperty(HTTP.HEADER_RANGE, range);
        mConnection.connect();
    }

    private void redirectIfAny(Source<HttpParameters> source, String method, long start, long end)
            throws IOException, CancelException, RequestException {
        checkConnection();
        HttpParameters params = source.parameters;
        int redirectTimes = 0;
        int code = getResponseCode();
        String location = mConnection.getHeaderField(HEADER_LOCATION);
        while (HTTP.isRedirect(code)) {
            if (null == location) {
                throw new IOException("Redirection Location is null" + " - " + mSource.url);
            }
            if (redirectTimes >= MAX_REDIRECTION) {
                throw new TryTooMuchException(MAX_REDIRECTION, "Max redirection done" + " - " + mSource.url);
            }

            params.redirectUrl = location;
            checkCancel("redirect " + location + " - " + mSource.url);
            realConnect(source, method, start, end);
            checkConnection();

            code = getResponseCode();
            location = mConnection.getHeaderField(HEADER_LOCATION);
            redirectTimes++;
        }
    }

    private void checkResponse() throws IOException {
        checkConnection();
        int code = getResponseCode();
        if (!isSuccessful(code))
            throw new IOException("res code: " + code + " - " + mSource.url);

        mResumeSupported = code == HTTP.HTTP_PARTIAL;
    }

    private int getResponseCode() throws IOException {
        return ((HttpURLConnection) mConnection).getResponseCode();
    }

    private long getFileLength() throws ResponseException {
        try {
            return Long.valueOf(mConnection.getHeaderField(HEADER_CONTENT_LEN));
        } catch (NumberFormatException | NullPointerException e) {
            throw new ResponseException("Unsupported Http Header: " + HEADER_CONTENT_LEN + " - " + mSource.url, e);
        }
    }

    private void checkCancel(String action) throws CancelException {
        if (mCancelled)
            throw new CancelException("Cancelled when " + action + " - " + mSource.url);
    }

    private void checkConnection() throws IOException {
        if (null == mConnection)
            throw new IOException("Connection is null" + " - " + mSource.url);
        if (!(mConnection instanceof HttpURLConnection))
            throw new IOException("Only supported HttpURLConnection" + " - " + mSource.url);
    }

    private void addHeaders(Map<String, List<String>> headers) {
        if (null == headers) return;

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            List<String> list = entry.getValue();
            if (null == list) continue;

            for (String value : list) {
                mConnection.addRequestProperty(name, value);
            }
        }
    }
}
