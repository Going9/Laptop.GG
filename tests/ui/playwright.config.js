const path = require('node:path');
const root = path.resolve(__dirname, '../..');
const { defineConfig } = require('@playwright/test');
const full = process.env.UI_FULL === '1';
const sizes = full ? [[360,800],[375,812],[390,844],[430,932],[768,1024],[1440,900]] : [[360,800],[390,844],[1440,900]];
module.exports = defineConfig({
  testDir: __dirname, testMatch: '*.spec.js', fullyParallel: true, workers: 4,
  timeout: 45000, expect: { timeout: 7000 }, retries: 0,
  updateSnapshots: 'none',
  snapshotPathTemplate: '{testDir}/baselines/{projectName}/{arg}{ext}',
  reporter: [['list'], ['html', { outputFolder: path.join(root, 'output/ui/report'), open: 'never' }], ['json', { outputFile: path.join(root, 'output/ui/results.json') }]],
  outputDir: path.join(root, 'output/ui/results'),
  use: { baseURL: 'http://127.0.0.1:18080', locale: 'ko-KR', timezoneId: 'Asia/Seoul',
    colorScheme: 'light', reducedMotion: 'reduce', trace: 'retain-on-failure', screenshot: 'only-on-failure' },
  projects: (full ? ['chromium','webkit'] : ['chromium']).flatMap(browserName => sizes.map(([width,height]) => ({
    name: `${browserName}-${width}`, use: { browserName, viewport: {width,height} }
  }))),
  webServer: { command: 'node tests/ui/server.mjs', cwd: root, url: 'http://127.0.0.1:18080/actuator/health/readiness',
    timeout: 180000, reuseExistingServer: false, gracefulShutdown: {signal: 'SIGTERM', timeout: 10000} },
});
