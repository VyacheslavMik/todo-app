(ns server.core
  (:require [clojure.data.json :as json]
            [hiccup2.core :as h]
            [org.httpkit.server :as hk-server]
            [reitit.core :as r]))

(defn index
  [_]
  [:html
   [:body
    [:div "Hello"]]])

(defn rpc
  [_]
  {:something 1})

(def router
  (r/router
   [["/" {:handler index
          :type :html}]
    ["/index" {:handler index
               :type :html}]
    ["/rpc" {:handler rpc
             :type :json}]]))

(defn app [{:keys [uri] :as request}]
  (if-let [route (r/match-by-path router uri)]
    (let [{:keys [handler type]} (:data route)]
      (case type
        :html
        {:status 200
         :headers {"Content-Type" "text/html"}
         :body (str
                (h/html
                 (handler request)))}

        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/write-str
                (handler request))}))
    {:status 404}))

(defonce server (atom nil))

(defn run-server
  "Runs web-server."
  [port]
  (reset! server (hk-server/run-server app {:port port})))

(defn stop-server
  "Stops web-server."
  []
  (let [server-shutdown @server]
    (server-shutdown :timeout 100)))
