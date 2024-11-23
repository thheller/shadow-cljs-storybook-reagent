(ns example.components.button)

(defn button [{:keys [label primary size background-color on-click]}]
  (let [mode (if primary
               "storybook-button--primary"
               "storybook-button--secondary")]

    [:button
     {:type "button"
      :class ["storybook-button" (str "storybook-button--" size) mode]
      :on-click on-click
      :style (when background-color {:backgroundColor background-color})}
     label]))