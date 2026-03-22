# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Elysee Events Augsburg** — A static HTML/CSS website for an event catering company. The main website lives in `elysee-events/` (its own git repo). The root `src/Main.java` is an unrelated IntelliJ IDEA placeholder.

## Architecture

All pages are self-contained HTML files with embedded CSS and inline JavaScript (no build step, no bundler, no framework). Shared font definitions are in `css/fonts.css`.

**Pages:** `elysee-events.html` (main/landing), `hochzeit.html` (weddings), `corporate.html` (corporate events), `kantine.html` (canteen/lunch). `index.html` redirects to `elysee-events.html`. Legal pages: `agb.html`, `datenschutz.html`, `impressum.html`.

**Design tokens** are defined as CSS custom properties in each page's `<style>` block (gold `#C9A84C`, dark `#1A1A1A`, light `#FAFAF8`, surface `#F2EFE9`). Fonts: DM Sans + Cormorant Garamond, self-hosted WOFF2 (GDPR-compliant).

**Forms** submit to FormSubmit.co. **Animations** use `[data-reveal]` / `[data-reveal-lr]` attributes with IntersectionObserver.

## Deployment

Static files served via Apache (`.htaccess` handles HTTPS redirect, compression, caching, security headers). An `nginx.conf` snippet is also provided. No CI/CD pipeline exists.

## Development

No build commands. Edit HTML/CSS directly. To preview locally, use any static file server from the `elysee-events/` directory.

## Key Conventions

- All content is in German
- DSGVO/GDPR compliance is critical: fonts must remain self-hosted, no external CDNs for assets
- Each HTML page includes its own Content-Security-Policy meta tag — update CSP when adding external resources
- Legal pages (`agb.html`, `datenschutz.html`, `impressum.html`) are blocked from crawling in `robots.txt`
- Update `sitemap.xml` when adding new public pages
