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

public class FileBlock {
    public final Source source;

    public final int blockIndex;
    public final long start; // start pos is inclusive.
    public final long end; // end pos is inclusive.

    public FileBlock(Source source, int blockIndex, long start, long end) {
        this.source = source;
        this.start = start;
        this.end = end;
        this.blockIndex = blockIndex;
    }

    final long blockSize() {
        return end - start + 1;
    }

    @Override
    public String toString() {
        return "FileBlock{" +
                "source=" + source +
                ", blockIndex=" + blockIndex +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
