(ns httper.recognizer
  (:require [clojure.string :as str]))

;; Matcher should recognize and destruct URL by:
;; host: domain
;; path: parts, splitted with "/"
;; queryparam: name/value pairs of query

(defprotocol Recognize
  (recognize [_ URL])
  (extract-params [this paramType paramPattern testURL]))

(defrecord Pattern [pattern]
    Recognize
    (recognize [this URL]
              (->>
                  (str/split pattern #";")
                  (map #(re-find #"(.+)\((.+)\)" (str/triml %)))
                  (map (fn[[_ k v]] [(keyword k) v]))
                  (map (fn[[k v]] (extract-params this k v URL)))
                  ((fn [list] (if-not (some empty? list)
                                (mapcat (fn[e] e) (rest list))
                                nil)))
              )
    )
    (extract-params [_ paramType paramPattern testURL]
               (if (= paramType :host)
                 (if (boolean (re-find (re-pattern (str "//" paramPattern "/")) testURL))  [paramType paramPattern] [])
                 (->>
                      (zipmap
                        (->>
                          (re-seq #"\?(.[^\/]+)" paramPattern)
                          (mapv (fn [[k v]] (keyword v))))
                        (let [reg (str/replace paramPattern #"\?(.[^\/]+)"  "[\\/\\&\\?]*(.[^\\/\\?\\&]+)")]
                          (rest (flatten (re-seq (re-pattern reg) testURL )))))
                        (into [])
                      )
                )
    )
)


(def twitter (Pattern. "host(twitter.com); path(?user/status/?id);"))
(recognize twitter "http://twitter.com/bradfitz/status/562360748727611392")
;; => ([:user "bradfitz"] [:id "562360748727611392"])

(def dribbble2 (Pattern. "host(dribbble.com); path(?user/shots/?id); queryparam(offset=?offset); queryparam(list=?type);"))
(recognize dribbble2 "https://dribbble.com/user/KRoLer/shots/1905065-Travel-Icons-pack?list=users&offset=10")
;; => ([:user "KRoLer"] [:id "1905065-Travel-Icons-pack"] [:offset "10"] [:type "users"])
(recognize dribbble2 "https://twitter.com/user/KRoLer/shots/1905065-Travel-Icons-pack?list=users&offset=10")
;; => nil (host mismatch)
(recognize dribbble2 "https://dribbble.com/user/KRoLer/shots/1905065-Travel-Icons-pack?list=users")
;; => nil (offset queryparam missing)
