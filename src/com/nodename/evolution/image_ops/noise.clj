(ns com.nodename.evolution.image_ops.noise
  (:import (java.awt.image BufferedImage))
  (:use clojure.contrib.math
        perlin.core))

(defn next-random-number
  [random-number]
  (rem (* random-number 16807) 2147483647))

(defn powers
  "Return a vector of length _length_ containing p-to-the-zeroth, p, p squared, etc"
    ([length]
      (powers 2 length))
    ([p length]
  (loop [result []]
    (let [i (count result)]
      (cond
        (== i length) result
        :else (recur (conj result (expt p i))))))))

(defn noise-pixel-octave-contrib-generator [x y z persistences freqs]
  (fn [octave]
  (let [ frequency (freqs octave)
        persistence (persistences octave)]
    (* persistence (noise (* frequency x) (* frequency y) (* frequency z))))))
       
(defn noise-pixel-sum [x y z octaves-count persistences freqs]
  (let [noise-pixel-octave-contrib (noise-pixel-octave-contrib-generator x y z persistences freqs)]
  (loop [octave 0
         sum 0]
    (cond
      (== octave octaves-count) sum
      :else (recur (inc octave) (+ sum (noise-pixel-octave-contrib octave)))))))
    
(defn noise-pixel-setter [y z py octaves-count persistences total-persistence freqs]
   (fn [x px bi]
   (let [ sum (noise-pixel-sum x y z octaves-count persistences freqs)
         grey-level (int (* 128 (+ 1 (/ sum total-persistence))))
         argb-color (bit-or 0xff000000 (bit-or (bit-shift-left grey-level 16) (bit-or (bit-shift-left grey-level 8) grey-level)))
         dummy (.setRGB bi px py argb-color)]
     bi)))
 
(defn noise-row-setter [width base-x z octaves-count persistences total-persistence freqs base-factor]
     (fn [y py bi]
       (let [set-noise-pixel (noise-pixel-setter y z py octaves-count persistences total-persistence freqs)]
       (loop [x base-x
              px 0
              image bi]
         (cond
           (== px width) image
           :else (recur (+ x base-factor) (inc px) (set-noise-pixel x px image)))))))

(defn bw-noise
	"Create a constant-z slice of 3D Perlin-noise texture. Parameters: seed; octaves-count; falloff; width, height of image; x, y, z: location of upper right corner of image in noise space"
	([seed octaves-count falloff width height]
 (bw-noise seed octaves-count falloff width height 0 0 0))
	([seed octaves-count falloff width height origin-x origin-y origin-z]
	(let [ bi (BufferedImage. width height BufferedImage/TYPE_INT_ARGB)
       scale 1
       base-factor (/ scale 64)
       x-offset (next-random-number seed)
       y-offset (next-random-number x-offset)
       z-offset (next-random-number y-offset)
       persistences (powers falloff octaves-count)
       freqs (powers octaves-count)
       total-persistence (reduce + persistences)
       base-x (+ (* origin-x base-factor) x-offset)
       z (+ (* origin-z base-factor) z-offset)
       set-noise-row (noise-row-setter width base-x z octaves-count persistences total-persistence freqs base-factor)]
   (loop [y (+ (* origin-y base-factor) y-offset)
          py 0
          image bi]
     (cond
       (== py height) image
       :else (recur (+ y base-factor) (inc py) (set-noise-row y py image)))))))

	
