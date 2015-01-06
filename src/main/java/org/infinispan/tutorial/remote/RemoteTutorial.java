package org.infinispan.tutorial.remote;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;

import java.util.Map;

public class RemoteTutorial {

   public static void main(String[] args) {
      RemoteCacheManager remoteCacheManager = new RemoteCacheManager();

      // Obtain the default cache
      RemoteCache<String, String> cache = remoteCacheManager.getCache();

      // Get cache statistics
      Map<String, String> stats = cache.stats().getStatsMap();

      // Print them out
      System.out.println(stats);

      // Stop the cache manager and release all resources
      remoteCacheManager.stop();
   }

}
