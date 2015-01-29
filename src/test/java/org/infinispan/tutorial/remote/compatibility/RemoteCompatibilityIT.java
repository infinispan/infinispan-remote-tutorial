package org.infinispan.tutorial.remote.compatibility;

import org.infinispan.arquillian.core.InfinispanResource;
import org.infinispan.arquillian.core.RemoteInfinispanServer;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;
import java.util.Set;

import static org.infinispan.tutorial.remote.util.JdkHttpClient.*;

@RunWith(Arquillian.class)
public class RemoteCompatibilityIT {

   static final String CONTAINER = "container-compatibility";

   // Container defined in arquillian.xml
   @InfinispanResource(CONTAINER)
   RemoteInfinispanServer server1;

   @Test
   public void remoteCompatibilityReadWrite() {
      Params params;

      ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
      configurationBuilder.addServer()
            .host(server1.getHotrodEndpoint().getInetAddress().getHostName())
            .port(server1.getHotrodEndpoint().getPort());

      RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configurationBuilder.build());

      try {
         RemoteCache<String, StockValue> remoteCache = remoteCacheManager.getCache();

         // Insert multiple stock entries using remote cache API
         StockValue rhtPrice = new StockValue(72.1f);
         remoteCache.put("NYSE:RHT", rhtPrice);
         StockValue abbPrice = new StockValue(32.5f);
         remoteCache.put("SIX:ABB", abbPrice);
         StockValue telPrice = new StockValue(14.3f);
         remoteCache.put("IBEX:TEL", telPrice);

         // Read individual stocks using the REST HTTP API
         params = Params.apply(Keys.ACCEPT, "application/x-java-serialized-object");
         Assert.assertEquals(rhtPrice, get(cacheKeyUri("NYSE:RHT"), params.map()).get(Keys.BODY));
         Assert.assertEquals(abbPrice, get(cacheKeyUri("SIX:ABB"), params.map()).get(Keys.BODY));
         Assert.assertEquals(telPrice, get(cacheKeyUri("IBEX:TEL"), params.map()).get(Keys.BODY));

         // Insert new values for stocks using REST HTTP API
         StockValue rhtUpdate = new StockValue(70.9f);
         params = Params.apply(Keys.BODY, rhtUpdate).add(Keys.CONTENT_TYPE, "application/x-java-serialized-object");
         Assert.assertEquals(200, put(cacheKeyUri("NYSE:RHT"), params.map()).get(Keys.STATUS_CODE));
         StockValue abbUpdate = new StockValue(33.2f);
         params = Params.apply(Keys.BODY, abbUpdate).add(Keys.CONTENT_TYPE, "application/x-java-serialized-object");
         Assert.assertEquals(200, put(cacheKeyUri("SIX:ABB"), params.map()).get(Keys.STATUS_CODE));
         StockValue telUpdate = new StockValue(14.3f);
         params = Params.apply(Keys.BODY, telUpdate).add(Keys.CONTENT_TYPE, "application/x-java-serialized-object");
         Assert.assertEquals(200, put(cacheKeyUri("IBEX:TEL"), params.map()).get(Keys.STATUS_CODE));

         // Read all stocks using remote cache API
         Set<String> symbols = remoteCache.keySet();
         for (String symbol : symbols) {
            switch (symbol) {
               case "NYSE:RHT":
                  Assert.assertEquals(rhtUpdate, remoteCache.get("NYSE:RHT"));
                  break;
               case "SIX:ABB":
                  Assert.assertEquals(abbUpdate, remoteCache.get("SIX:ABB"));
                  break;
               case "IBEX:TEL":
                  Assert.assertEquals(telUpdate, remoteCache.get("IBEX:TEL"));
                  break;
            }
         }
      } finally {
         // Release connection
         remoteCacheManager.stop();
      }
   }

   URI cacheKeyUri(String key) {
      return uri(server1.getRESTEndpoint().getInetAddress().getHostName(), 8080,
            String.format("/rest/___defaultcache/%s", key));
   }

}