(ns server.core
  (:require [com.fulcrologic.fulcro.server.api-middleware :as server]
            [org.httpkit.server :as http]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.resource :refer [wrap-resource]]
            [server.db :as db]
            [server.parser :refer [api-parser]]))

(def ^:private not-found-handler
  (fn [req]
    {:status 404
     :headers {"Content-Type" "text/plain"}
     :body "Not Found"}))

(defn wrap-api [handler connection uri]
  (fn [request]
    (if (= uri (:uri request))
      (server/handle-api-request
       (:transit-params request)
       (fn [tx] (api-parser request connection tx)))
      (handler request))))

(defonce state (atom {}))

(defn start [_args]
  (swap! state assoc :connection (db/connect :mem))

  (let [conn (:connection @state)]
    (when-not (db/initialized? conn)
      (db/initialize conn)))

  (let [middleware (-> not-found-handler
                       (wrap-api (:connection @state) "/api")
                       (server/wrap-transit-params)
                       (server/wrap-transit-response)
                       (wrap-resource "public")
                       wrap-cookies
                       wrap-content-type)]
    (swap! state assoc :stop-fn (http/run-server middleware {:port 3000})))

  (println "Server started"))

(defn stop []
  (when-let [stop-fn (:stop-fn @state)]
    (stop-fn)
    (swap! state dissoc :stop-fn)))
