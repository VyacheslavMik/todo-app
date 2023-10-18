(ns server.db
  (:require [buddy.core.codecs :as codecs]
            [buddy.core.mac :as mac]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [datomic.api :as d]
            [server.config :refer [config]]
            [server.schema :as schema]))

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

(def todo-schema
  [{:db/ident :todo/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The todo title."}

   {:db/ident :todo/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The todo description."}

   {:db/ident :todo/priority
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "The todo priority"}

   {:db/ident :todo/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Created datetime."}

   {:db/ident :todo/due-date
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "When the todo must be finished"}

   {:db/ident :todo/author
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "An author of the todo."}])

(defn initialized?
  "Returns a logical true if the database already is initialized"
  [conn]
  (d/q '[:find ?e .
         :where [?e :db/ident :user/name]]
       (d/db conn)))

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
  (d/transact conn (into user-schema todo-schema)))

(defn read-user
  "Returns a user from the database."
  [db user-name & [load-password?]]
  (if-let [user (d/q '[:find (pull ?e [:db/id
                                       :user/name
                                       :user/password]) .
                       :in $ ?user-name
                       :where [?e :user/name ?user-name]]
                     db
                     user-name)]
    (cond-> user
      :always (dissoc :db/id)
      :alwyas (assoc :user/id (:db/id user))
      (not load-password?) (dissoc :user/password))
    {:error "User not found"}))

(defn read-user-id
  "Reads the user :db/id from the database."
  [db user-name]
  (d/q '[:find ?e .
         :in $ ?user-name
         :where [?e :user/name ?user-name]]
       db
       user-name))

(defn user-exists?
  "Checks whether a user with the user-name exists."
  [db user-name]
  (some? (read-user-id db user-name)))
  
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
      (read-user (d/db conn) user-name))))

(defn save-todo
  "Transacts a todo to the database and returns an :ok."
  [conn todo]
  (if (s/valid? ::schema/todo todo)
    (do
      @(d/transact conn (cond-> [todo]
                          (and (:db/id todo) (not (:todo/due-date todo)))
                          (conj [:db/retract (:db/id todo) :todo/due-date])))
      {:result :ok})
    {:error "Invalid todo"}))

(defn delete-todo
  "Transacts a tx-data that retracts the todo and returns an :ok."
  [conn id]
  @(d/transact conn [[:db/retractEntity id]])
  {:result :ok})

(defn read-todos
  "Returns all todos of the user."
  [db {user-name :user/name}]
  (map (fn [todo]
         (-> todo
             (dissoc :db/id)
             (assoc :todo/id (:db/id todo))
             (update :todo/author :db/id)))
       (d/q '[:find [(pull ?e [:db/id
                               :todo/title
                               :todo/description
                               :todo/priority
                               :todo/created-at
                               :todo/due-date
                               :todo/author]) ...]
              :in $ ?user-name
              :where
              [?e :todo/author ?user]
              [?user :user/name ?user-name]]
            db
            user-name)))
