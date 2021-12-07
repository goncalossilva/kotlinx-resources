// Needed due to browser/node JS runtimes having to share the source set (KT-47038).
config.resolve = {
    fallback: {
        fs: false
    }
}
