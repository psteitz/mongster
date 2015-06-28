/* 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongster;

/**
 * Config for a Mongster server.
 */
public class MongsterConfig {
    public static final String DEFAULT_MONGO_HOST = "localhost";
    public static final String DEFAULT_MONGO_DATABASE = "mongster";
    public static final String DEFAULT_MONGO_COLLECTION = "messages";
    public static final int DEFAULT_MONGO_PORT = 27017;
    public static final int DEFAULT_SMTP_PORT = 25;
    
    private boolean inMemory = true; 
    private String mongoHost = DEFAULT_MONGO_HOST; 
    private int mongoPort = DEFAULT_MONGO_PORT ;
    private String mongoDatabase = DEFAULT_MONGO_DATABASE; 
    private String mongoCollection = DEFAULT_MONGO_COLLECTION;
    private int smtpPort = DEFAULT_SMTP_PORT;
    
    public MongsterConfig() {
        super();
    }
    
    public MongsterConfig(boolean inMemory, String mongoHost, int mongoPort,
                          String mongoDatabase, String mongoCollection,
                          int smptPort) {
        super();
        this.inMemory = inMemory;
        this.mongoHost = mongoHost;
        this.mongoPort = mongoPort;
        this.mongoDatabase = mongoDatabase;
        this.mongoCollection = mongoCollection;
        this.smtpPort = smptPort;
    }
    
    public boolean isInMemory() {
        return inMemory;
    }
    
    public void setInMemory(boolean inMemory) {
        this.inMemory = inMemory;
    }
    
    public String getMongoHost() {
        return mongoHost;
    }
    
    public void setMongoHost(String mongoHost) {
        this.mongoHost = mongoHost;
    }
    
    public int getMongoPort() {
        return mongoPort;
    }
    
    public void setMongoPort(int mongoPort) {
        this.mongoPort = mongoPort;
    }
    
    public String getMongoDatabase() {
        return mongoDatabase;
    }
    
    public void setMongoDatabase(String mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }
    
    public String getMongoCollection() {
        return mongoCollection;
    }
    
    public void setMongoCollection(String mongoCollection) {
        this.mongoCollection = mongoCollection;
    }
    
    public int getSmtpPort() {
        return smtpPort;
    }
    
    public void setSmtpPort(int smptPort) {
        this.smtpPort = smptPort;
    }
    
}
