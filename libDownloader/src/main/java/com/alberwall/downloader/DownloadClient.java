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

/**
 * Download Client Interface
 * @param <P>
 */
public interface DownloadClient<P extends ProtocolParameters> {

    /**
     * query length of file in remote server
     * @param source file in remote server
     * @return the file length
     */
    long queryFileLength(Source<P> source) throws IOException, CancelException, RequestException, ResponseException;

    /**
     * connect server
     * @param source server info with connect parameters
     * @param start start position of file, inclusive
     * @param end end position of file, inclusive
     */
    void connect(Source<P> source, long start, long end) throws IOException, CancelException, RequestException;

    /**
     * return the input stream for downloading file
     */
    InputStream getInputStream() throws IOException;

    /**
     * indicate the server whether support breakpoint resume or not.
     * @return
     * @throws IOException
     */
    boolean isResumeSupported() throws IOException;


    /**
     * Close the client.
     */
    void close();
}
