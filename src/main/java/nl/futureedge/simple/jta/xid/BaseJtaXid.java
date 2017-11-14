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
import java.nio.charset.Charset;

/**
 * Jta XID; not for external use!
 */
class BaseJtaXid implements JtaXid {

    static final int SIMPLE_JTA_FORMAT = 0x1ee3;

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private String transactionManager;
    private long transactionId;
    private Long branchId;

    private final int format;
    private final byte[] globalTransactionId;
    private final byte[] branchQualifier;

    BaseJtaXid(final String transactionManager, final long transactionId, final Long branchId) {
        this.transactionManager = transactionManager;
        this.transactionId = transactionId;
        this.branchId = branchId;

        this.format = SIMPLE_JTA_FORMAT;
        this.globalTransactionId = createGlobalTransactionId(this.transactionManager, this.transactionId);
        this.branchQualifier = createBranchId(this.branchId);
    }

    static byte[] createGlobalTransactionId(final String transactionManager, final long transactionId) {
        final ByteBuffer buffer = ByteBuffer.allocate(64);

        // Add transaction manager
        final byte[] tmBytes = transactionManager.getBytes(UTF8);
        int tmLength = tmBytes.length;
        buffer.putInt(tmLength);
        buffer.put(tmBytes, 0, Math.min(52, tmLength));

        // Add transaction id
        buffer.position(56);
        buffer.putLong(transactionId);

        return buffer.array();
    }

    private static byte[] createBranchId(final Long branchId) {
        if (branchId == null) {
            return new byte[]{};
        }

        final ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(branchId);

        return buffer.array();
    }

    /**
     * @return the transaction manager unique name
     */
    public String getTransactionManager() {
        return transactionManager;
    }

    /**
     * @return transaction id
     */
    public long getTransactionId() {
        return transactionId;
    }

    /**
     * @return return branch id (null if this is the global transaction)
     */
    public Long getBranchId() {
        return branchId;
    }

    @Override
    public int getFormatId() {
        return format;
    }

    @Override
    public byte[] getGlobalTransactionId() {
        return globalTransactionId;
    }

    @Override
    public byte[] getBranchQualifier() {
        return branchQualifier;
    }

}
