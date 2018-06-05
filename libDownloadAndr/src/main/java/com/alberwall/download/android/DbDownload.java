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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.alberwall.downloader.AwDownloader;
import com.alberwall.downloader.DownloadRequest;
import com.alberwall.downloader.FileBlock;
import com.alberwall.downloader.FileBlockRequest;
import com.alberwall.downloader.Source;
import com.alberwall.downloader.db.DownloadDatabase;
import com.alberwall.downloader.http.HttpParameters;
import com.alberwall.downloader.http.HttpSource;
import com.alberwall.greendao.DaoMaster;
import com.alberwall.greendao.DaoSession;
import com.alberwall.greendao.DownloadRequestColumnDao;
import com.alberwall.greendao.FileBlockRequestColumnDao;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alberwall.download.android.FileBlockRequestColumn.PROTOCOL_HTTP;

public class DbDownload implements DownloadDatabase {
    private final Context mAppContext;
    private boolean mInitialized = false;
    private AwDownloader mDownloader;
    private DaoSession mDaoSession;

    private DownloadRequestColumnDao mRequestDao;
    private FileBlockRequestColumnDao mBlockRequestDao;

    private volatile FileBlockRequestColumn cachedFileBlockColumn;

    public DbDownload(Context context) {
        mAppContext = context.getApplicationContext();

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(mAppContext, "test22");
        DaoMaster master = new DaoMaster(helper.getWritableDb());
        mDaoSession = master.newSession();
    }

    @Override
    public void init(AwDownloader downloader) {
        if (mInitialized) return;

        mDownloader = downloader;
        mRequestDao = mDaoSession.getDownloadRequestColumnDao();
        mBlockRequestDao = mDaoSession.getFileBlockRequestColumnDao();
        mInitialized = true;
    }

    public void cleanCache() {
        cachedFileBlockColumn = null;
    }

    @Override
    public List<DownloadRequest> getAllRequests() {
        List<DownloadRequestColumn> allColumns = mRequestDao.queryBuilder().list();
        if (null == allColumns) {
            return new ArrayList<>();
        }
        return convertAllRequestColumns(allColumns);
    }

    private List<DownloadRequest> convertAllRequestColumns(List<DownloadRequestColumn> allColumns) {
        List<DownloadRequest> res = new ArrayList<>(allColumns.size());
        for (DownloadRequestColumn col : allColumns) {
            DownloadRequest req = convertRequestColumn(col);
            if (req == null) continue;
            res.add(req);
        }
        return res;
    }

    @Nullable
    private DownloadRequest convertRequestColumn(DownloadRequestColumn col) {
        List<SourceBean> sourceBeans = convertStringToSourceList(col.sources);
        if (null == sourceBeans) return null;
        List<Source> source = new ArrayList<>(sourceBeans.size());
        for (SourceBean b : sourceBeans) {
            if (PROTOCOL_HTTP == b.type) {
                Source s = new HttpSource(b.url, new HttpParameters(b.connectTimeout, b.readTimeout, b.params));
                source.add(s);
            } else {
                return null;
            }
        }

        Source[] theSource = new Source[source.size()];
        source.toArray(theSource);
        long reqId = Long.parseLong(col.id);
        DownloadRequest req = new DownloadRequest(
                mDownloader,
                reqId, col.priority,
                col.downloadPath, col.downloadFileName,
                col.blockSize, col.totalLength, col.maxRetryTimes,
                theSource);
        List<FileBlockRequest> blockColumns = queryBlockRequest(req, col);
        req.setFileBlockRequests(blockColumns);
        return req;
    }

    private List<FileBlockRequest> queryBlockRequest(DownloadRequest req, DownloadRequestColumn reqCol) {
        List<FileBlockRequestColumn> blockColumns = mBlockRequestDao.queryBuilder()
                .where(FileBlockRequestColumnDao.Properties.RawReqId.eq(req.id))
                .list();
        if (null == blockColumns) {
            return new ArrayList<>(0);
        }

        List<FileBlockRequest> blockRequests = new ArrayList<>(blockColumns.size());
        for (FileBlockRequestColumn col : blockColumns) {
            SourceBean b = convertStringToSource(col.sources);
            if (null != b && PROTOCOL_HTTP == b.type) {
                Source s = new HttpSource(b.url, new HttpParameters(b.connectTimeout, b.readTimeout, b.params));
                FileBlock block = new FileBlock(s, col.blockIndex, col.start, col.end);
                FileBlockRequest blockRequest = new FileBlockRequest(req, block, col.downloadedBytes);
                blockRequests.add(blockRequest);
            }
        }
        return blockRequests;
    }

    @Override
    public void addOrUpdateDownloadRequest(DownloadRequest req) {
        DownloadRequestColumn col = convertDownloadReq(req);
        mRequestDao.insertOrReplaceInTx(col);

        List<FileBlockRequestColumn> cols = convertFileBlockReq(req);
        mBlockRequestDao.insertOrReplaceInTx(cols);
    }

    private DownloadRequestColumn convertDownloadReq(DownloadRequest req) {
        List<SourceBean> sourceBeans = new ArrayList<>(req.source.length);
        for (Source s : req.source) {
            SourceBean bean = createSourceBean(s);
            sourceBeans.add(bean);
        }

        return new DownloadRequestColumn(
                String.valueOf(req.id),
                convertToString(sourceBeans),
                req.downloadFile.getParentFile().getAbsolutePath(),
                req.downloadFile.getName(),
                req.priority,
                req.maxRetryTimes,
                req.tmpFile.getName(),
                req.blockSize, req.totalLength
        );
    }

    @NonNull
    private SourceBean createSourceBean(Source s) {
        SourceBean bean = new SourceBean();
        if (s instanceof HttpSource) { // TODO: ftp
            bean.setUrl(s.url);
            bean.setType(PROTOCOL_HTTP);
            bean.setConnectTimeout(s.parameters.connectTimeout);
            bean.setReadTimeout(s.parameters.readTimeout);
            bean.setParams(((HttpSource) s).parameters.headers);
        }
        return bean;
    }

    private List<FileBlockRequestColumn> convertFileBlockReq(DownloadRequest req) {
        List<FileBlockRequestColumn> cols = new ArrayList<>(req.blockRequests.size());
        for (FileBlockRequest br : req.blockRequests) {
            FileBlockRequestColumn brc = convertFileBlockRequest(br);
            cols.add(brc);
        }
        return cols;
    }

    @NonNull
    private FileBlockRequestColumn convertFileBlockRequest(FileBlockRequest br) {
        FileBlock fb = br.fileBlock;
        return new FileBlockRequestColumn(
                br.rawRequest.id + "-" + fb.blockIndex, br.rawRequest.id,
                convertToString(createSourceBean(fb.source)),
                fb.blockIndex, fb.start, fb.end, br.getDownloadedBytes(), -1
        );
    }

    @Override
    public DownloadRequest queryIfContainsIn(List<String> url) {
        if (null == url || url.isEmpty()) return null;
//        List<DownloadRequestColumn> res = mRequestDao.queryBuilder()
//                .where(DownloadRequestColumnDao.Properties.Urls.like(url.get(0)))
//                .limit(1).list();
//
//        if (null == res || res.isEmpty()) return null;
//        return convertRequestColumn(res.get(0));

        List<DownloadRequest> requests = getAllRequests();
        for (DownloadRequest req : requests) {
            if (Arrays.asList(req.urls()).contains(url.get(0))) {
                return req;
            }
        }
        return null;
    }

    @Override
    public void removeDownloadRequest(long id) {
        String k = String.valueOf(id);
        mRequestDao.deleteByKeyInTx(k);
    }

    @Override
    public void removeBlockInfo(long id) {
        List<FileBlockRequestColumn> cols = mBlockRequestDao.queryBuilder()
                .where(FileBlockRequestColumnDao.Properties.RawReqId.eq(id))
                .build().list();
        mBlockRequestDao.deleteInTx(cols);

        FileBlockRequestColumn col = cachedFileBlockColumn;
        if (null != col && id == col.rawReqId) {
            cachedFileBlockColumn = null;
        }
    }

    @Override
    public void updateFileBlockProgress(FileBlockRequest req, long downloadedBytes, long currentTime) {
//        FileBlockRequestColumn col = cachedFileBlockColumn;
//        if (null != col && req.rawRequest.id == col.rawReqId &&
//                req.fileBlock.blockIndex == col.blockIndex) {
//
//            col.downloadedBytes = downloadedBytes;
//            mBlockRequestDao.insertOrReplaceInTx(col);
//            return;
//        }
//
//        List<FileBlockRequestColumn> cols = mBlockRequestDao.queryBuilder()
//                .where(FileBlockRequestColumnDao.Properties.RawReqId.eq(req.rawRequest.id),
//                        FileBlockRequestColumnDao.Properties.BlockIndex.eq(req.fileBlock.blockIndex))
//                .build().list();
//        if (null == cols || cols.isEmpty()) return;
//
//        col = cols.get(0);
//        col.downloadedBytes = downloadedBytes;
//        mBlockRequestDao.insertOrReplaceInTx(col);
//
//        cachedFileBlockColumn = col;

        FileBlockRequestColumn col = convertFileBlockRequest(req);
        col.updatedTime = currentTime;
        mBlockRequestDao.insertOrReplaceInTx(col);
    }

    private static List<SourceBean> convertStringToSourceList(String strSource) {
        try {
            return new Gson().fromJson(strSource, new TypeToken<List<SourceBean>>() {
            }.getType());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static SourceBean convertStringToSource(String strSource) {
        try {
            return new Gson().fromJson(strSource, SourceBean.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Map<String, List<String>> convertString(String jsonStr) {
        if (null == jsonStr || jsonStr.isEmpty()) {
            return new HashMap<>(0);
        }

        try {
            return new Gson().fromJson(jsonStr, new TypeToken<Map<String, List<String>>>() {
            }.getType());
        } catch (Exception e) {
            Log.e("DbDownload", "convertString err: " + e);
            return new HashMap<>(0);
        }
    }

    private static List<String> convertListString(String jsonStr) {
        if (null == jsonStr || jsonStr.isEmpty()) {
            return new ArrayList<>(0);
        }

        try {
            return new Gson().fromJson(jsonStr, new TypeToken<List<String>>() {
            }.getType());
        } catch (Exception e) {
            Log.e("DbDownload", "convertListString err: " + e);
            return new ArrayList<>(0);
        }
    }

    private static String convertToString(Object obj) {
        if (null == obj) {
            return null;
        }
        try {
            return new Gson().toJson(obj);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("DbDownload", "convertObj err: " + e);
            return null;
        }
    }

}
