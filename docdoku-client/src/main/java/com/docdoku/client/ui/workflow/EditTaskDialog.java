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

package com.docdoku.client.ui.workflow;

import com.docdoku.client.localization.I18N;
import com.docdoku.core.workflow.TaskModel;

import java.awt.*;
import java.awt.event.ActionListener;


public class EditTaskDialog extends TaskModelDialog{

    private TaskModel mTaskModel;

    public EditTaskDialog(Frame pOwner, ActionListener pAction, TaskModel pTaskModel) {
        super(pOwner, I18N.BUNDLE.getString("EditTask_title"));
        init(pAction, pTaskModel);
    }

    public EditTaskDialog(Dialog pOwner, ActionListener pAction, TaskModel pTaskModel) {
        super(pOwner, I18N.BUNDLE.getString("EditTask_title"));
        init(pAction, pTaskModel);
    }

    protected void init(ActionListener pAction, TaskModel pTaskModel){
        mTaskModel=pTaskModel;
        mEditTaskModelPanel = new EditTaskModelPanel(mTaskModel);
        super.init(pAction);
        setVisible(true);
    }

    public TaskModel getTaskModel(){
        return mTaskModel;
    }
}
