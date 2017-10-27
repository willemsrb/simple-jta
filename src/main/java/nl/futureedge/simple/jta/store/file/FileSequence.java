package nl.futureedge.simple.jta.store.file;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicLong;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;

final class FileSequence implements Closeable {

    public static final String SEQUENCE = "sequence";

    private final File file;
    private final RandomAccessFile raf;
    private final AtomicLong sequence;

    FileSequence(final File baseDirectory) throws JtaTransactionStoreException {
        file = new File(baseDirectory, SEQUENCE + FilePersistentTransaction.SUFFIX);
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
