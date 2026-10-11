(require '[ssgr :refer [register-callback!]])
(require '[ssgr.token :as t])
(require '[clojure.string :as str])
(require 'fs)

(declare get-lightbox-modals) ; HACK(Richo): We should support namespaces

(def title "Sofilope")

(defn id->title [id]
  (-> id
      (str/trim)
      (str/replace #"_" " ")
      (str/capitalize)))

(def menu
  {"Home" "index.html"
   "Prices" (->> (fs/list-dir "src/prices")
                 (remove fs/directory?)
                 (sort)
                 (map (fn [file]
                        (let [[_ name _] (str/split (fs/file-name file) #"\.")
                              id (str/trim name)
                              title (id->title id)]
                          [title (str "prices.html#" id)])))
                 (into {}))
   "Terms of service" "tos.html"
   "Contact" "contact.html"
   "About me" "about.html"})

(def socials
  [:ul.nav.col-md-auto.list-unstyled.d-flex
   (->> [{:url "https://www.furaffinity.net/user/sofiamer/"
          :icon [:i.fa-solid.fa-otter.fa-2x.fa-fw]}
         {:url "https://x.com/Sofimercuri"
          :icon [:i.fa-brands.fa-twitter.fa-2x.fa-fw]}
         {:url "https://bsky.app/profile/sofimercuri.bsky.social"
          :icon [:i.fa-brands.fa-bluesky.fa-2x.fa-fw]}
         {:url "https://www.reddit.com/user/sofilope/"
          :icon [:i.fa-brands.fa-reddit.fa-2x.fa-fw]}
         {:url "https://www.deviantart.com/sofmer"
          :icon [:i.fa-brands.fa-deviantart.fa-2x.fa-fw]}
         {:url "https://t.me/sofiamercuri0"
          :icon [:i.fa-brands.fa-telegram.fa-2x.fa-fw]}]
        (map (fn [{:keys [icon url]}]
               [:li ;.mx-1
                [:a.text-body-secondary {:target "_blank" :href url}
                 icon]]))
        (doall))])

(defn navbar-button [file-name [name value]]
  (if (string? value)
    (let [current? (= file-name value)]
      [:li.nav-item.mx-2
       [:a.nav-link
        (cond-> {:href value}
          current? (assoc :aria-current "page"
                          :class "active"))
        name]])
    (let [current? (contains? (set (vals value))
                              file-name)]
      [:li.nav-item.dropdown.mx-2
       [:a.nav-link.dropdown-toggle
        (cond-> {:href "#" :role "button" :data-bs-toggle "dropdown"
                 :aria-expanded "false"}
          current? (assoc :aria-current "page"
                          :class "active"))
        name]
       (apply conj [:ul.dropdown-menu]
              (map (fn [[k v]]
                     [:li
                      [:a.dropdown-item {:href v}
                       k]])
                   value))])))

(defn navbar [file-name]
  [:nav.navbar.navbar-expand-lg.sticky-top.justify-content-center.bg-body-tertiary.border-bottom
   [:div.container
    [:a.navbar-brand {:href "index.html"}
     ; TODO(Richo): Acá podríamos agregar el iconito de sofilope
     "Sofilope"]
    [:button.navbar-toggler
     {:type "button"
      :data-bs-toggle "collapse"
      :data-bs-target "#navbarNavDropdown"
      :aria-controls "navbarNavDropdown"
      :aria-expanded "false"
      :aria-label "Toggle navigation"}
     [:span.navbar-toggler-icon ]]
    [:div#navbarNavDropdown.collapse.navbar-collapse.justify-content-end
     [:div.col.p-2]
     [:div.col-auto socials]
     [:div.col.p-2]
     [:ul.navbar-nav
      (map (partial navbar-button file-name)
           menu)]]]])

(def footer
  [:div.container-fluid.bg-body-tertiary.border-top
   [:div.container
    [:footer.d-flex.flex-wrap.justify-content-between.align-items-center.pt-3.pb-5
     [:div.col-md-4.mb-3.mb-md-0.text-body-secondary
      "©2026 Sofilope"]
     socials]]])

(defn apply-layout [document file-name]
  [:html {:lang "en"}
   [:head
    [:title title]
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/bootstrap@5.3.7/dist/css/bootstrap.min.css"
            :integrity "sha384-LN+7fdVzj6u52u30Kp6M/trliBMCMKTyK833zpbD+pXdCLuTusPj697FH4R/5mcr"
            :crossorigin "anonymous"}]
    [:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.7.2/css/all.min.css"
            :integrity "sha512-Evv84Mr4kqVGRNSgIGL/F/aIDqQb7xQ2vcrdIwxfjThSH8CSR7PBEakCr51Ck+w+/U6swU2Im1vVX0SVk9ABhg=="
            :crossorigin "anonymous" :referrerpolicy "no-referrer"}]
    [:link {:rel "stylesheet" :href "style.css"}]]
   [:body
    (navbar file-name)
    (case file-name
      "index.html" document
      [:div.container document])
    footer
    [:div (get-lightbox-modals document)]
    [:script {:src "https://cdn.jsdelivr.net/npm/bootstrap@5.3.7/dist/js/bootstrap.bundle.min.js"
              :integrity "sha384-ndDqU0Gzau9qJ1lfW4pNLlhNTkCfHzAVBReH9diLvGRem5+R9g2FzA8ZGN954O5Q"
              :crossorigin "anonymous"}]
    [:script {:src "script.js"}]]])

(defn md->html [file-name]
  (-> file-name
      (str/replace #"^src[/\\]" "")
      (str/replace #".(clj)?md$" ".html")))

(defn add-class [html & classes]
  (update html 0 (fn [tag] (keyword (subs (str/join "." (cons tag classes)) 1)))))

(defn replace-tag [html tag]
  (assoc html 0 tag))

(defn set-id [html id]
  (let [attrs (second html)]
    (if (and attrs (map? attrs))
      (assoc-in html [1 :id] id)
      (into (conj (subvec html 0 1) {:id id})
            (subvec html 1)))))

(defn sanitize-id [text]
  (let [id (-> (str/lower-case text)
               (str/replace #"\W+" " ")
               (str/trim)
               (str/replace #"\s" "_"))]
    (if (re-find #"^[a-z]" id)
      id
      (str "a_" id))))

(defn render
  [{:keys [type] :as element} rendered]
  (case type
    :ssgr.doc/emphasis
    (let [char (-> element meta :token t/input-value first)
          tag (if (= \* char)
                :span.text-success
                :span.text-danger)]
      (replace-tag rendered tag))

    :ssgr.doc/heading
    (let [token (-> element meta :token)]
      (-> rendered
          (add-class "pt-2" "my-4")
          (set-id (sanitize-id (t/input-value token)))))

    :ssgr.doc/blockquote
    [:div.alert.alert-dark.mt-4.py-4.mb-5 {:role "alert"}
     rendered]

    :ssgr.doc/document
    (let [file-name (md->html (-> element meta :file))]
      (apply-layout rendered file-name))
    rendered))

(register-callback! render)

(comment

  (defn sanitize-for-id [text]
    (let [text (-> (str/lower-case text)
                   (str/replace #"\W+" " ")
                   (str/trim)
                   (str/replace #"\s" "_"))]
      (if (re-find #"^[a-z]" text)
        text
        (str "a_" text))))

  (str/trim)
  (sanitize-for-id "Your character here")
  (sanitize-for-id "123  you up!!! ")

  (sanitize-for-id "# 123  you up!!! "))