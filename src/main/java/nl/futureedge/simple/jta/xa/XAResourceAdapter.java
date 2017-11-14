package nl.futureedge.simple.jta.xa;

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
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XA Resource adapter that contains the resource manager name (and additonal configuration) of a XA resource.
 */
public final class XAResourceAdapter implements XAResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(XAResourceAdapter.class);

    private final String resourceManager;
    private final boolean supportsJoin;
    private final boolean supportsSuspend;

    private final XAResource xaResource;

    public XAResourceAdapter(final String resourceManager, final boolean supportsJoin, final boolean supportsSuspend, final XAResource xaResource) {
        this.resourceManager = resourceManager;
        this.supportsJoin = supportsJoin;
        this.supportsSuspend = supportsSuspend;
        this.xaResource = xaResource;
    }

    public String getResourceManager() {
        return resourceManager;
    }

    public boolean supportsJoin() {
        return supportsJoin;
    }

    public boolean supportsSuspend() {
        return supportsSuspend;
    }

    @Override
    public void commit(final Xid xid, final boolean onePhase) throws XAException {
        LOGGER.trace("commit(xid={},onePhase={})", xid, onePhase);
        xaResource.commit(xid, onePhase);
    }

    @Override
    public void end(final Xid xid, final int flags) throws XAException {
        LOGGER.trace("end(xid={},flags={})", xid, flags);
        xaResource.end(xid, flags);
    }

    @Override
    public void forget(final Xid xid) throws XAException {
        LOGGER.trace("forget(xid={})", xid);
        xaResource.forget(xid);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        LOGGER.trace("getTransactionTimeout()");
        return xaResource.getTransactionTimeout();
    }

    @Override
    public boolean isSameRM(final XAResource otherResource) throws XAException {
        LOGGER.trace("isSameRM(otherResource={})", otherResource);
        // Unwrap by calling on 'other' side
        return otherResource.isSameRM(this.xaResource);
    }

    @Override
    public int prepare(final Xid xid) throws XAException {
        LOGGER.trace("prepare(xid={})", xid);
        return xaResource.prepare(xid);
    }

    @Override
    public Xid[] recover(final int flag) throws XAException {
        LOGGER.trace("recover(flag={})", flag);
        return xaResource.recover(flag);
    }

    @Override
    public void rollback(final Xid xid) throws XAException {
        LOGGER.trace("rollback(xid={})", xid);
        xaResource.rollback(xid);
    }

    @Override
    public boolean setTransactionTimeout(final int seconds) throws XAException {
        LOGGER.trace("setTransactionTimeout(seconds={})", seconds);
        return xaResource.setTransactionTimeout(seconds);
    }

    @Override
    public void start(final Xid xid, final int flags) throws XAException {
        LOGGER.trace("start(xid={},flags={})", xid, flags);
        xaResource.start(xid, flags);
    }

    @Override
    public String toString() {
        return "XAResourceAdapter{" +
                "resourceManager='" + resourceManager + '\'' +
                ", xaResource=" + xaResource +
                '}';
    }
}
