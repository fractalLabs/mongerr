(defproject mongerr "1.0.0-SNAPSHOT"
  :description "Wrapper over Monger, simpler to use and with some goodies"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [cheshire "5.11.0"]     ;json support
                 [clj-time "0.15.2"]
                 [compojure "1.7.0"]    ;for predefined routes
                 [comun "0.1.0-SNAPSHOT"]
                 [digitalize "0.1.1-SNAPSHOT"]
                 [environ "1.2.0"]      ;for env variables
                 [com.novemberain/monger "3.6.0"]
                 [medley "1.4.0"]]
  :repl-options {:init-ns mongerr.core})
