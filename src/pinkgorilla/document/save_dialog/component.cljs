(ns pinkgorilla.document.save-dialog.component
  (:require
   [taoensso.timbre :refer-macros [info error]]
   [reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch]]
   [re-com.core :refer [input-text radio-button]]
   [pinkgorilla.storage.filename-encoding :refer [split-filename encoding->extension]]
   [pinkgorilla.storage.protocols :refer [create-storage]]))

#_(defn do-save [form]
    (let [form2 (if (> (count (:file-directory form)) 0)
                  (assoc form :filename (str (:file-directory form) "/" (:filename form)))
                  form)]
      (dispatch-sync [:save-as-storage form2]) ; sets the :storage in app db
      (dispatch-sync [:save-dialog-cancel]) ; closes the dialog
      (dispatch-sync [:save-notebook]) ; save notebook
      (dispatch-sync [:nav-to-storage]))) ; navigates to the document just saved


(def empty-form
  {; storage-type to create storage
   :storage-type :file
   ; query-params for document-save
   ;:description ""
   :user ""
   :repo ""
   :filename "/my-new-notebook.cljg"
   :name "my-new-notebook"
   :path "/"
   ;encoding format
   :format :gorilla
   :explorer-root nil ; the keyword corresponding to the selected explorer root directory
   :file-directory "" ; directory corresponding to selected explorer root
   })

(defn calc-filename [state]
  (let [{:keys [format name path]} @state
        filename (str path name "." (encoding->extension format))]
    (swap! state assoc :filename filename)))

(defn make-storage [state]
  (let [storage-type (keyword (:storage-type state))
        _ (info "saving as storage type: " storage-type)
        storage (create-storage (assoc state :type storage-type))]
    (when-not storage
      (error "save-dialog could not make storage for: " state))
    storage))

(defn storage-type [state change!]
  [:div.m-2.w-16
   [:p "Storage"]
   [radio-button
    :label       "file"
    :value       :file
    :model       (:storage-type @state)
    :on-change   #(change! :storage-type %)]
   [radio-button
    :label       "gist"
    :value       :gist
    :model       (:storage-type @state)
    :on-change   #(change! :storage-type %)]
   [radio-button
    :label       "repo"
    :value       :repo
    :model       (:storage-type @state)
    :on-change   #(change! :storage-type %)]])

(defn storage-format [state change!]
  [:div.m-2.w-24
   [:p "Format"]
   [radio-button
    :label       "gorilla"
    :value       :gorilla
    :model       (:format @state)
    :on-change   #(change! :format %)]
   [radio-button
    :label       "jupyter"
    :value       :jupyter
    :model       (:format @state)
    :on-change   #(change! :format %)]
   [radio-button
    :label       "clj"
    :value       :clj
    :model       (:format @state)
    :on-change   #(change! :format %)]])

(defn github [state change!]
  [:div.m-2.mt-5.pt-3
   [input-text
    :model           (:user @state)
    :width            "300px"
    :placeholder      "github user name"
    :on-change        #(change! :user %)
    :disabled?        (not (contains? #{:repo :gist} (:storage-type @state)))]

   [input-text
    :model           (:repo @state)
    :width            "300px"
    :placeholder      "github repo name"
    :on-change        #(change! :repo %)
    :disabled?        (not (contains? #{:repo} (:storage-type @state)))]

   [input-text
    :model           (:description @state)
    :width            "300px"
    :placeholder      "gist description"
    :on-change        #(change! :description %)
    :disabled?        (not (= :gist (:storage-type @state)))]])

(defn file [state change!]
  (let [config (subscribe [:explorer/config])]
    (fn [state change!]
      (when-let [repositories-all (:repositories @config)]
        (let [repositories (filter :save repositories-all)]
          [:div
           [:div
            [:h3 "File Directory"]
            [:p (pr-str repositories)]
            (doall (map (fn [{:keys [name url]}]
                          ^{:key name}
                          [radio-button
                           :label      name
                           :value       name
                           :model       (:explorer-root @state)
                           :on-change   #(do (change! :explorer-root name)
                                             (change! :file-directory url))])
                        repositories))
            [:p (str "Directory: " (:file-directory @state))]]])))))

(defn filename [state change!]
  [:div
   [:div.flex.flex-row
    [:h3.w-16 "Path"]
    [:input.w-64 {;; blur does not work - prevents the click
            ;; :on-blur     #(dispatch [:save-dialog-cancel])
            ;; :on-mouse-down #(dispatch [:save-dialog-cancel])
                  :type      "text"
                  :value     (:path @state)
                  :on-change #(change! :path (-> % .-target .-value))}]]
   [:div.flex.flex-row
    [:h3.w-16 "Name"]
    [:input.w-64 {;; blur does not work - prevents the click
            ;; :on-blur     #(dispatch [:save-dialog-cancel])
            ;; :on-mouse-down #(dispatch [:save-dialog-cancel])
                  :type      "text"
                  :value     (:name @state)
                  :on-change #(change! :name (-> % .-target .-value))}]]
   [:div.flex.flex-row
    [:h3.w-16 "Full"]
    [:span (:filename @state)]]])

(defn storage->form [storage]
  (if storage
    (let [storage-seq (into {} storage)
          file-info (split-filename (:filename storage-seq))]
      {:filename (:full file-info)
       :format (or (:encoding file-info) :gorilla)
       :name (or (:name file-info) "")
       :path (or (:path file-info) "")})
    {}))

(defn save-dialog
  [{:keys [storage on-cancel on-save]}]
  (let [state (r/atom (merge empty-form (storage->form storage)))
        change! (fn [k v]
                  (swap! state assoc k v)
                  (calc-filename state))
        check-key (fn [form keycode]
                    (case keycode
                      27 (on-cancel) ; ESC
                      13 (on-save storage (make-storage @state))   ; Enter
                      nil))]
    (fn [{:keys [on-cancel on-save]}]
      [:div.bg-blue-300.m-5.border-solid.inline-block
       {:display {:width "200px"}
        :on-key-down   #(check-key @state (.-which %))}
       [:div.flex.flex-row.justify-start
        [storage-format state change!]
        [storage-type state change!]
        [github state change!]]
       [filename state change!]
       (when (= :file (:source @state))
         [file state change!])
       [:div.flex.flex-row.justify-between
        [:div.bg-red-700.m-2.w-16.p-1.text-center
         {:on-click #(on-cancel)}
         "Cancel"]
        [:div.bg-green-700.m-2.w-16.p-1.text-center
         {:on-click #(on-save storage (make-storage @state))}
         "Save"]]])))