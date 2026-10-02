const {
  withAndroidManifest,
  withDangerousMod,
} = require('@expo/config-plugins');

const fs = require('fs');
const path = require('path');

module.exports = function withCleartextTraffic(config) {
  // Add the Android manifest attributes
  config = withAndroidManifest(config, (config) => {
    const application = config.modResults.manifest.application?.[0];

    if (application) {
      application.$['android:usesCleartextTraffic'] = 'true';
      application.$['android:networkSecurityConfig'] =
        '@xml/network_security_config';
    }

    return config;
  });

  // Create Android network security configuration
  config = withDangerousMod(config, [
    'android',
    async (config) => {
      const projectRoot = config.modRequest.platformProjectRoot;

      const xmlDirectory = path.join(
        projectRoot,
        'app',
        'src',
        'main',
        'res',
        'xml'
      );

      fs.mkdirSync(xmlDirectory, { recursive: true });

      const networkSecurityConfig = `<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true" />
</network-security-config>
`;

      fs.writeFileSync(
        path.join(xmlDirectory, 'network_security_config.xml'),
        networkSecurityConfig
      );

      return config;
    },
  ]);

  return config;
};