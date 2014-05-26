(ns link-collective.core
  (:require [weasel.repl :as ws-repl]
            [hasch.core :refer [uuid]]
            [datascript :as d]
            [geschichte.stage :as s]
            [geschichte.sync :refer [client-peer]]
            [konserve.store :refer [new-mem-store]]
            [cljs.core.async :refer [put! chan <! >! alts! timeout close!] :as async]
            [kioo.om :refer [content set-attr do-> substitute listen]]
            [kioo.core :refer [handle-wrapper]]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


(enable-console-print!)

;; fire up repl
#_(do
    (ns weasel.startup)
    (require 'weasel.repl.websocket)
    (cemerick.piggieback/cljs-repl
        :repl-env (weasel.repl.websocket/repl-env
                   :ip "0.0.0.0" :port 17782)))

;; todo
;; - implement data model in repo
;;   - fix realize value
;; - load in templates

(ws-repl/connect "ws://localhost:17782" :verbose true)

(.log js/console "HAIL TO THE LAMBDA!")

(def eval-fn {'(fn replace [old params] params) (fn replace [old params] params)
              '(fn [old params]
                 (merge-with merge old params))
              (fn [old params]
                (merge-with merge old params))})

(def val-ch (chan))


(let [schema {:aka {:db/cardinality :db.cardinality/many}}
      conn   (d/create-conn schema)]
  (:db-after (d/transact @conn [ { :db/id -1
                                  :name  "Maksim"
                                  :age   45
                                  :aka   ["Maks Otto von Stirlitz", "Jack Ryan"] } ]))
  #_(d/q '[ :find  ?n ?a
          :where [?e :aka "Maks Otto von Stirlitz"]
                 [?e :name ?n]
                 [?e :age  ?a] ]
       @conn))



#_(go (def store
        (<! (new-mem-store
             (atom {#uuid "0912a672-6bc2-5297-9ffa-948998517273"
                    {:transactions
                     [[#uuid "2b21fbe0-e8a8-563d-bfba-e4b6022d056f"
                       #uuid "123ed64b-1e25-59fc-8c5b-038636ae6c3d"]],
                     :parents [],
                     :ts #inst "2014-05-24T19:14:16.158-00:00",
                     :author "eve@polyc0l0r.net"},
                    #uuid "123ed64b-1e25-59fc-8c5b-038636ae6c3d"
                    '(fn replace [old params] params),
                    "eve@polyc0l0r.net"
                    {#uuid "1bc987e2-f19e-4f6a-9341-8858ad4d4363"
                     {:description "link-collective discourse.",
                      :schema {:type "http://github.com/ghubber/geschichte", :version 1},
                      :pull-requests {},
                      :causal-order {#uuid "0912a672-6bc2-5297-9ffa-948998517273" []},
                      :public false,
                      :branches
                      {"master" #{#uuid "0912a672-6bc2-5297-9ffa-948998517273"}},
                      :head "master",
                      :last-update #inst "2014-05-24T19:14:16.158-00:00",
                      :id #uuid "1bc987e2-f19e-4f6a-9341-8858ad4d4363"}},
                    #uuid "2b21fbe0-e8a8-563d-bfba-e4b6022d056f"
                    {:posts
                     {#uuid "164564fb-f93b-5863-9ee2-4517a42d0e99"
                      {:title "Spiegel Online",
                       :content "http://spiegel.de #spon",
                       :author "kordano",
                       :ts #inst "2014-05-24T19:14:16.092-00:00"}},
                     :comments
                     {#uuid "098fc269-d8c1-524d-a049-9a8cc52d6268"
                      {:content "this is boring :-/",
                       :author "bob@polyc0l0r.net",
                       :ts #inst "2014-05-24T19:14:16.120-00:00"}},
                     :posts->comments
                     {#uuid "164564fb-f93b-5863-9ee2-4517a42d0e99"
                      #{#uuid "098fc269-d8c1-524d-a049-9a8cc52d6268"}},
                     :hashtags->posts
                     {#uuid "17d34c1b-0535-5a5b-8c2a-2113cae5aca3"
                      #{#uuid "164564fb-f93b-5863-9ee2-4517a42d0e99"}},
                     :posts->votes
                     {#uuid "164564fb-f93b-5863-9ee2-4517a42d0e99"
                      #{#uuid "254995fc-f837-530a-a4fa-a2923dc6db4a"}},
                     :hashtags {#uuid "17d34c1b-0535-5a5b-8c2a-2113cae5aca3" :#spon},
                     :votes
                     {#uuid "254995fc-f837-530a-a4fa-a2923dc6db4a"
                      {:author "bob@polyc0l0r.net", :type :down}}}}))))


      (def peer (client-peer "CLIENT-PEER" store))

      (def stage (<! (s/create-stage! "eve@polyc0l0r.net" peer eval-fn)))

      (async/tap (get-in @stage [:volatile :val-mult]) val-ch))



#_(let [post {:title "Spiegel Online"
            :content "http://spiegel.de #spon"
            :author "kordano"
            :ts (js/Date.)}
      post-id (uuid post)
      comment {:content "this is boring :-/"
               :author "bob@polyc0l0r.net"
               :ts (js/Date.)}
      comment-id (uuid comment)
      vote {:author "bob@polyc0l0r.net" :type :down}
      vote-id (uuid vote)]
  (go (<! (s/create-repo! stage "eve@polyc0l0r.net" "link-collective discourse."
                          {:posts {post-id post}
                           :hashtags {(uuid :#spon) :#spon}
                           :hashtags->posts {(uuid :#spon) #{post-id}}
                           :comments {comment-id comment}
                           :posts->comments {post-id #{comment-id}}
                           :votes {vote-id vote}
                           :posts->votes {post-id #{vote-id}}}
                          "master"))))


(comment
  (s/subscribe-repos! stage
                      {"eve@polyc0l0r.net" {#uuid "1bc987e2-f19e-4f6a-9341-8858ad4d4363"
                                            #{"master"}}})

  (get-in @stage ["eve@polyc0l0r.net" #uuid "1bc987e2-f19e-4f6a-9341-8858ad4d4363"])
  (get-in @stage [:volatile :val
                  "eve@polyc0l0r.net" #uuid "1bc987e2-f19e-4f6a-9341-8858ad4d4363" "master"])
  (get-in @stage [:volatile :val])

  (let [post {:title "Netzwertig"
              :content "http://netzwertig.de"
              :ts (js/Date.)}
        post-id (uuid post)]
    (go (<! (s/transact stage ["eve@polyc0l0r.net"
                               #uuid "1bc987e2-f19e-4f6a-9341-8858ad4d4363"
                               "master"]
                        {:posts {post-id post}}
                        '(fn [old params]
                           (merge-with merge old params))))))

  (go (<! (s/commit! stage {"eve@polyc0l0r.net" {#uuid "1bc987e2-f19e-4f6a-9341-8858ad4d4363"
                                                 #{"master"}}}))))
