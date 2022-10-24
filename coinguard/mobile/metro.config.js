/**
 * Metro configuration for React Native
 * https://github.com/facebook/react-native
 *
 * @format
 */

// see https://stackoverflow.com/a/55488446/855160
const defaultAssetExts = require("metro-config/src/defaults/defaults").assetExts;

// see https://facebook.github.io/metro/docs/configuration and default configuration: 
// https://github.com/facebook/metro/blob/main/packages/metro-config/src/defaults/index.js#L79
module.exports = {
  transformer: {
    getTransformOptions: async () => ({
      transform: {
        experimentalImportSupport: false,
        inlineRequires: true,
      },
    }),
  },

  resolver: {
    assetExts: [
      ...defaultAssetExts,
      'edn',
      'cljs'
    ]
  }
};
