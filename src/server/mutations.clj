(ns server.mutations
  (:require [server.resolvers :refer [list-table]]
            [com.wsscode.pathom.connect :as pc]
            [server.db :as db]
            [taoensso.timbre :as log]))

(defn foo [{:sign-up/keys [name password]}])

(pc/defmutation sign-up [env {:sign-up/keys [user-name password] :as props}]
  {::pc/sym `mutations/sign-up}
  (let [conn (db/connect :mem)]
    (db/add-user conn user-name password)))

(def mutations [sign-up])
