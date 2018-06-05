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
import com.alberwall.downloader.exceptions.ChecksumException;
import com.alberwall.downloader.exceptions.RequestException;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

class FileBlockDownloadTask extends Task<FileBlockRequest> {
    private static final long TIME_GAP_FOR_SYNC = 2000;
    private static final long MIN_BYTES_FOR_SYNC = 65536;

    private DownloadClient mClient;

    public FileBlockDownloadTask(FileBlockRequest req, AwDownloader dl) {
        super(req, dl);
    }

    @Override
    public void execute() throws RequestException, IOException, CancelException {
        checkCancelled("beginning download " + mReq);

        mReq.rawRequest.setState(DownloadRequest.STATE_DOWNLOADING);

        FileBlock block = mReq.fileBlock;
        if (null == mClient) {
            mClient = DownloadClientFactory.createClient(block.source);
        }

        long startOffset = block.start + mReq.getDownloadedBytes();
        //noinspection unchecked
        mClient.connect(block.source, startOffset, block.end);

        long lastSyncTime = -1;
        long lastSyncBytes = 0;
        InputStream inputStream = null;
        BufferedOutputStream outputStream = null;
        RandomAccessFile outputFile = null;
        try {
            inputStream = mClient.getInputStream();
            outputFile = new RandomAccessFile(mReq.rawRequest.tmpFile, "rw");
            FileDescriptor fd = outputFile.getFD();
            outputStream = new BufferedOutputStream(new FileOutputStream(fd));

            if (startOffset > 0) {
                outputFile.seek(startOffset);
            }

            checkCancelled("start read io" + mReq);
            byte[] buffer = new byte[mDownloader.bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                if (mReq.rawRequest.isCancelled()) {
                    sync(outputStream, fd);
                    throw new CancelException("Cancelled when " + "write io" + mReq); // TODO:pause
                }

                outputStream.write(buffer, 0, len);

                mReq.setDownloadedBytes(mReq.getDownloadedBytes() + len);
                if (needSync(lastSyncBytes, lastSyncTime)) {
                    sync(outputStream, fd);

                    lastSyncBytes = mReq.getDownloadedBytes();
                    lastSyncTime = System.currentTimeMillis();
                }
            }

            sync(outputStream, fd);
            if (mReq.isFileBlockCompleted()) {
                mReq.finished();
            } else {
                mReq.failed(new ChecksumException("Unknown io err"));
            }
        } finally {
            Utils.closeSafely(outputFile);
            Utils.closeSafely(inputStream);
            Utils.closeSafely(outputStream);
        }
    }

    @Override
    public void cancel() {
        if (null != mClient) {
            mClient.close();
        }
    }

    @Override
    public boolean isCancelled() {
        return mReq.rawRequest.isCancelled();
    }

    @Override
    public void failed(Exception e) {
        mReq.failed(e);
    }

    @Override
    public String getTaskName() {
        return "dl-" + mReq.fileBlock.blockIndex + "-" + mReq.rawRequest.downloadFile.getName();
    }

    private boolean needSync(long syncBytes, long syncTime) {
        long currentBytes = mReq.getDownloadedBytes();
        long currentTime = System.currentTimeMillis();
        long bytesDelta = currentBytes - syncBytes;
        long timeDelta = currentTime - syncTime;
        return syncTime < 0 || (bytesDelta > MIN_BYTES_FOR_SYNC && timeDelta > TIME_GAP_FOR_SYNC);
    }

    private void sync(BufferedOutputStream outputStream, FileDescriptor fileDescriptor)
            throws IOException {
        outputStream.flush();
        fileDescriptor.sync();
        if (mClient.isResumeSupported()) {
            mDownloader.database().updateFileBlockProgress(
                    mReq, mReq.getDownloadedBytes(), System.currentTimeMillis());
        }
    }

    private void checkCancelled(String message) throws CancelException {
        if (mReq.rawRequest.isCancelled()) {
            throw new CancelException("Cancelled when " + message);
        }
    }

}
