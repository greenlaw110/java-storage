package org.osgl.storage.impl;

/*-
 * #%L
 * Java Storage Service
 * %%
 * Copyright (C) 2013 - 2017 OSGL (Open Source General Library)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.osgl.exception.UnexpectedIOException;
import org.osgl.storage.ISObject;
import org.osgl.util.IO;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.nio.charset.Charset;

public class StorageObject<TYPE extends StorageObject, SVC extends StorageServiceBase<TYPE>> extends SObject {
    protected transient SVC svc;
    private SoftReference<byte[]> cache;
    protected transient ISObject buf;

    StorageObject(String key, SVC svc) {
        super(key);
        this.svc = svc;
        setAttributes(svc.getMeta(key));
    }

    @Override
    public long getLength() {
        String s = getAttribute(ISObject.ATTR_CONTENT_LENGTH);
        if (null != s) {
            return Long.parseLong(s);
        }
        if (null != cache) {
            byte[] ba = cache.get();
            if (null != ba) {
                return ba.length;
            }
        }
        return 0L;
    }

    protected ISObject buf() {
        if (null == buf) {
            synchronized (this) {
                if (null == buf) {
                    buf = loadBuf();
                    if (!buf.isValid()) {
                        this.setCause(buf.getException());
                    }
                }
            }
        }
        return buf;
    }

    protected ISObject loadBuf() {
        ISObject sobj = SObject.of(read());
        sobj.setAttributes(getAttributes());
        return sobj;
    }

    @Override
    public File asFile() throws UnexpectedIOException {
        return buf().asFile();
    }

    @Override
    public String asString() throws UnexpectedIOException {
        return buf().asString();
    }

    @Override
    public String asString(Charset charset) throws UnexpectedIOException {
        return buf().asString(charset);
    }

    @Override
    public byte[] asByteArray() throws UnexpectedIOException {
        return buf().asByteArray();
    }

    @Override
    public InputStream asInputStream() throws UnexpectedIOException {
        return svc.getInputStream(getKey());
    }

    private synchronized byte[] read() {
        if (null != cache) {
            byte[] ba = cache.get();
            if (null != ba) {
                return ba;
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = svc.getInputStream(getKey());
        IO.copy(is, baos);
        byte[] ba = baos.toByteArray();
        cache = new SoftReference<>(ba);
        return ba;
    }
}
