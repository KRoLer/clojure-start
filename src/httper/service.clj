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

(defn getShotLinkForFollower [bodys]
    (map #(get (get % "follower") "shots_url")bodys))

(defn getLikesUrl [bodys]
    (map #(get % "likes_url") bodys))

(defn getLikerName [bodys]
    (map #(get (get % "user") "username") bodys))


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
        (if  (<= remain 5)
           (Thread/sleep sleepTime))))

(defn getValueListByKeys [link result nextLinkKey bodyKey bodyFn]
  (def response (client/get (addToken link)
              {:insecure? true}
              {:as :json}))
    (checkLimits (get-in response [:headers]))
    (def nextLink (get-in response nextLinkKey))
    (if (and (not-empty nextLink) (not-empty nextLinkKey))
      (getValueListByKeys nextLink (concat result (bodyFn (json/read-str (get-in response bodyKey)))) nextLinkKey bodyKey bodyFn)
      (concat result (bodyFn (json/read-str (get-in response bodyKey))))))


(defn getLikers [name]
  (let [ likesUrls
    (let [shotLinks (getValueListByKeys (getFollowersLink testUser) () [:links :next :href] [:body] getShotLinkForFollower)]
       (map #(getValueListByKeys % () [:links :next :href] [:body] getLikesUrl) shotLinks)
    )]
      (map #(getValueListByKeys % () [:links :next :href] [:body] getLikerName)  (flatten likesUrls))))

(defn countName [user, userList]
   (reduce + (map #(if (= user %) 1 0) userList)))

(defn createUsersMap [userList]
  (let [userSet (into #{} userList)]
    (into {} (map #(vector % (countName % userList) ) userSet))))


(def usersListOfList (getLikers testUser))

(def flattenUsersList (flatten usersListOfList))

(doseq [user (take 10 (sort-by val > (createUsersMap flattenUsersList)))] (println (key user) (val user)))
