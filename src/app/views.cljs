(ns app.views
  (:require [reagent.core :as reagent :refer [atom]]
            ["react-grid-layout" :as GridLayout]
            [clojure.string :refer [join split]]
            ["chart.js" :as Chart]
            [brave.cmc :as cmc]))


;DATA------------------------------------------------------------------------------------------------

(def data (cmc/init {:apikey "testing-the-board11" :host "cc.zgen.hu" :protocol :https :reagent? true}))
(def state (atom {:darkmode true}))
(def colorvector (atom ["#00ADB5" "#36f6ff" "#c4fcff" "#016369" "#76D7C4" "#48C9B0" "#1ABC9C" "#17A589" "#148F77" "#117864" "#0E6251" "#117864"]))
(def key-list (atom '("a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m" "n" "o" "p" "q" "r" "s" "t" "u" "v" "w")))

;----------------------------------------------BOGUS DATA FUNCTIONS----------------------------------

(defn browser-checker []
 (let [userAgent (str (-> js/navigator .-userAgent))]
  (cond
    (clojure.string/includes? userAgent "SeaMonkey") "SeaMonkey"
    (clojure.string/includes? userAgent "Chromium") "Chromium"
    (clojure.string/includes? userAgent "Firefox") "Firefox"
    (clojure.string/includes? userAgent "Chrome") "Chrome"
    (clojure.string/includes? userAgent "Safari") "Safari"
    (or (clojure.string/includes? userAgent "OPR") (clojure.string/includes? userAgent "Opera")) "Opr"
    (or (clojure.string/includes? userAgent "MSIE") (clojure.string/includes? userAgent "Trident")) "MSIE"
    (clojure.string/includes? userAgent "Edg") "Edg"
    :else "Unknown browser")))

(defn gift [timestamp]
  (let [newuser (atom true)
        cookie? (-> js/navigator .-cookieEnabled)]
    (when cookie?
      (if (nil? (-> js/window .-localStorage (.getItem "id"))) (.setItem (.-localStorage js/window) "id" timestamp) (reset! newuser false)))
    (let [id (keyword (if cookie? (.getItem (.-localStorage js/window) "id") (.getTime (js/Date.))))]
      (swap! data assoc id
       {:newuser @newuser
        :browserName (browser-checker)
        :siteLocation (-> js/window .-location .-hostname)
        :osName (-> js/navigator .-platform)
        :cpuCores (-> js/navigator .-hardwareConcurrency)
        :deviceManufacturer (cond (< (-> js/screen .-width) 768) "mobile" (= (-> js/screen .-width) 768) "tablet" (> (-> js/screen .-width) 768) "desktop")
        :screenHeight (-> js/screen .-height)
        :screenWidth (-> js/screen .-width)
        :cookies? (-> js/navigator .-cookieEnabled)
        :cookies (-> js/document .-cookie)
        :colorDepth (-> js/screen .-colorDepth)
        :pixelDepth (-> js/screen .-pixelDepth)
        :pathName (-> js/window .-location .-pathname)
        :clientTime (.Date js/window)
        :referrer (-> js/document .-referrer)
        :prevSites (-> js/history .-length)
        :protocol (-> js/window .-location .-protocol)
        :browserLang (-> js/navigator .-language)
        :time (.getTime (js/Date.))}))))

(defn extraadd []
  (let [newuser (atom {:key true})]
    (swap! data assoc (keyword (str (.getTime (js/Date.)))) {:newuser true
                                                             :browserName (nth (list "SeaMonkey" "Chrome" "Chromium" "Firefox" "Microsoft Edge" "Opera" "Safari" "Internet Explorer") (rand-int 8))
                                                             :siteLocation (nth (list "zgen.hu" "incognito" "zawiasa.hu") (rand-int 3))
                                                             :osName (nth (list "Windows" "MacOS" "Android") (rand-int 3))
                                                             :cpuCores (-> js/navigator .-hardwareConcurrency)
                                                             :deviceManufacturer (nth (list "mobile" "desktop" "tablet") (rand-int 3))
                                                             :screenHeight (-> js/screen .-height)
                                                             :screenWidth (-> js/screen .-width)
                                                             :cookies? false
                                                             :cookies (-> js/document .-cookie)
                                                             :colorDepth (-> js/screen .-colorDepth)
                                                             :pixelDepth (-> js/screen .-pixelDepth)
                                                             :pathName (-> js/window .-location .-pathname)
                                                             :clientTime (.Date js/window)
                                                             :referrer (-> js/document .-referrer)
                                                             :prevSites (-> js/history .-length)
                                                             :protocol (-> js/window .-location .-protocol)
                                                             :browserLang (-> js/navigator .-language)
                                                             :time (- (.getTime (js/Date.)) (rand-int 604800000))})))

(defn adatbazisdel []
  (reset! data {}))

(defn remove-item! []
  (.removeItem (.-localStorage js/window) "id"))

;------------------------------------------------------------------------------------------------------
;---------------------------------------------FUNCTIONS------------------------------------------------
;------------------------------------------------------------------------------------------------------

(defn tovector [record key & [map?]]
  (let [list (atom ())]
    (doseq [i (keys @data)]
      (reset! list (conj @list (get-in @data [i (keyword key)]))))
    (let [val (atom (sorted-map))]
      (doseq [i (range (count @list))]
        (when (<= 0 ((keyword (str (nth @list i))) @val)) (swap! val update (keyword (str (nth @list i))) inc)))
      (if (nil? map?) (into [] (if (= record "key") (keys @val) (vals @val)))
                      @val))))

(defn threshold [record key percentage]
  (let [val (atom (sorted-map))]
    (doseq [i (keys (tovector record key true))]
      (if (< percentage (/ (i (tovector record key true))  (count @data)))
        (swap! val assoc i (i (tovector record key true)))
        (swap! val assoc :Other (i (tovector record key true)))))
    (if (= record "key") (keys @val) (vals @val))))

(defn userselector [new mode]
  (let [list (atom ())]
    (doseq [i (keys @data)]
      (when (case new
              0 true
              1 (get-in @data [i :newuser])
              2 (not (get-in @data [i :newuser])))
            (reset! list (conj @list (get-in @data [i :time])))))
    (let [val (atom ())]
      (doseq [i (range (count @list))]
        (when (>= (cond (= mode "day") 86400000
                        (= mode "week") 604800000
                        (= mode "active") 600000
                        (= mode "month") 2629746000) (- (.getTime (js/Date.)) (nth @list i)) (reset! val (conj @val (nth @list i))))))
      (str (count @val)))))

(defn truecounter [key]
  (let [list (atom ())]
    (doseq [i (keys @data)]
      (reset! list (conj @list (get-in @data [i (keyword key)]))))
    (let [val (atom {:true 0 :false 0})]
      (doseq [i (range (count @list))]
        (if (nth @list i) (swap! val update :true inc) (swap! val update :false inc)))
      (into [] (vals @val)))))

(defn time-to []
  (let [list (atom ())]
    (doseq [i (keys @data)]
      (reset! list (conj @list (get-in @data [i :time]))))
    (let [val (atom {:1 0 :2 0 :3 0 :4 0 :5 0})]
      (doseq [i @list]
        (if (> 1 (/ (- (.getTime (js/Date.)) i) 86400000)) (swap! val update-in [:1] inc) (if (> 2 (/ (- (.getTime (js/Date.)) i) 86400000)) (swap! val update-in [:2] inc) (if (> 3 (/ (- (.getTime (js/Date.)) i) 86400000)) (swap! val update-in [:3] inc) (if (> 4 (/ (- (.getTime (js/Date.)) i) 86400000)) (swap! val update-in [:4] inc) (if (> 5 (/ (- (.getTime (js/Date.)) i) 86400000)) (swap! val update-in [:5] inc)))))))
      (into [] (reverse (vals @val))))))

(defn dynamicText [size text]
  (join (list (- size (* 30 (count (str text)))) "px")))

(defonce timer (atom (js/Date.)))

(defonce time-updater (js/setInterval
                       #(reset! timer (js/Date.)) 1000))

(defn clock []
  (let [time-str (-> @timer .toTimeString (split " ") first)]
    [:div
     {:style {:color "#00ADB5"}}
     time-str]))

;------------------------------------------------------------------------------------------------------
;---------------------------------------------CHARTS---------------------------------------------------
;------------------------------------------------------------------------------------------------------

;DEVICES CHAR

(defn show-revenue-chart-devices
  []
  (let [context (.getContext (.getElementById js/document "rev-chartjs-devices") "2d")
        chart-data {:type "pie"
                    :data {
                           :labels (tovector "key" "deviceManufacturer")
                           :datasets [{:data (tovector "val" "deviceManufacturer")
                                       :backgroundColor @colorvector}]}
                    :options {:animation {:duration 0}
                              :legend {:display true :position "bottom" :align "start" :labels {:fontSize 20 :fontColor (if (not (:darkmode @state)) "#738598" "white")}}}}]
      (js/Chart. context (clj->js chart-data))))

(defn rev-chartjs-component-devices
  []
  (reagent/create-class
    {:component-did-mount #(show-revenue-chart-devices)
     :display-name        "chartjs-component-devices"
     :reagent-render      (fn []
                            [:canvas {:id "rev-chartjs-devices" :width "100%" :height "100%"}])}))

;BROWSER CHART-----------------------------------------------------------------------------------------

(defn show-revenue-chart-browser
  []
  (let [context (.getContext (.getElementById js/document "rev-chartjs-browser") "2d")
        chart-data {:type "doughnut"
                    :data {
                           :labels (threshold "key" "browserName" 0.1)
                           :datasets [{:data (threshold "val" "browserName" 0.1)
                                       :backgroundColor @colorvector}]}
                    :options {:animation {:duration 0}
                              :legend {:display true :position "bottom" :align "start" :labels {:fontSize 20 :fontColor (if (not (:darkmode @state)) "#738598" "white")}}}}]
      (js/Chart. context (clj->js chart-data))))

(defn rev-chartjs-component-browser
  []
  (reagent/create-class
    {:component-did-mount #(show-revenue-chart-browser)
     :display-name        "chartjs-component-browser"
     :reagent-render      (fn []
                            [:canvas {:id "rev-chartjs-browser" :width "100%" :height "100%"}])}))

;OS CHART-----------------------------------------------------------------------------------------

(defn show-revenue-chart-os
  []
  (let [context (.getContext (.getElementById js/document "rev-chartjs-os") "2d")
        chart-data {:type "doughnut"
                    :data {
                           :labels (tovector "key" "osName")
                           :datasets [{:data (tovector "val" "osName")
                                       :backgroundColor @colorvector}]}
                    :options {:animation {:duration 0}
                              :legend {:display true :position "bottom" :align "start" :labels {:fontSize 20 :fontColor (if (not (:darkmode @state)) "#738598" "white")}}}}]

      (js/Chart. context (clj->js chart-data))))

(defn rev-chartjs-component-os
  []
  (reagent/create-class
    {:component-did-mount #(show-revenue-chart-os)
     :display-name        "chartjs-component-os"
     :reagent-render      (fn []
                            [:canvas {:id "rev-chartjs-os" :width "100%" :height "100%"}])}))

;ALL CHART -----------------------------------------------------------------
(defn getDay [day]
    (nth '("Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat")
      (if (> 0 (- (.getDay (js/Date.)) day)) (+ 7 (- (.getDay (js/Date.)) day)) (- (.getDay (js/Date.)) day))))

(defn show-revenue-chart-line
  []
  (let [context (.getContext (.getElementById js/document "rev-chartjs-line") "2d")
        chart-data {:type "line"
                    :data {

                           :labels [(getDay 4)
                                    (getDay 3)
                                    (getDay 2)
                                    (getDay 1)
                                    (getDay 0)]

                           :datasets [{
                                       :backgroundColor "#00ADB5"
                                       :minBarlength 0
                                       :data (time-to)}]}
                    :options {:animation {:duration 0}
                              :legend {:display false}
                              :scales {
                                       :yAxes [{
                                                :ticks {
                                                        :beginAtZero true}}]}}}]
      (js/Chart. context (clj->js chart-data))))

(defn rev-chartjs-component-line
  []
  (reagent/create-class
    {:component-did-mount #(show-revenue-chart-line)
     :display-name        "chartjs-component-line"
     :reagent-render      (fn []
                            [:canvas {:id "rev-chartjs-line" :width "auto" :height "90%"}])}))

;COOKIE CHART-----------------------------------------------------------------------------------------

(defn show-revenue-chart-cookie
  []
  (let [context (.getContext (.getElementById js/document "rev-chartjs-cookie") "2d")
        chart-data {:type "bar"
                    :data {
                           :labels ["True" "False"]
                           :datasets [{
                                       :data (truecounter "cookies?")
                                       :backgroundColor @colorvector}]}

                    :options {:animation {:duration 0}
                              :legend {:display false}
                              :scales {
                                       :yAxes [{
                                                :ticks {
                                                        :beginAtZero true}}]}}}]
      (js/Chart. context (clj->js chart-data))))


(defn rev-chartjs-component-cookie
  []
  (reagent/create-class
    {:component-did-mount #(show-revenue-chart-cookie)
     :display-name        "chartjs-component-cookie"
     :reagent-render      (fn []
                            [:canvas {:id "rev-chartjs-cookie" :width "100%" :height "100%"}])}))

;Language CHART-----------------------------------------------------------------------------------------

(defn show-revenue-chart-lang
  []
  (let [context (.getContext (.getElementById js/document "rev-chartjs-lang") "2d")
        chart-data {:type "pie"
                    :data {
                           :labels (tovector "key" "osName")
                           :datasets [{:data (tovector "val" "osName")
                                       :backgroundColor @colorvector}]}
                    :options {:animation {:duration 0}
                              :legend {:display true :position "bottom" :align "start" :labels {:fontSize 20 :fontColor (if (not (:darkmode @state)) "#738598" "white")}}}}]

      (js/Chart. context (clj->js chart-data))))

(defn rev-chartjs-component-lang
  []
  (reagent/create-class
    {:component-did-mount #(show-revenue-chart-lang)
     :display-name        "chartjs-component-lang"
     :reagent-render      (fn []
                            [:canvas {:id "rev-chartjs-lang" :width "100%" :height "100%"}])}))

;------------------------------------------------------------------------------------------------------
;---------------------------------------------CARDS----------------------------------------------------
;------------------------------------------------------------------------------------------------------

;Page card
(defn page-card [page-name chart]
  ^{:key (rand-int 1000)}
  [:div.card.row {:data-grid {:x 3 :y 0 :w 3 :h 2}}
   [:div.total
    [:h1.cardTitle page-name]
    [:h1.cardNumber {:style {:font-size (dynamicText 250 (count @data))}} (count @data)]
    [:p.cardText "Active"]
    (let [active (atom (str (userselector 0 "active")))]
      (js/setInterval #(reset! active (str (userselector 0 "active"))) 5000)
      [:p.active (str @active)])]
   [:div.totalDetails
    [:div.details
     [:div.daily
      [:h1.cardNumber {:style {:font-size (dynamicText 180 (userselector 0 "day"))}} (userselector 0 "day")]
      [:p.cardText "Daily"]]
     [:div.weekly
      [:h1.cardNumber {:style {:font-size (dynamicText 180 (userselector 0 "week"))}} (userselector 0 "week")]
      [:p.cardText "Weekly"]]
     [:div.monthly
      [:h1.cardNumber {:style {:font-size (dynamicText 180 (userselector 0 "month"))}} (userselector 0 "month")]
      [:p.cardText "Monthly"]]]
    [:div.cardGraph chart]]])

;Browser, Cookie, OS, Devices, Language CARD
(defn small-card [card-title chart]
  ^{:key (rand-int 1000)}
  [:div.card.column {:data-grid {:x 1 :y 0 :w 1 :h 2}}
   [:div.browser
    [:h1.cardTitle card-title]
    [:div.cardGraph chart]]])

;New/Old Users CARD
(defn users-card [card-name]
  ^{:key (rand-int 1000)}
  [:div.card.row {:data-grid {:x 3 :y 0 :w 1 :h 1}}
   [:div.usercard
    [:h1 card-name]
    [:div.usersdetails.row
     [:div.users.column
      [:h1.cardNumber {:style {:font-size (dynamicText 180 (userselector 1 "day"))}} (userselector 1 "day")]
      [:h1.cardTitle "New User"]]
     [:div.users.column
      [:h1.cardNumber {:style {:font-size (dynamicText 180 (userselector 2 "day"))}} (userselector 2 "day")]
      [:h1.cardTitle "Old User"]]]]])

;Time CARD
(defn timer-card [title-cyan title clock]
  ^{:key (rand-int 1000)}
  [:div.card.column {:data-grid {:x 0 :y 0 :w 2 :h 1}}
   [:div.timer
    [:div.timerHeader
     [:div.timerTitle
      [:h1.titleZgen title-cyan]
      [:h3.titleAnalytics title]]
     [:div.togglebtn
      [:label.switch
       [:input {:type "checkbox" :on-click #(swap! state assoc :darkmode (not (:darkmode @state)))}]
       [:span.slider.round]]]]
    [:div.time clock]]])

;Crypro CARD
(defn crypto-card [crypto1 crypto2 crypto3]
  ^{:key (rand-int 1000)}
  [:div.card.column {:data-grid {:x 0 :y 4 :w 3 :h 2}}
   [:div.crypto.column
    [:div.vaults
     [:h1 crypto1]
     [:h2 "9000"]
     [:h3.center-all "USD"]
     [:h4.center-all "340"]]
    [:div.vaults
     [:h1 crypto2]
     [:h2 "345"]
     [:h3.center-all "USD"]
     [:h4.center-all "340"]]
    [:div.vaults
     [:h1 crypto3]
     [:h2 "1456111"]
     [:h3.center-all "USD"]
     [:h4.center-all "340"]]]])

;------------------------------------------------------------------------------------------------------
;---------------------------------------------APP------------------------------------------------------
;------------------------------------------------------------------------------------------------------

(defn app []
   [:div {:class [(if (:darkmode @state) "dark" "light")]}
    [:button {:on-click #(adatbazisdel) } "Database del"]
    [:button {:on-click #(gift (.getTime (js/Date.)))} "Local-Storage add"]
    [:button {:on-click #(remove-item!)} "Local-Storage del"]
    [:button {:on-click #(extraadd)} "Extra data"]
    [:button {:on-click #(doseq [i (range 10)] (extraadd))} "10x extra data"]
    [:button {:on-click #(doseq [i (range 100)] (extraadd))} "100x extra data"]
    [:> GridLayout {:cols (if (>= (-> js/screen .-availWidth) 3840) 10 5) :className "grid" :rowHeight 210 :width (if (= 0 (+ (-> js/window .-screenY) (-> js/window .-screenTop))) (-> js/screen .-width) (-> js/screen .-availWidth))}

      (page-card "Page Name" [#(rev-chartjs-component-line)])

      (small-card "Browsers" [(rev-chartjs-component-browser)])

      (small-card "Devices" [#(rev-chartjs-component-devices)])

      (small-card "Languages" "IDE KELL EGY CHART A 3 LEGGYAKORIBBROL (MAR ADDOOLTAM CSAK ATKELL IRNI)")

      (small-card "Cookie" [#(rev-chartjs-component-cookie)])

      (small-card "OS" [#(rev-chartjs-component-os)])

      (users-card "Last Week")

      (timer-card "ZGEN" "analytics" [#(clock)])

      (crypto-card "BTC" "ONE" "PRV")]])
