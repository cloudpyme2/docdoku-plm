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
package com.docdoku.server;

import com.docdoku.core.common.User;
import com.docdoku.core.document.DocumentIteration;
import com.docdoku.core.document.DocumentMaster;
import com.docdoku.core.services.IUserManagerLocal;
import com.docdoku.core.services.NotAllowedException;
import com.docdoku.core.workflow.Task;
import com.docdoku.core.workflow.TaskKey;
import com.docdoku.core.workflow.Workflow;
import com.docdoku.server.dao.TaskDAO;
import com.docdoku.server.dao.WorkflowDAO;
import java.util.Locale;
import javax.ejb.EJB;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@CheckActivity
@Interceptor
public class ActivityCheckerInterceptor {

    @EJB
    private IUserManagerLocal userManager;
    @PersistenceContext
    private EntityManager em;

    @AroundInvoke
    public Object check(InvocationContext ctx) throws Exception {    
        String workspaceId = (String) ctx.getParameters()[0];
        TaskKey taskKey = (TaskKey) ctx.getParameters()[1];
                
        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        Task task = new TaskDAO(new Locale(user.getLanguage()), em).loadTask(taskKey);
        Workflow workflow = task.getActivity().getWorkflow();
        DocumentMaster docM = new WorkflowDAO(em).getTarget(workflow);
        DocumentIteration doc = docM.getLastIteration();
        if (em.createNamedQuery("findLogByDocumentAndUserAndEvent").
                setParameter("userLogin", user.getLogin()).
                setParameter("documentWorkspaceId", doc.getWorkspaceId()).
                setParameter("documentId", doc.getDocumentMasterId()).
                setParameter("documentVersion", doc.getDocumentMasterVersion()).
                setParameter("documentIteration", doc.getIteration()).
                setParameter("event", "DOWNLOAD").
                getResultList().isEmpty()) {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException10");
        }

        return ctx.proceed();
    }
}
