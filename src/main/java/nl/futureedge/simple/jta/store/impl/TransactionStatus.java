package nl.futureedge.simple.jta.store.impl;

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

/**
 * Transaction status.
 */
public enum TransactionStatus {

    /** Active. */
    ACTIVE("ACTIVE"),

    /** Preparing. */
    PREPARING("PREPARING"),

    /** Prepared. */
    PREPARED("PREPARED"),

    /** Committing. */
    COMMITTING("COMMITTING"),

    /** Committed. */
    COMMITTED("COMMITTED"),

    /** Commit failed. */
    COMMIT_FAILED("COMMIT_FAILED"),

    /** Rolling back. */
    ROLLING_BACK("ROLLING_BACK"),

    /** Rolled back. */
    ROLLED_BACK("ROLLED_BACK"),

    /** Rollback failed. */
    ROLLBACK_FAILED("ROLLBACK_FAILED");

    private final String text;

    /**
     * Constructor.
     * @param text the status as written in the transaction store
     */
    TransactionStatus(final String text) {
        this.text = text;
    }

    /**
     * Get the status as written in the transaction store.
     * @return text
     */
    public String getText() {
        return text;
    }

}
