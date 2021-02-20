(ns fi.velotoken.ux.web3.provider
  (:require

   [re-frame.core :refer [reg-fx dispatch]]
   [fi.velotoken.ux.events :as events]
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer [<p!]]
   [async-error.core :refer-macros [go-try]]
   [oops.core :refer [ocall]]
   ["ethers" :as ethers]
   ["web3modal" :as w3m :default Web3Modal]
   ["@walletconnect/web3-provider" :default WalletConnectProvider]))

(defonce ^:dynamic *provider* (atom nil))

(def provider-options
  {:walletconnect
   {:package WalletConnectProvider,
    :options
    {:infuraId "663e21b048c2434bb01f364537fc4706"}}})

(defn create-modal []
  (Web3Modal. (clj->js {:providerOptions provider-options
                        :disableInjectedProvider false
                        :cacheProvider true
                        :theme "dark"})))

(defn provider []
  (when @*provider*
    (let [web3-provider (. ethers/providers -Web3Provider)]
      (web3-provider. @*provider*))))

(defn connect []
  (go-try
   (let [modal (create-modal)]
     (try
       (reset! *provider* (<p! (ocall modal :connect)))

       ;; register events of interest
       (.on @*provider* "accountsChanged" (fn [accounts] (dispatch [::events/web3-accounts-changed (js->clj accounts)])))
       (.on @*provider* "chainChanged" (fn [chain-id] (dispatch [::events/web3-chain-changed (js->clj chain-id)])))
       (.on @*provider* "connect" (fn [connect-info] (dispatch [::events/web3-connected (js->clj connect-info)])))
          ;; RPC Error {:message .., :code .., :data ..}
       (.on @*provider* "disconnect" (fn [rpc-error] (dispatch [::events/web3-disconnect (js->clj rpc-error)])))
       (.on @*provider* "message" (fn [message] (dispatch [::events/web3-message (js->clj message)])))

       (provider)
       (catch js/Error e
          ;; NOTE: after reading web3modal code, noticed there
          ;; is no way to get to the specific provider error.
          ;; https://github.com/Web3Modal/web3modal/blob/master/src/core/index.tsx#L71
          ;; maybe this changes in the future.
         (reset! *provider* nil)
         (throw (ex-info "Web3Modal connect error" {:type ::connect} (ex-cause e))))))))

(defn disconnect []
  (go-try
   (ocall @*provider* :?disconnect)
   (.clearCachedProvider (create-modal))
   (reset! *provider* nil)))

(defn cached-provider? []
  (not= (.-cachedProvider (create-modal)) ""))

#_(prn "BD" (.-cachedProvider (Web3Modal. (clj->js {:providerOptions  provider-options}))))
#_(connect)



