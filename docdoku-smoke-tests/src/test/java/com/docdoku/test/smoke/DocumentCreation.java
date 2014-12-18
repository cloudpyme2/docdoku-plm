/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2014 DocDoku SARL
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

package com.docdoku.test.smoke;


import com.docdoku.cli.tools.ScriptingTools;
import com.docdoku.core.services.IDocumentManagerWS;
import com.sun.enterprise.security.ee.auth.login.ProgrammaticLogin;

import static org.junit.Assert.assertNotNull;


/**
 * @author Asmae Chadid
 *
 */
public class DocumentCreation {

    private static final String DOCUMENT_ID = "Test-Document";
    private static final String FOLDER_NAME = "test";
    private SmokeTestProperties properties = new SmokeTestProperties();
    private ProgrammaticLogin loginP = new ProgrammaticLogin();
    private String password = "password";

    public void createDocument() throws Exception {
        loginP.login("demo", password.toCharArray());
        IDocumentManagerWS documentS = ScriptingTools.createDocumentService(properties.getURL(),properties.getLoginForUser1(),properties.getPassword());
        assertNotNull(documentS);
        documentS.createFolder(properties.getWorkspace(), FOLDER_NAME);
        documentS.createDocumentMaster(properties.getWorkspace() + "/" + FOLDER_NAME, DOCUMENT_ID, "", null, null, null, null, null, null);
        documentS.deleteFolder(properties.getWorkspace() + "/" + FOLDER_NAME);
    }


    public void deleteDocument() throws Exception {
        IDocumentManagerWS documentS = ScriptingTools.createDocumentService(properties.getURL(), properties.getLoginForUser1(), properties.getPassword());
        for (String oldFolder : documentS.getFolders(properties.getWorkspace())) {
            if (oldFolder.equals(FOLDER_NAME)) {
                documentS.deleteFolder(properties.getWorkspace() + "/" + FOLDER_NAME);
            }
        }
    }




}