(ns ^:figwheel-always ns-events.core
    (:require-macros [cljs.core.async.macros :refer [go go-loop]])
    (:require [cljs.core.async :as async :refer [chan put!]]
              [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:animals ["Aguila" "Burro" "Cocodrilo"]}))

(defn e->val [e]
  (.. e -target -value))

;; Note: data is not a cursor but a simple string which is a problem
(defn editable [data owner {:keys [ch]}]
  (reify
    om/IDisplayName
    (display-name [_] "Editable")
    om/IRender
    (render [_]
      (dom/input #js {:value data
                      :onChange #(put! ch [:edit (e->val %)])}))))

;; Higher order components don't work
(defn tagger [component ch index]
  (fn [data owner opts]
    (let [indexed-ch ch]
      (reify
        om/IDisplayName
        (display-name [_] "Tagged")
        om/IRender
        (render [_]
          (om/build component data {:opts (merge {:ch indexed-ch} opts)
                                    :react-key (str "editable-" index)}))))))

(defn sortable [data owner opts]
  (reify
    om/IDisplayName
    (display-name [_] "Sortable")
    om/IRender
    (render [_]
      (apply dom/ul nil
        (map-indexed #(om/build editable %2 {:opts opts})
          data)))))

(let [ch (chan)]
  (om/root
    (fn [data owner]
      (reify
        om/IWillMount
        (will-mount [_]
          (go-loop []
            (let [e (<! ch)]
              ;; This 0 is the biggest problem right here, tagging
              ;; things up
              (om/update! data [:animals 0] (second e))
              (println e))
            (recur)))
        om/IRender
        (render [_]
          (dom/div nil          
            (dom/h1 nil "Animales")
            (om/build sortable (:animals data) {:opts {:ch ch}})))))
    app-state
    {:target (. js/document (getElementById "app"))}))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
) 

