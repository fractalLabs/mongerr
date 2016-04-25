(defproject mongerr "1.0.0-SNAPSHOT"
  :description "Wrapper over Monger, simpler to use and with some goodies"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cheshire "5.6.1"]     ;json support
                 [org.clojure/clojure "1.8.0"]
                 [compojure "1.5.0"]    ;for predefined routes
                 [environ "1.0.2"]      ;for env variables
                 [com.novemberain/monger "3.0.2"]
                 [nillib "0.1.0-SNAPSHOT"]
                 [com.rpl/specter "0.9.3"]]
  :repl-options {:init-ns mongerr.core})
