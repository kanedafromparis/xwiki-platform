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
package com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Inserts an HTML fragment in place of the current selection. We overwrite the default implementation provided by the
 * predefined insertHTML command for two reasons:
 * <ul>
 * <li>Internet Explorer doesn't support the insertHTML predefined command.</li>
 * <li>Besides inserting the specified HTML in the edited DOM document, Mozilla also does some unwanted cleaning of the
 * DOM nodes like br's which leads to unexpected effects of executing this command. This is most annoying in tests when
 * we have to know how the DOM tree will be after executing the command.</li>
 * </ul>
 * 
 * @version $Id$
 */
public class InsertHTMLExecutable implements Executable
{
    /**
     * {@inheritDoc}
     * 
     * @see Executable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String param)
    {
        Selection selection = rta.getDocument().getSelection();
        selection.getRangeAt(0).deleteContents();

        Element container = rta.getDocument().xCreateDivElement().cast();
        container.xSetInnerHTML(param);

        Range range = selection.getRangeAt(0);
        assert (range.isCollapsed());
        Node rangeContainer = range.getCommonAncestorContainer();
        if (rangeContainer.getNodeType() == Node.ELEMENT_NODE) {
            if (range.getStartOffset() == rangeContainer.getChildNodes().getLength()) {
                rangeContainer.appendChild(container);
            } else {
                rangeContainer.insertBefore(container, rangeContainer.getChildNodes().getItem(range.getStartOffset()));
            }
        } else {
            // Text or Comment DOM node
            if (range.getStartOffset() == 0) {
                rangeContainer.getParentNode().insertBefore(container, rangeContainer);
            } else if (range.getStartOffset() < rangeContainer.getNodeValue().length()) {
                Node clone = rangeContainer.cloneNode(false);
                clone.setNodeValue(rangeContainer.getNodeValue().substring(range.getStartOffset()));
                rangeContainer.setNodeValue(rangeContainer.getNodeValue().substring(0, range.getStartOffset()));
                if (rangeContainer.getNextSibling() != null) {
                    rangeContainer.getParentNode().insertBefore(clone, rangeContainer.getNextSibling());
                } else {
                    rangeContainer.getParentNode().appendChild(clone);
                }
                rangeContainer.getParentNode().insertBefore(container, clone);
            } else if (rangeContainer.getNextSibling() != null) {
                rangeContainer.getParentNode().insertBefore(container, rangeContainer.getNextSibling());
            } else {
                rangeContainer.getParentNode().appendChild(container);
            }
        }

        container.unwrap();

        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#getParameter(RichTextArea)
     */
    public String getParameter(RichTextArea rta)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isEnabled(RichTextArea)
     */
    public boolean isEnabled(RichTextArea rta)
    {
        return rta.getDocument().getSelection().getRangeCount() > 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isExecuted(RichTextArea)
     */
    public boolean isExecuted(RichTextArea rta)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isSupported(RichTextArea)
     */
    public boolean isSupported(RichTextArea rta)
    {
        return true;
    }
}
