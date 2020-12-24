(ns fi.velotoken.ux.events
  (:require
   [re-frame.core :as re-frame :refer [inject-cofx]]
   [akiroz.re-frame.storage :refer [reg-co-fx!]]
   [fi.velotoken.ux.db :as db]
   [cljs-time.core :as t]
   [cljs-time.coerce :as tc]))

(reg-co-fx! :fi-velotoken-ux ;; local storage key
            {:fx :store      ;; re-frame fx ID
             :cofx :store})  ;; re-frame cofx ID

(re-frame/reg-event-fx
 ::initialize-db
 [(inject-cofx :store)]
 (fn [{:keys [store]} _]
   {:db (-> db/default-db
            ;; NOTE: disabled as we get them from
            ;; initialization
            #_ (assoc :accounts (:accounts store)) 
            )
    :coingecko [:update]
    :web3 [:initialize]
    :dispatch-interval-multiple
    [{:dispatch [::update-last-rebase-counter]
      :id :last-rebase-counter
      :ms 1000 }
     {:dispatch [::flash-update]
      :id :flash-update
      :ms 1000 }
     ;; reload rebase-data each x seconds
     {:dispatch [::web3-velo-rebase-data]
      :id :flash-update
      :ms 10000 }
     ;; reload coingecko-data each x seconds
     {:dispatch [::coingecko-sync]
      :id :flash-update
      :ms 10000 }
     ] 

    }))

(re-frame/reg-event-fx
  ::web3-initialized
  (fn [{:keys [db]} [_ {:keys [accounts]}]]
    {:db (-> db 
             (assoc :ethereum-injected? true)
             (assoc :accounts accounts))
     :web3-multiple [[:velo-rebase-data]]}))

(re-frame/reg-event-db
  ::web3-ethereum-not-present
  (fn [db _]
    (assoc db :ethereum-injected? false)))

(re-frame/reg-event-fx 
  ::web3-velo-rebase-data
  (fn [_ _]
    {:web3 [:velo-rebase-data]}))

(re-frame/reg-event-db
  ::web3-velo-rebase-data-recv
  (fn [db [_ data]]
    (prn data)
    (assoc db :rebase-data data)))

(re-frame/reg-event-db
  ::update-last-rebase-counter
  (fn [db [_]]
    (when-let [last-rebase (-> db :rebase-data :last-rebase)]
      (let [next-rebase (+ last-rebase (* 12 60 60))
            now (int  (/ (tc/to-long (t/now)) 1000))
            remaining (- next-rebase now)]
        (assoc db :last-rebase-counter remaining)))))

;; Error messages
(defn flash [db ftype message]
  (assoc db :flash {:type ftype :message message :duration 5}))

(re-frame/reg-event-db
  ::flash
  (fn [db [_ {:keys [type message]}]]
    (flash db type message)))

(re-frame/reg-event-db
  ::flash-update
  (fn [db _]
    (if (pos? (-> db :flash :duration))
      (update-in db [:flash :duration] dec)
      (dissoc db :flash))))


;; Coingecko update

(re-frame/reg-event-fx
  ::coingecko-sync
  (fn [_ [_ info]]
    {:coingecko [:update]}))

(re-frame/reg-event-db
  ::coingecko-update
  (fn [db [_ info]]
    (assoc db :coingecko info)))

;; Web3 add token
(re-frame/reg-event-fx
  ::web3-add-token
  (fn [_ _]
    {:web3 [:add-token {:type "ERC20"
                        :options 
                        {:address "0x98ad9b32dd10f8d8486927d846d4df8baf39abe2"
                         :symbol "VLO"
                         :decimals 18
                         :image "https://i.ibb.co/6gN5Mxb/logo-vector-red-on-yellow.png"
                         }}]}))

(re-frame/reg-event-db
  ::web3-add-token-confirmed
  (fn [_ _]
    (prn "web3 add token confirmed")))

(re-frame/reg-event-db
  ::web3-add-token-rejected
  (fn [_ _]
    (prn "web3 add token rejected")))

;; Web3 rebaser
(re-frame/reg-event-fx
  ::web3-call-rebase
  (fn [_ _]
    {:web3 [:velo-call-rebase]}))

(re-frame/reg-event-fx
  ::web3-call-rebase-success
  (fn [{:keys [db]} _]
    ;; on success reload the rebase data
    {:db (flash db :notice "We wrote history! Rebase call executed!")
     :web3 [:velo-rebase-data]}))

;; Web3 events

(re-frame/reg-event-fx
  ::web3-connect
  (fn [_ _]
    {:web3 [:connect]}))

(re-frame/reg-event-fx
  ::web3-velo-token-data
  (fn [_ _]
    {:web3 [:velo-token-data]}))

(re-frame/reg-event-db
  ::web3-locked
  (fn [db _]
     (assoc db :provider-not-present true)))

(re-frame/reg-event-fx
  ::web3-accounts-changed
  [(inject-cofx :store)]
  (fn [{:keys [store db]} [_ accounts]]
    {:db (assoc db :accounts accounts)
     :store (assoc store :accounts accounts)}
    ))

(re-frame/reg-event-db
  ::web3-chain-changed
  (fn [_ [_ chain-id]]))

(re-frame/reg-event-db
  ::web3-connected
  (fn [_ [_ connect-info]]
     (prn "connection-info" connect-info)))

(re-frame/reg-event-db
  ::web3-disconnect
  (fn [_ [_ rpc-error]]
     (prn "disconnected" rpc-error)))

(re-frame/reg-event-db
  ::web3-message
  (fn [_ [_ message]]
     (prn "message" message)))
