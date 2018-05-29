package io.github.stefanosbou;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;

public class ProxyCheckerVerticle extends AbstractVerticle {
   HttpClient client;

   @Override
   public void start(Future<Void> future) {
      EventBus eb = vertx.eventBus();

      MessageConsumer<JsonObject> consumer = eb.consumer("check-status");
      consumer.handler(this::handleProxyStatusCheck);

      vertx.createHttpServer().requestHandler(req -> {
         System.out.println("###########");
         System.out.println(req.remoteAddress().host());
         System.out.println("------------");
         req.headers().entries().stream().map((e) -> e.getKey() + ":" + e.getValue()).forEach(System.out::println);
         System.out.println("###########");
         req.response().end(req.remoteAddress().host());
      }).listen(9999);

      future.complete();
   }

   private void handleProxyStatusCheck(Message<JsonObject> message) {
      String host = message.body().getString("host");
      int port = Integer.parseInt(message.body().getString("port"));

//      System.out.println(message.body().getString("https"));
      boolean https = message.body().getString("https").equalsIgnoreCase("yes") ? true : false;
      client = vertx.createHttpClient(new HttpClientOptions()
         .setProxyOptions(new ProxyOptions()
            .setHost(host)
            .setPort(port))
//         .setSsl(https)
      );

      client.getNow(9999, "46.177.170.112", "/", response -> {
         if (response.statusCode() == 200) {
            response.bodyHandler(body -> {
               String res = body.toString().trim();
            System.out.println(res + " -> "  + host + " " + (host.equals(res) ? "active" : "inactive"));
               JsonObject obj = message.body();
               obj.put("status", (host.equals(res) ? "active" : "inactive"));
               message.reply(obj);
            });
         }
      });

   }
}
