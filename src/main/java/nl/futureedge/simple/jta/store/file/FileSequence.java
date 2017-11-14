package nl.futureedge.simple.jta.store.file;

/*-
 * #%L
 * Simple JTA
 * %%
 * Copyright (C) 2017 Future Edge IT
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
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicLong;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;

final class FileSequence implements Closeable {

    public static final String SEQUENCE_PREFIX = "sequence";

    private final File file;
    private final RandomAccessFile raf;
    private final AtomicLong sequence;

    FileSequence(final File baseDirectory) throws JtaTransactionStoreException {
        file = new File(baseDirectory, SEQUENCE_PREFIX + FilePersistentTransaction.SUFFIX);
        try {
            raf = new RandomAccessFile(file, "rws");
            sequence = new AtomicLong(read(raf));
        } catch (IOException e) {
            throw new JtaTransactionStoreException("Could not create, open or read sequence file", e);
        }
    }

    public long nextSequence() throws JtaTransactionStoreException {
        long result = sequence.incrementAndGet();
        try {
            write(result);
        } catch (IOException e) {
            throw new JtaTransactionStoreException("Could not store sequence", e);
        }
        return result;
    }

    private static long read(RandomAccessFile raf) throws IOException {
        if (raf.length() < 8) {
            return 0L;
        } else {
            return raf.readLong();
        }
    }

    private void write(long result) throws IOException {
        raf.setLength(0);
        raf.writeLong(result);
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }

}
