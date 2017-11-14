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
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class XAResourceAdapterTest {

    private Xid xid = Mockito.mock(Xid.class);
    private XAResource xaResource;
    private XAResourceAdapter subject;

    @Before
    public void setup() {
        xaResource = Mockito.mock(XAResource.class);

        subject = new XAResourceAdapter("resourceManager", true, false, xaResource);
    }

    @Test
    public void getters() {
        Assert.assertEquals("resourceManager", subject.getResourceManager());
        Assert.assertEquals(true, subject.supportsJoin());
        Assert.assertEquals(false, subject.supportsSuspend());
        Assert.assertNotNull(subject.toString());
    }

    @Test
    public void commit() throws Exception {
        subject.commit(xid, false);
        Mockito.verify(xaResource).commit(xid, false);
        Mockito.verifyNoMoreInteractions(xaResource);
    }

    @Test
    public void end() throws Exception {
        subject.end(xid, XAResource.TMNOFLAGS);
        Mockito.verify(xaResource).end(xid, XAResource.TMNOFLAGS);
        Mockito.verifyNoMoreInteractions(xaResource);
    }

    @Test
    public void forget() throws Exception {
        subject.forget(xid);
        Mockito.verify(xaResource).forget(xid);
        Mockito.verifyNoMoreInteractions(xaResource);
    }

    @Test
    public void getTransactionTimeout() throws Exception {
        Mockito.when(xaResource.getTransactionTimeout()).thenReturn(4500);
        Assert.assertEquals(4500, subject.getTransactionTimeout());
        Mockito.verify(xaResource).getTransactionTimeout();
        Mockito.verifyNoMoreInteractions(xaResource);
    }

    @Test
    public void prepare() throws Exception {
        Mockito.when(xaResource.prepare(xid)).thenReturn(XAResource.XA_RDONLY);
        Assert.assertEquals(XAResource.XA_RDONLY, subject.prepare(xid));
        Mockito.verify(xaResource).prepare(xid);
        Mockito.verifyNoMoreInteractions(xaResource);
    }

    @Test
    public void recover() throws Exception {
        Xid[] result = new Xid[]{};
        Mockito.when(xaResource.recover(XAResource.TMNOFLAGS)).thenReturn(result);
        Assert.assertSame(result, subject.recover(XAResource.TMNOFLAGS));
        Mockito.verify(xaResource).recover(XAResource.TMNOFLAGS);
        Mockito.verifyNoMoreInteractions(xaResource);
    }

    @Test
    public void rollback() throws Exception {
        subject.rollback(xid);
        Mockito.verify(xaResource).rollback(xid);
        Mockito.verifyNoMoreInteractions(xaResource);
    }

    @Test
    public void setTransactionTimeout() throws Exception {
        subject.setTransactionTimeout(34);
        Mockito.verify(xaResource).setTransactionTimeout(34);
        Mockito.verifyNoMoreInteractions(xaResource);
    }

    @Test
    public void start() throws Exception {
        subject.start(xid, XAResource.TMNOFLAGS);
        Mockito.verify(xaResource).start(xid, XAResource.TMNOFLAGS);
        Mockito.verifyNoMoreInteractions(xaResource);
    }

    @Test
    public void isSameRM() throws Exception {
        final XAResource otherResource = Mockito.mock(XAResource.class);

        subject.isSameRM(otherResource);

        Mockito.verify(otherResource).isSameRM(xaResource);
        Mockito.verifyNoMoreInteractions(otherResource, xaResource);
    }

}
