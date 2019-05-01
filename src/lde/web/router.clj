(ns lde.web.router
  (:require [reitit.ring :as ring]
            [reitit.ring.middleware.parameters :refer [parameters-middleware]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [lde.core.settings :as settings]
            [lde.web.css :as css]
            [lde.web.pages.topic :as topic]
            [lde.web.pages.event :as event]
            [lde.web.pages.home :as home]
            [lde.web.pages.login :as login]))

(defn authorize [handler]
  (fn [req]
    (if (get-in req [:session :id])
      (handler req)
      {:status 403
       :body "unauthorized"})))

(defn routes []
  [["/css/main.css" {:get css/handler}]
   ["/" {:get home/handler}]
   ["/login" {:get login/handler
              :post login/post-login}]
   ["/signup" {:get login/handler
               :post login/post-signup}]
   ["/logout" {:get login/logout}]
   ["/new" {:middleware [authorize]
            :get topic/handler
            :post topic/post-topic}]
   ["/for/:topic" {:middleware [authorize]}
    ["" {:get topic/overview}]
    ["/new" {:get event/new
             :post event/post}]
    ["/about/:event"
     ["/" {:get event/get}]
     ["/join" {:post event/join}]]]])

(defn make-context-middleware [ctx]
  (fn [handler]
    (fn [req]
      (handler (assoc req :ctx ctx)))))

(defn make-session-middleware [ctx]
  (let [store (cookie-store {:key (settings/get-cookie-secret ctx)})]
    (fn [handler]
      (wrap-session handler {:store store
                             :cookie-name "letsdoevents-session"}))))

(defn init [ctx]
  (ring/ring-handler
   (ring/router (routes))
   (ring/routes
     (ring/redirect-trailing-slash-handler)
     (ring/create-default-handler))
   {:middleware [parameters-middleware
                 wrap-keyword-params
                 (make-session-middleware ctx)
                 (make-context-middleware ctx)]}))
