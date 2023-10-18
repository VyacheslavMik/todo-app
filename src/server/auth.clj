(ns server.auth
  (:require [buddy.sign.jwt :as jwt]
            [server.config :refer [config]]
            [ring.middleware.cookies :as cookies]))

(defn read-token
  [token]
  (try
    (jwt/unsign token (:secret config))
    (catch Exception _
      nil)))

(defn get-token
  [request]
  (let [cookies (cookies/cookies-request request)]
    (-> cookies :cookies (get "token") :value)))
