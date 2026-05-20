#!/usr/bin/env bb --config /Users/waddie/.config/babashka/bb.edn
(ns html2hiccup
  (:require [babashka.cli :as cli]
            [camel-snake-kebab.core :as csk]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [zprint.core :as zprint])
  (:import [org.jsoup Jsoup]
           [org.jsoup.nodes
            Attribute
            Comment
            DataNode
            Element
            Node
            TextNode]))

(def default-opts
  {:add-classes-to-tag-keyword true
   :kebab-attrs  false
   :mapify-style false})

(def special-attr-cases
  {"baseprofile" "baseProfile"
   "viewbox"     "viewBox"})

(defn normalize-css-value
  [v]
  (cond (re-matches #"^-?\d+(\.\d+)?$" v) (Double/parseDouble v)
        (re-find #"^[a-zA-Z]+$" v) (keyword v)
        :else (str v)))

(defn mapifier
  [style-str]
  (try (into {}
             (for [[_ k v]
                   (re-seq #"(\S+?)\s*:\s*([^;]+);?" style-str)]
               [(-> k
                    str/lower-case
                    keyword)
                (normalize-css-value v)]))
       (catch Exception _ style-str)))

(defn normalize-attr-key
  [k kebab-attrs]
  (let [s (name k)]
    (keyword (or (special-attr-cases (str/lower-case s))
                 (if kebab-attrs (csk/->kebab-case s) (str/lower-case s))))))

(defn normalize-attrs
  [attrs {:keys [mapify-style kebab-attrs]}]
  (let [m (into {}
                (map (fn [[k v]] [(normalize-attr-key k kebab-attrs) v]))
                attrs)]
    (if (and mapify-style (:style m)) (update m :style mapifier) m)))

(defn valid-as-hiccup-kw?
  [s]
  (and s (not (re-matches #"^\d.*|.*[./:#@~`\[\]\(\){}].*" s))))

(defn tag+id [tag id] (str tag (when (valid-as-hiccup-kw? id) (str "#" id))))

(defn bisect-all-by
  [pred coll]
  (reduce (fn [[yes no] x] (if (pred x) [(conj yes x) no] [yes (conj no x)]))
          [[] []]
          coll))

(defn build-tag-with-classes
  [tag-w-id kw-classes]
  (str tag-w-id (when (seq kw-classes) (str "." (str/join "." kw-classes)))))

(declare node->hiccup)

(defn text-node->hiccup
  [^TextNode node]
  (let [s (some-> node
                  .text
                  str/trim)]
    (when-not (str/blank? s) s)))

(defn comment-node->hiccup [^Comment node] (list 'comment (.getData node)))

(defn attrs->map
  [^Element el]
  (into {}
        (map (fn [^Attribute a] [(.getKey a) (.getValue a)]))
        (.attributes el)))

(defn element->hiccup
  [^Element el options]
  (let [normalized-attrs (normalize-attrs (attrs->map el) options)

        {:keys [id class]} normalized-attrs

        lower-tag (.tagName el)

        tag-id (tag+id lower-tag id)

        classes (when class (str/split class #"\s+"))

        [kw-classes remaining-classes]
        (if-not (:add-classes-to-tag-keyword options)
          [[] classes]
          (bisect-all-by valid-as-hiccup-kw? classes))

        tag-name (build-tag-with-classes tag-id kw-classes)

        remaining-attrs
        (cond-> normalized-attrs
          true (dissoc :class)
          (seq remaining-classes) (assoc :class (vec remaining-classes))
          (valid-as-hiccup-kw? id) (dissoc :id))

        children
        (->> (.childNodes el)
             (map #(node->hiccup % options))
             (remove nil?))]
    (into (cond-> [(keyword tag-name)]
            (seq remaining-attrs) (conj remaining-attrs))
          children)))

(defn node->hiccup
  [^Node node options]
  (cond (instance? Element node) (element->hiccup node options)
        (instance? TextNode node) (text-node->hiccup node)
        (instance? Comment node) (comment-node->hiccup node)
        (instance? DataNode node) (some-> node
                                          .getWholeData
                                          str/trim
                                          not-empty)
        :else nil))

(defn html->hiccup
  ([html] (html->hiccup html default-opts))
  ([html options]
   (let [doc  (Jsoup/parseBodyFragment html)
         body (.body doc)]
     (->> (.childNodes body)
          (map #(node->hiccup % (merge default-opts options)))
          (remove nil?)
          vec))))

(defn pretty-print
  [form]
  (zprint/zprint-str form
                     {:map   {:comma? false}
                      :style :hiccup}))

(defn convert-html
  [html options]
  (->> (html->hiccup html options)
       (map pretty-print)
       (str/join "\n")))

(defn output-path
  [input-file]
  (let [f      (io/file input-file)
        parent (.getParent f)
        name   (.getName f)
        stem   (str/replace name #"\.[^.]+$" "")
        out    (str stem ".edn")]
    (if parent (str (io/file parent out)) out)))

(def cli-spec
  {:add-classes-to-tag-keyword {:coerce :boolean}
   :kebab-attrs  {:coerce :boolean}
   :mapify-style {:coerce :boolean}})

(defn usage
  []
  (println
   (str/join "\n"
             ["html2hiccup"
              ""
              "Usage:"
              "  html2hiccup [options]"
              "  html2hiccup [options] file1.html file2.htm ..."
              ""
              "No files:"
              "  Reads HTML from stdin"
              "  Writes hiccup to stdout"
              ""
              "Files:"
              "  Writes sibling .edn files"
              ""
              "Options:"
              "  --mapify-style true|false"
              "  --kebab-attrs true|false"
              "  --add-classes-to-tag-keyword true|false"])))

(defn process-file
  [path options]
  (let [html     (slurp path)
        out-path (output-path path)
        result   (convert-html html options)]
    (spit out-path result)
    (println "Wrote" out-path)))

(defn -main
  [& argv]
  (let [{:keys [opts args]}
        (cli/parse-args argv {:spec cli-spec})]
    (cond (or (:h opts) (:help opts)) (usage)
          (seq args) (doseq [path args]
                       (process-file path opts))
          :else (-> (slurp *in*)
                    (convert-html opts)
                    (println)))))

(apply -main *command-line-args*)
