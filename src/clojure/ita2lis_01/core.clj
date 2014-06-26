(ns ita2lis-01.core
   (:gen-class))

(require '[net.cgrand.enlive-html :as html]
         '[clojure.data.csv :as csv])
;;(net.cgrand.reload/auto-reload *ns*)

;;; Algorithm
;;; 1. Preformat the whole sentence (e.g. time format)
;;; 2. Split the whole sentence into a number of semantic notable subsentences
;;; 3. Discover the sentence containing the train message
;;; 4. Extract (and post-format, e.g. train number) the semantic values in the train message
;;; 5. Return the hash-map containingt the semantic values (emerge it)
;;; 6. Use the semantic values to fill the template corresponding to the specific message class

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

;;; Constants

(def lexicon-ita2sem (read-lexicon "./resources/ita2sem2lis-lis4all-01.csv"))


(def file-stazioni "./resources/elenco-stazioni-01.txt")
(defn read-stazioni
  ""
  [file-lexicon]
  (with-open [in-file (clojure.java.io/reader file-lexicon)]
    (let [ar (doall (csv/read-csv in-file))]
      (map #(if (= (nth % 2) "rail_station") (nth % 0)) ar))))

(def stazioni-dictionary
  ;;"(CHIVASSO|MILANO CENTRALE|FIRENZE|BARDONECCHIA|FOSSANO|IVREA|SUSA|IMPERIA ONEGLIA)"
  ;;(str "(" (clojure.string/replace (clojure.string/replace (slurp file-stazioni) #"\n$" "") #"\s*\n\s*" "|" ) ")" )
  (str "(" (clojure.string/join "|" (remove #(nil? %)  (read-stazioni "./resources/ita2sem2lis-lis4all-01.csv"))) ")"))

(def stazioni-dictionary-re (re-pattern stazioni-dictionary))
(def categorie-dictionary-re #"(REGIONALE|REGIONALE VELOCE|FRECCIAROSSA|SERVIZIO FERROVIARIO METROPOLITANO - LINEA 3|SERVIZIO FERROVIARIO METROPOLITANO - LINEA 7|ESPRESSO|EUROCITY|EURONIGHT|TGV|FRECCIARGENTO|FRECCIABIANCA|ITALO|INTERCITY|INTERCITY NOTTE|SUBURBANO|REGIOEXPRESS|MALPENSA EXPRESS|TRENO METROPOLITANO|ACCELERATO|DIRETTO|DIRETTISSIMO)")
(def imprese_ferroviarie-dictionary-re #"(TRENITALIA|NSV|GTT|TRENORD|SAD|ENTE AURONOMO VOLTURNO)")
(def fermate-dictionary-re (re-pattern (str "(FERMA IN TUTTE LE STAZIONI|FERMA A " stazioni-dictionary "+|IN TUTTE LE STAZIONI ECCETTO A " stazioni-dictionary "+|NON SONO PREVISTE FERMATE INTERMEDIE|OGGI FERMA ANCHE A " stazioni-dictionary  "+)")))
(def oggi-fermate-dictionary-re (re-pattern (str "(OGGI IL TRENO ARRIVA FINO A " stazioni-dictionary "|OGGI IL TRENO ARRIVA A " stazioni-dictionary "INVECE CHE A" stazioni-dictionary ")")))


(defn read-lexicon
  ""
  [file-lexicon]
  (with-open [in-file (clojure.java.io/reader file-lexicon)]
    (let [ ar (doall (csv/read-csv in-file))]
      (zipmap (map #(nth % 0) ar) (map #(nth % 3) ar)))))
(def lexicon-ita2sem (read-lexicon "./resources/ita2sem2lis-lis4all-01.csv"))

;;;String functions to pre-process the input
(def sentece-general-delimeters #"\. ")

(defn capitalize [s]  (.toUpperCase s))
(defn rm-useless-chars [s]  (clojure.string/replace s #"[,.]" " "))
(defn reduce-whites [s]  (clojure.string/replace s #" +" " "))
(defn time-format-normalize
  "Uniform all the time format to HH:MM"
  [sentence]
  (clojure.string/replace sentence #"(\d\d)[\.,](\d\d)" "$1:$2"))
(defn train-number-format-normalize
  "Uniform all the train number to "
  [sentence]
  (clojure.string/replace sentence #" +" ""))

(defn split-sentences
  "Split one single sentences into an array of sentences"
  [one-sentence]
  (map
   (comp reduce-whites rm-useless-chars capitalize clojure.string/trim time-format-normalize)
   (clojure.string/split one-sentence sentece-general-delimeters)))

(defn lc-values
  "lowercase the values in a hash"
  [hhh]
  (into {} (for [[k v] hhh] [k (if (string? v) (.toLowerCase v) v) ])))

;;; Auxiliary functions
(defn map-vals
  "apply a function to the vals of a hash"
  [hhh fff]
  (zipmap (keys hhh) (map fff (vals hhh))))

(defn ita-lex-sem
  "Returns the semantics of a specific italian word"
  [sem-hash lexicon]
  (map-vals sem-hash #(if (get lexicon %) (get lexicon %) %)))

(defn hm2ampm_hh_mm
  "The input is a time string hh:mm, the ouput is {:ampm morning/afternoon :hh hh :mm mm}. I use zero instead of 0 because openccg does not accept 0 as semantic value"
  [hhmm]
  (let [[hs ms] (clojure.string/split hhmm #":")
        h (Integer/parseInt hs)
        ampm (if (<=  h 12) "morning" "afternoon")
        hhh (if (<= h 12) h (- h 12))
        hh (if (== 0 hhh) "zero" (str hhh))
        m (Integer/parseInt ms)
        mm (if (== 0 m) "zero" (str m))
        ]
    {:ampm ampm :hh hh :mm mm}
    ;(hash-map :ampm ampm :hh hh :mm mm)
    ))

(defn unescape-chars
  "Remove escape html AND 0 with ZERO!!!!!!!!!"
  [tag-string]
  (.. #^String tag-string
    (replace  "&amp;" "&")
    (replace "&lt;" "<")
    (replace "&gt;" ">")
    (replace "&quot;" "\"")
    (replace  "name=\"0\"" "name=\"zero\"")
    ))

;;;Functions to extract the semantics from italian sentence
(defn extract-content-words-2
  ""
  []
  ;;(clojure.string/join "\n" (rest (sort
  (set (map #(let [mat-1 (re-matcher (re-pattern (str "proveniente da ([^,]+),")) %)
                    mat-2 (re-matcher (re-pattern (str "direto a ([^,]+),")) %)]
                (cond (re-find mat-1) ((re-groups mat-1) 1)
                      (re-find mat-2) ((re-groups mat-1) 1)))
             (clojure.string/split (slurp "./resources/Annunci-CSV.csv") #"\n"))))

(defn extract-content-words
  "extract the content words from PN messages"
  []
  (with-open [in-file (clojure.java.io/reader "./resources/Annunci-CSV-50.csv")]
    (let [result #{}]
      (doseq [line (line-seq in-file)]
          (let [
                mat-1 (re-matcher (re-pattern (str "proveniente da ([^,]+),")) line)
                mat-2 (re-matcher (re-pattern (str "direto a ([^,]+),")) line)]
            (cond (re-find mat-1) (def result (conj result ((re-groups mat-1) 1)))
                  (re-find mat-2) (def result (conj result ((re-groups mat-2) 1))))))
        (println result))))


(defn emerge-semantic-values
  "express expicitely the hidden semantic values in the hash. (nomore necessary lower-case)"
  [semantic-hash]

  (let [semantic-hash-temp (ita-lex-sem semantic-hash lexicon-ita2sem)]
    (if (:ora_arrivo semantic-hash)
               (merge semantic-hash-temp (hm2ampm_hh_mm (:ora_arrivo semantic-hash)))
               (merge semantic-hash-temp (hm2ampm_hh_mm (:ora_partenza semantic-hash))))))

(defn infer-message-type
  "Given a train message returns a hash-map containing the the MAS type (e.g. A1, A2,...,P1, P2, ...) and all the semantic values"
  [train-station-message semantic-slot-types]
  (let [pattern-a1 (re-pattern (str "IL TRENO\\s?"
                                    (:straordinario semantic-slot-types)
                                    "?\\s?"
                                    (:categoria semantic-slot-types)
                                    "\\s?"
                                    (:numero semantic-slot-types)
                                    "\\s?(DI)?\\s?"
                                    (:impresa_ferroviaria semantic-slot-types)                                                          "\\s?DELLE ORE\\s?"
                                    (:ora_arrivo semantic-slot-types)
                                    "\\s?(PROVENIENTE DA)?\\s?"
                                    (:località_di_provenienza semantic-slot-types)
                                    "?(E DIRETTO A)?\\s?"
                                    (:località_di_arrivo semantic-slot-types)
                                    "?\\s?È IN ARRIVO AL BINARIO\\s?"
                                    (:numero_del_binario semantic-slot-types)
                                    "$"))
        mat-a1 (re-matcher pattern-a1 train-station-message)
        pattern-a2 (re-pattern (str "IL TRENO\\s?"
                                    (:straordinario semantic-slot-types)
                                    "?\\s?"
                                    (:categoria semantic-slot-types)
                                    "\\s?"
                                    (:numero semantic-slot-types)
                                    "\\s?(DI)?\\s?"
                                    (:impresa_ferroviaria semantic-slot-types)                                                          "\\s?DELLE ORE\\s?"
                                    (:ora_arrivo semantic-slot-types)
                                    "\\s?(PROVENIENTE DA)?\\s?"
                                    (:località_di_provenienza semantic-slot-types)
                                    "?(E DIRETTO A)?\\s?"
                                    (:località_di_arrivo semantic-slot-types)
                                    "?\\s?È IN ARRIVO AL BINARIO\\s?"
                                    (:numero_del_binario semantic-slot-types)
                                    "\\s?INVECE CHE AL BINARIO\\s?"
                                    (:numero_del_binario semantic-slot-types)
                                    ))
        mat-a2 (re-matcher pattern-a2 train-station-message)
        pattern-a3 (re-pattern (str "ANNUNCIO RITARDO!"
                                    "\\s?IL TRENO\\s?"
                                    (:straordinario semantic-slot-types)
                                    "?\\s?"
                                    (:categoria semantic-slot-types)
                                    "\\s?"
                                    (:numero semantic-slot-types)
                                    "\\s?(DI)?\\s?"
                                    (:impresa_ferroviaria semantic-slot-types)                                                          "\\s?DELLE ORE\\s?"
                                    (:ora_arrivo semantic-slot-types)
                                    "\\s?(PROVENIENTE DA)?\\s?"
                                    (:località_di_provenienza semantic-slot-types)
                                    "?(E DIRETTO A)?\\s?"
                                    (:località_di_arrivo semantic-slot-types)
                                    "?\\s?ARRIVERÀ CON UN RITARDO PREVISTO DI\\s?"
                                    (:tempo_ritardo semantic-slot-types)
                                    "\\s?MINUTI\\s?"
                                    (:diversamente  semantic-slot-types)
                                    "?\\s?"
                                     "(PER)?"
                                    (:motivo_di_ritardo semantic-slot-types)
                                    "?\\s?"
                                    (:scuse_disagio semantic-slot-types)
                                    "?"
                                    ))
        mat-a3 (re-matcher pattern-a3 train-station-message)
        pattern-a5 (re-pattern (str "ANNUNCIO CANCELLAZIONE TRENO!\\s?IL ?TRENO\\s?"
                                    (:categoria semantic-slot-types)
                                    "\\s?"
                                    (:numero semantic-slot-types)
                                    "\\s?(DI)?\\s?"
                                    (:impresa_ferroviaria semantic-slot-types)                                                       "\\s?PREVISTO IN ARRIVO DA\\s?"
                                    (:località_di_provenienza semantic-slot-types)
                                     "?\\s?ALLE ORE\\s?"
                                    (:ora_arrivo semantic-slot-types)
                                    "\\s?(E DIRETTO A)?\\s?"
                                    (:località_di_arrivo semantic-slot-types)
                                    "?\\s?OGGI NON È STATO EFFETTUATO\\s?"
                                    "(PER)?"
                                    (:motivo_di_ritardo semantic-slot-types)
                                    "?"
                                    ;; "(I VIAGGIATORI PROVENIENTI DA)?"
                                    ;; (:località_di_partenza semantic-slot-types)
                                    ;; "ARRIVERANNO CON IL TRENO)?"
                                    ;; (:straordinario semantic-slot-types)
                                    ;; "?\\s?"
                                    ;; (:categoria semantic-slot-types)
                                    ;; "\\s?"
                                    ;; (:numero semantic-slot-types)
                                    ;; "\\s?(DI)?\\s?"
                                    ;; (:impresa_ferroviaria semantic-slot-types)
                                    ;;"\\s?(DELLE ORE)?\\s?"
                                    ;; (:ora_partenza semantic-slot-types)
                                    ;; "\\s?(PROVENIENTE DA)?\\s?"
                                    ;;(:località_di_provenienza semantic-slot-types)
                                    ;; "(E DIRETTO A)?\\s?"
                                    ;; (:località_di_arrivo semantic-slot-types)
                                    ;; "?\\s?(IN ARRIVO AL BINARIO)\\s?"
                                    ;; (:numero_del_binario semantic-slot-types)
                                    ;; "(I VIAGGIATORI PROVENIENTI DA)?\\s?"
                                    ;; (:località_di_provenienza semantic-slot-types)
                                    ;; "?\\s?(ARRIVERANNO CON UN AUTOBUS SOSTITUTIVO)?\\S?"
                                    ;; "?\\s?"
                                    ;; "\\s?(ALLE ORE)?\\s?"
                                    ;; (:ora_arrivo semantic-slot-types)
                                    (:scuse_disagio semantic-slot-types)
                                    "?"
                                    ))
        mat-a5 (re-matcher pattern-a5 train-station-message)
        pattern-p1 (re-pattern (str "^IL TRENO\\s?"
                                    (:straordinario semantic-slot-types)
                                    "?\\s?"
                                    (:categoria semantic-slot-types)
                                    "\\s?"
                                    (:numero semantic-slot-types)
                                    "\\s?(DI)?\\s?"
                                    (:impresa_ferroviaria semantic-slot-types)                                                       "\\s?DELLE ORE\\s?"
                                    (:ora_partenza semantic-slot-types)
                                    "\\s?PER\\s?"
                                    (:località_di_arrivo semantic-slot-types)
                                    "?(VIA)?\\s?"
                                    (:relazioni_di_percorrenza semantic-slot-types)
                                    "?\\s?È IN PARTENZA\\s?"
                                    (:in_ritardo semantic-slot-types)
                                    "?\\s?DAL BINARIO\\s?"
                                    (:numero_del_binario semantic-slot-types)
                                    "\\s?"
                                    (:fermate semantic-slot-types)
                                    "?" ))
        mat-p1 (re-matcher pattern-p1 train-station-message)
        pattern-p5 (re-pattern (str "ANNUNCIO RITARDO!"
                                    "\\s?IL TRENO\\s?"
                                    (:straordinario semantic-slot-types)
                                    "?\\s?"
                                    (:categoria semantic-slot-types)
                                    "\\s?"
                                    (:numero semantic-slot-types)
                                    "\\s?(DI)?\\s?"
                                    (:impresa_ferroviaria semantic-slot-types)                                                          "\\s?DELLE ORE\\s?"
                                    (:ora_partenza semantic-slot-types)
                                    "\\s?PER\\s?"
                                    (:località_di_arrivo semantic-slot-types)
                                    "?(VIA)?\\s?"
                                    (:relazioni_di_percorrenza semantic-slot-types)
                                    "?\\sPARTIRÀ"
                                    "?\\s?(DAL BINARIO)?\\s?"
                                    (:numero_del_binario semantic-slot-types)
                                    "?\\s?CON UN RITARDO PREVISTO DI\\s?"
                                    (:tempo_ritardo semantic-slot-types)
                                    "\\s?MINUTI\\s?"
                                    (:diversamente  semantic-slot-types)
                                    "?\\s?"
                                     "(PER)?"
                                    (:motivo_di_ritardo semantic-slot-types)
                                    "?\\s?"
                                    (:prestare_attenzione semantic-slot-types)
                                    "?\\s?"
                                    ;; "(I VIAGGIATORI DIRETTI A)?"
                                    ;; (:località_di_arrivo semantic-slot-types)
                                    ;; "(POSSONO UTILIZZARE IL TRENO)?"
                                    ;; (:straordinario semantic-slot-types)
                                    ;; "?\\s?"
                                    ;; (:categoria semantic-slot-types)
                                    ;; "\\s?"
                                    ;; (:numero semantic-slot-types)
                                    ;; "\\s?(DI)?\\s?"
                                    ;; (:impresa_ferroviaria semantic-slot-types)                                                          "\\s?(DELLE ORE)?\\s?"
                                    ;; (:ora_partenza semantic-slot-types)
                                    ;; "\\s?(PER)?\\s?"
                                    ;; (:località_di_arrivo semantic-slot-types)
                                    ;; "(IN PARTENZA)?\\s?"
                                    ;; (:in_ritardo semantic-slot-types)
                                    ;; "?\\s?(DAL BINARIO)\\s?"
                                    ;; (:numero_del_binario semantic-slot-types)
                                    ;; "?"
                                    "\\s?"
                                    (:scuse_disagio semantic-slot-types)
                                    "?"
                                    ))
        mat-p5 (re-matcher pattern-p5 train-station-message)
        pattern-p9 (re-pattern (str "ANNUNCIO CANCELLAZIONE TRENO!\\s?IL ?TRENO\\s?"
                                    (:categoria semantic-slot-types)
                                    "\\s?"
                                    (:numero semantic-slot-types)
                                    "\\s?(DI)?\\s?"
                                    (:impresa_ferroviaria semantic-slot-types)                                                        "?\\s?PREVISTO IN PARTENZA ALLE ORE\\s?"
                                    (:ora_partenza semantic-slot-types)
                                    "\\s?PER\\s?"
                                    (:località_di_arrivo semantic-slot-types)
                                    "?(VIA)?\\s?"
                                    (:relazioni_di_percorrenza semantic-slot-types)
                                    "?\\s?OGGI NON SARÀ EFFETTUATO\\s?"
                                    "(PER)?"
                                    (:motivo_di_ritardo semantic-slot-types)
                                    "?\\s?"
                                    (:scuse_disagio semantic-slot-types) ))
        mat-p9 (re-matcher pattern-p9 train-station-message)
        pattern-p13 (re-pattern (str "INFORMIAMO I VIAGGIATORI CHE IL TRENO\\s?"
                                    (:categoria semantic-slot-types)
                                    "\\s?"
                                    (:numero semantic-slot-types)
                                    "\\s?(DI)?\\s?"
                                    (:impresa_ferroviaria semantic-slot-types)
                                    "\\s?DELLE ORE\\s?"
                                    (:ora_partenza semantic-slot-types)
                                    "\\s?PER\\s?"
                                    (:località_di_arrivo semantic-slot-types)
                                    "?(VIA)?\\s?"
                                    (:relazioni_di_percorrenza semantic-slot-types)
                                    "?\\s?È IN PARTENZA\\s?"
                                    (:in_ritardo semantic-slot-types)
                                    "?\\s?DAL BINARIO\\s?"
                                    (:numero_del_binario semantic-slot-types)
                                    "\\s?"
                                    (:oggi_fermate semantic-slot-types)
                                    "(PER)?"
                                    (:motivo_di_ritardo semantic-slot-types)
                                    "?"
                                    (:fermate semantic-slot-types)
                                    "?\\s"
                                    (:scuse_disagio semantic-slot-types)
                                    "?"
                                    ))
        mat-p13 (re-matcher pattern-p13 train-station-message)
        ]
    ;;(print "pat-p1=" pattern-p1)
   (cond
    (re-find mat-a1)
    (let  [ar (re-groups mat-a1)] (hash-map :type :A1 :straordinario (ar 1) :categoria (ar 2) :numero (train-number-format-normalize (ar 3)) :impresa_ferroviaria (ar 6) :ora_arrivo (ar 7) :località_di_provenienza (ar 9) :località_di_arrivo (ar 10) :numero_del_binario (ar 12) ) )
    (re-find mat-a2)
    (let  [ar (re-groups mat-a2)] (hash-map :type :A2 :straordinario (ar 1) :categoria (ar 2) :numero (train-number-format-normalize (ar 3)) :impresa_ferroviaria (ar 6) :ora_arrivo (ar 7) :località_di_provenienza (ar 9) :località_di_arrivo (ar 10) :numero_del_binario (ar 12) :numero_del_binario_programmato (ar 13) ) )
    (re-find mat-a3)
    (let  [ar (re-groups mat-a3)] (hash-map :type :A3 :straordinario (ar 1) :categoria (ar 2) :numero (train-number-format-normalize (ar 3)) :impresa_ferroviaria (ar 6) :ora_arrivo (ar 7) :località_di_provenienza (ar 9) :località_di_arrivo (ar 10)  ) )
    (re-find mat-a5)
    (let  [ar (re-groups mat-a5)] (hash-map :type :A5  :categoria (ar 1) :numero (train-number-format-normalize (ar 2)) :impresa_ferroviaria (ar 5) :località_di_provenienza (ar 6) :ora_arrivo (ar 7)  :località_di_arrivo (ar 8)   :motivo_ritardo (ar 10) :scuse_disagio (ar 11) ) )
    (re-find mat-p1)
    (let  [ar (re-groups mat-p1)] (hash-map :type :P1 :straordinario (ar 1) :categoria (ar 2) :numero (train-number-format-normalize (ar 3)) :impresa_ferroviaria (ar 6) :ora_partenza (ar 7) :località_di_arrivo (ar 8)  :numero_del_binario (ar 12) :fermate (ar 13) ) )
    (re-find mat-p5)
    (let  [ar (re-groups mat-p5)] (hash-map :type :P5 :straordinario (ar 1) :categoria (ar 2) :numero (train-number-format-normalize (ar 3)) :impresa_ferroviaria (ar 6) :ora_partenza (ar 7) :località_di_arrivo (ar 8)  :numero_del_binario (ar 12) :tempo_ritardo (ar 13) :diversamente (ar 14) :motivo_ritardo (ar 15) :prestare_attenzione (ar 16) :scuse_disagio (ar 17) ) )
    (re-find mat-p9)
    (let  [ar (re-groups mat-p9)] (hash-map :type :P9  :categoria (ar 1) :numero (train-number-format-normalize (ar 2)) :impresa_ferroviaria (ar 5) :ora_partenza (ar 6) :località_di_arrivo (ar 7) :motivo_ritardo (ar 9)  ) )
    (re-find mat-p13)
    (let  [ar (re-groups mat-p13)] (hash-map :type :P13 :categoria (ar 1) :numero (train-number-format-normalize (ar 2)) :impresa_ferroviaria (ar 5) :ora_partenza (ar 6) :località_di_arrivo (ar 7) :relazioni_di_percorrenza (ar 8) :in_ritardo (ar 9) :numero_del_binario (ar 10) :oggi_fermate (ar 11) :motivo_ritardo (ar 12) :fermate (ar 13) :scuse_disagio (ar 14)) )
    ) ) )


(defn ita2sem
  "Given an Italian sentence returns a hash containing the semantic values. The slot names are derived from the MAS manual"
  [italian-sentence]
  (let [semantic-slot-types {
                             :straordinario #"(STRAORDINARIO)"
                             :categoria categorie-dictionary-re
                             :numero  #"((\d+ ?)+)"
                             :impresa_ferroviaria imprese_ferroviarie-dictionary-re
                             :ora_arrivo  #"(\d\d:\d\d)"
                             :località_di_provenienza stazioni-dictionary-re
                             :località_di_arrivo stazioni-dictionary-re
                             :numero_del_binario #"(\d+)"
                             :ora_partenza #"(\d\d:\d\d)"
                             :relazioni_di_percorrenza  #"(\w+)"
                             :in_ritardo #"(IN RITARDO)"
                             :fermate fermate-dictionary-re
                             :oggi_fermate oggi-fermate-dictionary-re
                             :tempo_ritardo  #"(\d+)"
                             :diversamente #"(DIVERSAMENTE DA QUANTO GIÀ ANNUNCIATO)"
                             :motivo_di_ritardo #"(\w+)";;Leggere da file??
                             :scuse_disagio #"(CI SCUSIAMO PER IL DISAGIO)"
                             :prestare_attenzione #"(INVITIAMO I VIAGGIATORI A PRESTARE ATTENZIONE A SUCCESSIVE COMUNICAZIONI DI PARTENZA)"
                             }]
    (infer-message-type italian-sentence semantic-slot-types)))




;;;Call Realizer

(defn create-xml-lf
  ""
  [semantic-hash]
  (cond
   (= (:type semantic-hash) :P1)
   "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<xml>
  <lf>
    <satop nom=\"a1:achievement\">
      <prop name=\"arrive\"/>
      <diamond mode=\"SYN-SUBJ\">
        <nom name=\"t1:station-objects\"/>
        <prop name=\"train\"/>
      </diamond>
    </satop>
  </lf>
</xml>"
   ))

(defn call-ATLASRealizer
  "call the realizer"
  [logical-form-string]
  (. (new realizer.ATLASRealizer) GetRealization "dummmy-string" logical-form-string)  )

(defn test-ATLASRealizer
  "call the realizer"
  [logical-form-string]
  (println (. (new realizer.ATLASRealizer) trialRealization logical-form-string))
  ;;(. (new realizer.ATLASRealizer) GetRealization "dummmy-string" logical-form-string)
  )

;;;Some examples
;;enlive trials
;(def ^:dynamic html/self-closing-tags #{:prop :nom})
(intern 'net.cgrand.enlive-html 'self-closing-tags #{:prop :nom})

(html/deftemplate lf-p1-simplified "templates-xml-lf/prova-num-00.xml"
  [post]
  [:prop#train-number]
  (html/do-> (html/set-attr :name  (str (first (:numero post))))
             (if (rest (:numero post)) (html/after (build-branch (rest (map str (:numero post))) "SYN-NOUN-CONTIN-DENOM")))
             ;;(if (rest (:numero post)) (html/after (html/emit* (build-branch-for-number-2 (rest (map str (:numero post)))))))
             )
  )
(def sample-hash {:numero "123"})
(defn prova-enlive [] (reduce str (lf-p1-simplified sample-hash)))


(html/deftemplate lf-p1 "templates-xml-lf/lf-p1-03.xml"
  [post]
  [:diamond#train-special] (if (empty? (:straordinario post))
                            (html/substitute "" ))
  [:prop#train-time-ampm] (html/set-attr :name (:ampm post) )
  [:prop#train-time-hh] (html/set-attr :name  (:hh post) )
  [:prop#train-time-mm] (html/set-attr :name  (:mm post) )
  [:prop#train-categ] (html/set-attr :name  (:categoria post) )
  [:prop#train-number]
  (html/do-> (html/set-attr :name  (str (first (:numero post))))
             (if (not-empty (rest (:numero post)))
               (html/after (build-branch (rest (map str (:numero post))) "SYN-NOUN-CONTIN-DENOM"))))
  ;;(html/set-attr :name  (:numero post) )
  [:prop#rail-number] (html/set-attr :name  (:numero_del_binario post) )
  [:prop#train-destination]
  (let
      [seq-nomi (seq (clojure.string/split (:località_di_arrivo post) #"\+"))]
      (html/do-> (html/set-attr :name  (first seq-nomi))
                 (html/after (if (not-empty (rest seq-nomi))
                               (build-branch (rest seq-nomi) "SYN-NOUN-APPOSITION")
                               ""))))
  ;;(html/set-attr :name  (:località_di_arrivo post) )
  [:prop#train-company] (html/set-attr :name  (:impresa_ferroviaria post) )
  ;;[:prop#train-company] (html/remove-attr :id )
)

;;(def hash-test-p1  {:ampm "evening" :hh "1" :mm "2" :categoria "redarrow" :numero "7" :numero_del_binario "4" :località_di_arrivo "salerno" :impresa_ferroviaria "trenitalia"})
(def hash-test-p1 {:fermate nil, :ampm "morning", :numero_del_binario "6", :ora_partenza "00:20", :straordinario nil, :categoria "regional", :hh "zero", :mm "59", :numero "4001", :type :P1, :località_di_arrivo "chivasso", :impresa_ferroviaria "trainitaly"})
(defn prova-enlive-2 [] (unescape-chars (reduce str (lf-p1 hash-test-p1))))
(defn prova-enlive-3 [] (unescape-chars (reduce str (lf-p1 (emerge-semantic-values (ita2sem (first (split-sentences (examples 2)))))))))

(defn build-branch
  ""
  [lista-elementi rel]
  (let [n (count lista-elementi)
        pre (str "\n                <diamond mode=\""
                 rel
                 "\">\n                 <nom name=\"w"
                 (java.util.UUID/randomUUID)
                 ":sem-obj\"/>\n                 <prop name=\""
                 (first lista-elementi)
                 "\"/>\n")
        post  "                </diamond>\n"]
    (str pre (if (> n 1) (build-branch (rest lista-elementi) rel) "")  post)))


(defn create-lis-sentence-naive
  "A very naive sentence realizer based on the semnatic values of the input.
   EXAMPLE:   treno frecciarossa numero 9572 binario numero 12 arrivare fut_prog"
  [hash-map-sentence]
  (let [message-type (:type hash-map-sentence)]
    (cond
     (= message-type :A1)
     (str "treno " (clojure.string/replace  (:categoria hash-map-sentence) #" " "_")
          " numero"  (clojure.string/replace (:numero hash-map-sentence) #"" " ")
          "binario numero " (:numero_del_binario hash-map-sentence)
          " arrivare fut_prog"
          ) ) ) )





(def examples
  ;;"Given the number of the line returns the corresponding string"
  ["AVVISIAMO CHE LE VETTURE IN TESTA AL TRENO  REGIONALE  10 4 2 9  DI  TRENITALIA  PER  ALESSANDRIA,  DELLE ORE  00,25  SONO  FUORI SERVIZIO.  SI INVITANO I VIAGGIATORI A PORTARSI VERSO LE  VETTURE  DI CENTRO  E  CODA TRENO.  OGGI  IL  TRENO  VIAGGIA  CON  VETTURE  DI  SOLA  SECONDA CLASSE.  CI SCUSIAMO PER IL DISAGIO" ;??
   "Il treno REGIONALE VELOCE, 20 32, di TRENITALIA, delle ore 00.10, proveniente da MILANO CENTRALE, è in arrivo al binario 18. Attenzione! Allontanarsi dalla linea gialla." ;A1
   "Il treno REGIONALE, 40 93, di TRENITALIA, delle ore 00.20, per MILANO CENTRALE, è in partenza dal binario 6.   Ferma in tutte le stazioni." ;P1
   "Il treno SERVIZIO FERROVIARIO METROPOLITANO - LINEA 3, 10 0 1 9, di TRENITALIA, delle ore 17.42, proveniente da BARDONECCHIA, è in arrivo al binario 12, invece che al binario 19. Attenzione! Allontanarsi dalla linea gialla. ." ;A2
   "Annuncio ritardo! Il treno REGIONALE VELOCE, 20 06, di TRENITALIA, delle ore 09.10, proveniente da MILANO CENTRALE, arriverà con un ritardo previsto di 10 MINUTI ." ;A3
   "Annuncio cancellazione treno! Il treno SERVIZIO FERROVIARIO METROPOLITANO - LINEA 7, 41 17, di TRENITALIA, previsto in partenza alle ore 13.00, per FOSSANO, oggi non sarà effettuato Ci scusiamo per il disagio." ;P9
   "Annuncio ritardo! Il treno REGIONALE, 24 8 2 5, di TRENITALIA,delle ore 17.28, per IVREA, partirà dal binario 15, con un ritardo previsto di 15 MINUTI  Invitiamo i viaggiatori a prestare attenzione a successive comunicazioni di partenza .";P5
   "Annuncio cancellazione treno! Il treno SERVIZIO FERROVIARIO METROPOLITANO - LINEA 3, 24 8 4 9, di TRENITALIA, previsto in arrivo da SUSA, alle ore 09.15, oggi non è stato effettuato . . Ci scusiamo per il disagio.";A5
   "Informiamo i viaggiatori che il treno REGIONALE, 24 8 1 7, di TRENITALIA, delle ore 11.28, per IVREA è in partenza dal binario 18.oggi il treno arriva fino a CHIVASSO,. Ferma a TORINO PORTA SUSA. . Ci scusiamo per il disagio.";P13

   "Il treno REGIONALE VELOCE, 18 38, di TRENITALIA, delle ore 22.50, proveniente da IMPERIA ONEGLIA, è in arrivo al binario 10, invece che al binario 6. Attenzione! Allontanarsi dalla linea gialla. ."
   ])







(defn total-test
  "call all the chain"
  []
  ;(test-ATLASRealizer (create-xml-lf (ita2sem (first (split-sentences (examples 2))))))
  ;(test-ATLASRealizer (slurp  "./resources/templates-xml-lf/lf-p1-03.xml"))
  (test-ATLASRealizer (prova-enlive-3)))
