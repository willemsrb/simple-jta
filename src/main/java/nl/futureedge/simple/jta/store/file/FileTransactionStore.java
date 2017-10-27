package nl.futureedge.simple.jta.store.file;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.store.impl.BaseTransactionStore;
import nl.futureedge.simple.jta.store.impl.PersistentTransaction;
import nl.futureedge.simple.jta.store.impl.TransactionStatus;
import nl.futureedge.simple.jta.xid.JtaXid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

/**
 * File based transaction store.
 *
 * Creates a separate file for each transaction registering the xid and state; removing the file when an end-state (committed or rollback) has been reached.
 */
public final class FileTransactionStore extends BaseTransactionStore implements InitializingBean, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileTransactionStore.class);

    private File baseDirectory;
    private FileSequence sequence;

    @Required
    public void setBaseDirectory(final File baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.info("Initializing file transaction store in {}", baseDirectory.getAbsolutePath());
        baseDirectory.mkdirs();
        sequence = new FileSequence(baseDirectory);
    }

    @Override
    public void destroy() throws Exception {
        sequence.close();
        sequence = null;

        synchronized (transactions) {
            for (final FilePersistentTransaction transaction : transactions.values()) {
                transaction.close();
            }
            transactions.clear();
        }
    }

    /* ************************** */
    /* *** CLEANUP ************** */
    /* ************************** */

    @Override
    public void cleanup() throws JtaTransactionStoreException {
        final File[] files = baseDirectory.listFiles((dir, name) -> {
            return name != null && name.startsWith(FilePersistentTransaction.PREFIX) && name.endsWith(FilePersistentTransaction.SUFFIX);
        });
        if (files == null) {
            return;
        }
        for (final File file : files) {
            int begin = FilePersistentTransaction.PREFIX.length();
            int end = file.getName().length() - FilePersistentTransaction.SUFFIX.length();

            long transactionId = Long.parseLong(file.getName().substring(begin, end));
            final FilePersistentTransaction transaction = new FilePersistentTransaction(null, baseDirectory, transactionId);
            final TransactionStatus transactionStatus = transaction.getStatus();

            if (CLEANABLE.containsKey(transactionStatus)
                    && isCleanable(transaction.getResourceStatusses(), CLEANABLE.get(transactionStatus))) {
                transaction.remove();
            }
        }
    }

    private boolean isCleanable(final Collection<TransactionStatus> resourceStatuses, final List<TransactionStatus> allowedResourceStatuses) {
        for (final TransactionStatus resourceStatus : resourceStatuses) {
            if (!allowedResourceStatuses.contains(resourceStatus)) {
                return false;
            }
        }
        return true;
    }

    /* ************************** */
    /* *** PERSISTENCE ********** */
    /* ************************** */

    @Override
    public long nextTransactionId() throws JtaTransactionStoreException {
        return sequence.nextSequence();
    }

    private final Map<Long, FilePersistentTransaction> transactions = new HashMap<>();

    @Override
    protected PersistentTransaction getPersistentTransaction(JtaXid xid) throws JtaTransactionStoreException {
        long transactionId = xid.getTransactionId();
        if (transactions.containsKey(transactionId)) {
            return transactions.get(transactionId);
        } else {
            synchronized (transactions) {
                if (!transactions.containsKey(transactionId)) {
                    transactions.put(transactionId, new FilePersistentTransaction(this, baseDirectory, transactionId));
                }
                return transactions.get(transactionId);
            }
        }
    }

    void persistentTransactionRemoved(long transactionId) {
        transactions.remove(transactionId);
    }
}
