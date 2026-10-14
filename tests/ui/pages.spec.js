const { test, expect } = require('@playwright/test');
const { layout, visual, submit } = require('./layout');

test.beforeEach(async ({context}) => {
  // Every page is real server-rendered HTML. Block unplanned external browser traffic.
  await context.route('**/*', route => new URL(route.request().url()).hostname === '127.0.0.1' ? route.continue() : route.abort());
});

test('form: input, presets, selections, validation, keyboard and submit', async ({page}, info) => {
  await page.goto('/recommends');
  // Deployment must not combine new HTML with a previous CDN-cached stylesheet or script.
  for (const [selector, pattern] of [
    ['link[rel="stylesheet"]', /^\/styles-[a-f0-9]{32}\.css$/],
    ['script[src^="/ui-"]', /^\/ui-[a-f0-9]{32}\.js$/],
    ['script[src^="/recommendation-form-"]', /^\/recommendation-form-[a-f0-9]{32}\.js$/],
  ]) await expect(page.locator(selector)).toHaveAttribute(selector.startsWith('link') ? 'href' : 'src', pattern);
  const font = await page.locator('link[as="font"]').getAttribute('href');
  expect(font).toMatch(/^\/fonts\/laptopgg-sans-[a-f0-9]{32}\.woff2$/);
  const css = await page.request.get(await page.locator('link[rel="stylesheet"]').getAttribute('href'));
  expect(css.status()).toBe(200);
  expect(await css.text()).toContain(font);
  expect((await page.request.get(font)).status()).toBe(200);
  await layout(page);
  await visual(page, info, 'form');
  await page.keyboard.press('Tab');
  await expect(page.getByRole('link', {name:'본문으로 건너뛰기'})).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page.locator('#main-content')).toBeFocused();
  await page.getByLabel('예산 원', {exact:true}).fill('1234567');
  await expect(page.locator('#budgetToggleValue')).toContainText('1,234,567원');
  await page.locator('#budgetPresetMenu button[data-value="1500000"]').click();
  await expect(page.locator('#budget')).toHaveValue('1500000');
  await expect(page.locator('#budgetPresetMenu button[data-value="1500000"]')).toHaveAttribute('aria-pressed','true');
  await page.locator('#weightPresetMenu button[data-value="1.5"]').click();
  await expect(page.locator('#weight')).toHaveValue('1.5');
  await expect(page.locator('.size-chip input').first()).toBeDisabled();
  await page.locator('#screenSizeMode_SELECT').check();
  await page.locator('#screenSize_14').check();
  await page.locator('#screenSize_16').check();
  await expect(page.locator('#screenSizeSelectionSummary')).toHaveText('선택한 화면 크기: 14형, 16형');
  await page.locator('#useCase_OFFICE_STUDY').focus();
  await page.keyboard.press('Space');
  await expect(page.locator('#useCase_OFFICE_STUDY')).toBeChecked();
  await expect(page.locator('#useCase_OFFICE_STUDY')).toBeFocused();
  expect(await page.locator('#useCase_OFFICE_STUDY').evaluate(node => getComputedStyle(node).outlineStyle)).not.toBe('none');
  await page.locator('#budget').fill('');
  await page.getByRole('button',{name:'추천받기',exact:true}).click();
  await expect(page).toHaveTitle('LaptopGG · 노트북 추천');
  expect(await page.locator('#budget').evaluate(node => node.validity.valueMissing)).toBe(true);
  await expect(page.locator('#budget')).toBeFocused();
  // Native validation intentionally overlays adjacent controls; dismiss it before hit-testing them.
  await page.keyboard.press('Escape');
  await page.keyboard.press('Tab');
  await expect(page.locator('#budgetPresetMenu button').first()).toBeFocused();
  await layout(page);
  await visual(page, info, 'form-validation');
  await page.locator('#budget').fill('10000001');
  expect(await page.locator('#budget').evaluate(node => node.validity.rangeOverflow)).toBe(true);
  await page.locator('#budget').fill('1500000');
  await page.getByRole('button',{name:'추천받기',exact:true}).click();
  await expect(page.locator('.laptop-tile')).toHaveCount(10);
  await expect(page.locator('.criteria-strip')).toContainText('화면 14형 · 16형');
  await expect(page.locator('.criteria-strip')).toContainText('문서');
});

test('results: sorting, paging, condition editing and detail navigation', async ({page}, info) => {
  await submit(page);
  await expect(page.locator('.toolbar-meta')).toContainText('25');
  await layout(page);
  await visual(page, info, 'results');
  for (const [name, sort, field, descending] of [
    ['저렴한 순','price,asc','price',false], ['비싼 순','price,desc','price',true],
    ['가벼운 순','weight,asc','weight',false], ['무거운 순','weight,desc','weight',true],
  ]) {
    await page.getByRole('button',{name,exact:true}).click();
    await expect(page.getByRole('button',{name,exact:true})).toHaveAttribute('aria-pressed','true');
    const values = field==='price' ? await page.locator('.tile-price').allTextContents() : await page.locator('.tile-spec:nth-child(2) .tile-value').allTextContents();
    const numbers=values.map(value=>Number(value.replace(/[^\d.]/g,'')));
    expect(numbers).toEqual([...numbers].sort((a,b)=>descending ? b-a : a-b));
    await expect(page.locator('.criteria-strip')).toContainText('1,500,000원');
    await page.getByRole('button',{name:'다음 페이지',exact:true}).click();
    await expect(page.locator('[aria-current="page"]')).toHaveText('2');
    await expect(page).toHaveURL(new RegExp(`page=1.*sort=${encodeURIComponent(sort).replace('%2C','(?:%2C|,)')}`));
    await expect(page.locator('#next-page input[name="budget"]')).toHaveValue('1500000');
    await expect(page.locator('#next-page input[name="maxWeightKg"]')).toHaveValue('1.5');
    await page.getByRole('button',{name:'이전 페이지',exact:true}).click();
    await expect(page.locator('[aria-current="page"]')).toHaveText('1');
  }
  await page.getByRole('button',{name:'추천순',exact:true}).click();
  await page.getByRole('button',{name:'조건 수정',exact:true}).click();
  await expect(page.locator('#budget')).toHaveValue('1500000');
  await expect(page.locator('#weight')).toHaveValue('1.5');
  await page.locator('#screenSizeMode_SELECT').check();
  await page.locator('#screenSize_14').check();
  await page.locator('#useCase_OFFICE_STUDY').check();
  await page.getByRole('button',{name:'추천받기',exact:true}).click();
  await page.getByRole('button',{name:'저렴한 순',exact:true}).click();
  await page.getByRole('button',{name:'다음 페이지',exact:true}).click();
  await expect(page.locator('#next-page input[name="screenSizes"]')).toHaveValue('14');
  await expect(page.locator('#next-page input[name="useCase"]')).toHaveValue('OFFICE_STUDY');
  await page.getByRole('button',{name:'조건 수정',exact:true}).click();
  await expect(page.locator('#screenSizeMode_SELECT')).toBeChecked();
  await expect(page.locator('#screenSize_14')).toBeChecked();
  await expect(page.locator('#useCase_OFFICE_STUDY')).toBeChecked();
  await page.getByRole('button',{name:'추천받기',exact:true}).click();
  await page.locator('.tile-title').first().click();
  await expect(page).toHaveURL(/\/laptops\/\d+$/);
  await expect(page.getByRole('heading',{name:'핵심 사양'})).toBeVisible();
});

test('detail: long product name, specifications and actions', async ({page}, info) => {
  await page.goto('/laptops/1');
  await expect(page.locator('h1')).toContainText('EnglishLongProductNameWithoutSpaces');
  await expect(page.locator('.detail-price')).toHaveText('810,000원');
  await expect(page.getByRole('link',{name:'다나와에서 보기'})).toHaveAttribute('rel','noopener noreferrer');
  await layout(page);
  await visual(page, info, 'detail');
});

test('missing: missing image, price and specifications', async ({page}, info) => {
  await page.goto('/laptops/26');
  await expect(page.locator('.detail-price')).toHaveText('가격 확인 불가');
  await expect(page.locator('[data-product-image]')).toHaveAttribute('src','/image-placeholder.svg');
  await expect(page.locator('.metric-value').first()).toHaveText('확인 불가');
  await layout(page);
  await visual(page, info, 'missing');
  await page.goto('/laptops/2');
  await page.locator('[data-product-image]').evaluate(node => node.src='/ui/unavailable-image.png');
  await expect(page.locator('[data-product-image]')).toHaveAttribute('src','/image-placeholder.svg');
  await layout(page);
});

test('empty: no recommendation matches', async ({page}, info) => {
  await submit(page, {budget:100000,weight:0.1});
  await expect(page.locator('.laptop-tile')).toHaveCount(0);
  await expect(page.locator('.empty-state')).toBeVisible();
  await layout(page);
  await visual(page, info, 'empty');
  await page.getByRole('button',{name:'조건 다시 조정하기',exact:true}).click();
  await expect(page.locator('#budget')).toHaveValue('100000');
  await expect(page.locator('#weight')).toHaveValue('0.1');
});

for (const [state, path, status] of [['not-found','/laptops/999999',404],['invalid','/laptops/invalid-id',400]]) {
  test(`error: ${state}`, async ({page}, info) => {
    const response=await page.goto(path);
    expect(response.status()).toBe(status);
    await expect(page.locator('.eyebrow').last()).toHaveText(String(status));
    await layout(page);
    await visual(page, info, state);
    await page.getByRole('link',{name:'추천으로 돌아가기'}).click();
    await expect(page.locator('#budget')).toHaveValue('1500000');
  });
}

test('stress: long reasons, URL, expanded comment forms and breakpoint edges', async ({page}, info) => {
  await submit(page);
  // Browser-only layout stress; this does not claim persisted recommendation data was replaced.
  await page.locator('.reason-pill').first().evaluate(node => node.textContent='긴 추천 이유를 줄바꿈하여 모두 읽을 수 있어야 합니다. '.repeat(12)+'https://example.invalid/'+ 'longurl'.repeat(25));
  await layout(page);
  await page.goto('/laptops/invalid-id');
  await page.locator('.section-lead').evaluate(node => node.textContent='긴 오류 안내도 끝까지 읽을 수 있어야 합니다. '.repeat(15)+'https://example.invalid/'+ 'longerror'.repeat(30));
  await layout(page);
  await page.goto('/laptops/1');
  if (info.project.use.viewport.width===1440) {
    for (const width of [599,600,899,900]) {
      await page.setViewportSize({width,height:900});
      await layout(page);
      await page.goto('/recommends');
      await layout(page);
      await submit(page);
      await layout(page);
      await page.goto('/laptops/1');
    }
  }
});
