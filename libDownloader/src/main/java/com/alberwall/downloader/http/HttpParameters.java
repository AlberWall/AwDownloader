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

import com.alberwall.downloader.ProtocolParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpParameters extends ProtocolParameters {
    public final Map<String, List<String>> headers;

    public volatile String redirectUrl;

    private HttpParameters(Builder builder) {
        super(builder.connectTimeout, builder.readTimeout);

        headers = builder.headers;
    }

    public HttpParameters(int connectTimeout, int readTimeout, Map<String, List<String>> headers) {
        super(connectTimeout, readTimeout);

        this.headers = headers;
    }

    public static class Builder {
        private int connectTimeout = -1;
        private int readTimeout = -1;
        private Map<String, List<String>> headers;

        public Builder readTimeout(int timeout) {
            if (timeout < 0)
                throw new IllegalArgumentException("Invalid read timeout: " + timeout);
            readTimeout = timeout;
            return this;
        }

        public Builder connectTimeout(int timeout) {
            if (timeout < 0)
                throw new IllegalArgumentException("Invalid connect timeout: " + timeout);
            connectTimeout = timeout;
            return this;
        }

        public Builder addHeader(String name, String value) {
            if (headers == null) {
                headers = new HashMap<>();
            }
            List<String> list = headers.get(name);
            if (list == null) {
                list = new ArrayList<>();
                headers.put(name, list);
            }
            if (!list.contains(value)) {
                list.add(value);
            }
            return this;
        }

        public HttpParameters build() {
            return new HttpParameters(this);
        }
    }
}
