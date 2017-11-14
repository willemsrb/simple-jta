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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.transaction.xa.Xid;

/**
 * Branch Jta XID; not for external use!
 */
public final class BranchJtaXid extends BaseJtaXid {

    BranchJtaXid(final String transactionManager, final long transactionId, final Long branchId) {
        super(transactionManager, transactionId, branchId);
    }

    /**
     * Filter the given list of XID's to retrieve the XID's for this transaction manager.
     * @param xids list of XID's
     * @param transactionManager transaction manager unique name
     * @return list of XID's that this transaction manager should recover
     */
    public static List<BranchJtaXid> filterRecoveryXids(final Xid[] xids, final String transactionManager) {
        final List<BranchJtaXid> result = new ArrayList<>();
        final byte[] globalTransactionId = createGlobalTransactionId(transactionManager, 0);

        for (final Xid xid : xids) {
            // Format should be the same,
            // global transaction ID should have the same transaction manager and
            // branch qualifier should contain a branch id (long)
            if (SIMPLE_JTA_FORMAT == xid.getFormatId()
                    && xid.getGlobalTransactionId() != null
                    && globalTransactionIdMatches(globalTransactionId, xid.getGlobalTransactionId())
                    && xid.getBranchQualifier() != null
                    && xid.getBranchQualifier().length == 8
                    ) {
                final ByteBuffer globalBuffer = ByteBuffer.wrap(xid.getGlobalTransactionId());
                final long transactionId = globalBuffer.getLong(56);

                final ByteBuffer branchBuffer = ByteBuffer.wrap(xid.getBranchQualifier());
                final long branchId = branchBuffer.getLong();

                result.add(new BranchJtaXid(transactionManager, transactionId, branchId));
            }
        }

        return result;
    }

    private static boolean globalTransactionIdMatches(final byte[] partial, final byte[] toValidate) {
        if (toValidate.length != 64) {
            return false;
        }

        // Compare up to byte 56 after which the transaction id is ignored
        for (int index = 0; index < 56; index++) {
            if (partial[index] != toValidate[index]) {
                return false;
            }
        }
        return true;
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseJtaXid)) {
            return false;
        }
        final BranchJtaXid jtaXid = (BranchJtaXid) o;
        return getTransactionId() == jtaXid.getTransactionId() &&
                Objects.equals(getTransactionManager(), jtaXid.getTransactionManager()) &&
                Objects.equals(getBranchId(), jtaXid.getBranchId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTransactionManager(), getTransactionId(), getBranchId());
    }

    @Override
    public String toString() {
        return "BranchJtaXid{" +
                "transactionManager='" + getTransactionManager() + '\'' +
                ", transactionId=" + getTransactionId() +
                ", branchId=" + getBranchId() +
                '}';
    }
}
