/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.ServletOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.test.MockitoOldcoreRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DownloadAction} using Mockito. This class is supposed to replace the
 * {@link DownloadActionTest} test, after all the tests have been moved in this one.
 *
 * @version $Id$
 * @since 7.2M2
 */
public class MockitoDownloadActionTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    private class StubServletOutputStream extends ServletOutputStream
    {
        public ByteArrayOutputStream baos = new ByteArrayOutputStream();

        public void write(int i) throws IOException
        {
            baos.write(i);
        }
    }

    @Test
    public void renderWhenAttachmentIsInANestedSpace() throws Exception
    {
        DownloadAction action = new DownloadAction();
        XWikiContext xcontext = this.oldcore.getXWikiContext();

        // Set the Request URL
        XWikiServletRequestStub request = new XWikiServletRequestStub();
        request.setRequestURI("http://localhost:8080/xwiki/bin/download/space1/space2/page/file.ext");
        xcontext.setRequest(request);

        // Setup the returned attachment
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(attachment.getContentSize(xcontext)).thenReturn(100);
        Date now = new Date();
        when(attachment.getDate()).thenReturn(now);
        when(attachment.getFilename()).thenReturn("file.ext");
        when(attachment.getContentInputStream(xcontext)).thenReturn(new ByteArrayInputStream("test".getBytes()));
        when(attachment.getMimeType(xcontext)).thenReturn("mimetype");

        // Set the current doc
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getAttachment("file.ext")).thenReturn(attachment);
        xcontext.setDoc(document);

        // Set the Plugin Manager
        XWikiPluginManager pluginManager = mock(XWikiPluginManager.class);
        when(pluginManager.downloadAttachment(attachment, xcontext)).thenReturn(attachment);
        when(this.oldcore.getMockXWiki().getPluginManager()).thenReturn(pluginManager);

        // Set the Response
        XWikiResponse response = mock(XWikiResponse.class);
        StubServletOutputStream ssos = new StubServletOutputStream();
        when(response.getOutputStream()).thenReturn(ssos);
        xcontext.setResponse(response);

        // Set the Resource Reference Manager used to parse the URL and extract the attachment name
        ResourceReferenceManager rrm = this.oldcore.getMocker().registerMockComponent(ResourceReferenceManager.class);
        when(rrm.getResourceReference()).thenReturn(new EntityResourceReference(new AttachmentReference("file.ext",
            new DocumentReference("wiki", Arrays.asList("space1", "space2"), "page")), EntityResourceAction.VIEW));

        // Note: we don't give PR and the attachment is not an authorized mime type.

        assertNull(action.render(xcontext));

        // This is the test, we verify what is set in the response
        verify(response).setContentType("mimetype");
        verify(response).setHeader("Accept-Ranges", "bytes");
        verify(response).addHeader("Content-disposition", "attachment; filename*=utf-8''file.ext");
        verify(response).setDateHeader("Last-Modified", now.getTime());
        verify(response).setContentLength(100);
        assertEquals("test", ssos.baos.toString());
    }
}
