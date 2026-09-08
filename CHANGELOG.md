## Unreleased

### Feat

- **settings**: redesign settings UI, add open-source license sheet, and implement profile overlay navigation
- **settings**: implement settings screen with theme, server, cache, and account management
- **data**: implement app theme preference support and centralized logout cleanup
- **universitice**: implement folder module detail screen with nested navigation and search
- **universitice**: implement open course in browser context menu action
- **ui**: refine zoom controls and implement LRU bitmap caching for PDF rendering
- **data**: implement course image disk caching and preloading
- **data**: implement HTML entity decoding and sanitize course and module text
- **universitice**: implement course hiding functionality with moodle synchronization
- **download**: implement multiplatform file downloader with progress notifications and local file opening
- **ui**: implement scroll-to-top action on top bar title click
- **ui**: implement state restoration across navigation backstack, scroll positions, and overlay components
- **ui**: implement multiplatform PDF viewer overlay with navigation, zoom, and rotation controls
- **media**: implement multiplatform video player overlay for Android and JVM
- **universitice**: implement image viewer overlay and file type detection for course resources
- **auth**: implement Moodle Web SSO login flow with deep link handling and passport validation
- **universitice**: implement secure token storage and optimize UI performance with immutable collections
- **navigation**: implement multiplatform back handler and backstack management
- **ui**: add Material 3 skeleton loading components and update UI loading states
- **cache**: implement file-based cache storage for Android and JVM
- **universitice**: redesign search bar with custom surface and focus states
- **universitice**: handle moodle token expiration and update login UI
- **universitice**: add course detail screen to view course sections and modules
- **search**: add fuzzy search for courses
- **universitice**: add course search functionality with filtering
- **universitice**: add pull-to-refresh support
- **cache**: add caching to reduce load time
- **style**: fix style issue
- **style**: now using real icons
- **login**: make login only when on universitice and also no more on a different screen
- **token**: add token storage and enhanced ui

### Fix

- **login**: only show login screen when realy needed

### Refactor

- **moodle**: add kotlinx-serialization dependency and clean up token checks
- prepare the app for multiple screens
