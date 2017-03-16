# httper
A Clojure project for calculating Top 10 likers across user's followers

## Usage
There are two variables can be configured:

- AUTH_KEY:  Client Access Token for registered Dribble application
- testUser:  target user (ex. swrwwt. ~3500 likers )

## Warnings
This application was created for tests and learn _Clojure_ purpose only.
- Due to API limits (60 req/min, 10000 req/day) users with a lot of followers can be processed for a long time (counts in hours)
- The parallel requests were avoided by the reason mentioned above.

## Sample response
For user *swrwwt* should be the next input:
```
disbag 39
divanraj 37
damour 31
liamhanda 30
edinabazi 27
Himitiom 27
omninos_abhinav 26
marcushanda 26
FilipProchazka 24
truemarmalade 24
```
