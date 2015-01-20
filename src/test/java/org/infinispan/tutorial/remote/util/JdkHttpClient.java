package org.infinispan.tutorial.remote.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdkHttpClient {

   public static URI uri(String hostname, int port, String query) {
      try {
         URL url = new URL("http", hostname, port, query);
         return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(),
               url.getPort(), url.getPath(), url.getQuery(), null);
      } catch (Exception e) {
         throw new AssertionError(e);
      }
   }

   public static class Params {
      private final Map<String, Object> map = new HashMap<>();

      private Params() {
         // Use static methods
      }

      public Params add(String k, Object value) {
         map.put(k, value);
         return this;
      }

      public Map<String, Object> map() {
         return Collections.unmodifiableMap(map);
      }

      public static Params apply(String k, Object value) {
         Params params = new Params();
         params.add(k, value);
         return params;
      }

      public static Map<String, Object> empty() {
         return Collections.emptyMap();
      }
   }

   public static Map<String, ?> get(final URI uri, final Map<String, ?> params) {
      return withHttp(uri, "GET", params, new HttpCallable<Map<String, ?>>() {
         @Override
         public Map<String, ?> call(HttpURLConnection httpcon) throws IOException {
            // 4xx: client error, 5xx: server error. See: http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html.
            boolean isError = httpcon.getResponseCode() >= 400;
            // The normal input stream doesn't work in error-cases.
            InputStream is = isError ? httpcon.getErrorStream() : httpcon.getInputStream();
            if (isError) {
               return getRsp(uri, httpcon, httpcon.getResponseMessage());
            } else {
               Object type = params.get(Keys.CONTENT_TYPE);
               if (type == null)
                  type = params.get(Keys.ACCEPT);

               switch (type.toString()) {
                  case "text/plain":
                  case "text/html":
                  case "application/xml":
                  case "application/json":
                     List<String> lines = new ArrayList<>();
                     String line;
                     try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                        while ((line = reader.readLine()) != null)
                           lines.add(line);

                        return getRsp(uri, httpcon, lines.size() > 1 ? lines : lines.get(0));
                     }
                  case "application/octet-stream":
                     int length = (Integer) params.get(Keys.LENGTH);
                     byte[] bytes = new byte[length];
                     is.read(bytes);
                     return getRsp(uri, httpcon, bytes);
                  default:
                     throw new IllegalStateException("Unsupported content type");
               }
            }
         }
      });
   }

   public static Map<String, ?> put(final URI uri, final Map<String, ?> params) {
      return withHttp(uri, "PUT", params, new HttpCallable<Map<String, ?>>() {
         @Override
         public Map<String, ?> call(HttpURLConnection httpcon) throws IOException {
            Object body = params.get(Keys.BODY);
            if (body instanceof String) {
               try (OutputStreamWriter writer = new OutputStreamWriter(httpcon.getOutputStream())) {
                  writer.write(body.toString());
                  writer.flush();
                  return putRsp(uri, httpcon);
               }
            } else if (body instanceof byte[]) {
               httpcon.getOutputStream().write((byte[]) body);
               return putRsp(uri, httpcon);
            } else {
               throw new IllegalStateException("Unsupported");
            }
         }
      });
   }

   public static Map<String, ?> delete(final URI uri, Map<String, ?> params) {
      return withHttp(uri, "DELETE", params, new HttpCallable<Map<String, ?>>() {
         @Override
         public Map<String, ?> call(HttpURLConnection httpcon) throws IOException {
            return deleteRsp(uri, httpcon);
         }
      });
   }

   private static Map<String, ?> withHttp(URI uri, String method, Map<String, ?> params,
         HttpCallable<Map<String, ?>> callable) {
      HttpURLConnection httpcon = null;
      try {
         httpcon = httpcon(uri, method, params);
         httpcon.connect();
         return callable.call(httpcon);
      } catch (IOException e) {
         throw new AssertionError(e);
      } finally {
         if (httpcon != null) httpcon.disconnect();
      }
   }

   private static HttpURLConnection httpcon(URI uri, String method, Map<String, ?> params) throws IOException {
      System.out.printf("==> %s(%s) \n", method, uri);
      HttpURLConnection httpcon = (HttpURLConnection) uri.toURL().openConnection();
      httpcon.setRequestMethod(method);
      httpcon.setDoOutput(true);
      for (Map.Entry<String, ?> e : params.entrySet())
         httpcon.setRequestProperty(e.getKey(), e.getValue().toString());
      return httpcon;
   }

   private static Map<String, ?> getRsp(URI uri, HttpURLConnection httpcon, Object response) throws IOException {
      Map<String, Object> resp = buildRsp(httpcon);
      resp.put(Keys.BODY, response);
      System.out.printf("<== GET(%s) %s\n", uri, resp);
      return resp;
   }

   private static Map<String, ?> putRsp(URI uri, HttpURLConnection httpcon) throws IOException {
      Map<String, Object> resp = buildRsp(httpcon);
      System.out.printf("<== PUT(%s) %s\n", uri, resp);
      return resp;
   }

   private static Map<String, ?> deleteRsp(URI uri, HttpURLConnection httpcon) throws IOException {
      Map<String, Object> resp = buildRsp(httpcon);
      System.out.printf("<== DELETE(%s) %s\n", uri, resp);
      return resp;
   }

   private static Map<String, Object> buildRsp(HttpURLConnection httpcon) throws IOException {
      Map<String, Object> resp = new HashMap<>();
      for (Map.Entry<String, List<String>> e : httpcon.getHeaderFields().entrySet()) {
         if (e.getValue().size() > 1)
            resp.put(e.getKey(), e.getValue());
         else
            resp.put(e.getKey(), e.getValue().get(0));
      }

      resp.put(Keys.STATUS_CODE, httpcon.getResponseCode());
      resp.put(Keys.MESSAGE, httpcon.getResponseMessage());
      return resp;
   }

   private interface HttpCallable<V> {
      V call(HttpURLConnection httpcon) throws IOException;
   }

   public static class Keys {
      public static final String ACCEPT = "Accept";
      public static final String BODY = "Body";
      public static final String CONTENT_TYPE = "Content-Type";
      public static final String ETAG = "ETag";
      public static final String IF_MATCH = "If-Match";
      public static final String LENGTH = "Length";
      public static final String MESSAGE = "Message";
      public static final String STATUS_CODE = "Status-Code";
   }

}
