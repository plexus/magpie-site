(ns software.magpie.site.styles
  {:ornament/prefix ""}
  (:require
   [clojure.string :as str]
   [lambdaisland.ornament :as o]))

(o/defprop --mirage      "#0c182e")
(o/defprop --arapawa     "#01095e")
(o/defprop --navy        "#020e9a")
(o/defprop --lavender    "#E6E8FF")
(o/defprop --laser-lemon "#f3f976")
(o/defprop --sandstorm   "#f5f7c8")
(o/defprop --butter      "#ffffe8")

(o/defprop --primary --arapawa)
(o/defprop --secondary --sandstorm)

(o/defprop --base-line-height 1.5)
(o/defprop --heading-line-height 1.15)

(defn utopia-clamp
  "Fluid clamp() CSS value using the Utopia formula."
  [min-base max-base ratio i min-width max-width]
  (let [min-size    (* min-base (Math/pow ratio i))
        max-size    (* max-base (Math/pow ratio i))
        slope       (/ (- max-size min-size)
                       (- max-width min-width))
        y-intercept (- min-size (* min-width slope))]
    (str/replace
     (format "clamp(%frem, %.6frem + %.6fvw, %frem)"
             (double min-size) (double y-intercept) (double (* 100 slope)) (double max-size))
     #"0+rem" "rem")))

(defmacro fluid-props []
  (let [min-vw 20
        max-vw 90
        min-base 1
        max-base 1.25
        ratio 1.25
        space-min 0.5
        space-max 0.75
        space-ratio 1.5]
    `(do
       ~@(for [i (range -3 7)]
           `(o/defprop ~(symbol (str "--text-fluid-" (if (neg-int? i)
                                                       (apply str "0" (repeat (- i) "0"))
                                                       i)))
              ~(utopia-clamp min-base max-base ratio i min-vw max-vw)))
       ~@(for [i (range -3 7)]
           `(o/defprop ~(symbol (str "--space-" (if (neg-int? i)
                                                  (apply str "0" (repeat (- i) "0"))
                                                  i)))
              ~(utopia-clamp space-min space-max space-ratio i min-vw max-vw))))))

(fluid-props)

(o/defrules reset
  [#{:* "*::before" "*::after"}
   {:box-sizing "border-box"
    :margin 0
    :padding 0}]
  [:body {:overflow-wrap "break-word"}])

(o/defrules text
  [:html {:font-size "100%"
          :line-height --base-line-height}]
  [#{:h1 :h2 :h3 :h4 :h5} {:line-height --heading-line-height}]
  [:h1   {:font-size --text-fluid-6}]
  [:h2   {:font-size --text-fluid-5}]
  [:h3   {:font-size --text-fluid-4}]
  [:h4   {:font-size --text-fluid-3}]
  [:h5   {:font-size --text-fluid-2}]
  [:body {:font-size --text-fluid-1}]
  [:.byline
   {:font-size --text-fluid-0}])

(o/defrules spacing
  [:main {:max-width "48em"
          :margin    "0 auto"
          :padding-left --space-0
          :padding-right --space-0}]
  [:footer
   [:>* {:max-width "48em"
         :margin    "0 auto"
         :padding-left --space-0
         :padding-right --space-0}]
   [:>:first-child {:padding-top --space-1}]
   [:>:last-child {:padding-bottom --space-2}]]
  [:h1 {:margin-top --space-4 :margin-bottom --space-2}]
  [:h2 {:margin-top --space-3 :margin-bottom --space-1}]
  [:h3 {:margin-top --space-2 :margin-bottom --space-0}]
  [:h4 {:margin-top --space-1 :margin-bottom --space-00}]
  [:header {:margin-bottom --space-3}]
  [:p {:margin-bottom --space-1}]
  [:pre {:margin-bottom --space-1
         :padding --space-0}]
  [:blockquote
   {:display "flex"
    :flex-direction "column"
    :gap --space-00
    :padding --space-0
    :margin-bottom --space-1}
   [">:first-child" {:margin-top 0}]
   [">:last-child" {:margin-bottom 0}]]
  [#{:ul :ol}
   {:margin-bottom --space-1
    :padding-left --space-2}])

(o/defrules typograhpy
  [:body
   {:font-family "system, sans-serif"}]
  [#{:h1 :h2 :h3 :h4 :h5}
   {:font-family "'Ostrich Sans', sans-serif"
    :font-weight "400"}]
  ["main::after" {:display "block"
                  :text-align "center"
                  :font-size --text-fluid-3
                  :content (pr-str "❦")
                  :margin-top --space-2
                  :margin-bottom --space-2}])

(o/defrules colour
  [:body
   {:background-color --sandstorm
    :color            --arapawa}]
  [:a {:color --navy}]
  [#{:a:focus :a:active} {:background-color --lavender}]
  [:footer
   {:background-color --arapawa
    :color            --sandstorm}
   [#{:a "a:visited" "a:active"} {:color --laser-lemon}]]
  [:pre
   {:color            "#222"
    :background-color "#eee"
    }]
  [:blockquote
   {:border-left      "3px solid var(--arapawa)"
    :background-color --butter}]
  )

(o/defrules styles
  [:a {:text-decoration "underline dotted"}]
  [:a:hover {:text-decoration "underline solid"}]
  [:pre
   {:border-radius    "0.1rem"
    :box-shadow       "rgba(0, 0, 0, 0.16) 0px 1px 4px"
    :overflow         "auto"}]

  [:blockquote
   {:border-radius    "0.2rem"
    :box-shadow       "rgba(0, 0, 0, 0.16) 0px 1px 4px"}]

  [:.invisible-whitespace
   {:display "inline-block"
    :width   0}]

  [:.visible-whitespace
   {:opacity "0.2"
    ;;:user-select "none"
    }]

  [".visible-whitespace::before"
   {:content "attr(data-content)"}]

  [:footer
   [:h2 [:a {:text-decoration "none"}]]
   [:p {:font-style "italic"}]
   ])
