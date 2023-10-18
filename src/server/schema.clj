(ns server.schema
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]))

(def non-blank-string? (comp not string/blank?))
(def non-negative-number? (comp not neg?))

(s/def :user/name (s/and string? non-blank-string?))

(s/def ::user (s/or :db/id number?
                    :user (s/keys :req [:user/name])))

(s/def :todo/title (s/and string? non-blank-string?))
(s/def :todo/description (s/and string? non-blank-string?))
(s/def :todo/priority (s/and number? non-negative-number?))
(s/def :todo/created-at inst?)
(s/def :todo/due-date inst?)
(s/def :todo/author ::user)

(s/def ::todo (s/keys :req [:todo/title
                            :todo/description
                            :todo/priority
                            :todo/created-at
                            :todo/author]
                      :opt [:todo/due-date]))

