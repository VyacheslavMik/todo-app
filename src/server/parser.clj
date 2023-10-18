(ns server.parser
  (:require [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]
            [datomic.api :as d]
            [server.mutations]
            [server.resolvers]
            [taoensso.timbre :as log]))

;; TODO: Uncomment this for the production use.
;; (def resolvers [server.resolvers/resolvers
;;                 server.mutations/mutations])

;; (def pathom-parser
;;   (p/parser {::p/env {::p/reader [p/map-reader
;;                                   pc/reader2
;;                                   pc/ident-reader
;;                                   pc/index-reader]
;;                       ::pc/mutation-join-globals [:tempids]}
;;              ::p/mutate pc/mutate
;;              ::p/plugins [(pc/connect-plugin {::pc/register resolvers})
;;                           p/error-handler-plugin
;;                           (p/post-process-parser-plugin p/elide-not-found)]}))

;; (defn api-parser [query]
;;   (log/info "Process" query)
;;   (pathom-parser {} query))


;; TODO: Remove this function because it is neede only for development
(defn api-parser [request connection query]
  (let [pathom-parser (p/parser {::p/env {::p/reader [p/map-reader
                                                      pc/reader2
                                                      pc/ident-reader
                                                      pc/index-reader]
                                          ::pc/mutation-join-globals [:tempids]}
                                 ::p/mutate pc/mutate
                                 ::p/plugins [(pc/connect-plugin {::pc/register [server.resolvers/resolvers
                                                                                 server.mutations/mutations]})
                                              p/error-handler-plugin
                                              p/request-cache-plugin
                                              (p/post-process-parser-plugin p/elide-not-found)]})]
    (log/info "Process" query)
    (pathom-parser {:request request
                    :conn connection
                    :db (d/db connection)}
                   query)))
