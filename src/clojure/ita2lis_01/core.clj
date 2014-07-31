(ns ita2lis-01.core
   (:gen-class))

(require '[net.cgrand.enlive-html :as html]
         '[clojure.data.csv :as csv]
         '[clojure.xml :as xml])
(import '[java.net DatagramSocket
                   DatagramPacket
                   InetSocketAddress
                   ServerSocket])
;;(net.cgrand.reload/auto-reload *ns*)

;;; Algorithm
;;; 1. Preformat the whole sentence (e.g. time format)
;;; 2. Split the whole sentence into a number of semantic notable subsentences
;;; 3. Discover the sentence containing the train message
;;; 4. Extract (and post-format, e.g. train number) the semantic values in the train message
;;; 5. Return the hash-map containingt the semantic values (emerge it)
;;; 6. Use the semantic values to fill the template corresponding to the specific message class


;;; Constants
(def ^:dynamic indice 100)
(defn getNewInteger
  "Returns the value of indice and then increments it"
  []
  (let [ret indice]
    (def indice (inc indice))
    ret))

;; From lis4all.ini file
(def lis4ll-properties (java.util.Properties.))
(.load lis4ll-properties (clojure.java.io/reader  "./resources/lis4all.ini"))
(def input-port (Integer/parseInt(get lis4ll-properties "input-port")))
(def output-port (Integer/parseInt (get lis4ll-properties "output-port")))
(def output-host (get lis4ll-properties "output-host"))



(def examples
  ;;"Given the number of the line returns the corresponding string"
  ["AVVISIAMO CHE LE VETTURE IN TESTA AL TRENO  REGIONALE  10 4 2 9  DI  TRENITALIA  PER  ALESSANDRIA,  DELLE ORE  00,25  SONO  FUORI SERVIZIO.  SI INVITANO I VIAGGIATORI A PORTARSI VERSO LE  VETTURE  DI CENTRO  E  CODA TRENO.  OGGI  IL  TRENO  VIAGGIA  CON  VETTURE  DI  SOLA  SECONDA CLASSE.  CI SCUSIAMO PER IL DISAGIO" ;??
   "Il treno REGIONALE VELOCE, 20 32, di TRENITALIA, delle ore 00.10, proveniente da MILANO CENTRALE, è in arrivo al binario 18. Attenzione! Allontanarsi dalla linea gialla." ;A1
   "Il treno STRAORDINARIO REGIONALE, 40 93, di TRENITALIA, delle ore 00.20, per MILANO CENTRALE, è in partenza IN RITARDO dal binario 6.   Ferma in tutte le stazioni." ;P1
   "Il treno SERVIZIO FERROVIARIO METROPOLITANO - LINEA 3, 10 0 1 9, di TRENITALIA, delle ore 17.42, proveniente da BARDONECCHIA, è in arrivo al binario 12, invece che al binario 19. Attenzione! Allontanarsi dalla linea gialla. ." ;A2
   "Annuncio ritardo! Il treno REGIONALE VELOCE, 20 06, di TRENITALIA, delle ore 09.10, proveniente da MILANO CENTRALE, arriverà con un ritardo previsto di 10 MINUTI ." ;A3
   "Annuncio cancellazione treno! Il treno SERVIZIO FERROVIARIO METROPOLITANO - LINEA 7, 41 17, di TRENITALIA, previsto in partenza alle ore 13.00, per FOSSANO, oggi non sarà effettuato Ci scusiamo per il disagio." ;P9
   "Annuncio ritardo! Il treno REGIONALE, 24 8 2 5, di TRENITALIA,delle ore 17.28, per IVREA, partirà dal binario 15, con un ritardo previsto di 15 MINUTI  Invitiamo i viaggiatori a prestare attenzione a successive comunicazioni di partenza .";P5
   "Annuncio cancellazione treno! Il treno SERVIZIO FERROVIARIO METROPOLITANO - LINEA 3, 24 8 4 9, di TRENITALIA, previsto in arrivo da SUSA, alle ore 09.15, oggi non è stato effettuato . . Ci scusiamo per il disagio.";A5
   "Informiamo i viaggiatori che il treno REGIONALE, 24 8 1 7, di TRENITALIA, delle ore 11.28, per IVREA è in partenza dal binario 18.oggi il treno arriva fino a CHIVASSO,. Ferma a TORINO PORTA SUSA. . Ci scusiamo per il disagio.";P13
   "Il treno REGIONALE VELOCE, 18 38, di TRENITALIA, delle ore 22.50, proveniente da IMPERIA ONEGLIA, è in arrivo al binario 10, invece che al binario 6. Attenzione! Allontanarsi dalla linea gialla. ."
   "Il treno STRAORDINARIO REGIONALE VELOCE 20 32 di TRENITALIA delle ore 00.10, proveniente da MILANO CENTRALE E DIRETTO A   SALERNO   è in arrivo al binario 18. Attenzione! Allontanarsi dalla linea gialla.";A1
   "Il treno STRAORDINARIO REGIONALE VELOCE 20 32 di TRENITALIA delle ore 00.10, proveniente da SALERNO  E DIRETTO A MILANO CENTRALE   è in arrivo al binario 18. Attenzione! Allontanarsi dalla linea gialla.";A1
   "Il treno STRAORDINARIO REGIONALE VELOCE 20 32 di TRENITALIA delle ore 00.10, proveniente da SALERNO  E DIRETTO A NOVI LIGURE è in arrivo al binario 18. Attenzione! Allontanarsi dalla linea gialla.";A1
   "Il treno REGIONALE, 10 4 2 8, di TRENITALIA, delle ore 07.25, proveniente da NOVI LIGURE, è in arrivo al binario 2, invece che al binario 12. Attenzione! Allontanarsi dalla linea gialla.";A2
   ])


;;(def lexicon-ita2sem (read-lexicon "./resources/ita2sem2lis-lis4all-01.csv"))
;;(def file-stazioni "./resources/elenco-stazioni-01.txt")

(defn read-stazioni
  ""
  [file-lexicon]
  (with-open [in-file (clojure.java.io/reader file-lexicon)]
    (let [ar (doall (csv/read-csv in-file))]
      (map #(if (= (nth % 2) "rail_station") (nth % 0)) ar))))

(def stazioni-dictionary
  ;;"(CHIVASSO|MILANO CENTRALE|FIRENZE|BARDONECCHIA|FOSSANO|IVREA|SUSA|IMPERIA ONEGLIA)"
  ;;(str "(" (clojure.string/join "|" (remove #(nil? %)  (read-stazioni "./resources/ita2sem2lis-lis4all-01.csv"))) ")")
  (str "(" (clojure.string/join "|" (remove #(nil? %)  (read-stazioni (clojure.java.io/resource "ita2sem2lis-lis4all-01.csv")))) ")")  )

(def stazioni-dictionary-re (re-pattern stazioni-dictionary))
(def categorie-dictionary-re #"(REGIONALE|REGIONALE VELOCE|FRECCIAROSSA|SERVIZIO FERROVIARIO METROPOLITANO - LINEA 3|SERVIZIO FERROVIARIO METROPOLITANO - LINEA 7|ESPRESSO|EUROCITY|EURONIGHT|TGV|FRECCIARGENTO|FRECCIABIANCA|ITALO|INTERCITY|INTERCITY NOTTE|SUBURBANO|REGIOEXPRESS|MALPENSA EXPRESS|TRENO METROPOLITANO|ACCELERATO|DIRETTO|DIRETTISSIMO)")
(def imprese_ferroviarie-dictionary-re #"(TRENITALIA|NSV|GTT|TRENORD|SAD|ENTE AUTONOMO VOLTURNO)")
(def fermate-dictionary-re (re-pattern (str "(FERMA IN TUTTE LE STAZIONI|FERMA A " stazioni-dictionary "+|IN TUTTE LE STAZIONI ECCETTO A " stazioni-dictionary "+|NON SONO PREVISTE FERMATE INTERMEDIE|OGGI FERMA ANCHE A " stazioni-dictionary  "+)")))
(def oggi-fermate-dictionary-re (re-pattern (str "(OGGI IL TRENO ARRIVA FINO A " stazioni-dictionary "|OGGI IL TRENO ARRIVA A " stazioni-dictionary "INVECE CHE A" stazioni-dictionary ")")))


(defn read-lexicon
  ""
  [file-lexicon]
  (with-open [in-file (clojure.java.io/reader file-lexicon)]
    (let [ ar (doall (csv/read-csv in-file))]
      (zipmap (map #(nth % 0) ar) (map #(nth % 3) ar)))))
(def lexicon-ita2sem
  ;;(read-lexicon "./resources/ita2sem2lis-lis4all-01.csv")
  (read-lexicon (clojure.java.io/resource "ita2sem2lis-lis4all-01.csv"))
  )


;;;String functions to pre-process the input
(def sentece-general-delimeters #"\. ")

(defn capitalize [s]  (.toUpperCase s))
(defn rm-useless-chars [s]  (clojure.string/replace s #"[,]|[.] | [.]" " "))
(defn reduce-whites [s]  (clojure.string/replace s #" +" " "))
(defn delete-aewlis-000 [s]  (clojure.string/replace s "<signSpatialLocation>0.0 0.0 0.0 </signSpatialLocation>\n" ""))



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
;; aux-udp
;; Constants necessary for UDP sender/receiver
(def socket-output (DatagramSocket. ))
(def socket-input (DatagramSocket. ))
;; (if (not (nil? (quote socket-input))) (.close socket-input))
;; (def socket-input (DatagramSocket. input-port))

(defn send-udp
"Send a short textual message over a DatagramSocket to the specified host and port. If the string is over 512 bytes long, it will be truncated."
[^DatagramSocket socket msg host port]
(let [payload (.getBytes msg)
            length (min (alength payload) 512)
            address (InetSocketAddress. host port)
            packet (DatagramPacket. payload length address)]
        (.send socket packet)))

(defn receive-udp
  "Block until a UDP message is received on the given DatagramSocket, and return the payload message as a string."
  [^DatagramSocket socket]
  (let [buffer (byte-array 512)
        packet (DatagramPacket. buffer 512)]
    (.receive socket packet)
    (String. (.getData packet)
             0 (.getLength packet)
             "UTF8")))

(defn receive-loop-udp
  "Given a function and DatagramSocket, will (in another thread) wait for the socket to receive a message, and whenever it does, will call the provided function on the incoming message."
  [socket f]
  (future (while true (f (receive-udp socket)))))

(defn send-msg-to-donna
  "Send a message to donna on the output socket"
  [msg]
  (send-udp socket-output msg output-host output-port) )

(defn send-filename-aewlis-to-donna
  "send the name of a aewlis file, which is in the excution dir, to donna"
  [file-name-aewlis]
  (send-msg-to-donna (str "play_aewlis " (System/getProperty "user.dir") "/" file-name-aewlis)))

;; aux tcp
(defn receive-tcp
  "Read a line of textual data from the given socket" [socket]
  (.readLine (clojure.java.io/reader socket)))

(defn send-tcp
  "Send the given string message out over the given socket" [socket msg]
  (let [writer (clojure.java.io/writer socket)]
    (.write writer msg)
    (.flush writer)))

(defn serve-tcp [port handler]
  (with-open [server-sock (ServerSocket. port)
              sock (.accept server-sock)]
    (let [msg-in (receive-tcp sock)
          msg-out (handler msg-in)]
      (send-tcp sock msg-out))))

(defn serve-tcp-persistent [port handler]
  "as serve but with a inner-loop thread safe"
  (let [running (atom true)]
    (future
      (with-open [server-sock (ServerSocket. port)]
        (while @running
          (with-open [sock (.accept server-sock)]
            (let [msg-in (receive-tcp sock) msg-out (handler msg-in)]
              (send-tcp sock msg-out))))))
    running))



;;aux-xml
;; (defn ppxml [xml]
;;   (let [in (javax.xml.transform.stream.StreamSource.
;;             (java.io.StringReader. xml))
;;         writer (java.io.StringWriter.)
;;         out (javax.xml.transform.stream.StreamResult. writer)
;;         transformer (.newTransformer
;;                      (javax.xml.transform.TransformerFactory/newInstance))]
;;     (.setOutputProperty transformer
;;                         javax.xml.transform.OutputKeys/INDENT "yes")
;;     (.setOutputProperty transformer
;;                         "{http://xml.apache.org/xslt}indent-amount" "2")
;;     (.setOutputProperty transformer
;;                         javax.xml.transform.OutputKeys/METHOD "xml")
;;     (.transform transformer in out)
;;     (-> out .getWriter .toString)))

(defn map-vals
  "apply a function to the vals of a hash"
  [hhh fff]
  (zipmap (keys hhh) (map fff (vals hhh))))

(defn map-kv
  "Given a map and a function of two arguments, returns the map resulting from applying the function to each of its entries. The provided function must return a pair (a two-element sequence.)"
  [m f]
  (into {} (map (fn [[k v]] (f k v)) m)))

(defn extract-cartelli
  "extract the keys/values corresponding to the cartelli"
  [semantic-hash]
  (map-kv semantic-hash (fn [k v]
                          (if (and (not (nil? v)) (not (keyword? v)) (re-find #"^cartello" v))
                            (vector k (get (re-find #"^cartello<(.+)>" v) 1))))))

(defn remove-cartelli
  "replace the values corresponding to the cartelli with their corresponding keys"
  [semantic-hash]
  (map-kv semantic-hash (fn [k v]
                          (if (and (not (nil? v)) (not (keyword? v)) (re-find #"^cartello" v))
                            (vector k (clojure.string/replace-first (str k) ":" ""))
                            (vector k v)))))


(defn ita-lex-sem
  "Returns the semantics of a specific italian word"
  [sem-hash lexicon]
  (map-vals sem-hash
            ;;#(if (get lexicon %) (get lexicon %) %)
            #(let [l (get lexicon %)]
               ;;(println "DEBUG::>" l "<")
               (cond
                (= l "??") (str  "cartello<" % ">")
                (not (nil? l)) l
                :else %))
            ) )

(defn hm2ampm_hh_mm
  "The input is a time string hh:mm, the ouput is {:ampm morning/afternoon :hh hh :mm mm}."
  [hhmm]
  (let [[hs ms] (clojure.string/split hhmm #":")
        h (Integer/parseInt hs)
        ampm (if (<=  h 12) "morning" "afternoon")
        hhh (if (<= h 12) h (- h 12))
        hh (if (== 0 hhh) "zero" (str hhh))
        m (Integer/parseInt ms)
        mm (if (== 0 m) "zero" (str m))
        ]
    {:ampm ampm :hh (str hhh) :mm (str (Integer/parseInt ms))} ))

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
(defn build-branch
  "Build a xml branch with several nodes"
  [lista-elementi rel]
  (let [n (count lista-elementi)
        pre (str "\n                <diamond mode=\""
                 rel
                 "\">\n                 <nom name=\"w"
                 ;;(java.util.UUID/randomUUID)
                 ;;(+ 100 (rand-int 300))
                 ;;(System/currentTimeMillis)
                 (getNewInteger)
                 ":sem-obj\"/>\n                 <prop name=\""
                 (first lista-elementi)
                 "\"/>\n")
        post  "                </diamond>\n"]
    (str pre (if (> n 1) (build-branch (rest lista-elementi) rel) "")  post)))



(defn extract-content-words-2
  ""
  []
  ;;(clojure.string/join "\n" (rest (sort
  (set (map #(let [mat-1 (re-matcher (re-pattern (str "proveniente da ([^,]+),")) %)
                    mat-2 (re-matcher (re-pattern (str "direto a ([^,]+),")) %)]
                (cond (re-find mat-1) ((re-groups mat-1) 1)
                      (re-find mat-2) ((re-groups mat-1) 1)))
            (clojure.string/split ;;(slurp "./resources/Annunci-CSV.csv")
             (slurp
              (clojure.java.io/resource "Annunci-CSV.csv"))
             #"\n"))))


(defn emerge-semantic-values
  "express expicitely the hidden semantic values in the hash. (nomore necessary lower-case)"
  [semantic-hash]
  (if (nil? semantic-hash)
    {}
    (let [semantic-hash-temp (ita-lex-sem semantic-hash lexicon-ita2sem)]
      (if (:ora_arrivo semantic-hash)
        (merge semantic-hash-temp (hm2ampm_hh_mm (:ora_arrivo semantic-hash)))
          (merge semantic-hash-temp (hm2ampm_hh_mm (:ora_partenza semantic-hash)))))))

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
                                    (:impresa_ferroviaria semantic-slot-types)
                                    "\\s?DELLE ORE\\s?"
                                    (:ora_arrivo semantic-slot-types)
                                    "\\s?(PROVENIENTE DA)?\\s?"
                                    (:località_di_provenienza semantic-slot-types)
                                    "\\s?(E DIRETTO A)?\\s?"
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
                                    (:impresa_ferroviaria semantic-slot-types)
                                    "\\s?DELLE ORE\\s?"
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
                                    (:impresa_ferroviaria semantic-slot-types)
                                    "\\s?DELLE ORE\\s?"
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
                                    (:impresa_ferroviaria semantic-slot-types)
                                    "\\s?DELLE ORE\\s?"
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
    (let  [ar (re-groups mat-a1)] (hash-map :type :A1 :straordinario (ar 1) :categoria (ar 2) :numero (train-number-format-normalize (ar 3)) :impresa_ferroviaria (ar 6) :ora_arrivo (ar 7) :località_di_provenienza (ar 9) :località_di_arrivo (ar 11) :numero_del_binario (ar 12) ) )
    (re-find mat-a2)
    (let  [ar (re-groups mat-a2)] (hash-map :type :A2 :straordinario (ar 1) :categoria (ar 2) :numero (train-number-format-normalize (ar 3)) :impresa_ferroviaria (ar 6) :ora_arrivo (ar 7) :località_di_provenienza (ar 9) :località_di_arrivo (ar 10) :numero_del_binario (ar 12) :numero_del_binario_programmato (ar 13) ) )
    (re-find mat-a3)
    (let  [ar (re-groups mat-a3)] (hash-map :type :A3 :straordinario (ar 1) :categoria (ar 2) :numero (train-number-format-normalize (ar 3)) :impresa_ferroviaria (ar 6) :ora_arrivo (ar 7) :località_di_provenienza (ar 9) :località_di_arrivo (ar 10)  ) )
    (re-find mat-a5)
    (let  [ar (re-groups mat-a5)] (hash-map :type :A5  :categoria (ar 1) :numero (train-number-format-normalize (ar 2)) :impresa_ferroviaria (ar 5) :località_di_provenienza (ar 6) :ora_arrivo (ar 7)  :località_di_arrivo (ar 8)   :motivo_ritardo (ar 10) :scuse_disagio (ar 11) ) )
    (re-find mat-p1)
    (let  [ar (re-groups mat-p1)] (hash-map :type :P1 :straordinario (ar 1) :categoria (ar 2) :numero (train-number-format-normalize (ar 3)) :impresa_ferroviaria (ar 6) :ora_partenza (ar 7) :località_di_arrivo (ar 8)  :in_ritardo (ar 11) :numero_del_binario (ar 12) :fermate (ar 13) ) )
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
(def javaRealizer (new realizer.ATLASRealizer))
(defn call-ATLASRealizer
  "call the realizer"
  [logical-form-string]
  (. (new realizer.ATLASRealizer) GetRealization "dummmy-string" logical-form-string)  )

(defn test-ATLASRealizer
  "call the realizer"
  [logical-form-string]
  (println (. (new realizer.ATLASRealizer) trialRealization logical-form-string)))

(defn realize-lf
  ""
  [logical-form-string]
  (. javaRealizer trialRealization logical-form-string))


;;;Some examples
;;enlive trials
;(def ^:dynamic html/self-closing-tags #{:prop :nom})
(intern 'net.cgrand.enlive-html 'self-closing-tags #{:prop :nom })

(html/deftemplate lf-p1-simplified "templates-xml-lf/prova-num-00.xml"
  [post]
  [:prop#train-number]
  (html/do-> (html/set-attr :name  (str (first (:numero post))))
             (if (rest (:numero post)) (html/after (build-branch (rest (map str (:numero post))) "SYN-NOUN-CONTIN-DENOM")))))
(def sample-hash {:numero "123"})
(defn prova-enlive [] (reduce str (lf-p1-simplified sample-hash)))


(html/deftemplate lf-p1 "templates-xml-lf/lf-p1-06.xml"
  [post]
  [:diamond#train-special] (if (empty? (:straordinario post)) (html/substitute "" ) (html/set-attr :train-special "special"))
  [:diamond#train-late] (if (empty? (:in_ritardo post)) (html/substitute "" ) (html/set-attr :train-late "late"))
  [:prop#train-time-ampm] (html/set-attr :name (:ampm post) )
  [:prop#train-time-hh] (html/set-attr :name  (:hh post) )
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; (html/do-> (html/set-attr :name  (str (first (:hh post))))                                                                     ;;
  ;;            (if (not-empty (rest (:hh post))) (html/after (build-branch (rest (map str (:hh post))) "SYN-NOUN-CONTIN-DENOM")))) ;;
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  [:prop#train-time-mm] ;(html/set-attr :name  (:mm post) )
  (html/do-> (html/set-attr :name  (str (first (:mm post))))                                                                     ;;
             (if (not-empty (rest (:mm post))) (html/after (build-branch (rest (map str (:mm post))) "SYN-NOUN-CONTIN-DENOM")))) ;;
  [:prop#train-categ] (html/set-attr :name  (:categoria post) )
  [:prop#train-number]  ;(html/set-attr :name  (:numero post) )
  (html/do-> (html/set-attr :name  (str (first (:numero post))))
             (if (not-empty (rest (:numero post))) (html/after (build-branch (rest (map str (:numero post))) "SYN-NOUN-CONTIN-DENOM"))))
  [:prop#rail-number] (html/set-attr :name  (:numero_del_binario post) )
   [:prop#train-destination] ;(html/set-attr :name  (:località_di_arrivo post) )
  (let [seq-nomi (seq (clojure.string/split (:località_di_arrivo post) #"\+"))]
    (html/do-> (html/set-attr :name  (first seq-nomi))
               (html/after (if (not-empty (rest seq-nomi)) (build-branch (rest seq-nomi) "SYN-NOUN-APPOSITION")))))
  [:prop#train-company] (html/set-attr :name  (:impresa_ferroviaria post) )
  ;;[:prop#train-company] (html/remove-attr :id )
)

(html/deftemplate lf-a1 "templates-xml-lf/lf-a1-03.xml"
  [post]
  [:diamond#train-special] (if (empty? (:straordinario post)) (html/substitute "" ) (html/set-attr :train-special "special"))
  [:prop#train-time-ampm] (html/set-attr :name (:ampm post) )
  [:prop#train-time-hh] (html/set-attr :name  (:hh post) )
  [:prop#train-time-mm]
  (html/do-> (html/set-attr :name  (str (first (:mm post))))
             (if (not-empty (rest (:mm post))) (html/after (build-branch (rest (map str (:mm post))) "SYN-NOUN-CONTIN-DENOM"))))
  [:prop#train-categ] (html/set-attr :name  (:categoria post) )
  [:prop#train-number]
  (html/do-> (html/set-attr :name  (str (first (:numero post))))
             (if (not-empty (rest (:numero post))) (html/after (build-branch (rest (map str (:numero post))) "SYN-NOUN-CONTIN-DENOM"))))
  [:prop#rail-number] (html/set-attr :name  (:numero_del_binario post) )
  [:prop#train-origin]
  (let [seq-nomi (seq (clojure.string/split (:località_di_provenienza post) #"\+"))]
    (html/do-> (html/set-attr :name  (first seq-nomi))
               (html/after (if (not-empty (rest seq-nomi)) (build-branch (rest seq-nomi) "SYN-NOUN-APPOSITION")))))
  [:prop#train-company] (html/set-attr :name  (:impresa_ferroviaria post) )
  [:diamond#train-destination-yn] (if (empty? (:località_di_arrivo post)) (html/substitute "" ) (html/set-attr :train-boo "foo"))
  [:prop#train-destination]
  (if (empty? (:località_di_arrivo post)) (html/substitute "" )
      (let [seq-nomi (seq (clojure.string/split (:località_di_arrivo post) #"\+"))]
        (html/do->
         (html/set-attr :name  (first seq-nomi))
         (html/after (if (not-empty (rest seq-nomi)) (build-branch (rest seq-nomi) "SYN-NOUN-APPOSITION")))))))


;;(def hash-test-p1  {:ampm "evening" :hh "1" :mm "2" :categoria "redarrow" :numero "7" :numero_del_binario "4" :località_di_arrivo "salerno" :impresa_ferroviaria "trenitalia"})
(def hash-test-p1 {:fermate nil, :ampm "morning", :numero_del_binario "6", :ora_partenza "00:20", :straordinario nil, :categoria "regional", :hh "0", :mm "59", :numero "4001", :type :P1, :località_di_arrivo "chivasso", :impresa_ferroviaria "trainitaly"})
(defn prova-enlive-2 [] (unescape-chars (reduce str (lf-p1 hash-test-p1))))
(defn prova-enlive-3 [] (unescape-chars (reduce str (lf-p1 (emerge-semantic-values (ita2sem (first (split-sentences (examples 2)))))))))
(defn prova-enlive-4 [] (unescape-chars (reduce str (lf-a1 (emerge-semantic-values (ita2sem (first (split-sentences (examples 10)))))))))

(defn call-enlive-template
  ""
  [semantic-hash]
  (let
      [type (get semantic-hash :type )]
    (cond
     (= type :A1)
     (unescape-chars (reduce str (lf-a1 semantic-hash)))
     (= type :P1)
     (unescape-chars (reduce str (lf-p1 semantic-hash)))
     (= type :A2)
     (unescape-chars (reduce str (lf-a1 semantic-hash)))
     (= type :A3)
     (unescape-chars (reduce str (lf-a1 semantic-hash)))
     (= type :P9)
     (unescape-chars (reduce str (lf-p1 semantic-hash)))
     (= type :P5)
     (unescape-chars (reduce str (lf-p1 semantic-hash)))
     (= type :A5)
     (unescape-chars (reduce str (lf-a1 semantic-hash)))
     (= type :P13)
     (unescape-chars (reduce str (lf-p1 semantic-hash)))
     :else ""     )))

(defn post-processing-cartelli
  "replace the correct cartello signs with the correct attribute for spelling"
  [seq-cartelli xml-out]
  (if (empty? seq-cartelli)
    xml-out
    (recur (rest seq-cartelli)
           ;; (clojure.string/replace-first
           ;;  xml-out ;; mettere qui<<<prima un replace di "(str (str (get (first seq-cartelli) 0)) ":" "") " number>")  con "$1 <signboard> (get (first seq-cartelli) 1)<signboard>"
           ;;  (str  (clojure.string/replace-first (str (get (first seq-cartelli) 0)) ":" "") "\"")
           ;;  (str "cartello\" content=\""  (get (first seq-cartelli) 1) "\""))
           (clojure.string/replace-first
            xml-out
            (re-pattern
             (str "(" (clojure.string/replace-first (str (get (first seq-cartelli) 0)) ":" "") ")"
                  "(\" idAtlasSign=\"\\d+\">\n)(<handsNumber>\\d</handsNumber>)"))
            (str "cartello$2$3\n"
                 "<signboard>" (get (first seq-cartelli) 1) "</signboard>")) )))




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



(defn total-test
  "call all the chain"
  []
  ;;(test-ATLASRealizer (create-xml-lf (ita2sem (first (split-sentences (examples 2))))))
  ;;(test-ATLASRealizer (slurp  "./resources/templates-xml-lf/lf-a1-02.xml"))
  (test-ATLASRealizer (prova-enlive-4))
  ;;(test-ATLASRealizer (slurp "./resources/templates-xml-lf/prova.xml"))
  )

(defn alea2plain
  ""
  [xml-alea]
  (let [pat (re-pattern (str "lemma=\"" "([^\\\"]+)" "\""
                             ".+"
                             "Sign=\"" "([^\\\"]+)" "\""
                             ".+\\\n.+\\\n"
                             "(<signboard>(.+)</signboard>)?"
                             ;;">" "([1,2])" "<" ".+";; bug??
                             ))]
    (reduce str  (map  (fn [x] (str  (nth x 1)
                                    (if (not (empty? (nth x 4)))  (str "<" (str  (nth x 4)) ">"))
                                    "-" (nth x 2) " ")) (re-seq pat xml-alea)))) )

;; Main functions
(defn analyze-and-generate
  "Per gestire i cartelli devo fare pre e post processing poiché openccg non gestisce lessici ;aperti'. La gestione dei cartelli avviene in parallelo usando sue hash: l'hash semantico viene modificato, cambiando il value dello slot con il nome dello slot; parallellamente un nuovo hash cartelli viene creato. In post processing viene riassegnato l'attributo giusto in corrispondenza di un segno cartello."
  [sentence]
  (let [emerged-semantics (emerge-semantic-values (ita2sem (first (split-sentences sentence))))
        modified-emerged-semantics (remove-cartelli emerged-semantics)
        hash-cartelli (extract-cartelli emerged-semantics)
        out-templating (call-enlive-template modified-emerged-semantics)]
    ;;(println "DEBUG:: MODIFIED Semantics=" modified-emerged-semantics)
    ;;(println "DEBUG:: OUT-enlive=" out-templating)
    (if (empty? out-templating)
      (slurp (clojure.java.io/resource "templates-xml-aewlis/template-aewlis-test.xml"))
      (post-processing-cartelli
       (seq  hash-cartelli)
       (delete-aewlis-000 (realize-lf out-templating))
       ))))

(defn analyze-and-generate-write-file
  ""
  [sentence file-name-aewlis]
  (spit (str (System/getProperty "user.dir") "/" file-name-aewlis) (analyze-and-generate sentence)))

(defn real-main
  ""
  [sentence]
  (let
      [file-name-aewlis (str "out-aewlis-" (System/currentTimeMillis) ".xml")]
    (do
      (println "inputSentence:" sentence)
      (println "semantic values:" (ita2sem (first (split-sentences sentence))))
      (println "outputTranslation:" (analyze-and-generate sentence))
      (analyze-and-generate-write-file sentence file-name-aewlis)
      (println "generated aewlis:" file-name-aewlis)
      ;;(send-filename-aewlis-to-donna file-name-aewlis) ;UDP
      (str file-name-aewlis) ;TCP
      )))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (do
    (println "Ready to translate train messages!")
    ;;(receive-loop-udp socket-input real-main)
    ;;(serve-tcp input-port real-main)
    (serve-tcp-persistent input-port real-main)
    ))
