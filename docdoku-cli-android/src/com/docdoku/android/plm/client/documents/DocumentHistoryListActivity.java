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

package com.docdoku.android.plm.client.documents;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import com.docdoku.android.plm.client.R;
import com.docdoku.android.plm.network.HttpGetTask;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author: martindevillers
 */
public class DocumentHistoryListActivity extends DocumentListActivity implements LoaderManager.LoaderCallbacks<Document>{

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        removeLoadingView();

        Log.i("com.docdoku.android.plm.client", "navigation history size: " + navigationHistory.getSize());
        documentArray= new ArrayList<Document>();
        documentAdapter = new DocumentAdapter(documentArray);
        documentListView.setAdapter(documentAdapter);

        Iterator<String> iterator = navigationHistory.getKeyIterator();
        int i = 0;
        while (iterator.hasNext()){
            Bundle bundle = new Bundle();
            bundle.putString("partKey", iterator.next());
            bundle.putString("workspace", getCurrentWorkspace());
            documentArray.add(null);
            getSupportLoaderManager().initLoader(i, bundle, this);
            i++;
        }
        Log.i("com.docdoku.android.plm.client", "Document history list size : " + documentArray.size());
    }

    /**
     * LoaderManager.LoaderCallbacks methods
     */

    @Override
    public Loader<Document> onCreateLoader(int id, Bundle bundle) {
        Log.i("com.docdoku.android.plm.client", "Querying information for part in history at position " + id + " with reference " + bundle.getString("partKey"));
        return new DocumentLoaderByDocument(this, bundle.getString("partKey"), bundle.getString("workspace"));
    }

    @Override
    public void onLoadFinished(Loader<Document> loader, Document document) {
        try{
            Log.i("com.docdoku.android.plm.client", "Received information for part in history at position " + loader.getId() + " with reference " + document.getIdentification());
            documentArray.set(loader.getId(), document);
            documentAdapter.notifyDataSetChanged();
        } catch (NullPointerException e){
            e.printStackTrace();
            Log.i("com.docdoku.android.plm.client", "Load of a document in history failed");
        }
    }

    @Override
    public void onLoaderReset(Loader<Document> loader) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected int getActivityButtonId() {
        return R.id.recentlyViewedDocuments;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private static class DocumentLoaderByDocument extends Loader<Document> implements HttpGetTask.HttpGetListener {

        private String elementId;
        private String workspace;
        private AsyncTask asyncTask;

        public DocumentLoaderByDocument(Context context, String elementId, String workspace) {
            super(context);
            this.elementId = elementId;
            this.workspace = workspace;
        }

        @Override
        protected void onStartLoading (){
            asyncTask = new HttpGetTask(this).execute("api/workspaces/" + workspace + "/documents/" +  elementId);
        }

        @Override
        protected void onStopLoading (){
            if (asyncTask != null){
                asyncTask.cancel(false);
            }
        }

        @Override
        protected void onReset (){
            if (asyncTask != null){
                asyncTask.cancel(false);
            }
            asyncTask = new HttpGetTask(this).execute("api/workspaces/" + workspace + "/documents/" +  elementId);
        }

        @Override
        protected void onForceLoad (){
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        protected void onAbandon (){
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onHttpGetResult(String result) {
            Document document = null;
            try {
                JSONObject documentJSON = new JSONObject(result);
                document = new Document(documentJSON.getString("id"));
                document.updateFromJSON(documentJSON, getContext().getResources());
            }catch (JSONException e){
                Log.e("docdoku.DocDokuPLM", "Error handling json object of a document");
                e.printStackTrace();
                Log.i("docdoku.DocDokuPLM", "Error message: " + e.getMessage());
            }
            deliverResult(document);
        }
    }
}