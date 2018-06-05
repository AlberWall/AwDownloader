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

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.alberwall.download.android.SourceBean;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {


    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.alberwall.downloader", appContext.getPackageName());
    }

    @Test
    public void testToJson() throws Exception {
        SourceBean obj = new SourceBean();

        HashMap<String, List<String>> params = new HashMap<>();
        params.put("x2", Arrays.asList("x22", "x222"));
        obj.setParams(params);
        obj.setUrl("https://xxx");
        Log.d("TestJson", "testToJson:" + new Gson().toJson(obj));
    }

    @Test
    public void testToJson2() throws Exception {
        SourceBean obj = new SourceBean();
        HashMap<String, List<String>> params = new HashMap<>();
        params.put("x2", Arrays.asList("x22", "x222"));
        obj.setParams(params);
        obj.setUrl("https://xxxxx");

        SourceBean obj2 = new SourceBean();
        HashMap<String, List<String>> params2 = new HashMap<>();
        params2.put("y2", Arrays.asList("y22", "y222"));
        obj2.setParams(params2);
        obj2.setUrl("https://uuuuuu");

        List<SourceBean> beans = new ArrayList<>();
        beans.add(obj);
        beans.add(obj2);

        Log.d("TestJson", "testToJson2:" + new Gson().toJson(beans));
    }

    @Test
    public void testFromJson() {
        String json = "{\n" +
                "\"url\":\"https://xxx\",\n" +
                "\"connectTimeout\":8000,\n" +
                "\"readTimeout\":8000,\n" +
                "\"params\": {\"head1\":[\"value1\", \"value1\"]},\n" +
                "\"type\": 1\n" +
                "}";

        Log.d("TestJson", "testFromJson:" + new Gson().fromJson(json, SourceBean.class));
    }

    @Test
    public void testFromJson2() {
        String json = "[{\n" +
                "\"url\":\"https://xxx\",\n" +
                "\"connectTimeout\":8000,\n" +
                "\"readTimeout\":8000,\n" +
                "\"params\": {\"head1\":[\"value1\", \"value1\"]},\n" +
                "\"type\": 1\n" +
                "},\n" +
                "{\n" +
                "\"url\":\"https://yyy\",\n" +
                "\"connectTimeout\":82000,\n" +
                "\"readTimeout\":80020,\n" +
                "\"params\": {\"xxxxxx\":[\"xxxxe\", \"xxxxe\"], \"xxxxxsx\":[\"xxxxe\", \"xxxxe\"]},\n" +
                "\"type\": 2\n" +
                "}\n" +
                "]";

        Log.d("TestJson", "testFromJson2:" + new Gson().fromJson(json, new TypeToken<List<SourceBean>>() {
        }.getType()));
    }

}
