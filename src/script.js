document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll(".carousel-wrapper").forEach(wrapper => {
        const carousel = wrapper.querySelector(".carousel")
        const scrollContainer = wrapper.querySelector('.thumbnail-scroll-container');

        if (carousel && scrollContainer) {
            carousel.addEventListener('slide.bs.carousel', function (event) {
                // Find the active indicator button matching the next slide index
                const targetIndex = event.to;
                const targetThumb = scrollContainer.querySelector(`[data-bs-slide-to="${targetIndex}"]`);

                if (targetThumb) {
                    // Remove the active class from all thumbnails
                    scrollContainer.querySelectorAll('.thumbnail-btn').forEach(btn => {
                        btn.classList.remove('active');
                    });
                    
                    // Mark the incoming thumbnail as active
                    targetThumb.classList.add('active');

                    // Smoothly center the active thumbnail in the scrollbar wrapper
                    const containerWidth = scrollContainer.clientWidth;
                    const thumbOffset = targetThumb.offsetLeft;
                    const thumbWidth = targetThumb.clientWidth;

                    scrollContainer.scrollTo({
                        left: thumbOffset - (containerWidth / 2) + (thumbWidth / 2),
                        behavior: 'smooth'
                    });
                }
            });
        }
    });
});
