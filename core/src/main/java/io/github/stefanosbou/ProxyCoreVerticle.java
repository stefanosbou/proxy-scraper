package io.github.stefanosbou;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

public class ProxyCoreVerticle  extends AbstractVerticle {

   private final String PROXY_WEBSITE = "https://www.sslproxies.org/";
   Set<JsonObject> proxies;
   private EventBus eb;

   @Override
   public void start(Future<Void> future) {
      eb = vertx.eventBus();
      proxies = new HashSet<>();
      crawlProxies();
      future.complete();
   }

   private void crawlProxies() {
      HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions()
         .setSsl(true));

      httpClient.requestAbs(HttpMethod.GET, PROXY_WEBSITE, response -> {
         response.bodyHandler(body -> {
            String htmlBody = body.getString(0, body.length());
            extractProxies(htmlBody);
         });
      }).end();

      vertx.setTimer(60000, id -> crawlProxies());
   }

   private void extractProxies(String htmlBody) {
      Document doc = Jsoup.parse(htmlBody);

      Element table = doc.select("table").get(0); //select the first table.
      Elements rows = table.select("tr");

      JsonObject proxy;
      for (int i = 1; i < rows.size(); i++) { //first row is the col names so skip it.
         Element row = rows.get(i);
         Elements cols = row.select("td");

         proxy = new JsonObject();
         if (!cols.isEmpty()) {
            String host = cols.get(0).text();
            String port = cols.get(1).text();
            String countryCode = cols.get(2).text();
            String country = cols.get(3).text();
            String anonymity = cols.get(4).text();
            String google = cols.get(5).text();
            String https = cols.get(6).text();

            proxy.put("host", host)
               .put("port", port)
               .put("country_code", countryCode)
               .put("country", country)
               .put("anonymity", anonymity)
               .put("google", google)
               .put("https", https);

            eb.send("check-status", proxy, message -> {
               proxies.add((JsonObject) message.result().body());
            });
         }

      }
      System.out.println("Total proxies in the list: " + proxies.size());
   }
}
