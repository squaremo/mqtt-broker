(ns mqtt-broker.test.subs
  (:require [mqtt-broker.subs :as subs]
            [clojure.test :as test]))

(test/deftest empty-is-empty
  (test/is (subs/empty? (subs/empty))))

(test/deftest empty-matches-nothing
  (test/is (empty? (subs/matches (subs/empty) []))))

(test/deftest match-inserted
  (test/is (= #{1} (let [topic ["foo" "bar"]
                         withsub (subs/insert (subs/empty) topic 1)]
                     (subs/matches withsub topic)))))

(test/deftest removed-is-empty
  (test/is (let [topic ["foo" "bar"]
                 withsub (subs/insert (subs/empty) topic 1)
                 withoutsub (subs/remove withsub topic 1)]
             (subs/empty? withoutsub))))

(test/deftest no-matches-once-removed
  (test/is (empty? (let [topic1 ["foo" "bar"]
                         topic2 ["foo" "bar" "baz"]
                         withsub1 (subs/insert (subs/empty) topic1 1)
                         withsub2 (subs/insert withsub1 topic2 2)
                         without1 (subs/remove withsub2 topic1 1)]
                     (subs/matches without1 topic1)))))

(test/deftest matches-unremoved
  (test/is (= #{2} (let [topic1 ["foo" "bar"]
                         topic2 ["foo" "bar" "baz"]
                         withsub1 (subs/insert (subs/empty) topic1 1)
                         withsub2 (subs/insert withsub1 topic2 2)
                         without1 (subs/remove withsub2 topic1 1)]
                     (subs/matches without1 topic2)))))
