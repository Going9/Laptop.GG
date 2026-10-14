const { test, expect } = require('@playwright/test');
const { layout, visual, submit } = require('./layout');

test.use({trace:'off'});

test.beforeEach(async ({context}) => {
  // Every page is real server-rendered HTML. Block unplanned external browser traffic.
  await context.route('**/*', route => new URL(route.request().url()).hostname === '127.0.0.1' ? route.continue() : route.abort());
});

test('comments: actual isolated DB create, wrong password, edit and delete', async ({page}, info) => {
  const widths=[360,375,390,430,768,1440];
  const id=2+widths.indexOf(info.project.use.viewport.width)+(info.project.use.browserName==='webkit'?6:0);
  const secret=require('node:crypto').randomBytes(24).toString('hex');
  const initialContent=('합성 댓글: 사용 경험을 남깁니다. '+'긴 내용 '.repeat(40)).padEnd(255,'가');
  const updatedContent=('수정된 합성 댓글 https://example.invalid/'+ 'long-path-'.repeat(12)).padEnd(255,'가');
  const author=`UI ${info.project.name}`;
  await page.goto(`/laptops/${id}`);
  const rejected=await page.request.post('/comments', {form:{laptopId:id,author,passWord:secret,content:'가'.repeat(256)}});
  expect(rejected.status()).toBe(400);
  await page.reload();
  await expect(page.locator('.comment-item')).toHaveCount(0);
  const form=page.locator('.comment-form');
  await form.getByLabel('작성자',{exact:true}).fill(author);
  await form.getByLabel('비밀번호',{exact:true}).fill(secret);
  await form.getByLabel('내용 (최대 255자)',{exact:true}).fill(initialContent+'초과');
  await expect(form.getByLabel('내용 (최대 255자)',{exact:true})).toHaveValue(initialContent);
  await form.getByRole('button',{name:'댓글 남기기'}).click();
  const comment=page.locator('.comment-item').filter({hasText:author});
  await expect(comment).toHaveCount(1);
  await expect(comment.locator('.comment-copy p')).toHaveText(initialContent);
  const editAction=await comment.locator('.comment-inline-form').getAttribute('action');
  const rejectedEdit=await page.request.post(editAction,{form:{passWord:secret,content:'나'.repeat(256)}});
  expect(rejectedEdit.status()).toBe(400);
  await page.reload();
  await expect(comment.locator('.comment-copy p')).toHaveText(initialContent);
  await comment.locator('summary').filter({hasText:'수정'}).click();
  await expect(comment.locator('.comment-inline-form')).toBeVisible();
  await comment.locator('summary').filter({hasText:'수정'}).focus();
  await page.keyboard.press('Enter');
  await expect(comment.locator('.comment-inline-form')).toBeHidden();
  await page.keyboard.press('Enter');
  await expect(comment.locator('.comment-inline-form')).toBeVisible();
  await layout(page);
  await visual(page, info, 'comment-edit');
  await comment.getByLabel('수정할 내용').fill(updatedContent);
  await comment.locator('.comment-inline-form').getByLabel('댓글 비밀번호').fill(secret+'wrong');
  await comment.getByRole('button',{name:'수정 저장'}).click();
  await expect(page.locator('.eyebrow').last()).toHaveText('403');
  await layout(page);
  await visual(page, info, 'forbidden');
  await page.goto(`/laptops/${id}`);
  await expect(comment.locator('.comment-copy')).toContainText('사용 경험을');
  await comment.locator('summary').filter({hasText:'수정'}).click();
  await comment.getByLabel('수정할 내용').fill(updatedContent);
  await comment.locator('.comment-inline-form').getByLabel('댓글 비밀번호').fill(secret);
  await comment.getByRole('button',{name:'수정 저장'}).click();
  await expect(comment.locator('.comment-copy')).toContainText('수정된 합성 댓글');
  await page.reload();
  await expect(comment.locator('.comment-copy p')).toHaveText(updatedContent);
  await comment.locator('summary').filter({hasText:'삭제'}).click();
  await expect(comment.locator('.comment-delete-form')).toBeVisible();
  await layout(page);
  await comment.locator('.comment-delete-form').getByLabel('댓글 비밀번호').fill(secret);
  await comment.getByRole('button',{name:'삭제하기'}).click();
  await expect(comment).toHaveCount(0);
  await page.reload();
  await expect(page.locator('.comment-empty')).toBeVisible();
});
