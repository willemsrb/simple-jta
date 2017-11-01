package nl.futureedge.simple.jta.store.file;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.store.impl.BaseTransactionStore;
import nl.futureedge.simple.jta.store.impl.PersistentTransaction;
import nl.futureedge.simple.jta.store.impl.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

/**
 * File based transaction store.
 *
 * Creates a separate file for each transaction registering the xid and state; removing the file when an end-state (committed or rollback) has been reached.
 */
public final class FileTransactionStore extends BaseTransactionStore implements InitializingBean {

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
    public void doDestroy() {
        try {
            sequence.close();
        } catch (IOException e) {
            LOGGER.warn("Could not close sequence file", e);
        }
        sequence = null;

    }

    /* ************************** */
    /* *** CLEANUP ************** */
    /* ************************** */

    @Override
    public void cleanup() throws JtaTransactionStoreException {
        final File[] files = baseDirectory.listFiles((dir, name) ->
                name != null && name.startsWith(FilePersistentTransaction.PREFIX) && name.endsWith(FilePersistentTransaction.SUFFIX)
        );
        if (files == null) {
            return;
        }
        for (final File file : files) {
            int begin = FilePersistentTransaction.PREFIX.length();
            int end = file.getName().length() - FilePersistentTransaction.SUFFIX.length();

            long transactionId = Long.parseLong(file.getName().substring(begin, end));
            final FilePersistentTransaction transaction = new FilePersistentTransaction(baseDirectory, transactionId);
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

    @Override
    protected PersistentTransaction createPersistentTransaction(long transactionId) throws JtaTransactionStoreException {
        return new FilePersistentTransaction(baseDirectory, transactionId);
    }
}
