(ns ita2lis-01.gui-lis4all-demo)
(use 'seesaw.core)

;;general definition
(native!)

;;aux
(def array-a1 (clojure.string/split-lines (slurp (clojure.java.io/resource "train-messages-corpora/a1.txt"))))
(defn rand-a1 []  (rand-nth array-a1))
(def array-a2 (clojure.string/split-lines (slurp (clojure.java.io/resource "train-messages-corpora/a3.txt"))))
(defn rand-a2 []  (rand-nth array-a2))
(def array-a3 (clojure.string/split-lines (slurp (clojure.java.io/resource "train-messages-corpora/a2.txt"))))
(defn rand-a3 []  (rand-nth array-a3))
(def array-p1 (clojure.string/split-lines (slurp (clojure.java.io/resource "train-messages-corpora/p1.txt"))))
(defn rand-p1 []  (rand-nth array-p1))

(defn setText [textName testo]  (text! textName testo))



;;defining frames,
(def f1 (frame :title "Frame Input"  :minimum-size [400 :by 800]))
(def f2 (frame :title "Frame output" :minimum-size [400 :by 800]))

;; defining panels F1
(def areaTop
  (horizontal-panel
   :items [
           ;; (text :multi-line? true :editable? false :font "HELVETICA-NEUE-30"
;;                  :foreground :blue;; "#D61201"
;;                  :text "LIS4ALL - Demo
           ;; Notte dei Ricercatori 2014")
           (label :icon (seesaw.icon/icon "icons/top-label-01.png") )
           ]
   :size  [400 :by 100]
   :background "#FFFFFF"
   :border (seesaw.border/line-border :color :blue :bottom 10)
   ;;:border (seesaw.border/empty-border :left 5 :right 5 :top 5 :down 5)
   ))

(def areaMsg
  (text :multi-line? true :font "HELVETICA-NUE-25" :text "LIS4ALL Demo NdR" :background "#FFFFFF" :wrap-lines? true :foreground :blue))
(def areaMsgPanel
  (horizontal-panel
   :items [(scrollable areaMsg)] :size  [400 :by 250] :border (seesaw.border/line-border :color :blue :top 10 :bottom 10 :left 5 :right 5) :background "#FFFFFF"))

(def b1 (button :background "#FFFFFF" :icon  (seesaw.icon/icon "icons/a1-03.png") :size  [30 :by 30]))
(listen b1 :action (fn [e] (setText areaMsg (rand-a1))))
(def b2 (button :background "#FFFFFF" :icon  (seesaw.icon/icon "icons/a2-03.png") :size  [30 :by 30] ))
(listen b2 :action (fn [e] (setText areaMsg (rand-a2))))
(def b3 (button :background "#FFFFFF" :icon  (seesaw.icon/icon "icons/a3-02.png") :size  [30 :by 30] ))
(listen b3 :action (fn [e] (setText areaMsg (rand-a3))))
(def b4 (button :background "#FFFFFF" :icon  (seesaw.icon/icon "icons/p1-02.png") :size  [30 :by 30] ))
(listen b4 :action (fn [e] (setText areaMsg (rand-p1))))
;;(def b5 (button :text "Translate!" :size [380 :by 100] :halign  :center :background "#FFFFFF"))
(def b5 (button :icon  (seesaw.icon/icon "icons/tra-04.png") :size [380 :by 100] :halign  :center :background "#FFFFFF"))
(listen b5 :action (fn [e] (alert e (text areaMsg))))

(def pannello-pulsanti-top
  (grid-panel
   :vgap 10
   :hgap 10
   ;;:border 10
   :border (seesaw.border/line-border :color :blue :top 0 :bottom 0 :left 5 :right 5)
   :size  [400 :by 300]
   :columns 2
   :items [b1 b2
           b3 b4]
   :background "#FFFFFF"
   ))

(def pannello-pulsante-bottom
  (horizontal-panel
   :border 10
   :items [b5]
   :size  [400 :by 150]
   :background "#FFFFFF"))

;; (def panel-b12 (border-panel :west prova :east b2 :vgap 1 :hgap 1 :border 1))
;; (def panel-b34 (border-panel :west b3 :east b4 :vgap 1 :hgap 1 :border 1))
;; (def panel-b1234 (border-panel :north panel-b12 :south panel-b34))
;; (def split-b12 (left-right-split b1 b2 :divider-location 1/2))
;; (def split-b34 (left-right-split b3 b4 :divider-location 1/2))
;; (def split-b1234 (top-bottom-split split-b12 split-b34 :divider-location 1/2 ))
;; (def buttons-panel (border-panel :north panello-pulsanti-top  :center areaMsg :south b5 :vgap 1 :hgap 1 :border 1))
;; (def area-buttons-panel (border-panel :north areaTop :south buttons-panel :vgap 1 :hgap 1 :border 1))
;;(def split-areaTop-buttonsPanel (top-bottom-split areaTop buttons-panel :divider-location 1/6))

(def all-panels
  (vertical-panel
   :items [areaTop
           pannello-pulsanti-top
           areaMsgPanel
           pannello-pulsante-bottom
           ]  ))

;;--------------------------F2
(def areaTextSemantics
  (text :multi-line? true :font "HELVETICA-NUE-18" :text "Semantics" :foreground :blue))
(def areaTextSemanticsPanel
  (horizontal-panel
   :items [areaTextSemantics]
   :border (seesaw.border/line-border :color :blue :top 10 :bottom 5 :left 5 :right 5)
   ))

(def areaTextOutGen
  (text :multi-line? true :font "HELVETICA-NUE-18" :text "Output Generator" :foreground :blue))
(def areaTextOutGenPanel
  (horizontal-panel
   :items [areaTextOutGen]
   :border (seesaw.border/line-border :color :blue :top 5 :bottom 5 :left 5 :right 5)
   ))

(def areaTextAEWLIS
  (text :multi-line? true :font "HELVETICA-NUE-18" :text "AEWLIS" :foreground :blue))
(def areaTextAEWLISPanel
  (horizontal-panel
   :items [areaTextAEWLIS]
   :border (seesaw.border/line-border :color :blue :top 5 :bottom 10 :left 5 :right 5)))

(def b6 (button :icon  (seesaw.icon/icon "icons/again-01.png") :size [350 :by 100] :halign  :center :background "#FFFFFF"))
(listen b6 :action (fn [e] (alert e (text areaTextAEWLIS))))
(def pannello-pulsante-bottom-2
  (horizontal-panel
   :border 10
   :items [b6]
   :size  [400 :by 150]
   :background "#FFFFFF"))

(def all-panels-2
  (vertical-panel
   :items [areaTextSemanticsPanel
           areaTextOutGenPanel
           areaTextAEWLISPanel
           pannello-pulsante-bottom-2]))


;;Main visualization
(defn display [frame content]
  (config! frame :content content)
  content)

(defn visualize
  "Defining the windows"
  []
  (-> f1 pack! show!)
  (-> f2 pack! show!)
  (display f1 all-panels)
  (display f2 all-panels-2)
  )
