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
package org.xwiki.test.ui.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

/**
 * Represents the actions possible on the Create Space template page.
 *
 * @version $Id$
 * @since 3.2M3
 */
public class CreateSpacePage extends ViewPage
{
    @FindBy(id = "name")
    private WebElement spaceTextField;

    public WYSIWYGEditPage createSpace(String spaceValue)
    {
        this.spaceTextField.sendKeys(spaceValue);
        this.spaceTextField.submit();
        return new WYSIWYGEditPage();
    }

    public boolean hasError()
    {
        // if there is at least one element with errormessage classname
        return getDriver().findElements(By.className("errormessage")).size() > 0;
    }
}
