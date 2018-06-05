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

import java.util.concurrent.atomic.AtomicInteger;

class TaskRunnable implements Runnable, Comparable<TaskRunnable> {
    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private Task realTask;

    TaskRunnable(Task task) {
        realTask = task;
    }

    public void run() {
        Task task = realTask;

        try {
            String no = (null == task) ? "dl-" : task.getTaskName();
            Thread.currentThread().setName(SEQ.incrementAndGet() + no);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (null != task) {
                try {
                    task.execute();
                } catch (Exception e) {
                    task.failed(e);
                }
            }
        } finally {
            realTask = null;
        }
    }

    void cancel() {
        final Task t = realTask;
        if (null != t) {
            t.cancel();
        }
    }

    Task getRealTask() {
        return realTask;
    }

    @Override
    public int compareTo(TaskRunnable t) {
        Task rt;
        if (null == t || null == (rt = t.getRealTask())) return 1;
        //noinspection unchecked
        return realTask.getRequest().compareTo(rt.getRequest());
    }
}