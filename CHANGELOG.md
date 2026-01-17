# [0.13.0](https://github.com/lackary/omnihub/compare/v0.12.0...v0.13.0) (2026-01-17)


### Bug Fixes

* **auth:** resolve race condition by using reactive deep link handling ([32637de](https://github.com/lackary/omnihub/commit/32637de62532fd25af63a9d95dfe679a96a10def))
* **build:** add fallback for Unsplash secret key in BuildKonfig ([1166992](https://github.com/lackary/omnihub/commit/116699244d01abc304aa83ae7149b97f797fce97))


### Features

* **auth:** implement cross-platform OAuth2 login flow ([030484a](https://github.com/lackary/omnihub/commit/030484a53d1d71682b4157c23c6aecc342d3f06f))
* **auth:** implement static OAuth callback page and deployment workflow ([202ff8e](https://github.com/lackary/omnihub/commit/202ff8e6167523a3f8f1a4468e77e5a8ea9bc577))
* **auth:** implement Unsplash OAuth authentication and deep linking ([c3beb0d](https://github.com/lackary/omnihub/commit/c3beb0d223294629f92bef0601f9947bcb04c632))
* **ios:** implement deep link handling ([8688d02](https://github.com/lackary/omnihub/commit/8688d025c203575af2f2825709dca66cc5ee4fdc))

# [0.12.0](https://github.com/lackary/omnihub/compare/v0.11.0...v0.12.0) (2026-01-13)


### Features

* **gallery:** unify data models and add comprehensive Compose previews ([5ba6488](https://github.com/lackary/omnihub/commit/5ba648852c857ec93cdac06d1e5149b84a0b5359))
* **ui:** implement custom navigation transitions for features ([3f681eb](https://github.com/lackary/omnihub/commit/3f681ebeae2e3a2d7d6419b28df06c98e57af118))
* **ui:** implement Snackbar support and centralized navigation handling ([ab593ec](https://github.com/lackary/omnihub/commit/ab593ecd479611b45c5662393f4233b8156e2f59))

# [0.11.0](https://github.com/lackary/omnihub/compare/v0.10.0...v0.11.0) (2026-01-12)


### Features

* **gallery:** add Unsplash external linking and cover attribution for Topics ([1ff49bc](https://github.com/lackary/omnihub/commit/1ff49bc5123b5421a5c94dd73fa14f0c06562558))
* **gallery:** implement clickable user attribution in Gallery screen ([e5d718b](https://github.com/lackary/omnihub/commit/e5d718b55d7a106cab657d418e61aa3076089feb))
* **gallery:** implement Unsplash attribution and external links ([ffd1814](https://github.com/lackary/omnihub/commit/ffd1814fa877ac735f162b796eded732a247d766))
* **gallery:** implement Unsplash attribution links and centralized utility ([b029a72](https://github.com/lackary/omnihub/commit/b029a72c641879e0d1544084e11ab2ae614a4228))
* **navigation:** implement smart navigation to prevent backstack loops ([b75afe2](https://github.com/lackary/omnihub/commit/b75afe2e4a2b1fc273f0dd49098bcba353c6e018))

# [0.10.0](https://github.com/lackary/omnihub/compare/v0.9.0...v0.10.0) (2026-01-11)


### Features

* **gallery:** enhance photo attribution and metadata display ([bb6ea59](https://github.com/lackary/omnihub/commit/bb6ea59b47c16a9c3ef5f54ecbb90dab970ad3e3))
* **gallery:** implement tabbed layout and paginated content for User Profile ([42f0d24](https://github.com/lackary/omnihub/commit/42f0d24e086c539ecd270f9dbd2e5b14c3a4daf4))
* **gallery:** implement user profile navigation and display real names ([bf3eac9](https://github.com/lackary/omnihub/commit/bf3eac98df09a84c46aeb181363ec7f9ea36e0ed))
* **gallery:** implement user profile screen and navigation ([78ad678](https://github.com/lackary/omnihub/commit/78ad678b449dba8fb5b855f96e98a9114c322376))

# [0.9.0](https://github.com/lackary/omnihub/compare/v0.8.0...v0.9.0) (2026-01-09)


### Features

* **gallery:** adjust TopicScreen grid layout and padding ([6386716](https://github.com/lackary/omnihub/commit/6386716d57ed7c2e24cca76fafef4e149542b961))
* **gallery:** implement Topic detail screen and navigation ([383c680](https://github.com/lackary/omnihub/commit/383c68025a41ee3a0b99f200054010b2352a7cb8))
* **gallery:** update collection screen text and visibility ([66efb01](https://github.com/lackary/omnihub/commit/66efb0171222758c6af13bc04c8a977f8dc44ca2))

# [0.8.0](https://github.com/lackary/omnihub/compare/v0.7.0...v0.8.0) (2026-01-08)


### Features

* **gallery:** implement collection detail screen and navigation ([f0da963](https://github.com/lackary/omnihub/commit/f0da963b3886a5b6dec9b2d46a0820086f8d9805))
* **gallery:** pass collection title in navigation and refine details UI ([5ba8e37](https://github.com/lackary/omnihub/commit/5ba8e37a494ddde27ca455b0e914d25a512b8c58))
* **gallery:** propagate shared transition scopes to collection details ([725a690](https://github.com/lackary/omnihub/commit/725a690cd1bda968579211f3176065e4f7c0a876))

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
