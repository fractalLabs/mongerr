(defproject mongerr "1.0.0-SNAPSHOT"
  :description "Wrapper over Monger, simpler to use and with some goodies"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cheshire "5.8.0"]     ;json support
                 [org.clojure/clojure "1.8.0"]
                 [compojure "1.6.0"]    ;for predefined routes
                 [comun "0.1.0-SNAPSHOT"]
                 [environ "1.1.0"]      ;for env variables
                 [com.novemberain/monger "3.1.0"]
                 [medley "1.0.0"]]
  :repl-options {:init-ns mongerr.core})
