(defproject ui-check "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.clojure/clojure "1.9.0-alpha12" :scope "provided"]
                 [org.clojure/clojurescript "1.9.229" :scope "provided"]
                 [org.clojure/test.check "0.8.2"]
                 [org.omcljs/om "1.0.0-alpha47"]
                 [figwheel-sidecar "0.5.8" :scope "provided"]
                 [com.cemerick/piggieback "0.2.1" :scope "provided"]
                 [org.clojure/tools.nrepl "0.2.12" :scope "provided"]]
  :clean-targets ^{:protect false} ["resources/public/js"]
  :source-paths [ "src"]
  :profiles {:dev {:repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}
  )