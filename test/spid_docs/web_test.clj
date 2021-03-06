(ns spid-docs.web-test
  (:require [clojure.string :as str]
            [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [spid-docs.web :refer :all]))

(defn- hash-exists? [hash body]
  (re-find (re-pattern (str "id=\"" hash "\"")) body))

(defn- link-valid? [link page-url body pages the-app]
  (let [attrs (:attrs link)
        href (:href attrs)
        [path hash] (and href (str/split href #"#"))]
    (cond
     ;; If there's no href, but there is an id or name, it's an anchor. Relax,
     ;; it's ok.
     (and (nil? href)
          (or (:id attrs) (:name attrs)))
     :valid-link

     ;; we use #? to postpone writing a link
     (= "#?" href)
     (do (println "TODO: The" (:content link) "at" page-url "needs to point somewhere")
         :valid-link)

     ;; in-page hash navigation
     (and (empty? path) (not (empty? hash)))
     (if (hash-exists? hash body)
       :valid-link
       :link-to-unknown-hash)

     ;; external link
     (or (.startsWith path "//")
         (.startsWith path "http://")
         (.startsWith path "https://")
         (.startsWith path "mailto:"))
     :valid-link

     ;; assets, we're currently not checking those
    (or (.endsWith path ".png")
		(.endsWith path ".pdf"))
     :valid-link

     ;; sequence diagrams are generated from the page contents, so they're there.
     (.startsWith path "/sequence-diagrams/")
     :valid-link

     ;; internal link
     (or (contains? pages path)
         (contains? pages (str path "index.html")))
     (if (or (empty? hash)
             (hash-exists? hash (:body (the-app {:uri path}))))
       :valid-link
       :link-to-unknown-hash)

     :else (if (and (not (re-find #"/$" path))
                    (link-valid? {:attrs {:href (str path "/" (when hash (str "#" hash)))}
                                  :content (:content link)}
                                 page-url body pages the-app))
             :link-without-trailing-slash
             :link-to-unknown-page))))

(fact
 :slow
 "Integration tests. Avoid running them with:

 lein with-profile test midje :autotest :filter -slow"

 (let [pages (get-pages)
       the-app (memoize app)]
   (doseq [url (keys pages)]
     (let [response (the-app {:uri url})
           status (:status response)
           body (:body response)]

       ;; Check that the pages respond with 200 OK.
       [url status] => [url 200]

       ;; Check that all links point to existing pages
       (doseq [link (-> body
                        java.io.StringReader.
                        enlive/html-resource
                        (enlive/select [:a]))]
         (let [href (get-in link [:attrs :href])]
           [url href (link-valid? link url body pages the-app)] => [url href :valid-link]))))))
