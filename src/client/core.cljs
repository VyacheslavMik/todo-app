(ns client.core
  (:require [clojure.walk]
            [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
            [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [com.fulcrologic.fulcro.algorithms.lookup :as ah]
            [com.fulcrologic.fulcro.algorithms.merge :as merge]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.networking.http-remote :as http]))

(declare SignUpForm TodoList UserInfo)

(def page-style {:display "flex"
                 :flexDirection "column"
                 :maxWidth "900px"
                 :marginLeft "auto"
                 :marginRight "auto"})

(def page-title-style {:marginLeft "auto"
                       :marginRight "auto"
                       :marginTop 0})

(def block-style {:display "flex"
                  :flexDirection "column"
                  :marginLeft "auto"
                  :marginRight "auto"
                  :backgroundColor "white"
                  :padding "40px 80px"
                  :borderRadius "8px"})

(def field-title-style {:color "#999"
                        :marginBottom "5px"})

(def field-input-style {:marginBottom "20px"
                        :outline "none"
                        :borderTop "none"
                        :borderLeft "none"
                        :borderRight "none"
                        :borderBottom "1px solid black"})

(def link-style {:cursor "pointer"
                 :color "blue"})

(def button-style {:borderRadius "8px"
                   :background "lightblue"
                   :color "white"
                   :border "none"
                   :padding "10px 40px"
                   :cursor "pointer"
                   :fontWeight "bold"
                   :letterSpacing "1.3"
                   :textTransform "uppercase"})

(def error-block-style {:padding "10px"
                        :textAlign "center"
                        :color "white"
                        :borderRadius "8px"
                        :background "#ff000070"})

(def todo-block-style {:marginBottom "10px"
                       :paddingBottom "5px"
                       :borderBottom "1px solid #666"
                       :display "flex"
                       :flexDirection "column"
                       :minWidth "600px"})

(def todo-title-style {:marginLeft "auto"
                       :marginRight "auto"
                       :fontWeight "bold"
                       :fontSize "18px"})

(def todo-subtitle-style {:marginLeft "auto"
                          :fontSize "12px"
                          :color "#969696"})

(def todo-controls-style {:marginLeft "auto"
                          :marginBottom "10px"
                          :marginTop "10px"})

(def todo-form-controls-style todo-controls-style)

(defonce app (app/fulcro-app
              {:remotes {:remote (http/fulcro-http-remote {})}}))

(defn format-date
  [date]
  (if (inst? date)
    (str (.getFullYear date) "/" (.getMonth date) "/" (.getDate date))
    date))

(defn format-input-date
  [date]
  (if (inst? date)
    (let [m (.getMonth date)
          d (.getDate date)]
      (str (.getFullYear date) "-"
           (when (< m 10) "0") (.getMonth date) "-"
           (when (< d 10) "0")
           (.getDate date)))
    date))

(defmutation mutations/show-error
  "Mutation: Shows the `error` on the `form`."
  [{:keys [form-id error]}]
  (action [{:keys [state]}]
          (swap! state update-in [:form/id form-id] merge error)))

(defmutation mutations/set-user-info
  "Mutation: Places the user info into the state."
  [{:keys [user-info]}]
  (action [{:keys [state]}]
          (merge/merge-component! app UserInfo user-info :replace [:user-info])))

(defmutation mutations/load-todos
  "Mutation: Changes a page to the todos page and triggers a todos loading."
  [_]
  (action [{:keys [state]}]
          (df/load! app :todo-list TodoList)))

(defmutation mutations/goto-page
  "Mutation: Changes a page to the specified in the params."
  [{:keys [page]}]
  (action [{:keys [state]}]
          (swap! state assoc :page page)))

(defmutation mutations/clear-form
  "Mutation: Clears form state."
  [{:keys [form-id]}]
  (action [{:keys [state]}]
          (swap! state assoc-in [:form/id form-id] {:form/id form-id})))

(defmutation mutations/sign-up
  "Mutation: Registers a new user with a `name` and a `password`."
  [props]
  (action [{:keys [state]}]
          (swap! state update-in [:form/id :sign-up] dissoc :sign-up/error))
  (remote [_env] true)
  (result-action [{:keys [state result]}]
                 (let [{:keys [error user]} (-> result :body (get 'mutations/sign-up))]
                   (if error
                     (comp/transact! app [`(mutations/show-error
                                            {:form-id :sign-up
                                             :error {:sign-up/error ~error}})])
                     (let [user-info (assoc user :user/signed-in? true)]
                       (comp/transact! app [`(mutations/set-user-info {:user-info ~user-info})
                                            `(mutations/goto-page {:page :todos})
                                            `(mutations/clear-form {:form-id :sign-up})
                                            `(mutations/load-todos)]))))))

(defmutation mutations/sign-in
  "Mutation: Authenticates the user with a `name` and a `password`."
  [props]
  (action [{:keys [state]}]
          (swap! state update-in [:form/id :sign-in] dissoc :sign-in/error))
  (remote [_env] true)
  (result-action [{:keys [state result]}]
                 (let [{:keys [error user]} (-> result :body (get 'mutations/sign-in))]
                   (if error
                     (comp/transact! app [`(mutations/show-error
                                            {:form-id :sign-in
                                             :error {:sign-in/error ~error}})])
                     (let [user-info (assoc user :user/signed-in? true)]
                       (comp/transact! app [`(mutations/set-user-info {:user-info ~user-info})
                                            `(mutations/goto-page {:page :todos})
                                            `(mutations/clear-form {:form-id :sign-in})
                                            `(mutations/load-todos)]))))))

(defmutation mutations/change-page
  "Mutation: Registers new user with the `name` and `password`."
  [props]
  (action [{:keys [state]}]
          (let [state @state
                user-info (fdn/follow-ref state
                                          (fdn/follow-ref state [:user-info]))
                {:keys [user/signed-in?]} user-info]
            (if signed-in?
              (comp/transact! app [`(mutations/goto-page {:page :todos})
                                   `(mutations/load-todos)])
              (comp/transact! app [`(mutations/goto-page {:page :sign-in})])))))

(defmutation mutations/logout
  "Mutation: Logs out the user."
  [_]
  (remote [_env] true)
  (result-action [_]
                 (comp/transact! app [`(mutations/goto-page {:page :sign-in})])))

(defmutation mutations/make-new-todo
  "Mutation: Makes a new todo."
  [_]
  (action [{:keys [state]}]
          (swap! state
                 (fn [old-state]
                   (-> old-state
                       (assoc-in [:todo/id -1] {:todo/id -1})
                       (assoc :todo [:todo/id -1]))))
          (comp/transact! app [`(mutations/goto-page {:page :todo-form})])))

(defmutation mutations/edit-todo
  "Mutation: Edits an existing todo."
  [{:keys [todo-id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [old-state]
                   (assoc old-state :todo [:todo/id todo-id])))
          (comp/transact! app [`(mutations/goto-page {:page :todo-form})])))

(defmutation mutations/save-todo
  "Mutation: Saves the todo on the server."
  [{:keys [todo/id]}]
  (remote [_env] true)
  (result-action [{:keys [state result]}]
                 (let [{:keys [error]} (-> result :body (get 'mutations/save-todo))]
                   (if error
                     (swap! state assoc-in [:todo/id id :todo/save-error] error)
                     (comp/transact! app [`(mutations/load-todos)
                                          `(mutations/goto-page {:page :todos})])))))

(defmutation mutations/delete-todo
  "Mutation: Deletes the todo on the server."
  [_]
  (remote [_env] true)
  (result-action [_]
                 (comp/transact! app [`(mutations/load-todos)
                                      `(mutations/goto-page {:page :todos})])))

;; TODO: Add validation.
(defsc SignUpForm [this {:keys [sign-up/user-name sign-up/password sign-up/error]}]
  {:query [:form/id
           :sign-up/user-name
           :sign-up/password
           :sign-up/error]
   :initial-state (fn [{:keys [id]}]
                    {:form/id id})
   :ident :form/id}
  (dom/div {:style (assoc block-style
                          :minWidth "300px"
                          :marginTop "auto"
                          :marginBottom "auto")}
           (dom/h3 {:style page-title-style}
                   "Sign Up")

           (when error
             (dom/div {:style (assoc error-block-style
                                     :marginBottom "10px")}
                      "Error: " error))

           (dom/span {:style field-title-style}
                     "User Name:")
           (dom/input {:style field-input-style
                       :value (or user-name "")
                       :type "text"
                       :onChange #(m/set-string! this :sign-up/user-name :event %)})

           (dom/span {:style field-title-style}
                     "Password:")
           (dom/input {:style (assoc field-input-style
                                     :marginBottom "5px")
                       :value (or password "")
                       :type "password"
                       :onChange #(m/set-string! this :sign-up/password :event %)})

           (dom/a {:style (assoc link-style
                                 :marginLeft "auto"
                                 :marginBottom "15px")
                   :onClick #(comp/transact! this
                                             [`(mutations/goto-page {:page :sign-in})])}
                  "Sign In")
           (dom/button {:style button-style
                        :onClick #(comp/transact! this
                                                  [`(mutations/sign-up
                                                     {:sign-up/user-name ~user-name
                                                      :sign-up/password ~password})])}
                       "Sign Up")))

(def ui-sign-up-form (comp/factory SignUpForm {:keyfn :form/id}))

(defsc SignInForm [this {:keys [sign-in/user-name sign-in/password sign-in/error]}]
  {:query [:form/id
           :sign-in/user-name
           :sign-in/password
           :sign-in/error]
   :initial-state (fn [{:keys [id]}]
                    {:form/id id})
   :ident :form/id}
  (dom/div {:style (assoc block-style
                          :minWidth "300px"
                          :marginTop "auto"
                          :marginBottom "auto")}
           (dom/h3 {:style page-title-style}
                   "Sign In")

           (when error
             (dom/div {:style (assoc error-block-style
                                     :marginBottom "10px")}
                      "Error: " error))

           (dom/span {:style field-title-style}
                     "User Name:")
           (dom/input {:style field-input-style
                       :value (or user-name "")
                       :type "text"
                       :onChange #(m/set-string! this :sign-in/user-name :event %)})

           (dom/span {:style field-title-style}
                     "Password:")
           (dom/input {:style (assoc field-input-style
                                     :marginBottom "5px")
                       :value (or password "")
                       :type "password"
                       :onChange #(m/set-string! this :sign-in/password :event %)})

           (dom/a {:style (assoc link-style
                                 :marginLeft "auto"
                                 :marginBottom "15px")
                   :onClick #(comp/transact! this
                                             [`(mutations/goto-page {:page :sign-up})])}
                  "Sign Up")
           (dom/button {:style button-style
                        :onClick #(comp/transact! this
                                                  [`(mutations/sign-in
                                                     {:sign-in/user-name ~user-name
                                                      :sign-in/password ~password})])}
                       "Sign In")))

(def ui-sign-in-form (comp/factory SignInForm {:keyfn :form/id}))

(defn loading []
  (dom/h3
   "Loading"))

(defsc UserInfo [this {:keys [user/id user/signed-in? user/name] :as props}]
  {:query [:user/id
           :user/signed-in?
           :user/name]
   :ident :user/id
   :initial-state (fn [_]
                    {:user/id -1
                     :user/signed-in? false})}
  (dom/div {:style {:marginLeft "auto"
                    :marginBottom "20px"}}
           (dom/span {:style {:marginRight "8px"}}
                     name)
           (dom/a {:style link-style
                   :onClick #(comp/transact! this [`(mutations/logout)])}
                  "Logout")))

(def ui-user-info (comp/factory UserInfo {:keyfn :user/id}))

(defsc Todo [this {:keys [todo/id
                          todo/title
                          todo/description
                          todo/priority
                          todo/created-at
                          todo/author
                          todo/due-date]}]
  {:query [:todo/id
           :todo/title
           :todo/description
           :todo/priority
           :todo/created-at
           :todo/author
           :todo/due-date]
   :ident :todo/id}
  (dom/div {:style todo-block-style}
           (dom/span {:style todo-title-style}
                     title)
           (dom/div {:style todo-subtitle-style}
            "Created At: "
            (dom/span (format-date created-at)))

           (when due-date
             (dom/div {:style todo-subtitle-style}
              "Due Date: "
              (dom/span (format-date due-date))))

           (dom/div {:style todo-subtitle-style}
            "Priority: "
            (dom/span (str priority)))

           (dom/span description)

           (dom/div {:style todo-controls-style}
            (dom/button {:style (assoc button-style :marginRight "10px")
                         :onClick #(comp/transact!
                                    app
                                    [`(mutations/edit-todo {:todo-id ~id})])}
                        "Edit")

            (dom/button {:style (assoc button-style :background "#ff000096")
                         :onClick #(comp/transact!
                                    app
                                    [`(mutations/delete-todo {:todo/id ~id})])}
                        "Delete"))))

(def ui-todo (comp/factory Todo {:keyfn :todo/id}))

(defsc TodoList [this {:keys [list/id list/todos]}]
  {:query [:list/id
           {:list/todos (comp/get-query Todo)}]
   :ident :list/id
   :initial-state (fn [_]
                    {:list/id :todos
                     :list/todos []})}
  (map ui-todo todos))

(def ui-todo-list (comp/factory TodoList {:keyfn :list/id}))

(defn input
  [component type title value attr]
  (comp/fragment

   (dom/span {:style field-title-style}
             (str title ":"))

   (if (= type "textarea")
     (dom/textarea {:style field-input-style
                    :value (or value "")
                    :rows 4
                    :onChange #(m/set-string! component attr :event %)})
     (dom/input {:style field-input-style
                 :type type
                 :value (or value "")
                 :onChange #(m/set-string! component attr :event %)}))))

(defsc TodoForm [this {:keys [todo/id
                              todo/title
                              todo/description
                              todo/priority
                              todo/due-date
                              todo/created-at
                              todo/author
                              todo/save-error]}]
  {:query [:todo/id
           :todo/title
           :todo/description
           :todo/priority
           :todo/created-at
           :todo/due-date
           :todo/author
           :todo/save-error]
   :ident :todo/id}
  (dom/div {:style (assoc block-style
                          :marginTop "20px"
                          :minWidth "600px")}

           (dom/h3 {:style page-title-style}
                   (if (= id -1)
                     "New TODO"
                     "Edit TODO"))

           (when save-error
             (dom/div {:style (assoc error-block-style
                                     :marginBottom "10px")}
                      "Error: " save-error))

           (input this "text" "Title" title :todo/title)
           (input this "number" "Priority" priority :todo/priority)
           (input this "date" "Due Date" (format-input-date due-date) :todo/due-date)
           (input this "textarea" "Description" description :todo/description)

           (dom/div {:style todo-form-controls-style}
                    (dom/button
                     {:style (assoc button-style :marginRight "10px")
                      :onClick #(comp/transact! app `[(mutations/goto-page {:page :todos})])}
                     "Cancel")

                    (dom/button
                     {:style button-style
                      :onClick #(comp/transact! app `[(mutations/save-todo
                                                       {:todo/id ~id
                                                        :todo/title ~title
                                                        :todo/description ~description
                                                        :todo/priority ~priority
                                                        :todo/due-date ~due-date
                                                        :todo/created-at ~created-at
                                                        :todo/author ~author})])}
                     "Save"))))

(def ui-todo-form (comp/factory TodoForm {:keyfn :todo/id}))

(defn todos
  [todo-list user-info]
  (dom/div {:style (assoc block-style
                          :marginTop "20px"
                          :minWidth "600px")}
           (dom/div {:style {:display "flex"
                             :flexDirection "column"
                             :marginBottom "20px"}}
                    (ui-user-info user-info)
                    (dom/h3 {:style page-title-style}
                            "TODOs")
                    (dom/button {:style (assoc button-style :marginRight "auto")
                                 :onClick #(comp/transact!
                                            app
                                            [`(mutations/make-new-todo)])}
                                "New Todo"))
           (ui-todo-list todo-list)))

(defsc Root [this {:keys [sign-up-form
                          sign-in-form
                          user-info
                          todo-list
                          todo
                          page]}]
  {:query [{:sign-up-form (comp/get-query SignUpForm)}
           {:sign-in-form (comp/get-query SignInForm)}
           {:user-info (comp/get-query UserInfo)}
           {:todo-list (comp/get-query TodoList)}
           {:todo (comp/get-query TodoForm)}
           :page]
   :initial-state (fn [_params]
                    {:page :loading
                     :todo-list (comp/get-initial-state TodoList)
                     :user-info (comp/get-initial-state UserInfo)
                     :sign-in-form (comp/get-initial-state SignInForm
                                                           {:id :sign-in})
                     :sign-up-form (comp/get-initial-state SignUpForm
                                                           {:id :sign-up})})}
  (dom/div {:style (cond-> page-style
                     (contains? #{:sign-up :sign-in} page)
                     (assoc :height "100%"))}
           (case page
             :loading (loading)
             :sign-up (ui-sign-up-form sign-up-form)
             :sign-in (ui-sign-in-form sign-in-form)
             :todos (todos todo-list user-info)
             :todo-form (ui-todo-form todo))))

(defn ^:export init
  "Shadow-cljs sets this up to be our entry-point function. See shadow-cljs.edn `:init-fn` in
  the modules of the main build."
  []
  (app/mount! app Root "app")
  (df/load! app :user-info UserInfo {:post-mutation `mutations/change-page})
  (js/console.log "Loaded"))

(defn ^:export refresh
  "During development, shadow-cljs will call this on every hot reload of source. See shadow-cljs.edn"
  []
  ;; re-mounting will cause forced UI refresh, update internals, etc.
  (app/mount! app Root "app")
  ;; As of Fulcro 3.3.0, this addition will help with stale queries when using dynamic routing:
  (comp/refresh-dynamic-queries! app)
  (js/console.log "Hot reload"))
