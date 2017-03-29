(ns httper.service
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(def AUTH_KEY "access_token=4cf2cda658076b1967b069295167566a9269576c02883861ea1ac67a3aa3a107")
;(def testUser "DarumaCreative")
(def testUser "swrwwt")

(defn addToken [link]
  (if (.contains link "access_token=")
  link
  (if (.contains link "?") (str link "&" AUTH_KEY) (str link "?" AUTH_KEY))))

(defn fetch-values-by-path [path bodys]
  (map #(get-in % path) bodys))

(defn getFollowersLink [user]
  (get (json/read-str (get (client/get (str "https://api.dribbble.com/v1/users/" user "?" AUTH_KEY)
            {:insecure? true}
            {:as :json}) :body))
  "followers_url"))

(defn checkLimits [header]
  (let [
        remain (Integer/parseInt (get header "X-RateLimit-Remaining"))
        newQuotaAvailable (java.util.Date. (* (Long/parseLong(get header "X-RateLimit-Reset")) 1000))
        sleepTime  (Math/abs (- (.getTime newQuotaAvailable) (.getTime  (java.util.Date.)))) ]
        (if  (<= remain 3)
           (Thread/sleep sleepTime))))

(defn getValueListByKeys [link result bodyFn & [nextLinkKey bodyKey]]
  (let [response (client/get (addToken link)
                  {:insecure? true}
                  {:as :json})
        nextLinkKey (or nextLinkKey [:links :next :href])
        bodyKey (or bodyKey [:body])]

    (checkLimits (get-in response [:headers]))
    (def nextLink (get-in response nextLinkKey))
    (if (and (not-empty nextLink) (not-empty nextLinkKey))
      (recur nextLink (concat result (bodyFn (json/read-str (get-in response bodyKey)))) bodyFn [nextLinkKey bodyKey])
      (concat result (bodyFn (json/read-str (get-in response bodyKey))))
    )
  ))

 (defn find-all-likers [name]
  (->>
    (getValueListByKeys (getFollowersLink testUser) () (partial fetch-values-by-path ["follower" "shots_url"]))
    (map #(getValueListByKeys % () (partial fetch-values-by-path ["likes_url"])))
    (flatten)
    (map #(getValueListByKeys % () (partial fetch-values-by-path ["user" "username"]))))
  )

(doseq [user (->>
  (find-all-likers testUser)
  (flatten)
  (frequencies)
  (sort-by val >)
  (take 10)
  )]
  (println (key user) (val user)))
