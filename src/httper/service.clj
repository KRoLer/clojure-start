(ns httper.service
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(def AUTH_KEY "access_token=4cf2cda658076b1967b069295167566a9269576c02883861ea1ac67a3aa3a107")
(def testUser "DarumaCreative")

(defn getFollowersLink [user]
  (get (json/read-str (get (client/get (str "https://api.dribbble.com/v1/users/" user "?" AUTH_KEY)
            {:insecure? true}
            {:as :json}) :body))
  "followers_url"))

(defn addToken [link]
  (if (.contains link "access_token=")
  link
  (if (.contains link "?") (str link "&" AUTH_KEY) (str link "?" AUTH_KEY))))

(defn getShotLinkForFollower [bodys]
    (map (fn [b]
           (get (get b "follower") "shots_url")
           ) bodys))

(defn getFollowersShotLinks [link, shots]
  (def response (client/get (addToken link)
              {:insecure? true}
              {:as :json}))
    (def nextLink (get-in response [:links :next :href]))

    (if (not-empty nextLink)
      (getFollowersShotLinks nextLink (concat shots (getShotLinkForFollower (json/read-str (get-in response [:body])))))
      shots
    ))




(def shots (getFollowersShotLinks "https://api.dribbble.com/v1/users/844597/followers" ()))
shots


