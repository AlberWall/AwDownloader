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
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@SuppressWarnings("WeakerAccess")
@Entity
public class FileBlockRequestColumn {

    public static final int PROTOCOL_HTTP = 1;
    public static final int PROTOCOL_FTP = 2;

    @Id
    private String blockId;
    public long rawReqId;

    /**
     * <pre>
     * HTTP/FTP:
     * {
     *   "url":"https://xxx"，
     *   "connectTimeout":8000,
     *   "readTimeout":8000,
     *   "params": {
     *       "head1":["value1", "value2"],
     *   }
     *   "type": PROTOCOL_HTTP/PROTOCOL_FTP
     * }
     * </pre>
     */
    public String sources;
    public int blockIndex;
    public long start;
    public long end;
    public long downloadedBytes;
    public long updatedTime;

    @Generated(hash = 1857668206)
    public FileBlockRequestColumn(String blockId, long rawReqId, String sources,
            int blockIndex, long start, long end, long downloadedBytes,
            long updatedTime) {
        this.blockId = blockId;
        this.rawReqId = rawReqId;
        this.sources = sources;
        this.blockIndex = blockIndex;
        this.start = start;
        this.end = end;
        this.downloadedBytes = downloadedBytes;
        this.updatedTime = updatedTime;
    }

    @Generated(hash = 205743976)
    public FileBlockRequestColumn() {
    }

    public String getBlockId() {
        return this.blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public long getRawReqId() {
        return this.rawReqId;
    }

    public void setRawReqId(long rawReqId) {
        this.rawReqId = rawReqId;
    }

    public int getBlockIndex() {
        return this.blockIndex;
    }

    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }

    public long getStart() {
        return this.start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return this.end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getDownloadedBytes() {
        return this.downloadedBytes;
    }

    public void setDownloadedBytes(long downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
    }

    public String getSources() {
        return this.sources;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

    public long getUpdatedTime() {
        return this.updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }
}
