package nl.futureedge.simple.jta.store.jdbc.sql;

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
import org.junit.Assert;
import org.junit.Test;

public class DefaultSqlTemplateTest {

    @Test
    public void test() {
        final DefaultSqlTemplate subject = new DefaultSqlTemplate();

        /* *** */

        Assert.assertNotEquals(null, subject.createTransactionIdSequence());
        subject.setCreateTransactionIdSequence("setCreateTransactionIdSequence");
        Assert.assertEquals("setCreateTransactionIdSequence", subject.createTransactionIdSequence());

        Assert.assertNotEquals(null, subject.selectNextTransactionId());
        subject.setSelectNextTransactionId("setSelectNextTransactionId");
        Assert.assertEquals("setSelectNextTransactionId", subject.selectNextTransactionId());

        /* *** */

        Assert.assertNotEquals(null, subject.createTransactionTable());
        subject.setCreateTransactionTable("setCreateTransactionTable");
        Assert.assertEquals("setCreateTransactionTable", subject.createTransactionTable());

        Assert.assertNotEquals(null, subject.selectTransactionIdAndStatus());
        subject.setSelectTransactionIdAndStatus("setSelectTransactionIdAndStatus");
        Assert.assertEquals("setSelectTransactionIdAndStatus", subject.selectTransactionIdAndStatus());

        Assert.assertNotEquals(null, subject.selectTransactionStatus());
        subject.setSelectTransactionStatus("setSelectTransactionStatus");
        Assert.assertEquals("setSelectTransactionStatus", subject.selectTransactionStatus());

        Assert.assertNotEquals(null, subject.insertTransactionStatus());
        subject.setInsertTransactionStatus("setInsertTransactionStatus");
        Assert.assertEquals("setInsertTransactionStatus", subject.insertTransactionStatus());

        Assert.assertNotEquals(null, subject.updateTransactionStatus());
        subject.setUpdateTransactionStatus("setUpdateTransactionStatus");
        Assert.assertEquals("setUpdateTransactionStatus", subject.updateTransactionStatus());

        Assert.assertNotEquals(null, subject.deleteTransactionStatus());
        subject.setDeleteTransactionStatus("setDeleteTransactionStatus");
        Assert.assertEquals("setDeleteTransactionStatus", subject.deleteTransactionStatus());

        /* *** */

        Assert.assertNotEquals(null, subject.createResourceTable());
        subject.setCreateResourceTable("setCreateResourceTable");
        Assert.assertEquals("setCreateResourceTable", subject.createResourceTable());

        Assert.assertNotEquals(null, subject.selectResourceStatus());
        subject.setSelectResourceStatus("setSelectResourceStatus");
        Assert.assertEquals("setSelectResourceStatus", subject.selectResourceStatus());

        Assert.assertNotEquals(null, subject.insertResourceStatus());
        subject.setInsertResourceStatus("setInsertResourceStatus");
        Assert.assertEquals("setInsertResourceStatus", subject.insertResourceStatus());

        Assert.assertNotEquals(null, subject.updateResourceStatus());
        subject.setUpdateResourceStatus("setUpdateResourceStatus");
        Assert.assertEquals("setUpdateResourceStatus", subject.updateResourceStatus());

        Assert.assertNotEquals(null, subject.deleteResourceStatus());
        subject.setDeleteResourceStatus("setDeleteResourceStatus");
        Assert.assertEquals("setDeleteResourceStatus", subject.deleteResourceStatus());
    }
}
