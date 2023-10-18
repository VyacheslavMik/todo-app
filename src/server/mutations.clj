(ns server.mutations
  (:require [buddy.core.codecs :as codecs]
            [buddy.core.mac :as mac]
            [buddy.sign.jwt :as jwt]
            [clojure.string :as string]
            [com.fulcrologic.fulcro.server.api-middleware :as server]
            [com.wsscode.pathom.connect :as pc]
            [ring.middleware.cookies :as cookies]
            [ring.util.response :as rr]
            [server.auth :as auth]
            [server.config :refer [config]]
            [server.db :as db]
            [taoensso.timbre :as log]))

(defn set-token-cookie
  [response user]
  (rr/set-cookie response
                 :token (jwt/sign {:user/name (:user/name user)}
                                  (:secret config))
                 {:http-only true}))

(pc/defmutation sign-up [{:keys [conn]} {:sign-up/keys [user-name password]}]
  {::pc/sym `mutations/sign-up}
  (let [{:keys [error] :as result} (db/add-user conn user-name password)]
    (if error
      result
      (server/augment-response
       {:user result}
       (fn [response]
         (set-token-cookie response result))))))

(pc/defmutation sign-in [{:keys [db]} {:sign-in/keys [user-name password]}]
  {::pc/sym `mutations/sign-in}
  (let [{:keys [error] :as result} (db/read-user db user-name :load-password)
        password-valid? (when-not error
                          (mac/verify password
                                      (-> result
                                          :user/password
                                          codecs/hex->bytes)
                                      {:key (:secret config)
                                       :alg :hmac+sha256}))]
    (if (or error (not password-valid?))
      {:error "Authentication failed"}
      (server/augment-response
       {:user (dissoc result :user/password)}
       (fn [response]
         (set-token-cookie response result))))))

(pc/defmutation logout [_env _params]
  {::pc/sym `mutations/logout}
  (server/augment-response
   {}
   (fn [response]
     (rr/set-cookie response :token nil))))

(pc/defmutation save-todo [{:keys [conn request]} params]
  {::pc/sym `mutations/save-todo}
  (let [user (-> request
                 (auth/get-token)
                 (auth/read-token))
        new-todo? (= (:todo/id params) -1)
        due-date (:todo/due-date params)
        todo (cond-> params
               (and (string? due-date) (not (string/blank? due-date)))
               (update :todo/due-date clojure.instant/read-instant-date)

               (and (string? due-date) (string/blank? due-date))
               (dissoc :todo/due-date)

               (nil? due-date)
               (dissoc :todo/due-date)

               (string? (:todo/priority params))
               (update :todo/priority parse-long)

               new-todo?
               (assoc :todo/created-at (java.util.Date.)
                      :todo/author user)

               :always
               (dissoc :todo/id)

               (not new-todo?)
               (assoc :db/id (:todo/id params)))]
    (db/save-todo conn todo)))

(pc/defmutation delete-todo [{:keys [conn request]} {:keys [todo/id]}]
  {::pc/sym `mutations/delete-todo}
  (db/delete-todo conn id))

(def mutations [sign-up sign-in logout save-todo delete-todo])
