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

package com.docdoku.core.workflow;

import com.docdoku.core.common.EntityKey;

import java.io.Serializable;

/**
 *
 * @author Florent Garin
 */
public class ActivityModelKey implements Serializable, EntityKey {
    
    private String workspaceId;
    private String workflowModelId;
    private int step;
    
    public ActivityModelKey() {
    }
    
    public ActivityModelKey(String pWorkspaceId, String pWorkflowModelId, int pStep) {
        workspaceId=pWorkspaceId;
        workflowModelId=pWorkflowModelId;
        step=pStep;
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
	hash = 31 * hash + workspaceId.hashCode();
	hash = 31 * hash + workflowModelId.hashCode();
        hash = 31 * hash + step;
	return hash;
    }
    
    @Override
    public boolean equals(Object pObj) {
        if (this == pObj) {
            return true;
        }
        if (!(pObj instanceof ActivityModelKey))
            return false;
        ActivityModelKey key = (ActivityModelKey) pObj;
        return ((key.workflowModelId.equals(workflowModelId)) && (key.workspaceId.equals(workspaceId)) && (key.step==step));
    }
    
    @Override
    public String toString() {
        return workspaceId + "-" + workflowModelId + "-" + step;
    }
    
    public String getWorkspaceId() {
        return workspaceId;
    }
    
    public void setWorkspaceId(String pWorkspaceId) {
        workspaceId = pWorkspaceId;
    }
    
    public String getWorkflowModelId() {
        return workflowModelId;
    }
    
    public void setWorkflowModelId(String pWorkflowModelId) {
        workflowModelId = pWorkflowModelId;
    }
    
    public int getStep(){
        return step;
    }
    
    public void setStep(int pStep){
        step=pStep;
    }
}
