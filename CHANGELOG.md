# [0.7.0](https://github.com/lackary/omnihub/compare/v0.6.0...v0.7.0) (2026-01-07)


### Bug Fixes

* **gallery:** prevent duplicate items during pagination ([29f572e](https://github.com/lackary/omnihub/commit/29f572e34291c30f94882d0cdf3bd3dcd70b94dd))
* **gallery:** support adaptive alignment in PhotoMetadataOverlay ([f977e60](https://github.com/lackary/omnihub/commit/f977e60789e58c92e3b779d44d46760ec418239c))
* **ui:** adjust bottom bar padding handling in PhotoScreen ([ccb5d91](https://github.com/lackary/omnihub/commit/ccb5d9154a57e688ebdb236f7c0aa9960ee93f59))
* **ui:** improve PhotoDetail immersion and system bar handling ([db18fd7](https://github.com/lackary/omnihub/commit/db18fd75712c2c5671753936ff101d73d27df873))


### Features

* **gallery:** enhance photo detail UI with metadata and statistics ([f6acb3c](https://github.com/lackary/omnihub/commit/f6acb3cabd752d8a7bbe2c324bead734a377957e))
* **gallery:** enhance PhotoOverlay UI with animations and gradient layout ([fc2f548](https://github.com/lackary/omnihub/commit/fc2f548adec18f167960d179519dccbf5ac52571))
* **gallery:** implement photo detail screen and shared element transitions ([608ee2f](https://github.com/lackary/omnihub/commit/608ee2f22363457ca9aced4991b2ced7bdca44e6)), closes [hi#resolution](https://github.com/hi/issues/resolution)
* **gallery:** implement relative time display and update photo info ([7dbb467](https://github.com/lackary/omnihub/commit/7dbb46746abe94db5a330a0054a4a73bdc228106))
* **ui:** implement adaptive layout for photo details and rename file ([e5f15cb](https://github.com/lackary/omnihub/commit/e5f15cb6e7ba5df7a223cc5485f1dcfdb68c88a6))

# [0.6.0](https://github.com/lackary/omnihub/compare/v0.5.0...v0.6.0) (2026-01-05)


### Features

* **desktop:** configure initial window state and dimensions ([af49408](https://github.com/lackary/omnihub/commit/af4940892bd2aa5c294a4ac933254e09a45124ba))
* **gallery:** implement image carousel for collection previews ([0333bff](https://github.com/lackary/omnihub/commit/0333bffbb5e597fb67a8d4c629a1a78f86ac871b))

# [0.5.0](https://github.com/lackary/omnihub/compare/v0.4.0...v0.5.0) (2025-12-30)


### Bug Fixes

* **gallery:** correct refresh state check for progress indicator ([439b30a](https://github.com/lackary/omnihub/commit/439b30a4102d90aa15e3a2e3b6307fb270b62a41))
* **ui:** add preview placeholder for AsyncImage in GalleryScreen ([33ee62b](https://github.com/lackary/omnihub/commit/33ee62bc017ebc3f6dadc2efb1c57b27d2a65193))


### Features

* **config:** add Unsplash API access key configuration via BuildKonfig ([3755e06](https://github.com/lackary/omnihub/commit/3755e06ebb7b9d3105ded1e7e6767523746f3ff9))
* **gallery:** add blurhash placeholders and dynamic aspect ratios ([69d6ef6](https://github.com/lackary/omnihub/commit/69d6ef665c042ecfb8455c8c92d206cd78e54c4c))
* **gallery:** enhance GalleryCard UI and unify display models ([6e032fc](https://github.com/lackary/omnihub/commit/6e032fc3eccec17d40b6963126d83f7822927448))
* **gallery:** grid layout for topics and optimized pagination ([54822f2](https://github.com/lackary/omnihub/commit/54822f229993a06b6382f549a861d8a93058034b))
* **gallery:** implement independent refresh/loading states for tabs ([6f9d2ef](https://github.com/lackary/omnihub/commit/6f9d2efee42aa4cb0b65fe5acd54f0f60c2dd7db))
* **gallery:** integrate Unsplash API with pagination and infinite scrolling ([0b254ee](https://github.com/lackary/omnihub/commit/0b254ee32a26f5f954bd411443977f18c635b1a4))
* **gallery:** refactor components and improve desktop refresh UX ([91b2e28](https://github.com/lackary/omnihub/commit/91b2e28b59a4360ef6ea21fafeffb2b5b4c5fdcf))
* **ui:** extract string resources and refine top app bar styling ([0f55a07](https://github.com/lackary/omnihub/commit/0f55a078ba03b350058ea8adad0e1f570e85ecfc))
* **ui:** migrate gallery to adaptive staggered grid layout ([fdfb52a](https://github.com/lackary/omnihub/commit/fdfb52ab587e0a2596e61b29f7fedb6788c5a520))
* **ui:** migrate to NavigationSuiteScaffold for adaptive layout ([e07fca3](https://github.com/lackary/omnihub/commit/e07fca379d2b39edb3cf327fa880414dd1bbc7ba))

# [0.4.0](https://github.com/lackary/omnihub/compare/v0.3.0...v0.4.0) (2025-12-19)


### Bug Fixes

* **ui:** adjust NavHost padding strategy and rename Photos to Gallery ([6596f2a](https://github.com/lackary/omnihub/commit/6596f2aa690a12737bf397dc6960176966b3ce5e))


### Features

* **gallery:** add initial Gallery screen with tabbed layout ([8bcdab7](https://github.com/lackary/omnihub/commit/8bcdab787a7f9331959dc2f61817e6c84b7a9d38))
* **gallery:** implement MVI architecture and integrate Koin and Coil ([c6bd70a](https://github.com/lackary/omnihub/commit/c6bd70a9ba6efe1143c47b0082eee682feb038fa))

# [0.3.0](https://github.com/lackary/omnihub/compare/v0.2.0...v0.3.0) (2025-12-16)


### Features

* **ui:** update VersionFooter to include build number ([d51022f](https://github.com/lackary/omnihub/commit/d51022f5068c4239f93579f77a2a23241f3d7769))
* **ui:** use generated APP_VERSION in AccountScreen ([a3d7f6e](https://github.com/lackary/omnihub/commit/a3d7f6e4303a3af3fded5d453f90b31eabe0cc9a))

# [0.2.0](https://github.com/lackary/omnihub/compare/v0.1.0...v0.2.0) (2025-12-14)


### Features

* **ui:** add JetBrains Compose previews for Home and Account screens ([6cad46f](https://github.com/lackary/omnihub/commit/6cad46f76c0080d98d6551e57113c39d127ab6a4))
* **ui:** implement core navigation and home screen structure ([8d3e90a](https://github.com/lackary/omnihub/commit/8d3e90a4ce06dac64c68b92fad3ff2b96e90e1ac))
* **ui:** implement initial Account screen with version footer ([17d7e90](https://github.com/lackary/omnihub/commit/17d7e9094af0b3a89be56793423e3cc3a6d7c060))
* **ui:** update TopAppBar title on HomeScreen ([c3e43a5](https://github.com/lackary/omnihub/commit/c3e43a526cea6a99e8826cd96ac7efa75bcd58c3))
