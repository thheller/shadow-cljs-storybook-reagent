(ns example.reagent-glue
  (:require
    [goog.string :as gstr]
    [goog.object :as gobj]
    ;; can be removed or replaced if you prefer something like cljs-devtools
    [shadow.cljs.devtools.client.console]
    [reagent.core :as r]))

;; taken from shadow.json/to-clj which expects only JSON data objects
;; but opts received from storybook may contain functions and other things
;; so added special case to only do basic transformations
;; not using clj->js because of non-customizable keyfn
(defn to-clj
  "simplified js->clj for JSON data, :key-fn default to keyword"
  ([x] (to-clj x {}))
  ([x opts]
   (cond
     (nil? x)
     x

     (number? x)
     x

     (string? x)
     x

     (boolean? x)
     x

     (array? x)
     (into [] (map #(to-clj % opts)) (array-seq x))

     (object? x)
     (let [key-fn (get opts :key-fn keyword)]
       (->> (gobj/getKeys x)
            (reduce
              (fn [result key]
                (let [value (gobj/get x key)]
                  (assoc! result
                    (if (string? key)
                      (key-fn key)
                      (to-clj key opts))
                    (to-clj value opts))))
              (transient {}))
            (persistent!)))

     :else
     x)))

(defn camel->kw [s]
  (keyword (gstr/toSelectorCase s)))

;; wraps a basic reagent component fn for use in a story
;; returns a new function that basically just takes care of translating passed in JS props object

;; the intent behind passing in the ns/name of the component is so that the actual component code
;; can be looked up by name (only works for dev builds) and doesn't need anything special on the component itself

;; this is intentionally as minimal as possible. could be extended as needed.
(defn ^:export wrap [ns component-name]
  (fn [js-opts]
    (let [opts (to-clj js-opts {:key-fn camel->kw})
          cmp (js/goog.getObjectByName (str ns "." component-name))]
      ;; (js/console.log "using opts" js-opts opts)
      (r/as-element [cmp opts]))))
