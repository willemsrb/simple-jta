package nl.futureedge.simple.jta.xid;

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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Global Jta XID; not for external use!
 */
public final class GlobalJtaXid extends BaseJtaXid {

    private final AtomicLong branchSequence = new AtomicLong();

    /**
     * Constructor (for XID created by this transaction manager).
     * @param transactionManager transaction manager unique name
     * @param transactionId transaction id
     */
    public GlobalJtaXid(final String transactionManager, final long transactionId) {
        super(transactionManager, transactionId, null);
    }


    /**
     * Create a branch XID for this global transaction id.
     * @return a branch XID (using a new branch id)
     */
    public BranchJtaXid createBranchXid() {
        return new BranchJtaXid(getTransactionManager(), getTransactionId(), branchSequence.incrementAndGet());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GlobalJtaXid)) {
            return false;
        }
        final GlobalJtaXid jtaXid = (GlobalJtaXid) o;
        return getTransactionId() == jtaXid.getTransactionId() &&
                Objects.equals(getTransactionManager(), jtaXid.getTransactionManager());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTransactionManager(), getTransactionId());
    }

    @Override
    public String toString() {
        return "GlobalJtaXid{" +
                "transactionManager='" + getTransactionManager() + '\'' +
                ", transactionId=" + getTransactionId() +
                '}';
    }

}
