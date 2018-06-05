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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class PrepareTask extends Task<DownloadRequest> {
    private volatile DownloadClient mClient;

    PrepareTask(DownloadRequest req, AwDownloader dl) {
        super(req, dl);
    }

    @Override
    public void execute()
            throws RequestException, ResponseException, IOException, CancelException {
        mReq.setState(DownloadRequest.STATE_PREPARE);
        if (mReq.downloadFile.exists()) { // has already been downloaded
            throw new RequestException("Download file exists: " + mReq.downloadFile);
        }

        Source source = mReq.source[0];
        checkCancelled("query file length for " + source);

        if (null == mClient) {
            mClient = DownloadClientFactory.createClient(source);
        }
        //noinspection unchecked
        long fileLength = mClient.queryFileLength(source);
        if (fileLength <= 0) {
            throw new ResponseException("Invalid file length(" + fileLength + ") - " + source.url);
        }

        DownloadRequest existedReq = mDownloader.database().queryIfContainsIn(Arrays.asList(mReq.urls()));
        if (null != existedReq && null != existedReq.tmpFile && existedReq.tmpFile.exists()) {
            if (fileLength != existedReq.totalLength || (!mClient.isResumeSupported())) {
                removeRecodedAndFile(existedReq);
            } else {
                mReq.blockSize = existedReq.blockSize;
                mReq.id = existedReq.id;
                mReq.tmpFile = existedReq.tmpFile;

                List<FileBlockRequest> blockRequests = new ArrayList<>(existedReq.blockRequests.size());
                for (FileBlockRequest br : existedReq.blockRequests) {
                    FileBlockRequest bkReq = new FileBlockRequest(mReq, br.fileBlock, br.getDownloadedBytes());
                    blockRequests.add(bkReq);
                }
                mReq.setFileBlockRequests(blockRequests);
            }
        }
        mReq.totalLength = fileLength;

        // create tmpfile。
        if (!mReq.tmpFile.exists()) {
            if (mReq.tmpFile.getParentFile() != null && !mReq.tmpFile.getParentFile().exists()) {
                //noinspection ResultOfMethodCallIgnored
                mReq.tmpFile.getParentFile().mkdirs();
            } else {
                //noinspection ResultOfMethodCallIgnored
                mReq.tmpFile.createNewFile();
            }
        }

        if (mReq.blockRequests.isEmpty()) {
            List<FileBlockRequest> blockRequests = createBlockRequests(
                    determineBlockCount(mClient.isResumeSupported())
            );

            mReq.setFileBlockRequests(blockRequests);
        }
        mDownloader.database().addOrUpdateDownloadRequest(mReq);

        mReq.setState(DownloadRequest.STATE_DOWNLOAD_QUEUE);
        for (FileBlockRequest blockReq : mReq.blockRequests) {
            checkCancelled("put block request: " + blockReq.fileBlock.source);
            if (blockReq.isFileBlockCompleted()) continue;

            try {
                mDownloader.dispatcher().enqueue(blockReq);
            } catch (Exception e) {
                if (!isCancelled()) {
                    throw new RequestException("Error when enqueue " + blockReq, e);
                }
            }
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
        return mReq.isCancelled();
    }

    @Override
    public void failed(Exception e) {
        mDownloader.dispatcher().failed(mReq, e);
    }

    @Override
    public String getTaskName() {
        return "dl-" + mReq.downloadFile.getName();
    }

    private void checkCancelled(String message) throws CancelException {
        if (mReq.isCancelled()) {
            throw new CancelException("Cancelled when " + message);
        }
    }

    private int determineBlockCount(boolean resumeSupported) {
        if (!resumeSupported) return 1;

        long minBlockSize = mDownloader.minFileBlockSize;
        if (mReq.totalLength < minBlockSize) return 1;

        return mDownloader.nThreadsOfRequest;
    }

    private List<FileBlockRequest> createBlockRequests(int size) {
        ArrayList<FileBlockRequest> blockRequests = new ArrayList<>(size);
        mReq.blockSize = mReq.totalLength / size;

        long offset = 0;
        long end;
        for (int i = 0; i < size; i++) {
            if (size - 1 == i) {
                end = mReq.totalLength - 1;
            } else {
                end = offset + mReq.blockSize - 1;
            }

            FileBlock block = new FileBlock(mReq.source[0], i, offset, end);
            FileBlockRequest blockRequest = new FileBlockRequest(mReq, block);
            blockRequest.setListener(mReq.getInnerListener());
            blockRequests.add(blockRequest);

            offset += mReq.blockSize;
        }

        return blockRequests;
    }

    private void removeRecodedAndFile(DownloadRequest existedReq) {
        Utils.deleteFile(existedReq.tmpFile);
        mDownloader.database().removeBlockInfo(existedReq.id);
        mDownloader.database().removeDownloadRequest(existedReq.id);
    }
}
