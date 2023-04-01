package org.smallmind.mongodb.throng;

import com.mongodb.client.MongoClient;
import org.smallmind.mongodb.utility.MongoWriteConcern;
import org.smallmind.mongodb.utility.MorphiaAcknowledgment;
import org.smallmind.mongodb.utility.spring.MongoClientFactoryBean;
import org.smallmind.mongodb.utility.spring.MongoClientSettingsFactoryBean;
import org.smallmind.mongodb.utility.spring.MongoCredentialFactoryBean;
import org.smallmind.mongodb.utility.spring.MongoServerFactoryBean;

public class Wombat {

  public static void main (String... args)
    throws Exception {

    MongoWriteConcern mongoWriteConcern = new MongoWriteConcern();
    MongoServerFactoryBean mongoServerFactoryBean = new MongoServerFactoryBean();
    MongoCredentialFactoryBean mongoCredentialFactoryBean = new MongoCredentialFactoryBean();
    MongoClientSettingsFactoryBean mongoClientSettingsFactoryBean = new MongoClientSettingsFactoryBean();
    MongoClientFactoryBean mongoClientFactoryBean = new MongoClientFactoryBean();

    mongoWriteConcern.setJournaled(true);
    mongoWriteConcern.setAcknowledgment(MorphiaAcknowledgment.ONE);

    mongoServerFactoryBean.setServerPattern("localhost");
    mongoServerFactoryBean.setServerSpread("");
    mongoServerFactoryBean.afterPropertiesSet();

    mongoCredentialFactoryBean.setUser("root");
    mongoCredentialFactoryBean.setPassword("secret");
    mongoCredentialFactoryBean.afterPropertiesSet();

    mongoClientSettingsFactoryBean.setMongoCredential(mongoCredentialFactoryBean.getObject());
    mongoClientSettingsFactoryBean.setServerAddresses(mongoServerFactoryBean.getObject());
    mongoClientSettingsFactoryBean.setWriteConcern(mongoWriteConcern.getObject());
    mongoClientSettingsFactoryBean.setSslEnabled(false);
    mongoClientSettingsFactoryBean.afterPropertiesSet();

    mongoClientFactoryBean.setClientSettings(mongoClientSettingsFactoryBean.getObject());
    mongoClientFactoryBean.afterPropertiesSet();

    MongoClient mongoClient = mongoClientFactoryBean.getObject();



    mongoClient.getDatabase("").getCollection("");
  }
}
