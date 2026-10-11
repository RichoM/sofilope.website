(require '[clojure.string :as str])
(require '[clojure.walk :refer [prewalk]])
(require 'ssgr)

(defn add-class [html & classes]
  (update html 0 (fn [tag] (keyword (subs (str/join "." (cons tag classes)) 1)))))


(def lightbox-registry (atom []))

(defn lightbox-modal [id img]
  [:div.modal.fade
   {:id id :tabindex "-1" :aria-hidden "true"}
   [:div.modal-dialog.modal-dialog-centered.modal-lg
    [:div.modal-content.bg-transparent.border-0
     [:div.modal-body.p-0.text-center.position-relative
      [:button.btn-close.btn-close-white.position-absolute.top-0.end-0.m-3
       {:type "button" :data-bs-dismiss "modal" :aria-label "Close"}]
      [:img.img-fluid
       {:src img :alt "Expanded View"
        :loading "lazy"}]]]]])

(defn lightbox [children & {:keys [img]}]
  (if (nil? img)
    children
    (let [{:keys [id]}
          (-> lightbox-registry
              (swap! (fn [lightboxes]
                       (let [id (str "lightbox" (count lightboxes))
                             modal (lightbox-modal id img)]
                         (conj lightboxes
                               {:id id
                                :modal modal}))))
              (peek))]
      (with-meta
        [:a {:href "#" :data-bs-toggle "modal" :data-bs-target (str "#" id)}
         children]
        {:lightbox id}))))

(defn get-lightbox-modals [doc]
  (let [ids (atom #{})]
    (prewalk
     (fn [el]
       (swap! ids conj (get (meta el) :lightbox))
       el)
     doc)
    (->> @lightbox-registry
         (filter (comp @ids :id))
         (map :modal))))


(defn gallery
  ([items] (gallery {} items))
  ([attrs items]
   [:div.gallery attrs items]))

(defn gallery-item [children]
  [:div.gallery-item children])


(defn carousel-slide
  [& {:keys [title subtitle img fullscreen-img alt-text] :as metadata}]
  (with-meta
    [:div.carousel-item
     (lightbox (list [:img.d-block.w-100 {:src img :alt alt-text
                                          :loading "lazy"}]
                     [:div.carousel-caption.text-light.p-4.text-center
                      {:style "filter:drop-shadow(1px 1px 2px #000000);"}
                      [:p.display-4.fw-bold (ssgr/markdown title)]
                      [:p.fs-3 (ssgr/markdown subtitle)]])
               :img (or fullscreen-img img))]

    metadata))

(def carousel-counter (atom 0))

(defn carousel [& slides]
  (let [id (str "#carousel" (swap! carousel-counter inc))]
    [:div.carousel-wrapper
     ; Main carousel
     [(keyword (str "div" id ".carousel.slide.pb-2"))
      {:data-bs-ride "carousel"}
      (apply conj [:div.carousel-indicators]
             (map (fn [idx]
                    [(if (= idx 0)
                       :button.active
                       :button)
                     {:type "button"
                      :data-bs-target id
                      :data-bs-slide-to (str idx)
                      :aria-current (= idx 0)
                      :aria-label (str "Slide " (inc idx))}])
                  (range (count slides))))
      (apply conj [:div.carousel-inner]
             (cons (add-class (first slides) "active")
                   (rest slides)))
      
      ; Prev-Next arrows
      [:button.carousel-control-prev {:type "button"
                                      :data-bs-target id
                                      :data-bs-slide "prev"}
       [:span.carousel-control-prev-icon {:aria-hidden "true"}]
       [:span.visually-hidden "Previous"]]
      [:button.carousel-control-next {:type "button"
                                      :data-bs-target id
                                      :data-bs-slide "next"}
       [:span.carousel-control-next-icon {:aria-hidden "true"}]
       [:span.visually-hidden "Next"]]]
     
     ; Thumbnails
     [:div.thumbnail-scroll-container.overflow-x-auto.text-center
      (->> slides
           (map meta)
           (map-indexed
            (fn [idx {:keys [img alt-text]}]
              [(if (= idx 0)
                 :button.thumbnail-btn.active
                 :button.thumbnail-btn)
               {:type "button"
                :data-bs-target id
                :data-bs-slide-to (str idx)
                :aria-current (= idx 0)
                :aria-label (str "Slide " (inc idx))}
               [:img.img-thumbnail {:src img :alt alt-text
                                    :loading "lazy"}]])))]]))

(defn featurette
  [& {:keys [title subtitle content img]}]
  [:div.container
   [:div.row.featurette
    [:div.col-md-6
     (when title
       [:h2.featurette-heading.fw-normal.lh-1 (ssgr/markdown title)])
     (when subtitle
       [:h2.text-body-secondary (ssgr/markdown subtitle)])
     (when content
       [:p.lead (ssgr/markdown content)])]
    [:div.col-md-6
     [:img.w-100.rounded-3.shadow {:src img}]]]])

(defn columns [& column-data]
  [:div.container.text-center.py-5
   [:div.row.justify-content-center
    (map (fn [{:keys [icon title content]}]
           [:div {:class (str "col-lg-" (int (/ 12 (count column-data))))}
            icon
            [:h2.mt-2.fw-normal (ssgr/markdown title)]
            [:p (ssgr/markdown (str/join "\n\n" content))]])
         column-data)]])

(defn youtube-video [embed-url]
  [:p.video-container
   [:iframe.w-100.h-100
    {:src embed-url
     :frameborder "0"
     :allow
     "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
     :referrerpolicy "strict-origin-when-cross-origin"
     :allowfullscreen true}]])