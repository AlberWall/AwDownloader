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

package com.alberwall.downloader.ftp;

import com.alberwall.downloader.ProtocolParameters;

//TODO:ftp support
public class FtpParameters extends ProtocolParameters {
    public final String user;
    public final String pwd;

    private FtpParameters(Builder builder) {
        super(builder.connectTimeout, builder.readTimeout);
        user = builder.user;
        pwd = builder.pwd;
    }

    public static class Builder {
        private int connectTimeout = -1;
        private int readTimeout = -1;
        public String user;
        public String pwd;

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

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder password(String pwd) {
            this.pwd = pwd;
            return this;
        }

        public FtpParameters build() {
            return new FtpParameters(this);
        }
    }
}
