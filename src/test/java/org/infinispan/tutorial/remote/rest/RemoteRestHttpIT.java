package org.infinispan.tutorial.remote.rest;

import org.infinispan.arquillian.core.InfinispanResource;
import org.infinispan.arquillian.core.RemoteInfinispanServer;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.infinispan.tutorial.remote.util.JdkHttpClient.*;

@RunWith(Arquillian.class)
public class RemoteRestHttpIT {

   // Container defined in arquillian.xml
   @InfinispanResource("container-rest")
   RemoteInfinispanServer server1;

   @Test
   public void remoteHttpRestCacheReadWrite() {
      URI keyUri; Params params;

      // Attempt to retrieve a non-existing entry
      keyUri = cacheKeyUri("key-string");
      params = Params.apply(Keys.ACCEPT, "text/plain");
      Assert.assertEquals(404, get(keyUri, params.map()).get(Keys.STATUS_CODE));

      // Store plain String content
      params = Params.apply(Keys.BODY, "hello-world").add(Keys.CONTENT_TYPE, "text/plain");
      Assert.assertEquals(200, put(keyUri, params.map()).get(Keys.STATUS_CODE));

      // Retrieve plain String content
      params = Params.apply(Keys.ACCEPT, "text/plain");
      Assert.assertEquals("hello-world", get(keyUri, params.map()).get(Keys.BODY));

      // Delete String content
      Assert.assertEquals(200, delete(keyUri, Params.empty()).get(Keys.STATUS_CODE));

      // Store binary content
      keyUri = cacheKeyUri("key-bytes");
      params = Params.apply(Keys.BODY, new byte[]{1, 2, 3}).add(Keys.CONTENT_TYPE, "application/octet-stream");
      Assert.assertEquals(200, put(keyUri, params.map()).get(Keys.STATUS_CODE));

      // Retrieve binary content
      params = Params.apply(Keys.ACCEPT, "application/octet-stream").add(Keys.LENGTH, 3);
      Assert.assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) get(keyUri, params.map()).get(Keys.BODY));

      // Delete binary content
      Assert.assertEquals(200, delete(keyUri, Params.empty()).get(Keys.STATUS_CODE));
   }

   @Test
   public void remoteHttpRestCacheReadKeySet() {
      Params params; Map<String, ?> rsp; List<String> expected;

      // Store multiple cache entries
      params = Params.apply(Keys.BODY, "hello-a").add(Keys.CONTENT_TYPE, "text/plain");
      Assert.assertEquals(200, put(cacheKeyUri("key-a"), params.map()).get(Keys.STATUS_CODE));
      params = Params.apply(Keys.BODY, "hello-b").add(Keys.CONTENT_TYPE, "text/plain");
      Assert.assertEquals(200, put(cacheKeyUri("key-b"), params.map()).get(Keys.STATUS_CODE));
      params = Params.apply(Keys.BODY, "hello-c").add(Keys.CONTENT_TYPE, "text/plain");
      Assert.assertEquals(200, put(cacheKeyUri("key-c"), params.map()).get(Keys.STATUS_CODE));

      // Retrieve all keys in plain text format
      params = Params.apply(Keys.ACCEPT, "text/plain");
      rsp = get(cacheUri(), params.map());
      Assert.assertEquals(200, rsp.get(Keys.STATUS_CODE));
      List<?> keysSet = (List<?>) rsp.get(Keys.BODY);
      Assert.assertEquals(3, keysSet.size());
      expected = Arrays.asList("key-a", "key-b", "key-c");
      Assert.assertTrue(keysSet.toString(),expected.containsAll(keysSet) && keysSet.containsAll(expected));

      // Retrieve all keys in XML format
      params = Params.apply(Keys.ACCEPT, "application/xml");
      rsp = get(cacheUri(), params.map());
      Assert.assertEquals(200, rsp.get(Keys.STATUS_CODE));
      List<?> xml = (List<?>) rsp.get(Keys.BODY);
      Assert.assertEquals(xml.toString(), 3, xml.size());
      String keysXml = xml.get(2).toString();
      Assert.assertTrue(keysXml, keysXml.contains("<keys>"));
      Assert.assertTrue(keysXml, keysXml.contains("<key>key-a</key>"));
      Assert.assertTrue(keysXml, keysXml.contains("<key>key-b</key>"));
      Assert.assertTrue(keysXml, keysXml.contains("<key>key-c</key>"));
      Assert.assertTrue(keysXml, keysXml.contains("</keys>"));

      // Retrieve all keys in HTML format
      params = Params.apply(Keys.ACCEPT, "text/html");
      rsp = get(cacheUri(), params.map());
      Assert.assertEquals(200, rsp.get(Keys.STATUS_CODE));
      String html = (String) rsp.get(Keys.BODY);
      Assert.assertTrue(html, html.contains("<a href=\"key-a\">key-a</a><br/>"));
      Assert.assertTrue(html, html.contains("<a href=\"key-b\">key-b</a><br/>"));
      Assert.assertTrue(html, html.contains("<a href=\"key-c\">key-c</a><br/>"));

      // Retrieve all keys in JSON format
      params = Params.apply(Keys.ACCEPT, "application/json");
      rsp = get(cacheUri(), params.map());
      Assert.assertEquals(200, rsp.get(Keys.STATUS_CODE));
      String json = (String) rsp.get(Keys.BODY);
      Assert.assertTrue(json, json.contains("keys=["));
      Assert.assertTrue(json, json.contains("\"key-a\""));
      Assert.assertTrue(json, json.contains("\"key-b\""));
      Assert.assertTrue(json, json.contains("\"key-c\""));
      Assert.assertTrue(json, json.contains("keys=["));
   }

   @Test
   public void remoteHttpRestCacheConditionalPutIfMatch() {
      URI keyUri; Params params; Map<String, ?> rsp;

      // Store entry
      keyUri = cacheKeyUri("key-put-if-match");
      params = Params.apply(Keys.BODY, "hello-world").add(Keys.CONTENT_TYPE, "text/plain");
      Assert.assertEquals(200, put(keyUri, params.map()).get(Keys.STATUS_CODE));

      // Retrieve entry along with its ETag
      params = Params.apply(Keys.ACCEPT, "text/plain");
      rsp = get(keyUri, params.map());
      Assert.assertEquals("hello-world", rsp.get(Keys.BODY));
      Assert.assertEquals(200, rsp.get(Keys.STATUS_CODE));
      String etag = rsp.get(Keys.ETAG).toString();

      // Modify entry with given ETag
      params = Params.apply(Keys.BODY, "bye-world").add(Keys.CONTENT_TYPE, "text/plain").add(Keys.IF_MATCH, etag);
      Assert.assertEquals(200, put(keyUri, params.map()).get(Keys.STATUS_CODE));

      // Modify entry with an invalid ETag
      params = Params.apply(Keys.BODY, "not-so-fast!").add(Keys.CONTENT_TYPE, "text/plain").add(Keys.IF_MATCH, "xxx");
      Assert.assertEquals(412, put(keyUri, params.map()).get(Keys.STATUS_CODE));

      // Delete entry
      Assert.assertEquals(200, delete(keyUri, Params.empty()).get(Keys.STATUS_CODE));
   }

   @Test
   public void remoteHttpRestCacheReadWriteEphemeral() throws InterruptedException {
      URI keyUri; Params params; Map<String, ?> rsp;

      // Store entry
      keyUri = cacheKeyUri("key-get-if-unmodified");
      params = Params.apply(Keys.BODY, "hello-world").add(Keys.CONTENT_TYPE, "text/plain")
         .add("timeToLiveSeconds", "1");
      Assert.assertEquals(200, put(keyUri, params.map()).get(Keys.STATUS_CODE));

      // Retrieve entry immediately
      params = Params.apply(Keys.ACCEPT, "text/plain");
      rsp = get(keyUri, params.map());
      Assert.assertEquals("hello-world", rsp.get(Keys.BODY));
      Assert.assertEquals(200, rsp.get(Keys.STATUS_CODE));

      // Let's wait a couple of seconds
      Thread.sleep(TimeUnit.SECONDS.toMillis(2));

      // Print it out again and assert. It will have died
      params = Params.apply(Keys.ACCEPT, "text/plain");
      Assert.assertEquals(404, get(keyUri, params.map()).get(Keys.STATUS_CODE));
   }

   URI cacheKeyUri(String key) {
      return uri(server1.getRESTEndpoint().getInetAddress().getHostName(), 8080,
         String.format("/rest/___defaultcache/%s", key));
   }

   URI cacheUri() {
      return uri(server1.getRESTEndpoint().getInetAddress().getHostName(), 8080,
            String.format("/rest/___defaultcache"));
   }

}
