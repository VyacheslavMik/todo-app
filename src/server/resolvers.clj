(ns server.resolvers
  (:require [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]
            [server.auth :as auth]
            [ring.middleware.cookies :as cookies]
            [server.config :refer [config]]
            [server.db :as db]))

(pc/defresolver user-info-resolver [{:keys [request db]} _]
  {::pc/input #{}
   ::pc/output [{:user-info [:user/id
                             :user/signed-in?
                             :user/name]}]}
  (let [token (auth/get-token request)
        {user-name :user/name} (when token
                                 (auth/read-token token))
        user-id (when user-name
                  (db/read-user-id db user-name))
        user-info (if user-id
                    {:user/id user-id
                     :user/signed-in? true
                     :user/name user-name}
                    {:user/id -1
                     :user/signed-in? false})]
    {:user-info user-info}))

(pc/defresolver user-info-resolver [{:keys [request db]} _]
  {::pc/input #{}
   ::pc/output [{:user-info [:user/id
                             :user/signed-in?
                             :user/name]}]}
  (let [token (auth/get-token request)
        {user-name :user/name} (when token
                                 (auth/read-token token))
        user-id (when user-name
                  (db/read-user-id db user-name))
        user-info (if user-id
                    {:user/id user-id
                     :user/signed-in? true
                     :user/name user-name}
                    {:user/id -1
                     :user/signed-in? false})]
    {:user-info user-info}))

(pc/defresolver todo-list-resolver [{:keys [request db]} _]
  {::pc/input #{}
   ::pc/output [{:todo-list [:list/id
                             :list/todos]}]}
  (let [user (-> request
                 (auth/get-token)
                 (auth/read-token))]
    {:todo-list {:list/id :todos
                 :list/todos (db/read-todos db user)}}))

(def resolvers [user-info-resolver
                todo-list-resolver])
