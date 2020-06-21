(ns pinkgorilla.explore.events-fetch-index
  "load list of explored notebooks"
  (:require
   ;[taoensso.timbre :as timbre :refer-macros (info)]
   [re-frame.core :refer [reg-event-db reg-event-fx]] 
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   ; PinkGorilla Libraries
   ; dependencies needed to be in cljs bundle:
   [pinkgorilla.storage.storage :refer [create-storage]]
   [pinkgorilla.storage.file]
   [pinkgorilla.storage.gist]
   [pinkgorilla.storage.repo]
   [pinkgorilla.storage.bitbucket]
   ;PinkGorilla Notebook
   ;[pinkgorilla.events.helper :refer [standard-interceptors]]
   ))

(reg-event-fx
 :explorer/fetch-index
 (fn [{:keys [db]} [_ repository]]
   ;(add-notification (notification :info (str "Exploring " (:name repository))))
   {:db         db
    :http-xhrio {:method          :get
                 :uri             (:url repository)
                 :timeout         10000                    ;; optional
                 :response-format (ajax/json-response-format {:keywords? true}) ; (ajax/transit-response-format) ;; IMPORTANT!: You must provide this.
                 :on-success      [:explorer/fetch-response]
                 :on-failure      [:process-error-response "load-explore-data"]}}))

(defn remove-repo-id [item]
  (if (= (:type item) :repo)
    (dissoc item :id)
    item))

(defn add-storage [item]
  (assoc item :storage (create-storage item)))

(defn preprocess-item [start-index idx item]
  (-> item
      (assoc :type (keyword (:type item)) :index (+ start-index idx))
      (remove-repo-id)
      (add-storage)))

(defn preprocess-list [start-index response]
  (let [list (:data response)]
    (vec (map-indexed (partial preprocess-item start-index) list))))

(reg-event-db
 :explorer/fetch-response
 ;[standard-interceptors]
 (fn [db [_ response]]
   (let [existing-data (get-in db [:explorer :notebooks])
         start-index (count existing-data)
         new-data (preprocess-list start-index response)]
     (-> (assoc-in db [:explorer :notebooks] (concat existing-data new-data))))))



(reg-event-fx
 :explorer/fetch-indices
 (fn [{:keys [db]}]
   (let [events (->> (get-in db [:explorer :config :repositories])
                     (map (fn [repo] (conj [:explorer/fetch-index] repo)))
                     vec)]
     {:db         db
      :dispatch-n events})))
