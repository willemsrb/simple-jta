package nl.futureedge.simple.jta.store.file;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.store.impl.PersistentTransaction;
import nl.futureedge.simple.jta.store.impl.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FilePersistentTransaction implements PersistentTransaction, Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePersistentTransaction.class);

    public static final String PREFIX = "trans-";
    public static final String SUFFIX = ".log";

    private static final String BRANCH_SEPARATOR = "-";
    private static final String RESOURCE_MANAGER_SEPARATOR = ":";
    private static final String ENTRY_SEPARATOR = "\n";

    private final long transactionId;
    private final File file;

    private final RandomAccessFile raf;

    private TransactionStatus status;
    private Map<String, TransactionStatus> resourceStatuses = new HashMap<>();

    private final FileTransactionStore store;

    FilePersistentTransaction(FileTransactionStore store, final File baseDirectory, final long transactionId) throws JtaTransactionStoreException {
        this.transactionId = transactionId;
        this.file = new File(baseDirectory, PREFIX + transactionId + SUFFIX);
        this.store = store;
        try {
            raf = new RandomAccessFile(file, "rws");
            readStatus();
        } catch (IOException e) {
            throw new JtaTransactionStoreException("Could not create, open or read sequence file", e);
        }
    }

    private void readStatus() throws IOException {
        for (String line = ""; line != null; line = raf.readLine()) {
            if (line.isEmpty()) {
                continue;
            }

            final int resourceIndex = line.indexOf(RESOURCE_MANAGER_SEPARATOR);
            if (resourceIndex != -1) {
                // Line contains resource status
                resourceStatuses.put(line.substring(0, resourceIndex), TransactionStatus.valueOf(line.substring(resourceIndex + 1)));
            } else {
                // Line contains global status
                status = TransactionStatus.valueOf(line);
            }
        }
    }

    @Override
    public void save(final TransactionStatus status) throws JtaTransactionStoreException {
        write(status, null, null);
        this.status = status;
    }

    @Override
    public void save(final TransactionStatus status, long branchId, final String resourceManager) throws JtaTransactionStoreException {
        save(status, branchId, resourceManager, null);
    }

    @Override
    public void save(TransactionStatus status, long branchId, String resourceManager, Exception cause) throws JtaTransactionStoreException {
        this.resourceStatuses.put(resourceManager + BRANCH_SEPARATOR + Long.toString(branchId), status);
        write(status, branchId, resourceManager);
    }

    private void write(TransactionStatus status, Long branchId, String resourceManager) throws JtaTransactionStoreException {
        try {
            StringBuilder lineToWrite = new StringBuilder();

            if (resourceManager != null) {
                lineToWrite.append(resourceManager);
                lineToWrite.append(BRANCH_SEPARATOR);
                lineToWrite.append(Long.toString(branchId));
                lineToWrite.append(RESOURCE_MANAGER_SEPARATOR);
            }
            lineToWrite.append(status.getText());
            lineToWrite.append(ENTRY_SEPARATOR);

            // Flush to disk
            raf.writeChars(lineToWrite.toString());
        } catch (IOException e) {
            throw new JtaTransactionStoreException("Could not write transaction file", e);
        }
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }

    @Override
    public void remove() throws JtaTransactionStoreException {
        try {
            close();
        } catch (final IOException e) {
            // Ignore
            LOGGER.warn("Could not close file access", e);
        }

        try {
            Files.delete(Paths.get(file.toURI()));
        } catch (final IOException e) {
            // Ignore
            LOGGER.warn("Could not delete transaction file", e);
        }

        if (store != null) {
            store.persistentTransactionRemoved(transactionId);
        }
    }

    @Override
    public TransactionStatus getStatus() {
        return status;
    }

    public Collection<TransactionStatus> getResourceStatusses() {
        return resourceStatuses.values();
    }
}
