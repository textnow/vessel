/**
 * MIT License
 *
 * Copyright (c) 2020 TextNow, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.textnow.android.vessel;

import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.textnow.android.vessel.model.SimpleData;
import com.textnow.android.vessel.model.SimpleDataKt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Validate functionality of the Vessel implementation.
 * These tests will rely on the in-memory room database so we can verify proper serialization.
 *
 * This subset of tests only validates the Java-specific accessors.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = Build.VERSION_CODES.P)
public class JavaAccessTest extends BaseVesselTest {

    public JavaAccessTest() {
        super();
    }

    @Test
    public void classesHaveReasonableNames() {
        SimpleData simpleData = SimpleDataKt.getFirstSimple();
        String type = getVessel().typeNameOf(simpleData);
        assertEquals("com.textnow.android.vessel.model.SimpleData", type);

        type = getVessel().typeNameOf("");
        assertEquals("kotlin.String", type);
    }

    @Test
    public void verifyJavaGetAccessor() {
        SimpleData simpleData = SimpleDataKt.getFirstSimple();
        getVessel().setBlocking(simpleData);

        SimpleData result = getVessel().getBlocking(SimpleData.class);
        assertNotNull(result);
        assertEquals(simpleData.getName(), result.getName());
        assertEquals(simpleData.getNumber(), result.getNumber());
    }

    @Test
    public void verifyJavaDeleteAccessor() {
        SimpleData simpleData = SimpleDataKt.getFirstSimple();
        getVessel().setBlocking(simpleData);

        getVessel().deleteBlocking(SimpleData.class);

        SimpleData result = getVessel().getBlocking(SimpleData.class);
        assertNull(result);
    }
}
