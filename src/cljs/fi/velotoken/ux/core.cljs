(ns fi.velotoken.ux.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [fi.velotoken.ux.events :as events]
   [fi.velotoken.ux.views :as views]
   [fi.velotoken.ux.config :as config]
   [fi.velotoken.ux.web3.fx]
   [fi.velotoken.ux.coingecko-fx]
   [fi.velotoken.ux.interval-fx]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))

