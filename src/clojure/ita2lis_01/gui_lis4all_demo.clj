(ns ita2lis-01.gui-lis4all-demo)
(use 'seesaw.core)

;(require '[ita2lis-01.core :as core] )

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


;; (defn pp-hash
;;   ""
;;   [hash]
;;   (str  (map #(str  (nth % 0)  (nth % 1) "\n") (seq hash))))
  ;;(str (doseq [[k v] (map vector (keys hash) (vals hash))] (str k ":  " v "\n")))


;;defining frames,
(def f1 (frame :title "Frame Input"  :minimum-size [400 :by 700]))
(def f2 (frame :title "Frame output" :minimum-size [400 :by 700]))

;; defining panels F1
(def areaTop
  (horizontal-panel
   :items [
           ;; (text :multi-line? true :editable? false :font "HELVETICA-NEUE-30"
;;                  :foreground :blue;; "#D61201"
;;                  :text "LIS4ALL - Demo
           ;; Notte dei Ricercatori 2014")
           (label :icon (seesaw.icon/icon "icons/top-label-04.png") )
           ]
   :size  [400 :by 100]
   :background "#FFFFFF"
   :border (seesaw.border/line-border :color :blue :bottom 0)
   ;;:border (seesaw.border/empty-border :left 5 :right 5 :top 5 :down 5)
   ))

(def areaMsg
  (text :multi-line? true :font "HELVETICA-NUE-25" :text "LIS4ALL Demo NdR" :background "#FFFFFF" :wrap-lines? true :foreground "#941100"));:blue
(def areaMsgPanel
  (horizontal-panel
   :items [(scrollable areaMsg)] :size  [400 :by 250] :border (seesaw.border/line-border :color :blue :top 0 :bottom 0 :left 0 :right 0) :background "#FFFFFF"))

(def b1 (button :background "#FFFFFF" :icon  (seesaw.icon/icon "icons/a1-03.png")))
(listen b1 :action (fn [e] (setText areaMsg (rand-a1))))
(def b2 (button :background "#FFFFFF" :icon  (seesaw.icon/icon "icons/a2-04.png")))
(listen b2 :action (fn [e] (setText areaMsg (rand-a3))))
(def b3 (button :background "#FFFFFF" :icon  (seesaw.icon/icon "icons/a3-02.png")))
(listen b3 :action (fn [e] (setText areaMsg (rand-a2))))
(def b4 (button :background "#FFFFFF" :icon  (seesaw.icon/icon "icons/p1-02.png")))
(listen b4 :action (fn [e] (setText areaMsg (rand-p1))))
;;(def b5 (button :text "Translate!" :size [380 :by 100] :halign  :center :background "#FFFFFF"))
(def b5 (button :icon  (seesaw.icon/icon "icons/tra-05.png") :size [380 :by 100] :halign  :center :background "#FFFFFF"))
;;(listen b5 :action (fn [e] (translate! (text areaMsg)))); in core

(def pannello-pulsanti-top
  (grid-panel
   :vgap 10
   :hgap 10
   ;;:border 10
   :border (seesaw.border/line-border :color :blue :top 0 :bottom 0 :left 0 :right 0)
   :size  [400 :by 250]
   :columns 2
   :items [b1 b2
           b3 b4]
   :background "#FFFFFF"
   ))

(def pannello-pulsante-bottom
  (horizontal-panel
   :border 10
   :items [b5]
   :size  [400 :by 80]
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
  (text :multi-line? true :font "HELVETICA-NUE-20" :text "Semantics" :foreground "#FF3334"))
(def areaTextSemanticsPanel
  (horizontal-panel
   :items [(scrollable areaTextSemantics)]
   :border (seesaw.border/line-border :color "#011992" :top 0 :bottom 0 :left 0 :right 0)
   ))

(def areaTextOutGen
  (text :multi-line? true :font "HELVETICA-NUE-10" :text "Output Generator" :foreground "#0302FB"))
(def areaTextOutGenPanel
  (horizontal-panel
   :items [(scrollable areaTextOutGen)]
   :border (seesaw.border/line-border :color "#011992" :top 0 :bottom 0 :left 0 :right 0)
   ))

(def areaTextAEWLIS
  (text :multi-line? true :font "HELVETICA-NUE-10" :text "AEWLIS" :foreground "#0302FB"))
(def areaTextAEWLISPanel
  (horizontal-panel
   :items [(scrollable areaTextAEWLIS)]
   :border (seesaw.border/line-border :color "#011992" :top 0 :bottom 0 :left 0 :right 0)))

(def areaTextPlain
  (text :multi-line? true :font "HELVETICA-NUE-20" :text "PLAIN AEWLIS" :foreground "#011992"))
(def areaTextPlainPanel
  (horizontal-panel
   :items [(scrollable areaTextPlain)]
   :border (seesaw.border/line-border :color "#011992" :top 0 :bottom 0 :left 0 :right 0)))


(def b6 (button :icon  (seesaw.icon/icon "icons/again-03.png") :size [350 :by 100] :halign  :center :background "#FFFFFF"))
;;(listen b6 :action (fn [e] (alert e (text areaTextAEWLIS)))) ;;in core
(def pannello-pulsante-bottom-2
  (horizontal-panel
   :border 10
   :items [b6]
   :size  [400 :by 80]
   :background "#FFFFFF"))

(def all-panels-2
  (vertical-panel
   :items [areaTextSemanticsPanel
           areaTextOutGenPanel
           areaTextAEWLISPanel
           areaTextPlainPanel
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

;; (defn translate!
;;   "Function called when the button translate! is pressed"
;;   [sentence]
;;   (let
;;       [
;;        emerged-semantics (core/emerge-semantic-values (core/ita2sem (first (core/split-sentences sentence))))
;;        modified-emerged-semantics (core/remove-cartelli emerged-semantics)
;;        ;;hash-cartelli (core/extract-cartelli emerged-semantics)
;;        out-templating (core/call-enlive-template modified-emerged-semantics)
;;        aewlis (core/analyze-and-generate sentence)
;;        ]
;;     (do
;;       (setText areaTextSemantics (str  emerged-semantics))
;;       (setText areaTextOutGen out-templating)
;;       (setText areaTextAEWLIS aewlis))
;;     ))
