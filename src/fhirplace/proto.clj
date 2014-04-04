(ns fhirplace.proto)


(def request-mw [h & opts]
  (fn [req]
    (h (modify req))))

(req-mw [req]
  (assoc req :data "here"))

(def responce-mw [h & opts]
  (fn [req]
    (let [res (h req)]
      req)))

(res-mw [res]
  (assoc res :data "here"))

(def interceptor-mw [h & opts]
  (fn [req]
    (if true
      (h req)
      {:status 500 :body "ups"})))

(def middle-ware [h & opts]
  (fn [req]
    ;; anything
    (h req)
    ;; anything
    ))
