package com.treasure_data.client.bulkimport;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.simple.JSONValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.msgpack.MessagePack;

import com.treasure_data.auth.TreasureDataCredentials;
import com.treasure_data.client.Config;
import com.treasure_data.client.PostMethodTestUtil;
import com.treasure_data.client.TreasureDataClient;
import com.treasure_data.model.bulkimport.CommitSessionRequest;
import com.treasure_data.model.bulkimport.CommitSessionResult;
import com.treasure_data.model.bulkimport.Session;

public class TestCommitSession extends
        PostMethodTestUtil<CommitSessionRequest, CommitSessionResult, BulkImportClientAdaptorImpl> {

    @Test @Ignore
    public void test00() throws Exception {
        Properties props = System.getProperties();
        props.load(this.getClass().getClassLoader().getResourceAsStream("treasure-data.properties"));
        TreasureDataClient client = new TreasureDataClient(
                new TreasureDataCredentials(), props);
        BulkImportClient biclient = new BulkImportClient(client);

        MessagePack msgpack = new MessagePack();
        long baseTime = System.currentTimeMillis() / 1000;


        String sessionName = "sess01";
        String databaseName = "mugadb";
        String tableName = "test04";
        Session sess = null;
        try { // create
            {
//                CreateSessionRequest request = new CreateSessionRequest(sessionName, databaseName, tableName);
//                CreateSessionResult result = biclient.createSession(request);
//                sess = result.getSession();
//                System.out.println(sess);
            }

            { // upload
//                List<String> parts = new ArrayList<String>();
//                parts.add("01d");
//                parts.add("02d");
//                parts.add("03d");
//
//                for (int i = 0; i < parts.size(); i++) {
//                    ByteArrayOutputStream out = new ByteArrayOutputStream();
//                    GZIPOutputStream gzout = new GZIPOutputStream(out);
//                    Packer pk = msgpack.createPacker(gzout);
//                    Map<String, Object> src = new HashMap<String, Object>();
//                    src.put("k", i);
//                    src.put("v", "muga:" + i);
//                    src.put("time", baseTime + 3600 * i);
//                    pk.write(src);
//                    gzout.finish();
//                    gzout.close();
//                    byte[] bytes = out.toByteArray();
//
//                    UploadPartRequest request = new UploadPartRequest(sess, parts.get(i), bytes);
//                    UploadPartResult result = biclient.uploadPart(request);
//                }
            }

            { // freeze
//                sess = new Session(sessionName, databaseName, tableName);
//                FreezeSessionRequest request = new FreezeSessionRequest(sess);
//                FreezeSessionResult result = biclient.freezeSession(request);
            }

            { // perform
//                sess = new Session(sessionName, databaseName, tableName);
//                PerformSessionRequest request = new PerformSessionRequest(sess);
//                PerformSessionResult result = biclient.performSession(request);
            }

            { // commit
//                sess = new Session(sessionName, databaseName, tableName);
//                CommitSessionRequest request = new CommitSessionRequest(sess);
//                CommitSessionResult result = biclient.commitSession(request);
            }
        } finally {
            // delete
//            sess = new Session(sessionName, databaseName, tableName);
//            DeleteSessionRequest request = new DeleteSessionRequest(sess);
//            DeleteSessionResult result = biclient.deleteSession(request);
//            System.out.println(result.getSessionName());
        }

    }

    private String sessionName;
    private String databaseName;
    private String tableName;
    private CommitSessionRequest request;

    @Override
    public BulkImportClientAdaptorImpl createClientAdaptorImpl(Config conf) {
        Properties props = System.getProperties();
        props.setProperty("td.api.key", "xxxx");
        TreasureDataClient client = new TreasureDataClient(props);
        return new BulkImportClientAdaptorImpl(client);
    }

    @Before
    public void createResources() throws Exception {
        super.createResources();
        sessionName = "testSess";
        databaseName = "testdb";
        tableName = "testtbl";
        request = new CommitSessionRequest(new Session(sessionName,
                databaseName, tableName));
    }

    @After
    public void deleteResources() throws Exception {
        super.deleteResources();
        sessionName = null;
        databaseName = null;
        tableName = null;
        request = null;
    }

    @Override
    public void checkNormalBehavior0() throws Exception {
        CommitSessionResult result = doBusinessLogic();
        assertEquals(sessionName, result.getSession().getName());
    }

    @Override
    public String getJSONTextForChecking() {
        // {"name":"sess01"}
        Map<String, String> map = new HashMap<String, String>();
        map.put("name", sessionName);
        return JSONValue.toJSONString(map);
    }

    @Override
    public CommitSessionResult doBusinessLogic() throws Exception {
        return clientAdaptor.commitSession(request);
    }
}
