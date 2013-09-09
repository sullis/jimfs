/*
 * Copyright 2013 Google Inc.
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

package com.google.jimfs.internal;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link InputStream} for reading from a file's {@link ByteStore}.
 *
 * @author Colin Decker
 */
final class JimfsInputStream extends InputStream {

  private final Object lock = new Object();

  private volatile File file;
  private volatile ByteStore store;

  private int pos;

  public JimfsInputStream(File file) {
    this.file = file;
    this.store = file.content();
  }

  @Override
  public int read() throws IOException {
    synchronized (lock) {
      checkNotClosed();

      file.updateAccessTime();
      return store.read(pos++); // it's ok for pos to go beyond size()
    }
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    synchronized (lock) {
      checkNotClosed();
      int read = store.read(pos, b, off, len);
      if (read != -1) {
        pos += read;
      }

      file.updateAccessTime();
      return read;
    }
  }

  @Override
  public long skip(long n) throws IOException {
    if (n <= 0) {
      return 0;
    }

    synchronized (lock) {
      // available() must be an int, so the min must be also
      int skip = (int) Math.min(Math.max(store.sizeInBytes() - pos, 0), n);
      pos += skip;
      return skip;
    }
  }

  @Override
  public int available() throws IOException {
    synchronized (lock) {
      return Math.max(store.sizeInBytes() - pos, 0);
    }
  }

  private void checkNotClosed() throws IOException {
    if (store == null) {
      throw new IOException("stream is closed");
    }
  }

  @Override
  public void close() throws IOException {
    synchronized (lock) {
      file = null;
      store = null;
    }
  }
}