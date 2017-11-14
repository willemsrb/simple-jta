package nl.futureedge.simple.jta;

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
import javax.transaction.Status;

public enum JtaTransactionStatus {

    ACTIVE(Status.STATUS_ACTIVE),

    SUSPENDED(Status.STATUS_NO_TRANSACTION),

    PREPARING(Status.STATUS_PREPARING),
    PREPARED(Status.STATUS_PREPARED),

    COMMITTING(Status.STATUS_COMMITTING),
    COMMITTED(Status.STATUS_COMMITTED),
    COMMIT_FAILED(Status.STATUS_UNKNOWN),

    MARKED_ROLLBACK(Status.STATUS_MARKED_ROLLBACK),

    ROLLING_BACK(Status.STATUS_ROLLING_BACK),
    ROLLED_BACK(Status.STATUS_ROLLEDBACK),
    ROLLBACK_FAILED(Status.STATUS_UNKNOWN);

    private final int jtaStatus;

    JtaTransactionStatus(int jtaStatus) {
        this.jtaStatus = jtaStatus;
    }


    int getJtaStatus() {
        return jtaStatus;
    }

}
