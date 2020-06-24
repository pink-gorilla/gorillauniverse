(ns pinkgorilla.document.subscriptions
  (:require
   [clojure.walk]
   [taoensso.timbre :as timbre :refer [debug info warn error]]
   [re-frame.core :refer [reg-sub subscribe dispatch]]
   [pinkgorilla.document.events]))


(reg-sub
 :document/get
 (fn [db [_ storage]]
   (info "document view for: " storage)
   (get-in db [:documents storage])))