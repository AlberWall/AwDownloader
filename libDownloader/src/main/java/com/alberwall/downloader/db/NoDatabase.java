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

package com.alberwall.downloader.db;

import com.alberwall.downloader.AwDownloader;
import com.alberwall.downloader.DownloadRequest;
import com.alberwall.downloader.FileBlockRequest;

import java.util.ArrayList;
import java.util.List;

public class NoDatabase implements DownloadDatabase {
    @Override
    public void init(AwDownloader downloader) {
        // No-op
    }

    @Override
    public List<DownloadRequest> getAllRequests() {
        return new ArrayList<>(0);
    }

    @Override
    public void addOrUpdateDownloadRequest(DownloadRequest req) {
        // No-op
    }

    @Override
    public DownloadRequest queryIfContainsIn(List<String> url) {
        return null;
    }

    @Override
    public void removeDownloadRequest(long id) {
        // No-op
    }

    @Override
    public void removeBlockInfo(long id) {
        // No-op
    }

    @Override
    public void updateFileBlockProgress(FileBlockRequest req, long downloadedBytes, long currentTime) {
        // No-op
    }
}
