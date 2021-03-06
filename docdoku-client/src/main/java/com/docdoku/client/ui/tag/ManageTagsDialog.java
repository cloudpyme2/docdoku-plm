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

package com.docdoku.client.ui.tag;

import com.docdoku.client.localization.I18N;
import com.docdoku.client.ui.common.OKCancelPanel;
import com.docdoku.core.document.DocumentMaster;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ManageTagsDialog extends JDialog implements ActionListener {
    private ManageTagsPanel mManageTagsPanel;
    private OKCancelPanel mOKCancelPanel;
    private ActionListener mAction;
    private DocumentMaster mEditedDocM;

    public ManageTagsDialog(Frame pOwner, DocumentMaster pEditedDocM, ActionListener pAction) {
        super(pOwner, I18N.BUNDLE.getString("ManageTagsDialog_title"), true);
        setLocationRelativeTo(pOwner);
        mEditedDocM=pEditedDocM;
        mManageTagsPanel = new ManageTagsPanel(mEditedDocM.getTags());
        mOKCancelPanel = new OKCancelPanel(this, this);
        mAction = pAction;
        createLayout();
        createListener();
        setVisible(true);
    }

    private void createLayout() {
        getRootPane().setDefaultButton(mOKCancelPanel.getOKButton());
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(mManageTagsPanel, BorderLayout.CENTER);
        mainPanel.add(mOKCancelPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);
        mOKCancelPanel.setEnabled(true);
        pack();
    }

    private void createListener() {
        
    }

    public DocumentMaster getDocM() {
        return mEditedDocM;
    }

    public String[] getTags() {
        String[] tags = new String[mManageTagsPanel.getTagsListModel().getSize()];
        for(int i=0;i<mManageTagsPanel.getTagsListModel().getSize();i++)
            tags[i]=(String)mManageTagsPanel.getTagsListModel().get(i);

        return tags;
    }
    
    public void actionPerformed(ActionEvent pAE) {
        mAction.actionPerformed(new ActionEvent(this, 0, null));
    }
}
