// start.js

document.addEventListener('DOMContentLoaded', function() {
    const bounceButton = document.getElementById('bounce-button');
    const slideUpContent = document.getElementById('slide-up-content');
    const slideUpInnerContent = document.getElementById('slide-up-inner-content');

    console.log('Start.js: Document is ready');

    if (bounceButton && slideUpContent && slideUpInnerContent) {
        console.log('Start.js: Elements found:', bounceButton, slideUpContent, slideUpInnerContent);

        bounceButton.addEventListener('click', function(event) {
            console.log('Start button clicked');
            fetch('/static/templates/start.html')
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Network response was not ok');
                    }
                    return response.text();
                })
                .then(data => {
                    console.log('Data fetched:', data);
                    slideUpInnerContent.innerHTML = data;
                    slideUpContent.classList.add('active');
                    // 메뉴바 닫기
                    const menuContainer = document.querySelector('.menu-container');
                    if (menuContainer) {
                        menuContainer.classList.remove('active');
                    }
                })
                .catch(error => console.error('Error loading start.html:', error));
            event.stopPropagation();
        });

        document.body.addEventListener('click', function(event) {
            if (slideUpContent.classList.contains('active')) {
                console.log('Body clicked');
                slideUpContent.classList.remove('active');
            }
        });

        slideUpContent.addEventListener('click', function(event) {
            event.stopPropagation();
        });
    } else {
        console.error('Start.js: Button, slide-up content, or inner content not found');
    }
});