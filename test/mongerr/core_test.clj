(ns mongerr.core-test
  (:require [clojure.test :refer :all]
            [mongerr.core :refer :all]))

(deftest test-collection-names
  (testing "db"
    (is (coll? (db)))))

(deftest test-insert-doc
  (testing "can insert and find stuff"
    (is (== 1 (get (first (db-insert :mongerr-tests {:a 1})) "ok")))
    (is (= {:a 1} (select-keys (db-findf :mongerr-tests {:a 1}) [:a])))))

(deftest test-delete-doc
  (testing "can delete stuff"
    (is (number? (.getN (db-remove :mongerr-tests {:a 1}))))
    (is (nil? (db-findf :mongerr-tests {:a 1})))))

(deftest test-drop-collection
  (testing "can drop collection"
    (is (nil? (db-drop :mongerr-tests)))
    (is (empty? (db :mongerr-tests)))))
