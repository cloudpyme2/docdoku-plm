/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2013 DocDoku SARL
 *
 * This file is part of DocDokuPLM.
 *
 * DocDokuPLM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DocDokuPLM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with DocDokuPLM.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.docdoku.client.actions;


import com.docdoku.client.ui.common.CreateAttributeTemplateDialog;
import com.docdoku.client.ui.template.EditAttributeTemplatesPanel;
import com.docdoku.core.meta.InstanceAttributeTemplate;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;

public class AddAttributeTemplateActionListener implements ActionListener {


    public void actionPerformed(ActionEvent pAE) {
        final EditAttributeTemplatesPanel sourcePanel = (EditAttributeTemplatesPanel) pAE.getSource();
        Dialog owner = (Dialog) SwingUtilities.getAncestorOfClass(Dialog.class, sourcePanel);
        ActionListener action = new ActionListener() {
            public void actionPerformed(ActionEvent pAE) {
                CreateAttributeTemplateDialog source = (CreateAttributeTemplateDialog) pAE.getSource();
                InstanceAttributeTemplate attr = new InstanceAttributeTemplate();
                attr.setName(source.getAttributeName());
                attr.setAttributeType(source.getAttributeType());
                sourcePanel.getAttributesListModel().addElement(attr);
            }
        };
        new CreateAttributeTemplateDialog(owner, action);
    }
}