package io.heapy.komok.tech.api.dsl.ui

/**
 * Embedded JavaScript code for interactive features.
 *
 * Uses vanilla JavaScript for lightweight performance.
 * Features include:
 * - Theme toggle (light/dark mode)
 * - Search functionality for endpoints
 * - Smooth scrolling for navigation
 * - Deep linking support
 * - Local storage for theme persistence
 */
internal val JAVASCRIPT_CODE = """
// ===== Theme Toggle =====
(function initTheme() {
    const themeToggle = document.getElementById('theme-toggle');
    const body = document.body;
    const themeIcon = themeToggle.querySelector('.theme-icon');

    // Load saved theme from localStorage
    const savedTheme = localStorage.getItem('theme') || 'light';
    body.setAttribute('data-theme', savedTheme);
    updateThemeIcon(savedTheme);

    themeToggle.addEventListener('click', () => {
        const currentTheme = body.getAttribute('data-theme');
        const newTheme = currentTheme === 'light' ? 'dark' : 'light';

        body.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
        updateThemeIcon(newTheme);
    });

    function updateThemeIcon(theme) {
        themeIcon.textContent = theme === 'light' ? '🌙' : '☀️';
    }
})();

// ===== Search Functionality =====
(function initSearch() {
    const searchToggle = document.getElementById('search-toggle');
    const searchContainer = document.getElementById('search-container');
    const searchInput = document.getElementById('search-input');
    let searchVisible = false;

    searchToggle.addEventListener('click', () => {
        searchVisible = !searchVisible;
        searchContainer.style.display = searchVisible ? 'block' : 'none';

        if (searchVisible) {
            searchInput.focus();
        }
    });

    // Search implementation
    searchInput.addEventListener('input', (e) => {
        const query = e.target.value.toLowerCase();
        const operations = document.querySelectorAll('.operation');
        const endpointItems = document.querySelectorAll('.endpoint-item');

        // Filter operations in main content
        operations.forEach(operation => {
            const text = operation.textContent.toLowerCase();
            const matches = text.includes(query);
            operation.style.display = matches ? 'block' : 'none';
        });

        // Filter sidebar items
        endpointItems.forEach(item => {
            const text = item.textContent.toLowerCase();
            const matches = text.includes(query);
            item.style.display = matches ? 'block' : 'none';
        });
    });

    // Close search on Escape
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && searchVisible) {
            searchVisible = false;
            searchContainer.style.display = 'none';
            searchInput.value = '';

            // Reset visibility
            document.querySelectorAll('.operation, .endpoint-item').forEach(el => {
                el.style.display = '';
            });
        }
    });
})();

// ===== Smooth Scrolling =====
(function initSmoothScrolling() {
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const targetId = this.getAttribute('href');

            if (targetId === '#') return;

            const targetElement = document.querySelector(targetId);
            if (targetElement) {
                targetElement.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });

                // Update URL without jumping
                history.pushState(null, null, targetId);

                // Highlight active link
                document.querySelectorAll('.sidebar a').forEach(link => {
                    link.style.backgroundColor = '';
                });
                this.style.backgroundColor = 'var(--bg-tertiary)';
            }
        });
    });
})();

// ===== Deep Linking =====
(function initDeepLinking() {
    // Handle initial hash on page load
    if (window.location.hash) {
        setTimeout(() => {
            const targetElement = document.querySelector(window.location.hash);
            if (targetElement) {
                targetElement.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        }, 100);
    }

    // Highlight active section on scroll
    const observerOptions = {
        root: null,
        rootMargin: '-20% 0px -70% 0px',
        threshold: 0
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const id = entry.target.id;
                if (id) {
                    // Update active link in sidebar
                    document.querySelectorAll('.sidebar a').forEach(link => {
                        const href = link.getAttribute('href');
                        if (href === '#' + id) {
                            link.style.backgroundColor = 'var(--bg-tertiary)';
                        } else {
                            link.style.backgroundColor = '';
                        }
                    });
                }
            }
        });
    }, observerOptions);

    // Observe all sections and operations
    document.querySelectorAll('.content-section, .operation').forEach(section => {
        if (section.id) {
            observer.observe(section);
        }
    });
})();

// ===== Keyboard Navigation =====
(function initKeyboardNav() {
    document.addEventListener('keydown', (e) => {
        // Ctrl/Cmd + K to open search
        if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
            e.preventDefault();
            const searchToggle = document.getElementById('search-toggle');
            searchToggle.click();
        }

        // Ctrl/Cmd + D to toggle theme
        if ((e.ctrlKey || e.metaKey) && e.key === 'd') {
            e.preventDefault();
            const themeToggle = document.getElementById('theme-toggle');
            themeToggle.click();
        }
    });
})();

// ===== Performance Monitoring =====
(function logPerformance() {
    if (window.performance && window.performance.timing) {
        window.addEventListener('load', () => {
            setTimeout(() => {
                const perfData = window.performance.timing;
                const pageLoadTime = perfData.loadEventEnd - perfData.navigationStart;
                const connectTime = perfData.responseEnd - perfData.requestStart;
                const renderTime = perfData.domComplete - perfData.domLoading;

                console.log('OpenAPI UI Performance:');
                console.log('  Total Load Time:', pageLoadTime + 'ms');
                console.log('  Network Time:', connectTime + 'ms');
                console.log('  Render Time:', renderTime + 'ms');
            }, 0);
        });
    }
})();

console.log('OpenAPI Documentation UI initialized');
""".trimIndent()
