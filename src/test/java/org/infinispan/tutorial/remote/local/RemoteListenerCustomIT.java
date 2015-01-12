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
import org.infinispan.client.hotrod.event.ClientCacheEntryCustomEvent;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverterFactory;
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

import java.io.Serializable;

@RunWith(Arquillian.class)
public class RemoteListenerCustomIT {

   private static final String CONTAINER = "container-default";

   // Container defined in arquillian.xml
   @InfinispanResource(CONTAINER)
   RemoteInfinispanServer server1;

   // Infinispan Servers can be plugged with remote event converters to customize events
   // These converters are plugged by deploying archives containing them, along with a service definition

   @Deployment(testable = false, name = "example-cache-event-converter-1") // A deployment containing only converter, no tests
   @TargetsContainer(CONTAINER) // Target container
   @OverProtocol("jmx-as7") // Needs to be deployed over JMX (instead of Servlet)
   public static Archive<?> deployEventFilters() {
      // Create jar archive, with converter factory instance and service provider definition
      return ShrinkWrap.create(JavaArchive.class, "example-cache-event-converter.jar")
            .addClasses(ValueAddConverterFactory.class)
            .addAsServiceProvider(CacheEventConverterFactory.class, ValueAddConverterFactory.class);
   }

   @Test
   public void remoteCacheCustomEvents() {
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
      FilterEventPrintListener<ValueAddEvent> listener = new FilterEventPrintListener<>();
      try {
         // Add remote listener
         remoteCache.addClientListener(listener);

         // Work on some keys and check output to see both and key values being printed
         // Each operation should result in an event
         remoteCache.put(1, "one");
         remoteCache.put(1, "new-one");
         remoteCache.remove(1);
         remoteCache.put(99, "ninetynine");
         remoteCache.put(99, "new-ninetynine");
         remoteCache.remove(99);
      } finally {
         // Remove added listener
         remoteCache.removeClientListener(listener);

         // Release connection
         remoteCacheManager.stop();
      }
   }

   // Default remote events do not contain value information, so in this
   // converter example, a custom event is created with the value so that
   // it can be sent to clients
   @NamedFactory(name = "value-add-converter-factory")
   public static class ValueAddConverterFactory implements CacheEventConverterFactory {
      public CacheEventConverter<Integer, String, ValueAddEvent> getConverter(final Object[] params) {
         return new CacheEventConverter<Integer, String, ValueAddEvent>() {
            @Override
            public ValueAddEvent convert(Integer key, String oldValue, Metadata oldMetadata,
                  String newValue, Metadata newMetadata, EventType eventType) {
               return new ValueAddEvent(key, newValue);
            }
         };
      }
   }

   // The custom event to send to clients needs to be Serializable or Externalizable
   static class ValueAddEvent implements Serializable {
      final Integer key;
      final String value;
      ValueAddEvent(Integer key, String value) {
         this.key = key;
         this.value = value;
      }

      @Override
      public String toString() {
         return "ValueAddEvent{" + "key=" + key + ", value='" + value + '\'' + '}';
      }
   }

   // A remote listener definition with methods annotated for the events that can be received
   // Link up client listener with associated converter factory
   @ClientListener(converterFactoryName = "value-add-converter-factory")
   public class FilterEventPrintListener<T> {
      // Listeners associated with a converter receive a custom event
      // instance as callback, regardless of the event type, containing
      // the custom event sent from the server

      @ClientCacheEntryCreated
      public void createdEntry(ClientCacheEntryCustomEvent<T> event) {
         System.out.printf("** '%s' was created\n", event.getEventData());
      }

      @ClientCacheEntryModified
      public void modifiedEntry(ClientCacheEntryCustomEvent<T> event) {
         System.out.printf("** '%s' was modified\n", event.getEventData());
      }

      @ClientCacheEntryRemoved
      public void removedEntry(ClientCacheEntryCustomEvent<T> event) {
         System.out.printf("** '%s' was removed\n", event.getEventData());
      }
   }

}
