(defproject mongerr "1.0.0-SNAPSHOT"
  :description "Wrapper over Monger, simpler to use and with some goodies"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [cheshire "5.8.1"]     ;json support
                 [compojure "1.6.1"]    ;for predefined routes
                 [comun "0.1.0-SNAPSHOT"]
                 [digitalize "0.1.0-SNAPSHOT"]
                 [environ "1.1.0"]      ;for env variables
                 [com.novemberain/monger "3.5.0"]
                 [medley "1.1.0"]]
  :repl-options {:init-ns mongerr.core})
