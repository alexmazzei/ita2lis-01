(ns ita2lis-01.core-test
  (:require [clojure.test :refer :all]
            [ita2lis-01.core :refer :all]))

(deftest test-ita2sem
  "simple test of the semantic values analyzer"
  (let [italian-examples
        ["AVVISIAMO CHE LE VETTURE IN TESTA AL TRENO  REGIONALE  10 4 2 9  DI  TRENITALIA  PER  ALESSANDRIA,  DELLE ORE  00,25  SONO  FUORI SERVIZIO.  SI INVITANO I VIAGGIATORI A PORTARSI VERSO LE  VETTURE  DI CENTRO  E  CODA TRENO.  OGGI  IL  TRENO  VIAGGIA  CON  VETTURE  DI  SOLA  SECONDA CLASSE.  CI SCUSIAMO PER IL DISAGIO" ;??
   "Il treno REGIONALE VELOCE, 20 32, di TRENITALIA, delle ore 00.10, proveniente da MILANO CENTRALE, è in arrivo al binario 18. Attenzione! Allontanarsi dalla linea gialla." ;A1
   "Il treno REGIONALE, 40 93, di TRENITALIA, delle ore 00.20, per CHIVASSO, è in partenza dal binario 6.   Ferma in tutte le stazioni." ;P1
   "Il treno SERVIZIO FERROVIARIO METROPOLITANO - LINEA 3, 10 0 1 9, di TRENITALIA, delle ore 17.42, proveniente da BARDONECCHIA, è in arrivo al binario 12, invece che al binario 19. Attenzione! Allontanarsi dalla linea gialla. ." ;A2
   "Annuncio ritardo! Il treno REGIONALE VELOCE, 20 06, di TRENITALIA, delle ore 09.10, proveniente da MILANO CENTRALE, arriverà con un ritardo previsto di 10 MINUTI ." ;A3
   "Annuncio cancellazione treno! Il treno SERVIZIO FERROVIARIO METROPOLITANO - LINEA 7, 41 17, di TRENITALIA, previsto in partenza alle ore 13.00, per FOSSANO, oggi non sarà effettuato Ci scusiamo per il disagio." ;P9
   "Annuncio ritardo! Il treno REGIONALE, 24 8 2 5, di TRENITALIA,delle ore 17.28, per IVREA, partirà dal binario 15, con un ritardo previsto di 15 MINUTI  Invitiamo i viaggiatori a prestare attenzione a successive comunicazioni di partenza .";P5
   "Annuncio cancellazione treno! Il treno SERVIZIO FERROVIARIO METROPOLITANO - LINEA 3, 24 8 4 9, di TRENITALIA, previsto in arrivo da SUSA, alle ore 09.15, oggi non è stato effettuato . . Ci scusiamo per il disagio.";A5
   "Informiamo i viaggiatori che il treno REGIONALE, 24 8 1 7, di TRENITALIA, delle ore 11.28, per IVREA è in partenza dal binario 18.oggi il treno arriva fino a CHIVASSO,. Ferma a TORINO PORTA SUSA. . Ci scusiamo per il disagio.";P13

   "Il treno REGIONALE VELOCE, 18 38, di TRENITALIA, delle ore 22.50, proveniente da IMPERIA ONEGLIA, è in arrivo al binario 10, invece che al binario 6. Attenzione! Allontanarsi dalla linea gialla. ."
   ] ]
    (is (= (ita2sem (first  (split-sentences (italian-examples 1))))
           {:località_di_provenienza "MILANO CENTRALE", :numero_del_binario "18", :straordinario nil, :ora_arrivo "00:10", :categoria "REGIONALE VELOCE", :numero "2032", :type :A1, :località_di_arrivo nil, :impresa_ferroviaria "TRENITALIA"} ) )
    (is (= (ita2sem (first  (split-sentences (italian-examples 2))))
           {:fermate nil, :numero_del_binario "6", :ora_partenza "00:20", :straordinario nil, :categoria "REGIONALE", :numero "4093", :type :P1, :località_di_arrivo "CHIVASSO", :impresa_ferroviaria "TRENITALIA"}))
    (is (= (ita2sem (first  (split-sentences (italian-examples 3))))
           {:località_di_provenienza "BARDONECCHIA", :numero_del_binario "12", :straordinario nil, :ora_arrivo "17:42", :categoria "SERVIZIO FERROVIARIO METROPOLITANO - LINEA 3", :numero "10019", :type :A2, :località_di_arrivo nil, :numero_del_binario_programmato "19", :impresa_ferroviaria "TRENITALIA"} ))
    (is (= (ita2sem (first  (split-sentences (italian-examples 4))))
           {:località_di_provenienza "MILANO CENTRALE", :straordinario nil, :ora_arrivo "09:10", :categoria "REGIONALE VELOCE", :numero "2006", :type :A3, :località_di_arrivo nil, :impresa_ferroviaria "TRENITALIA"} ))
    (is (= (ita2sem (first  (split-sentences (italian-examples 5))))
           {:ora_partenza "13:00", :categoria "SERVIZIO FERROVIARIO METROPOLITANO - LINEA 7", :motivo_ritardo nil, :numero "4117", :type :P9, :località_di_arrivo "FOSSANO", :impresa_ferroviaria "TRENITALIA"} ))
    (is (= (ita2sem (first  (split-sentences (italian-examples 6))))
           {:diversamente nil, :numero_del_binario "15", :ora_partenza "17:28", :straordinario nil, :categoria "REGIONALE", :motivo_ritardo nil, :numero "24825", :prestare_attenzione "INVITIAMO", :type :P5, :scuse_disagio nil, :tempo_ritardo "15", :località_di_arrivo "IVREA", :impresa_ferroviaria "TRENITALIA"} ))
    (is (= (ita2sem (first  (split-sentences (italian-examples 7))))
           {:località_di_provenienza "SUSA", :ora_arrivo "09:15", :categoria "SERVIZIO FERROVIARIO METROPOLITANO - LINEA 3", :motivo_ritardo nil, :numero "24849", :type :A5, :scuse_disagio nil, :località_di_arrivo nil, :impresa_ferroviaria "TRENITALIA"} ))
    (is (= (ita2sem (first  (split-sentences (italian-examples 8))))
           {:fermate "CHIVASSO", :in_ritardo nil, :numero_del_binario nil, :ora_partenza "11:28", :oggi_fermate "18", :categoria "REGIONALE", :relazioni_di_percorrenza nil, :motivo_ritardo "OGGI IL TRENO ARRIVA FINO A CHIVASSO", :numero "24817", :type :P13, :scuse_disagio nil, :località_di_arrivo "IVREA", :impresa_ferroviaria "TRENITALIA"} ))
    (is (= (ita2sem (first  (split-sentences (italian-examples 9))))
           {:località_di_provenienza "IMPERIA ONEGLIA", :numero_del_binario "10", :straordinario nil, :ora_arrivo "22:50", :categoria "REGIONALE VELOCE", :numero "1838", :type :A2, :località_di_arrivo nil, :numero_del_binario_programmato "6", :impresa_ferroviaria "TRENITALIA"}  )) ) )
