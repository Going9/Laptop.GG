document.querySelectorAll('[data-product-image]').forEach((image) => {
    const showPlaceholder = () => {
        if (image.getAttribute('src') === '/image-placeholder.svg') return;
        image.src = '/image-placeholder.svg';
    };
    image.addEventListener('error', showPlaceholder);
    if (image.complete && image.naturalWidth === 0) showPlaceholder();
});
