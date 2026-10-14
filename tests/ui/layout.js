const { expect } = require('@playwright/test');

async function ready(page) {
  await expect(page.locator('h1')).toBeVisible();
  await page.evaluate(() => document.fonts.ready);
  expect(await page.evaluate(() => document.fonts.check('16px "LaptopGG Sans"', '노트북'))).toBe(true);
  for (const image of await page.locator('[data-product-image]').all()) {
    await image.scrollIntoViewIfNeeded();
    await expect.poll(() => image.evaluate(node => node.complete && node.naturalWidth > 0)).toBe(true);
  }
}

async function layout(page) {
  await ready(page);
  const overflow = await page.evaluate(() => ({ viewport: innerWidth, width: document.documentElement.scrollWidth }));
  expect(overflow.width, JSON.stringify(overflow)).toBeLessThanOrEqual(overflow.viewport);
  const collisions = await page.evaluate(() => {
    const groups = ['.results-stream > .laptop-tile', '.metric-band > .metric-card', '.usecase-card-grid > label',
      '.mode-chip-grid > label', '.size-chip-grid > label', '.sort-pills > form', '.preset-dropdown-pills > button',
      '.pagination-shell > .page-chip, .pagination-shell > .page-chip-form', '.input-grid > .field-block', '.tile-specs > .tile-spec', '.action-row > *', '.detail-actions > *', '.comment-form-grid > label'];
    return groups.flatMap(selector => {
      const nodes = [...document.querySelectorAll(selector)].filter(node => node.getClientRects().length);
      const failures = [];
      for (let i = 0; i < nodes.length; i++) for (let j = i + 1; j < nodes.length; j++) {
        if (nodes[i].parentElement !== nodes[j].parentElement) continue;
        const a = nodes[i].getBoundingClientRect(), b = nodes[j].getBoundingClientRect();
        if (Math.min(a.right,b.right) - Math.max(a.left,b.left) > 1 && Math.min(a.bottom,b.bottom) - Math.max(a.top,b.top) > 1)
          failures.push({selector, a: a.toJSON(), b: b.toJSON()});
      }
      return failures;
    });
  });
  expect(collisions, 'Sibling cards/controls overlap').toEqual([]);
  const clipped = await page.locator('h1, .tile-title, .tile-price, .tile-value, .metric-value, .spec-desc, .reason-pill, button, .primary-button, .secondary-button').evaluateAll(nodes =>
    nodes.filter(node => node.getClientRects().length && getComputedStyle(node).display !== 'inline' &&
      (node.scrollWidth > node.clientWidth + 1 || node.scrollHeight > node.clientHeight + 1))
      .map(node => ({text: node.textContent, width: node.clientWidth, scrollWidth: node.scrollWidth, height: node.clientHeight, scrollHeight: node.scrollHeight})));
  expect(clipped, 'Text/control clipping').toEqual([]);
  for (const image of await page.locator('[data-product-image]').all()) {
    await expect(image).toHaveJSProperty('complete', true);
    expect(await image.evaluate(node => node.naturalWidth)).toBeGreaterThan(0);
    const box = await image.boundingBox();
    expect(box.width / box.height).toBeCloseTo(1.6, 1);
    expect(await image.evaluate(node => getComputedStyle(node).objectFit)).toBe('contain');
  }
  // Hit-test named navigation/actions, not every DOM node or intentional label/input nesting.
  for (const control of await page.locator('button:not(:disabled), a.primary-button, a.secondary-button, a.ghost-button, a.tile-title, .header-action, summary').all()) {
    if (!(await control.isVisible())) continue;
    await control.scrollIntoViewIfNeeded();
    const hit = await control.evaluate(node => {
      const rect = node.getBoundingClientRect();
      const target = document.elementFromPoint(rect.x + rect.width/2, rect.y + rect.height/2);
      return {visible: node === target || node.contains(target), height: rect.height, text: node.textContent};
    });
    expect(hit.visible, `Covered control: ${hit.text}`).toBe(true);
    // Product titles are text within a card; standalone actions must meet the touch target.
    if (!(await control.evaluate(node => node.classList.contains('tile-title')))) expect(hit.height).toBeGreaterThanOrEqual(44);
  }
  await page.evaluate(() => window.scrollTo(0,0));
}

async function visual(page, testInfo, name) {
  await page.mouse.move(0, 0);
  if (process.env.UI_CAPTURE === '1') {
    const path = require('node:path');
    const fs = require('node:fs');
    const folder = path.resolve('output/ui/candidates', testInfo.project.name);
    fs.mkdirSync(folder, {recursive: true});
    await page.screenshot({path: path.join(folder, `${name}.png`), fullPage: true, animations: 'disabled'});
  }
  if (process.env.UI_FULL === '1' && process.env.UI_LAYOUT_ONLY !== '1')
    await expect(page).toHaveScreenshot(`${name}.png`, {fullPage: true, animations: 'disabled'});
}

async function submit(page, values = {}) {
  await page.goto('/recommends');
  if (values.budget) await page.locator('#budget').fill(String(values.budget));
  if (values.weight) await page.locator('#weight').fill(String(values.weight));
  await page.getByRole('button', {name: '추천받기', exact: true}).click();
  await expect(page).toHaveTitle('추천된 노트북 목록');
}
module.exports = {layout, visual, submit};
