package nl.futureedge.simple.jta.store.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.store.impl.PersistentTransaction;
import nl.futureedge.simple.jta.store.impl.TransactionStatus;
import nl.futureedge.simple.jta.xid.JtaXid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FilePersistentTransaction implements PersistentTransaction {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePersistentTransaction.class);

    public static final String PREFIX = "trans-";
    public static final String SUFFIX = ".log";

    private static final String RESOURCE_MANAGER_SEPARATOR = ":";
    private static final String ENTRY_SEPARATOR = "\n";

    private final File file;

    private final FileOutputStream output;
    private final Writer writer;

    private TransactionStatus status;

    FilePersistentTransaction(File baseDirectory, JtaXid xid) throws JtaTransactionStoreException {
        file = new File(baseDirectory, PREFIX + xid.getTransactionId() + SUFFIX);
        if (file.exists()) {
            // Read
            status = readStatus(file);
        }

        try {
            output = new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            throw new JtaTransactionStoreException("Could not open transaction log file", e);
        }
        writer = new OutputStreamWriter(output);
    }

    private static TransactionStatus readStatus(File file) throws JtaTransactionStoreException {
        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String lastStatus = null;
            for (String line = ""; line != null; line = reader.readLine()) {
                if (line.isEmpty()) {
                    continue;
                }
                if (line.contains(RESOURCE_MANAGER_SEPARATOR)) {
                    continue;
                }
                lastStatus = line;
            }
            return TransactionStatus.fromText(lastStatus);
        } catch (final IOException e) {
            throw new JtaTransactionStoreException("Could not read existing transaction log", e);
        }
    }

    @Override
    public void save(final TransactionStatus status) throws JtaTransactionStoreException {
        write(status, null, null, null);
        this.status = status;
    }

    @Override
    public void save(final TransactionStatus status, long branchId, final String resourceManager) throws JtaTransactionStoreException {
        write(status, null, resourceManager, null);
    }

    @Override
    public void save(TransactionStatus status, long branchId, String resourceManager, Exception cause) throws JtaTransactionStoreException {
        write(status, branchId, resourceManager, cause);
    }

    private void write(TransactionStatus status, Long branchId, String resourceManager, Exception cause) throws JtaTransactionStoreException {
        try {
            if (resourceManager != null) {
                writer.write(resourceManager);
                writer.write(RESOURCE_MANAGER_SEPARATOR);
                writer.write(Long.toString(branchId));
                writer.write(RESOURCE_MANAGER_SEPARATOR);
            }
            writer.write(status.getText());
            writer.write(ENTRY_SEPARATOR);

            // Flush to disk
            writer.flush();
            output.getFD().sync();
        } catch (IOException e) {
            throw new JtaTransactionStoreException("Could not write transaction file", e);
        }
    }

    @Override
    public void remove() throws JtaTransactionStoreException {
        try {
            writer.flush();
            output.getFD().sync();
        } catch (IOException e) {
            // Ignore
            LOGGER.warn("Could not flush transaction file", e);
        }

        try {
            writer.close();
        } catch (final IOException e) {
            // Ignore
            LOGGER.warn("Could not close transaction writer", e);
        }
        try {
            output.close();
        } catch (final IOException e) {
            // Ignore
            LOGGER.warn("Could not close transaction file", e);
        }

        if (!file.renameTo(new File(file.getParentFile(), file.getName() + ".completed." + System.currentTimeMillis()))) {
            LOGGER.warn("Could not remove transaction file");
        }
        //file.delete();
    }

    @Override
    public TransactionStatus getStatus() throws JtaTransactionStoreException {
        return status;
    }
}
