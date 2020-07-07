(ns demo.views
  (:require
   [taoensso.timbre :as timbre :refer [debug info warn error]]
   [pinkgorilla.ui.config :refer [link-css]]
   [pinkgorilla.ui.ui.dialog :refer [modal-container]]
   [pinkgorilla.bidi.routes :refer [goto! current query-params]]
   [pinkgorilla.explore.component :refer [notebook-explorer]]
   [pinkgorilla.document.component :refer [document-page]]
   [pinkgorilla.explorer.bidi :refer [goto-notebook!]]
   [demo.save-dialog-demo :refer [save-dialogs]]))

(defn document-view-dummy [storage document]
  [:div

   #_[:div.m-3.bg-blue-300
    [:a {:on-click #(goto! :ui/explorer)}
     "explorer"]]

   [:div.m-16.bg-orange-400
    [:h1 "Document Meta Info - Replace me with your document viewer!"]
    [:p " storage: " (pr-str storage)]]
   [:div.m-16.bg-blue-300 "document: " @document]])

(defn open-notebook [nb]
  (info "load-notebook-click" nb)
  (goto-notebook! (:storage nb)))


(defn main []
  [:div
   [:h1 "demo - explorer"]
   [:a.bg-green-300 {:href "/explorer"} "explorer"]
   [:a.bg-red-300 {:href "/demo/save"} "save-as dialog demo"]])

(defn not-found []
  [:div
   [:h1 "route handler not found: "]
   [:p @current]])

(defn app []
  [:div
   [link-css "tailwindcss/dist/tailwind.css"]
   [link-css "@fortawesome/fontawesome-free/css/all.min.css"]
   [modal-container]
   ;[:h1 "explorer-ui"]
   ;[:p (str "route: " (pr-str @current))]
   (case (:handler @current)
     :ui/explorer [notebook-explorer open-notebook]
     :ui/notebook [document-page @query-params document-view-dummy]
     :demo/main [main]
     :demo/save [save-dialogs]
     [not-found])])


