(ns httper.recognizer)
(use '[clojure.string :only (split triml)])

;; Matcher should recognize and destruct URL by:
;; host: domain
;; path: parts, splitted with "/"
;; queryparam: name/value pairs of query

(defprotocol Recognize
  (recognize [this URL])
  (extractParam [this paramType paramPattern testURL]))

(deftype Pattern [pattern]
    Recognize
    (recognize [this URL]
              (->>
                  (split pattern #";")
                  (map #(re-find #"(.+)\((.+)\)" (triml %)))
                  (map (fn[[o k v]] [(keyword k) v]))
                  (map (fn[[k v]] (extractParam this k v URL)))
                  ((fn [list] (if-not (some empty? list)
                                (mapcat (fn[e] e) (rest list))
                                nil)))
              )
    )
    (extractParam [this paramType paramPattern testURL]
               (case paramType
                    :host (if (boolean (re-find (re-pattern (str "//" paramPattern "/")) testURL))  [paramType paramPattern] [])
                    (->>
                      (zipmap
                        (->>
                          (re-seq #"\?(.[^\/]+)" paramPattern)
                          (map (fn [[k v]] (keyword v)))
                          (into []))
                        (let [reg (clojure.string/replace paramPattern #"\?(.[^\/]+)"  "[\\/\\&\\?]*(.[^\\/\\?\\&]+)")]
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
