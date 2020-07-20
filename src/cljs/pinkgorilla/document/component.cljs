(ns pinkgorilla.document.component
  (:require
   [clojure.walk]
   [taoensso.timbre :as timbre :refer [debug info warn error]]
   [re-frame.core :refer [subscribe dispatch]]
   [pinkgorilla.storage.protocols :refer [query-params-to-storage]]
   [pinkgorilla.document.events] ; side effects
   [pinkgorilla.document.subscriptions] ; side effects
   [pinkgorilla.document.header :refer [document-view-with-header]]))

(defn err [storage document message]
  [:div.m-6.p-6.bg-red-300.border-solid
   [:h1 message]
   (when storage
     [:p "storage: " (pr-str storage)])
   (when document
     [:p (pr-str (:error document))])])

(defn document-viewer [document-view storage document]
  [:div
       ;[:h1 "Document Viewer"]
       ;[:p (pr-str params)]
   (cond
     (not storage)
     [err storage @document "storage parameter are bad!"]

     (nil? @document)
     [err storage @document "requesting document"]

     (= :document/loading @document)
     [err storage @document "Loading .."]

     (:error @document)
     [err storage @document "Error Loading Document"]

     :else
     [document-view-with-header storage document document-view])])

(defn get-storage [params]
  (let [kparams (clojure.walk/keywordize-keys params)
        ;_ (info "document page kw params: " kparams)
        stype (keyword (:source kparams))
        storage (query-params-to-storage stype kparams)]
    storage))

(defn document-page [params document-view]
  (let [storage (get-storage params)
        document (when storage (subscribe [:document/get storage]))]
    (when (and storage (not @document))
      ;(info "loading document storage: " storage)
      (dispatch [:document/load storage]))
    (info "rendering document-page params: " params)
    [document-viewer document-view storage document]))





