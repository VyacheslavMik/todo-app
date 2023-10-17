(ns client.core
  (:require [clojure.walk]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [com.fulcrologic.fulcro.algorithms.lookup :as ah]
            [com.fulcrologic.fulcro.algorithms.merge :as merge]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.networking.http-remote :as http]))

(declare SignUpForm)

(defonce app (app/fulcro-app
              {:remotes {:remote (http/fulcro-http-remote {})}}))

(defmutation mutations/sign-up
  "Mutation: Registers new user with the `name` and `password`."
  [props]
  (remote [_env] true)
  (result-action [{:keys [state result]}]
                 (println state)
                 (let [{:keys [error user]} (-> result :body (get 'mutations/sign-up))]
                   (when error
                     (swap! state
                            update-in
                            [:form/id :sign-up]
                            merge
                            {:sign-up/error error})))))

;; TODO: Add validation.
(defsc SignUpForm [this {:keys [sign-up/user-name sign-up/password sign-up/error] :as props}]
  {:query [:form/id
           :sign-up/user-name
           :sign-up/password
           :sign-up/error]
   :initial-state (fn [{:keys [id]}]
                    {:form/id id})
   :ident :form/id}
  (dom/div
   (dom/h3
    "Sig Up")
   (dom/div "Error:" error)
   (dom/input {:value (or user-name "")
               :type "text"
               :onChange #(m/set-string! this :sign-up/user-name :event %)})
   (dom/input {:value (or password "")
               :type "password"
               :onChange #(m/set-string! this :sign-up/password :event %)})
   (dom/button {:onClick #(comp/transact! this
                                          [`(mutations/sign-up
                                             {:sign-up/user-name ~user-name
                                              :sign-up/password ~password})])}
               "Sign Up")))

(def ui-sign-up-form (comp/factory SignUpForm {:keyfn :form/id}))

(defsc Root [this {:keys [sign-up-form]}]
  {:query [{:sign-up-form (comp/get-query SignUpForm)}]
   :initial-state (fn [_params]
                    {:sign-up-form (comp/get-initial-state SignUpForm
                                                           {:id :sign-up})})}
  (ui-sign-up-form sign-up-form))

(defn ^:export init
  "Shadow-cljs sets this up to be our entry-point function. See shadow-cljs.edn `:init-fn` in the modules of the main build."
  []
  (app/mount! app Root "app")
  (js/console.log "Loaded"))

(defn ^:export refresh
  "During development, shadow-cljs will call this on every hot reload of source. See shadow-cljs.edn"
  []
  ;; re-mounting will cause forced UI refresh, update internals, etc.
  (app/mount! app Root "app")
  ;; As of Fulcro 3.3.0, this addition will help with stale queries when using dynamic routing:
  (comp/refresh-dynamic-queries! app)
  (js/console.log "Hot reload"))
