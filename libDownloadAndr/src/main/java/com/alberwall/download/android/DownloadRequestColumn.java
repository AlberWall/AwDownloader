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

package com.alberwall.download.android;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

@SuppressWarnings("WeakerAccess")
@Entity
public class DownloadRequestColumn {
    @Id
    public String id;
    /**
     * <pre>
     * [
     *  {
     *     "url":"https://xxx",
     *     "connectTimeout":8000,
     *     "readTimeout":8000,
     *     "params": {
     *         "head1":["value1", "value2"],
     *      },
     *     "type": 1 // PROTOCOL_HTTP/PROTOCOL_FTP
     *   }
     * ]
     * </pre>
     */
    public String sources;

    public String downloadPath;
    public String downloadFileName;
    public int priority;
    public int maxRetryTimes;
    public String tmpFile;
    public long blockSize = -1;
    public long totalLength = -1;


    @Generated(hash = 1921426843)
    public DownloadRequestColumn(String id, String sources, String downloadPath,
            String downloadFileName, int priority, int maxRetryTimes,
            String tmpFile, long blockSize, long totalLength) {
        this.id = id;
        this.sources = sources;
        this.downloadPath = downloadPath;
        this.downloadFileName = downloadFileName;
        this.priority = priority;
        this.maxRetryTimes = maxRetryTimes;
        this.tmpFile = tmpFile;
        this.blockSize = blockSize;
        this.totalLength = totalLength;
    }

    @Generated(hash = 1601519265)
    public DownloadRequestColumn() {
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getMaxRetryTimes() {
        return this.maxRetryTimes;
    }

    public void setMaxRetryTimes(int maxRetryTimes) {
        this.maxRetryTimes = maxRetryTimes;
    }

    public String getTmpFile() {
        return this.tmpFile;
    }

    public void setTmpFile(String tmpFile) {
        this.tmpFile = tmpFile;
    }

    public long getBlockSize() {
        return this.blockSize;
    }

    public void setBlockSize(long blockSize) {
        this.blockSize = blockSize;
    }

    public long getTotalLength() {
        return this.totalLength;
    }

    public void setTotalLength(long totalLength) {
        this.totalLength = totalLength;
    }

    public String getDownloadPath() {
        return this.downloadPath;
    }

    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
    }

    public String getDownloadFileName() {
        return this.downloadFileName;
    }

    public void setDownloadFileName(String downloadFileName) {
        this.downloadFileName = downloadFileName;
    }

    public String getSources() {
        return this.sources;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }
}
