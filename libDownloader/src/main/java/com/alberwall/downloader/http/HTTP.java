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

@SuppressWarnings("WeakerAccess")
public class HTTP {
    public static final String GET = "GET";
    public static final String HEAD = "HEAD";

    public static final int MAX_REDIRECTION = 3;

    public static final String HEADER_LOCATION = "Location";
    public static final String HEADER_CONTENT_LEN = "Content-Length";
    public static final String HEADER_RANGE = "Range";
    public static final String HEADER_ETAG = "ETag";
    public static final String HEADER_USER_AGENT = "User-Agent";

    public static final String USER_AGENT = "User-Agent";

    public static final int HTTP_OK = 200; // Multiple Choice
    public static final int HTTP_PARTIAL = 206; // support break-point resume

    /// HTTP RESPONSE CODE 3XX
    public static final int HTTP_MULTIPLE_CHOICE = 300; // Multiple Choice
    public static final int HTTP_MOVED_PERM = 301; // Moved Permanently
    public static final int HTTP_MOVED_TEMP = 302; // Moved Temporarily
    public static final int HTTP_SEE_OTHER = 303; // See Other
    public static final int HTTP_TEMPORARY_REDIRECT = 307; // Temporary Redirect
    public static final int HTTP_PERMANENT_REDIRECT = 308; // Permanent Redirect


    /// HTTP RESPONSE CODE 4XX
    public static final int HTTP_RANGE_NOT_SATISFIABLE = 416;

    public static boolean isRedirect(int code) {
        return code == HTTP_MULTIPLE_CHOICE ||
                code == HTTP_MOVED_PERM ||
                code == HTTP_MOVED_TEMP ||
                code == HTTP_SEE_OTHER ||
                code == HTTP_TEMPORARY_REDIRECT ||
                code == HTTP_PERMANENT_REDIRECT;
    }

    public static boolean isSuccessful(int responseCode) {
        return responseCode >= HTTP_OK && responseCode < HTTP_MULTIPLE_CHOICE;
    }
}
