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
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilterFactory;
import org.infinispan.notifications.cachelistener.filter.EventType;
import org.infinispan.notifications.cachelistener.filter.NamedFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OverProtocol;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(Arquillian.class)
public class RemoteListenerFilterIT {

   private static final String CONTAINER = "container-default";

   // Container defined in arquillian.xml
   @InfinispanResource(CONTAINER)
   RemoteInfinispanServer server1;

   // Infinispan Servers can be plugged with remote event filters to reduce volume of events
   // These filters are plugged by deploying archives containing them, along with a service definition

   @Deployment(testable = false, name = "example-cache-event-filter-1") // A deployment containing only filter, no tests
   @TargetsContainer(CONTAINER) // Target container
   @OverProtocol("jmx-as7") // Needs to be deployed over JMX (instead of Servlet)
   public static Archive<?> deployEventFilters() {
      // Create jar archive, with filter factory instance and service provider definition
      return ShrinkWrap.create(JavaArchive.class, "example-cache-event-filter.jar")
            .addClasses(ExampleEventFilterFactory.class, ExampleEventFilterFactory.ExampleCacheEventFilter.class)
            .addAsServiceProvider(CacheEventFilterFactory.class, ExampleEventFilterFactory.class);
   }

   @Test
   public void remoteCacheFilterEvents() {
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
      FilterEventPrintListener listener = new FilterEventPrintListener();
      try {
         // Add remote listener, passing keys '1' and '2' as parameters to the filter factory
         // Listener will only receive events for these keys
         remoteCache.addClientListener(listener, new Object[]{1, 2}, null);

         // Work on keys that the client expects to receive events on
         // Each operation should result in an event
         remoteCache.put(1, "one");
         remoteCache.put(1, "new-one");
         remoteCache.remove(1);
         remoteCache.put(2, "two");
         remoteCache.put(2, "new-two");
         remoteCache.remove(2);

         // Work on any other key that's filtered out
         // None of these operations should result on remote events being fired
         remoteCache.put(3, "three");
         remoteCache.put(3, "new-three");
         remoteCache.remove(3);
         remoteCache.put(50, "fifty");
         remoteCache.put(50, "new-fifty");
         remoteCache.remove(50);
      } finally {
         // Remove added listener
         remoteCache.removeClientListener(listener);

         // Release connection
         remoteCacheManager.stop();
      }
   }

   // Remote event filter factories need to be given a name for the client to reference
   @NamedFactory(name = "example-filter-factory")
   public static class ExampleEventFilterFactory implements CacheEventFilterFactory {
      @Override
      public CacheEventFilter<Integer, String> getFilter(Object[] params) {
         // In this example, a filter instance created for each listener added

         // Alternative implementations might return a constant
         // if they do not depend on parameters passed when adding listener

         return new ExampleCacheEventFilter(Arrays.asList(params));
      }

      static class ExampleCacheEventFilter implements CacheEventFilter<Integer, String> {
         private final List<?> acceptedKeys;

         public ExampleCacheEventFilter(List<?> acceptedKeys) {
            this.acceptedKeys = acceptedKeys;
         }

         @Override
         public boolean accept(Integer key, String oldValue, Metadata oldMetadata, String newValue, Metadata newMetadata, EventType eventType) {
            // Only events for the keys passed when adding listener are to be sent to the client
            return acceptedKeys.contains(key);
         }
      }
   }

   // A remote listener definition with methods annotated for the events that can be received
   // Link up client listener with associated filter factory
   @ClientListener(filterFactoryName = "example-filter-factory")
   public class FilterEventPrintListener<K> {
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
