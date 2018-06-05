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

import android.support.annotation.NonNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Utils {

    public static void renameFile(@NonNull File src, @NonNull File dst) {
        //noinspection ResultOfMethodCallIgnored
        src.renameTo(dst);
    }

    public static void deleteFile(@NonNull File f) {
        if (f.exists() && f.isFile()) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }

    public static void closeSafely(Closeable c) {
        if (null != c) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }

    private void closeSafely(RandomAccessFile outputFile) {
        if (null != outputFile) {
            try {
                outputFile.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}