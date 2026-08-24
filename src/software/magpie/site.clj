(ns software.magpie.site
  "https://Magpie.software static site"
  (:require
   [babashka.http-server :as bb-server]
   [clj-yaml.core :as yaml]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [lambdaisland.hiccup :as hiccup]
   [lambdaisland.kramdown :as kramdown]
   [lambdaisland.ornament :as o]
   [net.cgrand.enlive-html :as enlive]
   [org.httpkit.server :as server])
  (:import
   (java.io StringReader)))

(require 'software.magpie.site.styles)

(def site-origin "https://magpie.software")
(def target-dir "docs") ;; gh-pages requirement

(defn iso-date [^java.util.Date jud]
  (subs (str (.toInstant jud)) 0 10))

(defn xml->hiccup [node]
  (cond
    (string? node)
    node

    (seq? node)
    (into [:<>] (map xml->hiccup node))

    (map? node)
    (into (cond-> [(:tag node)]
            (:attrs node)
            (conj (:attrs node)))
          (map xml->hiccup (:content node)))))

(defn parse-markdown [contents]
  (-> contents
      kramdown/parse-gfm
      StringReader.
      enlive/html-resource
      (enlive/select [:body])
      first
      :content
      xml->hiccup))

(defn slurp-md-with-preamble [file]
  (let [text (slurp file)
        preamble (re-find #"^---\n[\s\S]*?\n---\n" text)]
    (when-not preamble
      (println "WARN: no valid preamble in" (str file)))
    (when preamble
      (merge
       {:md  (subs text (count preamble))
        :content (parse-markdown (subs text (count preamble)))
        :slug (str/replace (.getName (io/file file)) #".md$" "")}
       (yaml/parse-string (str/replace preamble "---\n" ""))
       ))))

(def language-filters
  {"inline-html"
   (fn [[_ o]]
     (into [::hiccup/unsafe-html] (drop 2 o)))})

(defn handle-language-filters [hiccup]
  (walk/postwalk
   (fn [o]
     (if (and (vector? o)
              (= :pre (first o))
              (vector? (second o))
              (= :code (first (second o)))
              (:class (second (second o))))
       (if-let [[_ lang] (re-find #"language-(.*)" (:class (second (second o))))]
         (if-let [f (get language-filters lang)]
           (f o)
           o)
         o)
       o))
   hiccup))

(defn inline-content [hiccup]
  (walk/postwalk
   (fn [o]
     (if-let [f (and (vector? o)
                     (= :div (first o))
                     (:inline (second o)))]
       [::hiccup/unsafe-html (slurp (io/file "site" f))]
       o))
   hiccup))

(defn layout [{:keys [title]} content & [footer]]
  [:html
   [:head
    [:title
     (when title (str title " | "))
     "Magpie Solutions"]
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:link {:rel "stylesheet" :href (str "/styles.css?" (System/currentTimeMillis))}]
    [:link {:rel "stylesheet" :href "/fonts/outfit.css"}]]
   [:body
    [:main
     content]
    (when footer
      footer)]])

(defn read-pages []
  (->> "site"
       io/file
       .listFiles
       (filter #(.isFile (io/file %)))
       (filter #(.endsWith (str %) ".md"))
       (remove #(= "index.md" (.getName %)))
       (keep slurp-md-with-preamble)
       (map handle-language-filters)
       (map inline-content)
       reverse))

(defn index []
  [:<>
   (parse-markdown (slurp "site/index.md"))
   ])

(defn page [{:keys [title date slug content]}]
  [:<>
   [:article
    [:header
     [:h1 title]]
    [:<> content]]])

(def footer
  [:footer
   ])

(defn paths []
  (let [pages (read-pages)]
    (-> {"/index.html" [layout {} [index] footer]
         "/styles.css" {:status 200
                        :headers {"content-type" "text/css"}
                        :body (o/defined-styles {:compress? false})}}
        (into (map (fn [{:keys [slug] :as p}]
                     [(str "/" (:slug p) ".html")
                      [layout p
                       [page p]
                       footer]]))
              pages))))

(defn response [o]
  (cond
    (map? o) o
    (string? o) {:status 200, :body o}
    (vector? o) {:status 200, :headers {"content-type" "text/html"} :body (hiccup/render o)}))

(defn render []
  (io/make-parents (str target-dir "/index.html"))
  (doseq [[path res] (paths)]
    (spit (str target-dir path) (:body (response res)))))

(defn handler [opts]
  (let [dirs (map #(#'bb-server/file-router % nil)
                  ["./public" "./assets"])]
    (fn [{:keys [uri request-method] :as req}]
      (println " " (str/upper-case (name request-method)) uri)
      (let [paths (paths)
            uri (if (= "/" uri) "/index.html" uri)]
        (if-let [res (get paths uri)]
          (response res)
          (let [req (update req :headers dissoc "range")]
            (some (fn [d]
                    (let [res (d req)]
                      (when (not= 404 (:status res))
                        #_(let [uri (if (= "/" uri) "/index.html" uri)
                                target  (io/file "out" (subs uri 1))]
                            (.mkdirs (io/file (.getParent target)))
                            (spit target (slurp (:body res))))
                        res)))
                  dirs)))))))

(defn run-server [opts]
  (let [port (:port opts 3801)]
    (println "Starting server on port" port)
    (server/run-server (fn [req] ((handler opts) req)) {:port port})))

(defn -main [& cmd]
  (if (= "dev" cmd)
    (run-server {})
    (render)))

(comment
  (run-server {})

  (clojure.java.browse/browse-url "http://localhost:3801")

  (let [p (inline-content (slurp-md-with-preamble "/home/arne/repos/arnebrasseur.net/site/coderdojo_pingpong.md"))]
    (hiccup/render     (layout p   (page p))))

  )
