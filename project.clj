(defproject ita2lis-01 "0.1.0-SNAPSHOT"
  :description "Analyzer and generator for LIS4ALL project"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["mvn_local_repo" "file:/Users/mazzei/Applications/libs/mvn_local_repo/"]]
  ;:repositories {"mvn_local_repo" ~(str (.toURI (java.io.File. "/Users/mazzei/Applications/libs/mvn_local_repo/")))}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [trove "2.1.1"]
                 [org.clojars.nakkaya/jdom "1.1.2"]
                 [enlive "1.1.5"]
                 [openccg "0.9.4"]
                 [org.clojure/data.csv "0.1.2"]
                 [seesaw "1.4.2" :exclusions [org.clojure/clojure]]
                 [org.clojars.hozumi/clj-commons-exec "1.1.0"]
                 ;;[clj-http "1.0.0"]
                 [clj-http-lite "0.2.0"]
                 ]
  :main ^:skip-aot ita2lis-01.core
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"]
;;  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"  "-classpath" "/Users/mazzei/lavori/Projects/ATLAS/softwareMazzei/xalan-j_2_7_1:/Users/mazzei/Applications/libs/:/Users/mazzei/Applications/libs/openccg.jar:/Users/mazzei/Applications/libs/jdom.jar:/Users/mazzei/Applications/libs/TUP.jar:/Users/mazzei/Applications/libs/LinguisticDescription.jar:/Users/mazzei/Applications/libs/trove.jar:/Users/mazzei/Applications/libs/mysql-connector-java-5.1.16-bin.jar:."]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
