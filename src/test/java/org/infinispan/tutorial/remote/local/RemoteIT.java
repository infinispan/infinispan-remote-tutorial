package org.infinispan.tutorial.remote.local;

import org.infinispan.arquillian.core.InfinispanResource;
import org.infinispan.arquillian.core.RemoteInfinispanServer;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(Arquillian.class)
public class RemoteIT {

   // Container defined in arquillian.xml
   @InfinispanResource("container-default")
   RemoteInfinispanServer server1;

   @Test
   public void remoteCacheReadingWriting() {
      // Construct configuration to connect to running server
      ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
      configurationBuilder.addServer()
            .host(server1.getHotrodEndpoint().getInetAddress().getHostName())
            .port(server1.getHotrodEndpoint().getPort());

      // Create a remote cache manager with built configuration
      RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configurationBuilder.build());

      try {
         // Obtain the default cache
         RemoteCache<String, String> remoteCache = remoteCacheManager.getCache();

         // Insert an entry into the cache
         remoteCache.put("key", "value");

         // Read an entry from the cache
         String value = remoteCache.get("key");

         // Print it out
         System.out.printf("key ==> %s\n", value);

         // Assert the value is the expected one
         Assert.assertEquals("value", value);
      } finally {
         // Release connection
         remoteCacheManager.stop();
      }
   }

   @Test
   public void remoteCacheWritingMortal() throws InterruptedException {
      // Construct configuration to connect to running server
      ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
      configurationBuilder.addServer()
            .host(server1.getHotrodEndpoint().getInetAddress().getHostName())
            .port(server1.getHotrodEndpoint().getPort());

      // Create a remote cache manager with built configuration
      RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configurationBuilder.build());

      try {
         // Obtain the default cache
         RemoteCache<String, String> remoteCache = remoteCacheManager.getCache();

         // Insert a mortal entry, i.e. an entry which will expire
         remoteCache.put("mortal", "I am going to die", 1, TimeUnit.SECONDS);

         // Read mortal entry immediately from the remote cache
         String mortalValue = remoteCache.get("mortal");

         // Print it out and assert value
         System.out.printf("mortal ==> %s\n", mortalValue);
         Assert.assertEquals("I am going to die", mortalValue);

         // Let's wait a couple of seconds
         System.out.println("Sleeping for 2 seconds...");
         Thread.sleep(TimeUnit.SECONDS.toMillis(2));

         // Print it out again and assert. It will have died
         String mortalNonExist = remoteCache.get("mortal");
         System.out.printf("mortal ==> %s\n", mortalNonExist);
         Assert.assertNull(mortalNonExist);
      } finally {
         // Release connection
         remoteCacheManager.stop();
      }
   }

}
