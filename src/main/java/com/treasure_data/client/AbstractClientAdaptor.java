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
package com.treasure_data.client;

import com.treasure_data.auth.TreasureDataCredentials;

public abstract class AbstractClientAdaptor {
    private Config conf;
    protected HttpConnectionImpl conn = null;

    public AbstractClientAdaptor(Config conf) {
	this.conf = conf;
    }

    public Config getConfig() {
	return conf;
    }

    public TreasureDataCredentials getTreasureDataCredentials() {
        return conf.getCredentials();
    }

    public void setTreasureDataCredentials(TreasureDataCredentials credentials) {
        conf.setCredentials(credentials);
    }

    protected HttpConnectionImpl getConnection() {
        return conn;
    }

    protected HttpConnectionImpl createConnection() {
        if (conn == null) {
            conn = new HttpConnectionImpl(getConfig().getProperties());
        }
        return conn;
    }

    protected void setConnection(HttpConnectionImpl conn) {
        this.conn = conn;
    }
}
