//
// Java Client Library for Treasure Data Cloud
//
// Copyright (C) 2011 - 2013 Muga Nishizawa
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package com.treasure_data.client.bulkimport;

import java.io.EOFException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.simple.JSONValue;
import org.msgpack.unpacker.Unpacker;

import com.treasure_data.client.AbstractClientAdaptor;
import com.treasure_data.client.ClientException;
import com.treasure_data.client.HttpClientException;
import com.treasure_data.client.HttpConnectionImpl;
import com.treasure_data.client.TreasureDataClient;
import com.treasure_data.client.Validator;
import com.treasure_data.model.bulkimport.CommitSessionRequest;
import com.treasure_data.model.bulkimport.CommitSessionResult;
import com.treasure_data.model.bulkimport.CreateSessionRequest;
import com.treasure_data.model.bulkimport.CreateSessionResult;
import com.treasure_data.model.bulkimport.DeletePartRequest;
import com.treasure_data.model.bulkimport.DeletePartResult;
import com.treasure_data.model.bulkimport.DeleteSessionRequest;
import com.treasure_data.model.bulkimport.DeleteSessionResult;
import com.treasure_data.model.bulkimport.FreezeSessionRequest;
import com.treasure_data.model.bulkimport.FreezeSessionResult;
import com.treasure_data.model.bulkimport.GetErrorRecordsRequest;
import com.treasure_data.model.bulkimport.GetErrorRecordsResult;
import com.treasure_data.model.bulkimport.ListPartsRequest;
import com.treasure_data.model.bulkimport.ListPartsResult;
import com.treasure_data.model.bulkimport.ListSessions;
import com.treasure_data.model.bulkimport.ListSessionsRequest;
import com.treasure_data.model.bulkimport.ListSessionsResult;
import com.treasure_data.model.bulkimport.PerformSessionRequest;
import com.treasure_data.model.bulkimport.PerformSessionResult;
import com.treasure_data.model.bulkimport.SessionSummary;
import com.treasure_data.model.bulkimport.UnfreezeSessionRequest;
import com.treasure_data.model.bulkimport.UnfreezeSessionResult;
import com.treasure_data.model.bulkimport.UploadPartRequest;
import com.treasure_data.model.bulkimport.UploadPartResult;

public class BulkImportClientAdaptorImpl extends AbstractClientAdaptor
        implements BulkImportClientAdaptor {
    private static Logger LOG = Logger.getLogger(BulkImportClientAdaptorImpl.class.getName());

    private TreasureDataClient client;

    private Validator validator;

    private HttpConnectionImpl conn;

    BulkImportClientAdaptorImpl(TreasureDataClient client) {
        super(client.getConfig());
        this.client = client;
        validator = new Validator();
    }

    @Override
    public ListSessionsResult listSessions(ListSessionsRequest request)
            throws ClientException {
        request.setCredentials(client.getTreasureDataCredentials());
        validator.validateCredentials(client, request);

        String jsonData = null;
        String message = null;
        int code = 0;
        try {
            conn = createConnection();

            // send request
            String path = HttpURL.V3_LIST;
            Map<String, String> header = null;
            Map<String, String> params = null;
            conn.doGetRequest(request, path, header, params);

            // receive response code and body
            code = conn.getResponseCode();
            message = conn.getResponseMessage();
            if (code != HttpURLConnection.HTTP_OK) {
                String errMessage = conn.getErrorMessage();
                LOG.severe(HttpClientException.toMessage(
                        "List sessions failed", message, code));
                LOG.severe(errMessage);
                throw new HttpClientException("List sessions failed",
                        message + ", detail = " + errMessage, code);
            }

            // receive response body
            jsonData = conn.getResponseBody();
            validator.validateJSONData(jsonData);
        } catch (IOException e) {
            LOG.throwing(getClass().getName(), "listSessions", e);
            LOG.severe(HttpClientException.toMessage(e.getMessage(), message, code));
            throw new HttpClientException("List sessions failed", message, code, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // parse JSON data
        // {"bulk_imports":
        //   [
        //     {"name":"t01", "database":"sfdb", "table":"bi02",
        //      "status":"ready", "upload_frozen":false,
        //      "job_id":"70220", "valid_records":100,
        //      "error_records":10, "valid_parts":2, "error_parts":1},
        //     {"name":"sess01", "database":"mugadb", "table":"test04",
        //      "status":"uploading", "upload_frozen":false,
        //      "job_id":null, "valid_records":null,
        //      "error_records":null, "valid_parts":null, "error_parts":null}
        //   ]}
        @SuppressWarnings("rawtypes")
        Map map = (Map) JSONValue.parse(jsonData);
        validator.validateJavaObject(jsonData, map);

        @SuppressWarnings("unchecked")
        Iterator<Map<String, Object>> sessIter =
            ((List<Map<String, Object>>) map.get("bulk_imports")).iterator();
        List<SessionSummary> sessions = new ArrayList<SessionSummary>();
        while (sessIter.hasNext()) {
            Map<String, Object> sess = sessIter.next();
            String name = (String) sess.get("name");
            String database = (String) sess.get("database");
            String table = (String) sess.get("table");
            String status = (String) sess.get("status");
            boolean upload_frozen = (Boolean) sess.get("upload_frozen");
            String job_id = (String) sess.get("job_id");
            Long vr = (Long) sess.get("valid_records");
            long valid_records = vr != null ? vr : 0;
            Long er = (Long) sess.get("error_records");
            long error_records = er != null ? er : 0;
            Long vp = (Long) sess.get("valid_parts");
            long valid_parts = vp != null ? vp : 0;
            Long ep = (Long) sess.get("error_parts");
            long error_parts = ep != null ? ep : 0;
            SessionSummary summary = new SessionSummary(name, database, table,
                    SessionSummary.Status.fromString(status), upload_frozen, job_id,
                    valid_records, error_records, valid_parts, error_parts);
            sessions.add(summary);
        }

        return new ListSessionsResult(new ListSessions<SessionSummary>(sessions));
    }

    @Override
    public ListPartsResult listParts(ListPartsRequest request)
            throws ClientException {
        request.setCredentials(client.getTreasureDataCredentials());
        validator.validateCredentials(client, request);

        String jsonData = null;
        int code = 0;
        String message = null;
        try {
            conn = createConnection();

            // send request
            String path = String.format(HttpURL.V3_LIST_PARTS,
                    HttpConnectionImpl.e(request.getSessionName()));
            Map<String, String> header = null;
            Map<String, String> params = null;
            conn.doGetRequest(request, path, header, params);

            // receive response code and body
            code = conn.getResponseCode();
            message = conn.getResponseMessage();
            if (code != HttpURLConnection.HTTP_OK) {
                String errMessage = conn.getErrorMessage();
                LOG.severe(HttpClientException.toMessage(
                        "List parts failed", message, code));
                LOG.severe(errMessage);
                throw new HttpClientException("List parts failed",
                        message + ", detail = " + errMessage, code);
            }

            // receive response body
            jsonData = conn.getResponseBody();
            validator.validateJSONData(jsonData);
        } catch (IOException e) {
            LOG.throwing(getClass().getName(), "listParts", e);
            LOG.severe(HttpClientException.toMessage(e.getMessage(), message, code));
            throw new HttpClientException("List parts failed", message, code, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // parse JSON data {"name":"t01","parts":["error01","ok01"]}
        @SuppressWarnings("rawtypes")
        Map map = (Map) JSONValue.parse(jsonData);
        validator.validateJavaObject(jsonData, map);

        @SuppressWarnings("unchecked")
        List<String> parts = (List<String>) map.get("parts");
        return new ListPartsResult(request.getSession(), parts);
    }

    @Override
    public CreateSessionResult createSession(CreateSessionRequest request)
            throws ClientException {
        request.setCredentials(client.getTreasureDataCredentials());
        validator.validateCredentials(client, request);

        String jsonData = null;
        int code = 0;
        String message = null;
        try {
            conn = createConnection();

            // send request
            String path = String.format(HttpURL.V3_CREATE,
                    HttpConnectionImpl.e(request.getSessionName()),
                    HttpConnectionImpl.e(request.getDatabaseName()),
                    HttpConnectionImpl.e(request.getTableName()));
            Map<String, String> header = null;
            Map<String, String> params = null;
            conn.doPostRequest(request, path, header, params);

            // receive response code
            code = conn.getResponseCode();
            message = conn.getResponseMessage();
            if (code != HttpURLConnection.HTTP_OK) {
                String errMessage = conn.getErrorMessage();
                LOG.severe(HttpClientException.toMessage(
                        "Create session failed", message, code));
                LOG.severe(errMessage);
                throw new HttpClientException("Create session failed",
                        message + ", detail = " + errMessage, code);
            }

            // receive response body
            jsonData = conn.getResponseBody();
            validator.validateJSONData(jsonData);
        } catch (IOException e) {
            LOG.throwing(getClass().getName(), "createSession", e);
            LOG.severe(HttpClientException.toMessage(e.getMessage(), message, code));
            throw new HttpClientException("Create session failed", message, code, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // parse JSON data {"name":"sess01"}
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Map map = (Map<String, String>) JSONValue.parse(jsonData);
        validator.validateJavaObject(jsonData, map);

        return new CreateSessionResult(request.getSession());
    }

    @Override
    public UploadPartResult uploadPart(UploadPartRequest request)
            throws ClientException {
        request.setCredentials(client.getTreasureDataCredentials());
        validator.validateCredentials(client, request);

        String jsonData = null;
        int code = 0;
        String message = null;
        try {
            conn = createConnection();

            // send request
            String path = String.format(HttpURL.V3_UPLOAD_PART,
                    HttpConnectionImpl.e(request.getSessionName()),
                    HttpConnectionImpl.e(request.getPartID()));
            conn.doPutRequest(request, path, request.getInputStream(), request.getSize());

            // receive response code
            code = conn.getResponseCode();
            message = conn.getResponseMessage();
            if (code != HttpURLConnection.HTTP_OK) {
                String errMessage = conn.getErrorMessage();
                LOG.severe(HttpClientException.toMessage(
                        "Upload part failed", message, code));
                LOG.severe(errMessage);
                throw new HttpClientException("Upload part failed",
                        message + ", detail = " + errMessage, code);
            }

            // receive response body
            jsonData = conn.getResponseBody();
            validator.validateJSONData(jsonData);
        } catch (IOException e) {
            LOG.throwing(getClass().getName(), "uploadPart", e);
            LOG.severe(HttpClientException.toMessage(e.getMessage(), message, code));
            throw new HttpClientException("Upload part failed", message, code, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // parse JSON data {"name":"sess01"}
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) JSONValue.parse(jsonData);
        validator.validateJavaObject(jsonData, map);

        return new UploadPartResult(request.getSession());
    }

    @Override
    public DeletePartResult deletePart(DeletePartRequest request)
            throws ClientException {
        request.setCredentials(client.getTreasureDataCredentials());
        validator.validateCredentials(client, request);

        String jsonData = null;
        int code = 0;
        String message = null;
        try {
            conn = createConnection();

            // send request
            String path = String.format(HttpURL.V3_DELETE_PART,
                    HttpConnectionImpl.e(request.getSessionName()),
                    HttpConnectionImpl.e(request.getPartID()));
            Map<String, String> header = null;
            Map<String, String> params = null;
            conn.doPostRequest(request, path, header, params);

            // receive response code
            code = conn.getResponseCode();
            message = conn.getResponseMessage();
            if (code != HttpURLConnection.HTTP_OK) {
                String errMessage = conn.getErrorMessage();
                LOG.severe(HttpClientException.toMessage(
                        "Delete part failed", message, code));
                LOG.severe(errMessage);
                throw new HttpClientException("Delete part failed",
                        message + ", detail = " + errMessage, code);
            }

            // receive response body
            jsonData = conn.getResponseBody();
            validator.validateJSONData(jsonData);
        } catch (IOException e) {
            LOG.throwing(getClass().getName(), "deletePart", e);
            LOG.severe(HttpClientException.toMessage(e.getMessage(), message, code));
            throw new HttpClientException("Delete part failed", message, code, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // parse JSON data {"name":"sess01"}
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) JSONValue.parse(jsonData);
        validator.validateJavaObject(jsonData, map);

        return new DeletePartResult(request.getSession());
    }

    @Override
    public PerformSessionResult performSession(PerformSessionRequest request)
            throws ClientException {
        request.setCredentials(client.getTreasureDataCredentials());
        validator.validateCredentials(client, request);

        String jsonData = null;
        int code = 0;
        String message = null;
        try {
            conn = createConnection();

            // send request
            String path = String.format(HttpURL.V3_PERFORM,
                    HttpConnectionImpl.e(request.getSessionName()));
            Map<String, String> header = null;
            Map<String, String> params = null;
            conn.doPostRequest(request, path, header, params);

            // receive response code
            code = conn.getResponseCode();
            message = conn.getResponseMessage();
            if (code != HttpURLConnection.HTTP_OK) {
                String errMessage = conn.getErrorMessage();
                LOG.severe(HttpClientException.toMessage(
                        "Perform session failed", message, code));
                LOG.severe(errMessage);
                throw new HttpClientException("Perform session failed",
                        message + ", detail = " + errMessage, code);
            }

            // receive response body
            jsonData = conn.getResponseBody();
            validator.validateJSONData(jsonData);
        } catch (IOException e) {
            LOG.throwing(getClass().getName(), "performSession", e);
            LOG.severe(HttpClientException.toMessage(e.getMessage(), message, code));
            throw new HttpClientException("Perform session failed", message, code, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // parse JSON data {"name":"sess01","job_id":"127949"}
        @SuppressWarnings({ "rawtypes" })
        Map map = (Map) JSONValue.parse(jsonData);
        validator.validateJavaObject(jsonData, map);

        return new PerformSessionResult(request.getSession());
    }

    @Override
    public GetErrorRecordsResult getErrorRecords(GetErrorRecordsRequest request)
            throws ClientException {
        request.setCredentials(client.getTreasureDataCredentials());
        validator.validateCredentials(client, request);

        Unpacker unpacker = null;
        int code = 0;
        String message = null;
        try {
            conn = createConnection();

            // send request
            String path = String.format(HttpURL.V3_ERROR_RECORDS,
                    HttpConnectionImpl.e(request.getSessionName()));
            Map<String, String> header = null;
            Map<String, String> params = null;
            conn.doGetRequest(request, path, header, params);

            // receive response code and body
            code = conn.getResponseCode();
            message = conn.getResponseMessage();
            if (code != HttpURLConnection.HTTP_OK) {
                String errMessage = conn.getErrorMessage();
                LOG.severe(HttpClientException.toMessage(
                        "Get error_records failed", message, code));
                LOG.severe(errMessage);
                throw new HttpClientException("Get error_records failed",
                        message + ", detail = " + errMessage, code);
            }

            // receive response body
            try {
                unpacker = conn.getResponseBodyBinary();
            } catch (EOFException e) {
                // ignore
            }
        } catch (IOException e) {
            LOG.throwing(getClass().getName(), "getErrorRecords", e);
            LOG.severe(HttpClientException.toMessage(e.getMessage(), message, code));
            throw new HttpClientException("Get error_records failed", message, code, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return new GetErrorRecordsResult(request.getSession(), unpacker);
    }

    @Override
    public CommitSessionResult commitSession(CommitSessionRequest request)
            throws ClientException {
        request.setCredentials(client.getTreasureDataCredentials());
        validator.validateCredentials(client, request);

        String jsonData = null;
        int code = 0;
        String message = null;
        try {
            conn = createConnection();

            // send request
            String path = String.format(HttpURL.V3_COMMIT,
                    HttpConnectionImpl.e(request.getSessionName()));
            Map<String, String> header = null;
            Map<String, String> params = null;
            conn.doPostRequest(request, path, header, params);

            // receive response code
            code = conn.getResponseCode();
            message = conn.getResponseMessage();
            if (code != HttpURLConnection.HTTP_OK) {
                String errMessage = conn.getErrorMessage();
                LOG.severe(HttpClientException.toMessage(
                        "Commit session failed", message, code));
                LOG.severe(errMessage);
                throw new HttpClientException("Commit session failed",
                        message + ", detail = " + errMessage, code);
            }

            // receive response body
            jsonData = conn.getResponseBody();
            validator.validateJSONData(jsonData);
        } catch (IOException e) {
            LOG.throwing(getClass().getName(), "commitSession", e);
            LOG.severe(HttpClientException.toMessage(e.getMessage(), message, code));
            throw new HttpClientException("Commit session failed", message, code, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // parse JSON data {"name":"sess01"}
        @SuppressWarnings({ "rawtypes" })
        Map map = (Map) JSONValue.parse(jsonData);
        validator.validateJavaObject(jsonData, map);

        return new CommitSessionResult(request.getSession());
    }

    @Override
    public DeleteSessionResult deleteSession(DeleteSessionRequest request)
            throws ClientException {
        request.setCredentials(client.getTreasureDataCredentials());
        validator.validateCredentials(client, request);

        String jsonData = null;
        int code = 0;
        String message = null;
        try {
            conn = createConnection();

            // send request
            String path = String.format(HttpURL.V3_DELETE,
                    HttpConnectionImpl.e(request.getSessionName()));
            Map<String, String> header = null;
            Map<String, String> params = null;
            conn.doPostRequest(request, path, header, params);

            // receive response code
            code = conn.getResponseCode();
            message = conn.getResponseMessage();
            if (code != HttpURLConnection.HTTP_OK) {
                String errMessage = conn.getErrorMessage();
                LOG.severe(HttpClientException.toMessage(
                        "Delete session failed", message, code));
                LOG.severe(errMessage);
                throw new HttpClientException("Delete session failed",
                        message + ", detail = " + errMessage, code);
            }

            // receive response body
            jsonData = conn.getResponseBody();
            validator.validateJSONData(jsonData);
        } catch (IOException e) {
            LOG.throwing(getClass().getName(), "deleteSession", e);
            LOG.severe(HttpClientException.toMessage(e.getMessage(), message, code));
            throw new HttpClientException("Delete session failed", message, code, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // parse JSON data {"name":"sess01"}
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) JSONValue.parse(jsonData);
        validator.validateJavaObject(jsonData, map);

        return new DeleteSessionResult(request.getSession());
    }

    @Override
    public FreezeSessionResult freezeSession(FreezeSessionRequest request)
            throws ClientException {
        request.setCredentials(client.getTreasureDataCredentials());
        validator.validateCredentials(client, request);

        String jsonData = null;
        int code = 0;
        String message = null;
        try {
            conn = createConnection();

            // send request
            String path = String.format(HttpURL.V3_FREEZE,
                    HttpConnectionImpl.e(request.getSessionName()));
            Map<String, String> header = null;
            Map<String, String> params = null;
            conn.doPostRequest(request, path, header, params);

            // receive response code
            code = conn.getResponseCode();
            message = conn.getResponseMessage();
            if (code != HttpURLConnection.HTTP_OK) {
                String errMessage = conn.getErrorMessage();
                LOG.severe(HttpClientException.toMessage(
                        "Freeze session failed", message, code));
                LOG.severe(errMessage);
                throw new HttpClientException("Freeze session failed",
                        message + ", detail = " + errMessage, code);
            }

            // receive response body
            jsonData = conn.getResponseBody();
            validator.validateJSONData(jsonData);
        } catch (IOException e) {
            LOG.throwing(getClass().getName(), "freezeSession", e);
            LOG.severe(HttpClientException.toMessage(e.getMessage(), message, code));
            throw new HttpClientException("Freeze session failed", message, code, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // parse JSON data {"name":"sess01"}
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) JSONValue.parse(jsonData);
        validator.validateJavaObject(jsonData, map);

        return new FreezeSessionResult(request.getSession());
    }

    @Override
    public UnfreezeSessionResult unfreezeSession(UnfreezeSessionRequest request)
            throws ClientException {
        request.setCredentials(client.getTreasureDataCredentials());
        validator.validateCredentials(client, request);

        String jsonData = null;
        int code = 0;
        String message = null;
        try {
            conn = createConnection();

            // send request
            String path = String.format(HttpURL.V3_UNFREEZE,
                    HttpConnectionImpl.e(request.getSessionName()));
            Map<String, String> header = null;
            Map<String, String> params = null;
            conn.doPostRequest(request, path, header, params);

            // receive response code
            code = conn.getResponseCode();
            message = conn.getResponseMessage();
            if (code != HttpURLConnection.HTTP_OK) {
                String errMessage = conn.getErrorMessage();
                LOG.severe(HttpClientException.toMessage(
                        "Unfreeze session failed", message, code));
                LOG.severe(errMessage);
                throw new HttpClientException("Unfreeze session failed",
                        message + ", detail = " + errMessage, code);
            }

            // receive response body
            jsonData = conn.getResponseBody();
            validator.validateJSONData(jsonData);
        } catch (IOException e) {
            LOG.throwing(getClass().getName(), "unfreezeSession", e);
            LOG.severe(HttpClientException.toMessage(e.getMessage(), message, code));
            throw new HttpClientException("Unfreeze session failed", message, code, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // parse JSON data {"name":"sess01"}
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) JSONValue.parse(jsonData);
        validator.validateJavaObject(jsonData, map);

        return new UnfreezeSessionResult(request.getSession());
    }

    static interface HttpURL {
        String V3_LIST = "/v3/bulk_import/list";

        String V3_CREATE = "/v3/bulk_import/create/%s/%s/%s";

        String V3_DELETE = "/v3/bulk_import/delete/%s";

        String V3_UPLOAD_PART = "/v3/bulk_import/upload_part/%s/%s";

        String V3_DELETE_PART = "/v3/bulk_import/delete_part/%s/%s";

        String V3_LIST_PARTS = "/v3/bulk_import/list_parts/%s";

        String V3_FREEZE = "/v3/bulk_import/freeze/%s";

        String V3_UNFREEZE = "/v3/bulk_import/unfreeze/%s";

        String V3_PERFORM = "/v3/bulk_import/perform/%s";

        String V3_COMMIT = "/v3/bulk_import/commit/%s";

        String V3_ERROR_RECORDS = "/v3/bulk_import/error_records/%s";

        String V3_PERFORM_FINISHED = "/v3/bulk_import/perform_finished/%s";

        String V3_COMMIT_FINISHED = "/v3/bulk_import/commit_finished/%s";
    }
}
