package nl.futureedge.simple.jta.store.file;

import java.io.File;
import nl.futureedge.simple.jta.JtaXid;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.store.impl.BaseTransactionStore;
import nl.futureedge.simple.jta.store.impl.PersistentTransaction;
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

    /* ************************** */
    /* *** CLEANUP ************** */
    /* ************************** */

    @Override
    public void cleanup() throws JtaTransactionStoreException {
        // TODO
    }

    /* ************************** */
    /* *** PERSISTENCE ********** */
    /* ************************** */

    @Override
    public long nextTransactionId() throws JtaTransactionStoreException {
        return sequence.nextSequence();
    }


    @Override
    protected PersistentTransaction getPersistentTransaction(JtaXid xid) throws JtaTransactionStoreException {
        return new FilePersistentTransaction(baseDirectory, xid);
    }
}
