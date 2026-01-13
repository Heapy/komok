package io.heapy.komok.tech.api.dsl.ui

/**
 * Embedded CSS styles for the OpenAPI documentation UI.
 *
 * Includes:
 * - CSS variables for theming (light/dark mode)
 * - Responsive layout with mobile support
 * - HTTP method badges with colors
 * - Clean, modern design
 * - Accessibility features
 */
internal val CSS_STYLES = """
/* ===== CSS Variables for Theming ===== */
:root {
    --bg-primary: #ffffff;
    --bg-secondary: #f8f9fa;
    --bg-tertiary: #e9ecef;
    --text-primary: #212529;
    --text-secondary: #6c757d;
    --text-link: #0d6efd;
    --border-color: #dee2e6;
    --shadow: rgba(0, 0, 0, 0.1);

    /* HTTP Method Colors */
    --method-get: #61affe;
    --method-post: #49cc90;
    --method-put: #fca130;
    --method-delete: #f93e3e;
    --method-patch: #50e3c2;
    --method-head: #9012fe;
    --method-options: #0d5aa7;
    --method-trace: #6b6b6b;

    /* Status Code Colors */
    --status-2xx: #49cc90;
    --status-3xx: #61affe;
    --status-4xx: #fca130;
    --status-5xx: #f93e3e;

    /* Spacing */
    --spacing-xs: 0.25rem;
    --spacing-sm: 0.5rem;
    --spacing-md: 1rem;
    --spacing-lg: 1.5rem;
    --spacing-xl: 2rem;

    /* Typography */
    --font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    --font-family-mono: "SF Mono", Monaco, "Cascadia Code", "Roboto Mono", Consolas, "Courier New", monospace;
    --font-size-base: 16px;
    --font-size-sm: 0.875rem;
    --font-size-lg: 1.125rem;
    --line-height: 1.6;

    /* Layout */
    --sidebar-width: 280px;
    --header-height: 64px;
}

[data-theme="dark"] {
    --bg-primary: #1a1d23;
    --bg-secondary: #242830;
    --bg-tertiary: #2e323a;
    --text-primary: #e9ecef;
    --text-secondary: #adb5bd;
    --text-link: #66b3ff;
    --border-color: #3d4149;
    --shadow: rgba(0, 0, 0, 0.3);
}

/* ===== Base Styles ===== */
* {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
}

html {
    font-size: var(--font-size-base);
}

body {
    font-family: var(--font-family);
    line-height: var(--line-height);
    color: var(--text-primary);
    background-color: var(--bg-primary);
    overflow-x: hidden;
}

/* ===== Layout ===== */
.api-doc-container {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
}

.header {
    position: sticky;
    top: 0;
    z-index: 100;
    background-color: var(--bg-primary);
    border-bottom: 1px solid var(--border-color);
    box-shadow: 0 2px 4px var(--shadow);
}

/* Add scroll margin to all sections and operations to prevent hiding behind sticky header */
.content-section,
.operation {
    scroll-margin-top: calc(var(--header-height) + var(--spacing-md));
}

.header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--spacing-md) var(--spacing-lg);
    max-width: 1400px;
    margin: 0 auto;
}

.header-left {
    display: flex;
    align-items: center;
    gap: var(--spacing-md);
    flex-wrap: wrap;
}

.header-left h1 {
    font-size: 1.5rem;
    font-weight: 600;
    margin: 0;
}

.version-badge {
    display: inline-block;
    padding: var(--spacing-xs) var(--spacing-sm);
    background-color: var(--bg-tertiary);
    color: var(--text-secondary);
    border-radius: 4px;
    font-size: var(--font-size-sm);
    font-weight: 500;
}

.api-summary {
    color: var(--text-secondary);
    font-size: var(--font-size-sm);
    margin: 0;
    flex-basis: 100%;
}

.header-right {
    display: flex;
    gap: var(--spacing-sm);
}

.theme-toggle,
.search-toggle {
    background: none;
    border: 1px solid var(--border-color);
    border-radius: 4px;
    padding: var(--spacing-sm) var(--spacing-md);
    cursor: pointer;
    font-size: 1.2rem;
    transition: all 0.2s;
    color: var(--text-primary);
}

.theme-toggle:hover,
.search-toggle:hover {
    background-color: var(--bg-secondary);
}

.search-container {
    padding: 0 var(--spacing-lg) var(--spacing-md);
}

.search-input {
    width: 100%;
    max-width: 600px;
    padding: var(--spacing-sm) var(--spacing-md);
    border: 1px solid var(--border-color);
    border-radius: 4px;
    font-size: 1rem;
    background-color: var(--bg-secondary);
    color: var(--text-primary);
}

.search-input:focus {
    outline: none;
    border-color: var(--text-link);
}

.main-content {
    display: flex;
    flex: 1;
    max-width: 1400px;
    margin: 0 auto;
    width: 100%;
}

.sidebar {
    width: var(--sidebar-width);
    background-color: var(--bg-secondary);
    border-right: 1px solid var(--border-color);
    padding: var(--spacing-lg);
    overflow-y: auto;
    position: sticky;
    top: var(--header-height);
    height: calc(100vh - var(--header-height));
}

.sidebar-section {
    margin-bottom: var(--spacing-xl);
}

.sidebar-section h3 {
    font-size: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--text-secondary);
    margin-bottom: var(--spacing-md);
    font-weight: 600;
}

.sidebar-section ul {
    list-style: none;
}

.sidebar-section li {
    margin-bottom: var(--spacing-sm);
}

.sidebar-section a {
    display: block;
    padding: var(--spacing-xs) var(--spacing-sm);
    color: var(--text-primary);
    text-decoration: none;
    border-radius: 4px;
    transition: background-color 0.2s;
    font-size: var(--font-size-sm);
}

.sidebar-section a:hover {
    background-color: var(--bg-tertiary);
}

.endpoint-list {
    list-style: none;
}

.endpoint-item a {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
}

.endpoint-path {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.content {
    flex: 1;
    padding: var(--spacing-xl);
    overflow-y: auto;
}

.content-section {
    margin-bottom: var(--spacing-xl);
    padding-bottom: var(--spacing-xl);
    border-bottom: 1px solid var(--border-color);
}

.content-section:last-child {
    border-bottom: none;
}

.content-section h2 {
    font-size: 1.75rem;
    margin-bottom: var(--spacing-lg);
    font-weight: 600;
}

.content-section h3 {
    font-size: 1.25rem;
    margin-bottom: var(--spacing-md);
    font-weight: 600;
}

.content-section h4 {
    font-size: 1rem;
    margin-bottom: var(--spacing-sm);
    font-weight: 600;
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.05em;
}

/* ===== Method Badges ===== */
.method-badge {
    display: inline-block;
    padding: var(--spacing-xs) var(--spacing-sm);
    border-radius: 4px;
    font-size: 0.75rem;
    font-weight: 700;
    text-transform: uppercase;
    color: white;
    letter-spacing: 0.05em;
    min-width: 60px;
    text-align: center;
}

.method-get { background-color: var(--method-get); }
.method-post { background-color: var(--method-post); }
.method-put { background-color: var(--method-put); }
.method-delete { background-color: var(--method-delete); }
.method-patch { background-color: var(--method-patch); }
.method-head { background-color: var(--method-head); }
.method-options { background-color: var(--method-options); }
.method-trace { background-color: var(--method-trace); }

/* ===== Tag Groups ===== */
.tag-group {
    margin-bottom: var(--spacing-xl);
}

.tag-name {
    font-size: 1.5rem;
    margin-bottom: var(--spacing-md);
    padding-bottom: var(--spacing-sm);
    border-bottom: 2px solid var(--border-color);
    color: var(--text-primary);
    font-weight: 600;
    scroll-margin-top: calc(var(--header-height) + var(--spacing-md));
}

.tag-description {
    color: var(--text-secondary);
    margin-bottom: var(--spacing-lg);
    font-size: var(--font-size-sm);
}

.sidebar-tag-group {
    margin-bottom: var(--spacing-md);
}

.sidebar-tag-header {
    display: flex;
    align-items: center;
    gap: var(--spacing-xs);
    margin-bottom: var(--spacing-xs);
}

.sidebar-tag-toggle {
    background: none;
    border: none;
    padding: var(--spacing-xs);
    cursor: pointer;
    color: var(--text-secondary);
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background-color 0.2s, color 0.2s;
    flex-shrink: 0;
}

.sidebar-tag-toggle:hover {
    background-color: var(--bg-tertiary);
    color: var(--text-primary);
}

.toggle-icon {
    font-size: 0.6rem;
    transition: transform 0.2s ease;
    display: inline-block;
}

.sidebar-tag-group.collapsed .toggle-icon {
    transform: rotate(-90deg);
}

.sidebar-tag-group.collapsed .endpoint-list {
    display: none;
}

a.sidebar-tag-name {
    font-weight: 600;
    font-size: 0.85rem;
    text-transform: capitalize;
    color: var(--text-primary);
    text-decoration: none;
    padding: var(--spacing-xs) 0;
    flex: 1;
}

a.sidebar-tag-name:hover {
    color: var(--text-link);
}

/* ===== Operations ===== */
.operation {
    margin-bottom: var(--spacing-lg);
    padding: var(--spacing-lg);
    background-color: var(--bg-secondary);
    border-radius: 8px;
    border: 1px solid var(--border-color);
}

.operation-header {
    display: flex;
    align-items: center;
    gap: var(--spacing-md);
    margin-bottom: var(--spacing-md);
    flex-wrap: wrap;
}

.operation-path {
    font-family: var(--font-family-mono);
    font-size: 1.1rem;
    background-color: var(--bg-tertiary);
    padding: var(--spacing-xs) var(--spacing-sm);
    border-radius: 4px;
}

.operation-summary {
    color: var(--text-secondary);
    font-weight: 500;
}

.operation-description {
    color: var(--text-secondary);
    margin-bottom: var(--spacing-md);
}

.operation-section {
    margin-top: var(--spacing-lg);
}

/* ===== Tables ===== */
.params-table {
    width: 100%;
    border-collapse: collapse;
    background-color: var(--bg-primary);
    border-radius: 4px;
    overflow: hidden;
}

.params-table th {
    background-color: var(--bg-tertiary);
    padding: var(--spacing-sm) var(--spacing-md);
    text-align: left;
    font-weight: 600;
    font-size: var(--font-size-sm);
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.05em;
}

.params-table td {
    padding: var(--spacing-sm) var(--spacing-md);
    border-top: 1px solid var(--border-color);
}

.params-table code {
    font-family: var(--font-family-mono);
    background-color: var(--bg-tertiary);
    padding: 2px 6px;
    border-radius: 3px;
    font-size: 0.9em;
}

.param-in {
    display: inline-block;
    padding: 2px 8px;
    background-color: var(--bg-tertiary);
    border-radius: 3px;
    font-size: var(--font-size-sm);
    font-family: var(--font-family-mono);
}

/* ===== Content Types ===== */
.content-types {
    display: flex;
    gap: var(--spacing-sm);
    flex-wrap: wrap;
    margin-top: var(--spacing-sm);
}

.content-type-badge {
    display: inline-block;
    padding: var(--spacing-xs) var(--spacing-sm);
    background-color: var(--bg-tertiary);
    border-radius: 4px;
    font-size: var(--font-size-sm);
    font-family: var(--font-family-mono);
}

/* ===== Responses ===== */
.responses {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-md);
}

.response-item {
    padding: var(--spacing-md);
    background-color: var(--bg-primary);
    border-radius: 4px;
    border-left: 4px solid var(--text-secondary);
}

.status-code {
    display: inline-block;
    padding: var(--spacing-xs) var(--spacing-sm);
    border-radius: 4px;
    font-weight: 700;
    font-family: var(--font-family-mono);
    color: white;
    margin-right: var(--spacing-sm);
}

.status-2xx { background-color: var(--status-2xx); }
.status-3xx { background-color: var(--status-3xx); }
.status-4xx { background-color: var(--status-4xx); }
.status-5xx { background-color: var(--status-5xx); }

.response-summary {
    font-weight: 500;
}

.response-description {
    margin-top: var(--spacing-sm);
    color: var(--text-secondary);
    font-size: var(--font-size-sm);
}

/* ===== Info Section ===== */
.description {
    margin-bottom: var(--spacing-lg);
    line-height: 1.8;
}

.info-details {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-sm);
}

.info-details p {
    margin: 0;
}

.info-details a {
    color: var(--text-link);
    text-decoration: none;
}

.info-details a:hover {
    text-decoration: underline;
}

/* ===== Servers ===== */
.servers-list {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-md);
}

.server-item {
    padding: var(--spacing-md);
    background-color: var(--bg-secondary);
    border-radius: 4px;
    border-left: 4px solid var(--text-link);
}

.server-url {
    font-family: var(--font-family-mono);
    font-size: 1rem;
    display: block;
    margin-bottom: var(--spacing-sm);
}

.server-description {
    color: var(--text-secondary);
    font-size: var(--font-size-sm);
    margin: 0;
}

/* ===== Schemas ===== */
.schema-item {
    margin-bottom: var(--spacing-lg);
}

.schema-code {
    background-color: var(--bg-secondary);
    padding: var(--spacing-md);
    border-radius: 4px;
    overflow-x: auto;
    font-family: var(--font-family-mono);
    font-size: var(--font-size-sm);
}

.schema-code code {
    color: var(--text-primary);
}

/* ===== Responsive Design ===== */
@media (max-width: 768px) {
    .sidebar {
        display: none;
    }

    .content {
        padding: var(--spacing-md);
    }

    .header-content {
        padding: var(--spacing-md);
    }

    .operation-header {
        flex-direction: column;
        align-items: flex-start;
    }

    .params-table {
        font-size: var(--font-size-sm);
    }
}

/* ===== Accessibility ===== */
@media (prefers-reduced-motion: reduce) {
    * {
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
        transition-duration: 0.01ms !important;
    }
}

:focus-visible {
    outline: 2px solid var(--text-link);
    outline-offset: 2px;
}

/* ===== Utility Classes ===== */
.hidden {
    display: none !important;
}
""".trimIndent()
