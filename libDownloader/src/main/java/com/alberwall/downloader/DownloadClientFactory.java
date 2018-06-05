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

import com.alberwall.downloader.http.HttpRealClient;

import java.net.MalformedURLException;
import java.net.URL;

public class DownloadClientFactory {

    public static DownloadClient createClient(Source source) throws MalformedURLException {
        URL url = new URL(source.url);
        switch (url.getProtocol()) {
            case "https":
            case "HTTPS":
            case "http":
            case "HTTP":
                return new HttpRealClient(source);
            default:
                throw new MalformedURLException("Unsupported protocol(" + url.getProtocol() + ") NOW");
        }
    }
}
