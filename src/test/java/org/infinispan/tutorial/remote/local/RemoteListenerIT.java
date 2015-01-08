package org.infinispan.tutorial.remote.local;

import org.infinispan.arquillian.core.InfinispanResource;
import org.infinispan.arquillian.core.RemoteInfinispanServer;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class RemoteListenerIT {

   // Container defined in arquillian.xml
   @InfinispanResource("container-default")
   RemoteInfinispanServer server1;

   @Test
   public void remoteCacheEvents() {
      // Construct configuration to connect to running server
      ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
      configurationBuilder.addServer()
            .host(server1.getHotrodEndpoint().getInetAddress().getHostName())
            .port(server1.getHotrodEndpoint().getPort());

      // Create a remote cache manager with built configuration
      RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configurationBuilder.build());

      // Obtain the default cache
      RemoteCache<Integer, String> remoteCache = remoteCacheManager.getCache();

      // Instantiate the listener
      EventPrintListener listener = new EventPrintListener();
      try {
         // Add remote listener
         remoteCache.addClientListener(listener);

         // Store a new entry, modify it and remote it
         // Each operation should result in an event
         remoteCache.put(1, "one");
         remoteCache.put(1, "new-one");
         remoteCache.remove(1);
      } finally {
         // Remove added listener
         remoteCache.removeClientListener(listener);

         // Release connection
         remoteCacheManager.stop();
      }
   }

   // A remote listener definition with methods annotated for the events that can be received
   @ClientListener
   public class EventPrintListener<K> {
      @ClientCacheEntryCreated
      public void createdEntry(ClientCacheEntryCreatedEvent<K> event) {
         System.out.printf("** Key '%s' was created\n", event.getKey());
      }

      @ClientCacheEntryModified
      public void modifiedEntry(ClientCacheEntryModifiedEvent<K> event) {
         System.out.printf("** Key '%s' was modified\n", event.getKey());
      }

      @ClientCacheEntryRemoved
      public void removedEntry(ClientCacheEntryRemovedEvent<K> event) {
         System.out.printf("** Key '%s' was removed\n", event.getKey());
      }
   }

}