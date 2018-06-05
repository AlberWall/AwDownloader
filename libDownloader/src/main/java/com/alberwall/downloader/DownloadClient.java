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
import java.io.InputStream;

public interface DownloadClient<P extends ProtocolParameters> {

    long queryFileLength(Source<P> source) throws IOException, CancelException, RequestException, ResponseException;

    void connect(Source<P> source, long start, long end) throws IOException, CancelException, RequestException;

    InputStream getInputStream() throws IOException;

    boolean isResumeSupported() throws IOException;

    void cancel();

    void close();
}
