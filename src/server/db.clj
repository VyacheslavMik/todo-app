(ns server.db
  (:require [buddy.core.codecs :as codecs]
            [buddy.core.mac :as mac]
            [clojure.string :as string]
            [datomic.api :as d]
            [server.config :refer [config]]))

(def urls
  "Contains a mapping between a database type and a database url"
  {:mem "datomic:mem://todo-app"})

(def user-schema
  [{:db/ident :user/name
    :db/valueType :db.type/string
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one
    :db/doc "The user name."}

   {:db/ident :user/password
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The user password's md5 hash."}])

(defn connect
  "Connects to the datomic database."
  [db-type]
  (if-let [url (get urls db-type)]
    (do
      (d/create-database url)
      (d/connect url))
    (throw (ex-info "Unsupported database type." {:db-type db-type}))))

(defn initialize
  "Initializes a database schema"
  [conn]
  (d/transact conn user-schema))

(defn read-user
  "Returns a user from the database."
  [db user-name]
  (if-let [user (d/q '[:find (pull ?e [:user/name]) .
                       :in $ ?user-name
                       :where [?e :user/name ?user-name]]
                     db
                     user-name)]
    user
    {:error "User not found"}))

(defn user-exists?
  "Checks whether a user with the user-name exists."
  [db user-name]
  (d/q '[:find ?e .
         :in $ ?user-name
         :where [?e :user/name ?user-name]]
       db
       user-name))
  

(defn add-user
  "Transacts a new user to the database and returns the newly created user."
  [conn user-name password]
  (cond
    (string/blank? user-name)
    {:error "User name cannot be blank"}

    (string/blank? password)
    {:error "Password cannot be blank"}

    (user-exists? (d/db conn) user-name)
    {:error "User already added"}

    :else
    (let [password-hash (-> (mac/hash password {:key (:secret config)
                                                :alg :hmac+sha256})
                            (codecs/bytes->hex))]
      @(d/transact conn
                   [{:user/name user-name
                     :user/password password-hash}])
      {:user (read-user (d/db conn) user-name)})))
