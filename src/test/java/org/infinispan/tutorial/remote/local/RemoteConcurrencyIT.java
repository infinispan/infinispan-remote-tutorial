package org.infinispan.tutorial.remote.local;

import org.infinispan.arquillian.core.InfinispanResource;
import org.infinispan.arquillian.core.RemoteInfinispanServer;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@RunWith(Arquillian.class)
public class RemoteConcurrencyIT {

   static final int NUM_THREADS = 10;
   static final int OPS_PER_THREAD = 200;
   static final int TIMEOUT_MINUTES = 2;
   static final String KEY = "Counter";

   // Container defined in arquillian.xml
   @InfinispanResource("container-default")
   RemoteInfinispanServer server1;

   @Test
   public void remoteCacheConcurrentCounter() throws Exception {
      // Construct configuration to connect to running server
      ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
      configurationBuilder.addServer()
            .host(server1.getHotrodEndpoint().getInetAddress().getHostName())
            .port(server1.getHotrodEndpoint().getPort());

      // Create a remote cache manager with built configuration
      RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configurationBuilder.build());

      // Set up an executor with the number of concurrent updater threads
      ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

      try {
         // Obtain the default cache
         RemoteCache<String, Integer> remoteCache = remoteCacheManager.getCache();

         // Set up a list with the results of each concurrent updater
         List<Future<Integer>> results = new ArrayList<>(NUM_THREADS);

         // Start counter updaters
         for (int i = 0; i < NUM_THREADS; i++)
            results.add(executor.submit(new CounterUpdater(remoteCache)));

         // Count the number of times clients incremented the counter
         int clientCounts = 0;
         for (Future<Integer> f : results) clientCounts += f.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);

         // Retrieve server side counter value
         int serverCounts = remoteCache.get(KEY);

         // Print both client and server side counters and assert that
         // the client count and the server counter state are equals
         System.out.printf("client side count ==> %s\n", clientCounts);
         System.out.printf("server side count ==> %s\n", serverCounts);
         Assert.assertEquals(clientCounts, serverCounts);
      } finally {
         // Release connection and stop executor
         remoteCacheManager.stop();
         executor.shutdown();
      }
   }

   static class CounterUpdater implements Callable<Integer> {
      final RemoteCache<String, Integer> cache;

      CounterUpdater(RemoteCache<String, Integer> cache) {
         this.cache = cache;
      }

      @Override
      public Integer call() throws Exception {
         int counted = 0;
         while (counted < OPS_PER_THREAD)
            counted = incrementCounter(counted);

         return counted;
      }

      private int incrementCounter(int counted) {
         // Updating a counter atomically requires an endless loop in which
         // compare-and-swap like operations are attempted.
         while (true) {
            // Start by retrieving the value of the counter value,
            // and its version which uniquely identifies the counter state
            VersionedValue<Integer> versioned = cache.getVersioned(KEY);

            // If no counter updates have been executed yet, the initial value
            // needs to be set atomically
            if (versioned == null) {
               // To detect first counter update, a flag to return the previous
               // value is passed to putIfAbsent() operation, to be able to rely
               // on its return to know whether it was first or not
               if (cache.withFlags(Flag.FORCE_RETURN_VALUE).putIfAbsent(KEY, 1) == null)
                  return counted + 1;
            } else {
               // Increment counter by 1
               int val = versioned.getValue() + 1;

               // Use the cached entry's state version as a way to do a compare-and-swap
               // operation in which the counter is only updated as long as the version
               // remains the same. If a concurrent modification occurred in the mean time,
               // the code retries until it succeeds
               long version = versioned.getVersion();
               if (cache.replaceWithVersion(KEY, val, version))
                  return counted + 1;
            }
         }
      }
   }

}