if (config.ignoreWarnings) {
    config.ignoreWarnings.push(/Critical dependency: the request of a dependency is an expression/);
} else {
    config.ignoreWarnings = [/Critical dependency: the request of a dependency is an expression/];
}
