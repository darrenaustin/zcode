;(clojure/in-ns 'zmachine)
;(clojure/refer 'clojure)
;(clojure/refer 'util)
;(clojure/refer 'zstory)
;(clojure/refer 'zmem)

;; Frame Stack
(defn current-frame []
  (first @(current-story :frames)))

(defn frame-pop []
  (let [frame (first @(current-story :frames))]
	(when-not frame
	  (error "Frame stack underflow"))
	(dosync (alter (current-story :frames) rest))
	frame))

(defn frame-push [frame]
  (dosync (alter (current-story :frames) conj frame)))

;; Stack
(defn stack-pop []
  (let [value (first @((current-frame) :stack))]
	(when-not value
	  (error "Game stack underflow"))
	(dosync (alter ((current-frame) :stack) rest))
	value))

(defn stack-push [value]
  (dosync (alter ((current-frame) :stack) conj value)))

;; PC
(defn pc []
  @((current-frame) :pc))

(defn pc-ref []
  ((current-frame) :pc))

(defn set-pc [addr]
  (dosync (ref-set ((current-frame) :pc) addr)))

(defn inc-pc
  ([] (set-pc (inc (pc))))
  ([x] (set-pc (+ (pc) x))))

(defn next-byte []
  (let [byte (zbyte (pc))]
	(inc-pc)
	byte))

(defn next-word []
  (let [word (zword (pc))]
	(inc-pc 2)
	word))

;; Breakpoints
(defn at-breakpoint? [addr]
  (contains? @(:breakpoints current-story) addr))

(defn set-breakpoint [addr]
  (dosync (commute (:breakpoints current-story) conj addr)))

(defn clear-breakpoint [addr]
  (dosync (commute (:breakpoints current-story) disj addr)))

(defn clear-breakpoints []
  (dosync (ref-set (:breakpoints current-story) #{})))

;; Global variables
(defn get-global-variable [var]
  (let [table (zword header-global-variable-table)]
	(zword (+ table (* var 2)))))

(defn set-global-variable [var value]
  (let [table (zword header-global-variable-table)]
	(set-zword (+ table (* var 2)) value)))

;; Local variables
(defn num-locals []
  (count @((current-frame) :locals)))

(defn local [n]
  (@((current-frame) :locals) n))

(defn set-local [n value]
  (dosync (alter ((current-frame) :locals) assoc n value)))

(defn get-locals []
  @((current-frame) :locals))

(defn set-locals [locals]
  (dosync (ref-set ((current-frame) :locals) locals)))

(defmulti read-locals version)
(defmethod-for-values read-locals '(1 2 3 4) [num-locals]
  (set-locals 
   (loop [locals []
		  num num-locals]
	 (if (> num 0)
	   (recur (conj locals (next-word)) (dec num))
	   locals))))
(defmethod read-locals :default [num-locals]
  (set-locals (if (zero? num-locals)
				nil 
				(vec (replicate num-locals 0)))))

;; Variables
(defn get-variable [var]
  (cond (= var 0x0) (stack-pop)
		(<= 0x1 var 0xf) (local (dec var))
		:else (get-global-variable (- var 0x10))))

(defn set-variable [var value]
  (cond (= var 0x0) (stack-push value)
		(<= 0x1 var 0xf) (set-local (dec var) value)
		:else (set-global-variable (- var 0x10) value)))

(defn inc-variable [var]
  (set-variable var (inc (signed-word (get-variable var)))))

(defn dec-variable [var]
  (set-variable var (dec (signed-word (get-variable var)))))

;; Initial frame 
(defmulti push-initial-frame version)
(defmethod push-initial-frame :default []
  (let [start-pc (zword header-main-address)]
	(frame-push (create-frame start-pc))))
(defmethod push-initial-frame 6 []
  (let [start-pc (routine-address (zword header-main-address))
		num-locals (zbyte start-pc)]
	(frame-push (create-frame (inc start-pc)))
	(read-locals num-locals)))

(defn merge-locals-with-operands [locals operands]
  (let [lc (count locals)
		oc (count operands)]
	(vec (if (<= lc oc) 
		   (take lc operands)
		   (concat operands (subvec locals oc))))))

;; Routine calls
(defn call-routine [routine-address operands result]
  (cond (zero? routine-address) (when result (set-variable result 0))
		:else (dosync 
			   (ref-set ((current-frame) :result-store) result)
			   (frame-push (create-frame routine-address))
			   (read-locals (next-byte))
			   (set-locals (merge-locals-with-operands (get-locals) operands))
			   )))

(defn routine-return [result]
  (dosync
   (when (= (count @(current-story :frames)) 1)
	 (error "Illegal return from main routine"))
   (frame-pop)
   (let [result-store @((current-frame) :result-store)]
	 (when result-store
	   (set-variable result-store result)))))

(defn return-true [] (routine-return 1))
(defn return-false [] (routine-return 0))

(defn jump-if [value branch-info]
  (if (= value (first branch-info))
	(let [address (second branch-info)]
	  (cond (= address :ret-true) (return-true)
			(= address :ret-false) (return-false)
			:else (set-pc address)))))
