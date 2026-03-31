package going9.laptopgg.service.crawler

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.Request
import com.microsoft.playwright.options.WaitUntilState
import org.slf4j.Logger
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque

internal class DanawaListBrowserSession(
    private val crawlSource: CrawlerService.CrawlSource,
    private val logger: Logger,
    private val userAgent: String,
) : AutoCloseable {
    private val playwright = Playwright.create()
    private val browser = playwright.chromium().launch(
        BrowserType.LaunchOptions()
            .setHeadless(true)
            .setArgs(listOf("--disable-blink-features=AutomationControlled")),
    )
    private val context = browser.newContext(
        Browser.NewContextOptions()
            .setUserAgent(userAgent)
            .setLocale("ko-KR")
            .setTimezoneId("Asia/Seoul")
            .setViewportSize(1440, 2200),
    )
    private val page = context.newPage()
    private val recentListRequests = ArrayDeque<ListRequestTrace>()
    private var initialized = false
    private var currentPageNumber = 0
    private var currentHtml: String? = null

    init {
        page.onRequest { request ->
            if (request.url().contains("getProductList.ajax.php")) {
                recentListRequests.addLast(ListRequestTrace.from(request))
                while (recentListRequests.size > MAX_STORED_LIST_REQUESTS) {
                    recentListRequests.removeFirst()
                }
            }
        }
    }

    fun fetchPageHtml(targetPage: Int): String {
        require(targetPage >= 1) { "page must be >= 1" }

        ensureInitialized()

        if (targetPage < currentPageNumber) {
            reopenSourcePage()
        }

        if (targetPage != currentPageNumber) {
            moveToPage(targetPage)
            currentPageNumber = targetPage
            currentHtml = null
        }

        return currentHtml ?: page.content().also { currentHtml = it }
    }

    fun latestListRequestTrace(): ListRequestTrace? = recentListRequests.lastOrNull()

    private fun ensureInitialized() {
        if (initialized) {
            return
        }

        reopenSourcePage()
        initialized = true
    }

    private fun reopenSourcePage() {
        page.navigate(
            crawlSource.listUrl,
            Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED),
        )
        waitForListReady()

        if (crawlSource.attributeFilters.isNotEmpty()) {
            applyAttributeFilters(crawlSource.attributeFilters)
        }

        applySortMethod("MinPrice")

        currentPageNumber = 1
        currentHtml = page.content()
    }

    private fun applyAttributeFilters(filters: List<CrawlerAttributeFilter>) {
        page.evaluate(
            """
            () => {
              document.querySelector('#searchOptionAll')?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
              document.querySelectorAll('.btn_spec_view.btn_view_more').forEach((button) => {
                if (button instanceof HTMLElement) {
                  button.click();
                }
              });
            }
            """.trimIndent(),
        )
        page.waitForTimeout(300.0)

        filters.forEach { filter ->
            val clicked = page.evaluate(
                """
                (value) => {
                  const inputs = Array.from(
                    document.querySelectorAll('input[name="searchAttributeValueRep[]"], input[name="searchAttributeValue[]"]')
                  );
                  const matchingInputs = inputs.filter((input) => input.value === value);
                  const isVisible = (element) => {
                    if (!(element instanceof HTMLElement)) {
                      return false;
                    }
                    const style = window.getComputedStyle(element);
                    const rect = element.getBoundingClientRect();
                    return style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0;
                  };
                  const visibleInput = matchingInputs.find((input) => isVisible(input));
                  const preferred = matchingInputs.find((input) => input.name === 'searchAttributeValueRep[]');
                  const target = visibleInput ?? preferred ?? matchingInputs[0];
                  if (!target) {
                    return false;
                  }
                  if (target.checked) {
                    return true;
                  }

                  const candidates = [];
                  if (target.id) {
                    candidates.push(document.querySelector(`label[for="${'$'}{target.id}"]`));
                  }
                  candidates.push(target.closest('label'));
                  candidates.push(target.closest('li')?.querySelector('label'));
                  candidates.push(target.closest('dd')?.querySelector('label'));
                  const clickable = candidates.find((candidate) => candidate instanceof HTMLElement && isVisible(candidate));
                  if (clickable instanceof HTMLElement) {
                    clickable.click();
                  } else if (typeof target.click === 'function') {
                    target.click();
                  } else {
                    target.dispatchEvent(new MouseEvent('click', { bubbles: true }));
                  }
                  return true;
                }
                """.trimIndent(),
                filter.value,
            ) as? Boolean ?: false

            if (!clicked) {
                throw IllegalStateException("CPU 코드명 필터를 브라우저 세션에서 찾지 못했습니다: ${filter.name} / ${filter.value}")
            }

            page.waitForFunction(
                """
                (value) => {
                  const inputs = Array.from(
                    document.querySelectorAll('input[name="searchAttributeValueRep[]"], input[name="searchAttributeValue[]"]')
                  );
                  return inputs.some((input) => input.value === value && input.checked);
                }
                """.trimIndent(),
                filter.value,
                Page.WaitForFunctionOptions().setTimeout(PAGE_TRANSITION_TIMEOUT_MILLIS.toDouble()),
            )
            waitForListReady()
        }

        normalizeSelectedAttributeFilters()

        logger.info(
            "브라우저 세션에서 목록 필터를 적용했습니다. source={}, filterCount={}",
            crawlSource.key,
            filters.size,
        )
    }

    private fun normalizeSelectedAttributeFilters() {
        page.evaluate(
            """
            () => {
              const inputs = Array.from(
                document.querySelectorAll('input[name="searchAttributeValueRep[]"], input[name="searchAttributeValue[]"]')
              );
              const isVisible = (element) => {
                if (!(element instanceof HTMLElement)) {
                  return false;
                }
                const style = window.getComputedStyle(element);
                const rect = element.getBoundingClientRect();
                return style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0;
              };

              const groups = new Map();
              inputs.forEach((input) => {
                if (!groups.has(input.value)) {
                  groups.set(input.value, []);
                }
                groups.get(input.value).push(input);
              });

              groups.forEach((group) => {
                const checkedInputs = group.filter((input) => input.checked);
                if (checkedInputs.length <= 1) {
                  return;
                }
                const keeper =
                  checkedInputs.find((input) => isVisible(input)) ??
                  checkedInputs.find((input) => input.name === 'searchAttributeValueRep[]') ??
                  checkedInputs[0];
                checkedInputs.forEach((input) => {
                  if (input !== keeper) {
                    input.checked = false;
                  }
                });
              });

              if (Array.isArray(window.previousSearchOption)) {
                window.previousSearchOption = Array.from(new Set(window.previousSearchOption));
              }

              if (window.oHistoryOptionCommand && Array.isArray(window.oHistoryOptionCommand.searchOption)) {
                window.oHistoryOptionCommand.searchOption = Array.from(new Set(window.oHistoryOptionCommand.searchOption));
              }

              if (window.oHistoryOptionCommand && Array.isArray(window.oHistoryOptionCommand.param)) {
                const seen = new Set();
                window.oHistoryOptionCommand.param = window.oHistoryOptionCommand.param.filter((entry) => {
                  if (typeof entry !== 'string' || !entry.startsWith('searchAttributeValue[]=')) {
                    return true;
                  }
                  if (seen.has(entry)) {
                    return false;
                  }
                  seen.add(entry);
                  return true;
                });
              }
            }
            """.trimIndent(),
        )
    }

    private fun moveToPage(targetPage: Int) {
        var navigationStep = 0
        while (navigationStep < MAX_NAVIGATION_STEPS) {
            navigationStep++
            val navigationState = readNavigationState()
            if (navigationState.currentPage == targetPage) {
                return
            }

            if (navigationState.visiblePages.contains(targetPage)) {
                clickVisiblePage(targetPage)
                return
            }

            val minVisiblePage = navigationState.visiblePages.minOrNull()
            val maxVisiblePage = navigationState.visiblePages.maxOrNull()

            when {
                maxVisiblePage != null && targetPage > maxVisiblePage && navigationState.hasNextButton -> {
                    clickEdgeNavigation(".num_nav_wrap .edge_nav.nav_next", maxVisiblePage + 1)
                }
                minVisiblePage != null && targetPage < minVisiblePage && navigationState.hasPrevButton -> {
                    clickEdgeNavigation(".num_nav_wrap .edge_nav.nav_prev", (minVisiblePage - 1).coerceAtLeast(1))
                }
                else -> {
                    break
                }
            }
        }

        val previousSignature = pageSignature()
        val moved = page.evaluate(
            """
            (targetPage) => {
              const numberedLink = Array.from(document.querySelectorAll('.num_nav_wrap .number_wrap a.num'))
                .find((anchor) => anchor.textContent?.trim() === String(targetPage));
              if (numberedLink instanceof HTMLElement) {
                numberedLink.click();
                return true;
              }

              if (typeof movePage === 'function') {
                movePage(targetPage);
                return true;
              }

              return false;
            }
            """.trimIndent(),
            targetPage,
        ) as? Boolean ?: false

        if (!moved) {
            throw IllegalStateException("브라우저에서 목록 페이지를 이동할 수 없습니다. source=${crawlSource.key}, page=$targetPage")
        }

        waitForPageTransition(targetPage, previousSignature)
    }

    private fun clickVisiblePage(targetPage: Int) {
        val previousSignature = pageSignature()
        val moved = page.evaluate(
            """
            (targetPage) => {
              const numberedLink = Array.from(document.querySelectorAll('.num_nav_wrap .number_wrap a.num'))
                .find((anchor) => anchor.textContent?.trim() === String(targetPage));
              if (!(numberedLink instanceof HTMLElement)) {
                return false;
              }
              numberedLink.click();
              return true;
            }
            """.trimIndent(),
            targetPage,
        ) as? Boolean ?: false

        if (!moved) {
            throw IllegalStateException("브라우저에서 가시 페이지 링크를 클릭할 수 없습니다. source=${crawlSource.key}, page=$targetPage")
        }

        waitForPageTransition(targetPage, previousSignature)
    }

    private fun clickEdgeNavigation(selector: String, expectedPage: Int) {
        val previousSignature = pageSignature()
        val moved = page.evaluate(
            """
            (selector) => {
              const button = document.querySelector(selector);
              if (!(button instanceof HTMLElement)) {
                return false;
              }
              button.click();
              return true;
            }
            """.trimIndent(),
            selector,
        ) as? Boolean ?: false

        if (!moved) {
            throw IllegalStateException("브라우저에서 목록 블록 네비게이션을 클릭할 수 없습니다. source=${crawlSource.key}, selector=$selector")
        }

        waitForPageTransition(expectedPage, previousSignature)
    }

    private fun waitForPageTransition(expectedPage: Int, previousSignature: String) {
        waitForAjaxIdle()
        page.waitForFunction(
            """
            ([expectedPage, previousSignature]) => {
              const currentPage = document.querySelector('.num_nav_wrap .number_wrap .now_on')?.textContent?.trim() ?? '';
              const signature = Array.from(document.querySelectorAll('li.prod_item a[name="productName"]'))
                .map((anchor) => anchor.getAttribute('href') ?? '')
                .join('||');
              return currentPage === String(expectedPage) || signature !== previousSignature;
            }
            """.trimIndent(),
            listOf(expectedPage, previousSignature),
            Page.WaitForFunctionOptions().setTimeout(PAGE_TRANSITION_TIMEOUT_MILLIS.toDouble()),
        )
        waitForListReady()
    }

    private fun waitForListReady() {
        page.waitForSelector(
            "li.prod_item",
            Page.WaitForSelectorOptions().setTimeout(PAGE_TRANSITION_TIMEOUT_MILLIS.toDouble()),
        )
        waitForAjaxIdle()
        page.waitForTimeout(120.0)
    }

    private fun waitForAjaxIdle() {
        page.waitForFunction(
            "typeof window.jQuery === 'undefined' || window.jQuery.active === 0",
            null,
            Page.WaitForFunctionOptions().setTimeout(PAGE_TRANSITION_TIMEOUT_MILLIS.toDouble()),
        )
    }

    private fun applySortMethod(sortMethod: String) {
        val currentSort = page.evaluate(
            """
            () => typeof getSortMethod === 'function' ? getSortMethod() : null
            """.trimIndent(),
        ) as? String

        if (currentSort == sortMethod) {
            return
        }

        val previousSignature = pageSignature()
        val clicked = page.evaluate(
            """
            (sortMethod) => {
              const anchor = document.querySelector(`li.order_item[data-sort-method="${'$'}{sortMethod}"] a`);
              if (!(anchor instanceof HTMLElement)) {
                return false;
              }
              anchor.click();
              return true;
            }
            """.trimIndent(),
            sortMethod,
        ) as? Boolean ?: false

        if (!clicked) {
            logger.warn("브라우저 세션에서 목록 정렬을 찾지 못했습니다. source={}, sortMethod={}", crawlSource.key, sortMethod)
            return
        }

        waitForPageTransition(1, previousSignature)
    }

    private fun pageSignature(): String {
        return page.evaluate(
            """
            () => Array.from(document.querySelectorAll('li.prod_item a[name="productName"]'))
              .map((anchor) => anchor.getAttribute('href') ?? '')
              .join('||')
            """.trimIndent(),
        ) as? String ?: ""
    }

    private fun readNavigationState(): NavigationState {
        return page.evaluate(
            """
            () => {
              const visiblePages = Array.from(document.querySelectorAll('.num_nav_wrap .number_wrap a.num'))
                .map((anchor) => Number.parseInt(anchor.textContent?.trim() ?? '', 10))
                .filter((value) => Number.isFinite(value));
              const currentPage = Number.parseInt(
                document.querySelector('.num_nav_wrap .number_wrap .now_on')?.textContent?.trim() ?? '',
                10,
              );
              return {
                visiblePages,
                currentPage: Number.isFinite(currentPage) ? currentPage : null,
                hasNextButton: document.querySelector('.num_nav_wrap .edge_nav.nav_next') !== null,
                hasPrevButton: document.querySelector('.num_nav_wrap .edge_nav.nav_prev') !== null,
              };
            }
            """.trimIndent(),
        )?.let { raw ->
            @Suppress("UNCHECKED_CAST")
            val map = raw as Map<String, Any?>
            NavigationState(
                visiblePages = (map["visiblePages"] as? List<*>)?.mapNotNull {
                    when (it) {
                        is Number -> it.toInt()
                        is String -> it.toIntOrNull()
                        else -> null
                    }
                } ?: emptyList(),
                currentPage = when (val value = map["currentPage"]) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull()
                    else -> null
                },
                hasNextButton = map["hasNextButton"] as? Boolean ?: false,
                hasPrevButton = map["hasPrevButton"] as? Boolean ?: false,
            )
        } ?: NavigationState()
    }

    private data class NavigationState(
        val visiblePages: List<Int> = emptyList(),
        val currentPage: Int? = null,
        val hasNextButton: Boolean = false,
        val hasPrevButton: Boolean = false,
    )

    data class ListRequestTrace(
        val page: Int?,
        val sortMethod: String?,
        val filterValueCount: Int,
        val distinctFilterValueCount: Int,
        val rawPostData: String?,
    ) {
        companion object {
            fun from(request: Request): ListRequestTrace {
                val postData = request.postData()
                val pairs = parseFormBody(postData)
                val filterValues = pairs.filter { it.first == "searchAttributeValue[]" }.map { it.second }
                return ListRequestTrace(
                    page = pairs.firstOrNull { it.first == "page" }?.second?.toIntOrNull(),
                    sortMethod = pairs.firstOrNull { it.first == "sortMethod" }?.second,
                    filterValueCount = filterValues.size,
                    distinctFilterValueCount = filterValues.toSet().size,
                    rawPostData = postData,
                )
            }

            private fun parseFormBody(postData: String?): List<Pair<String, String>> {
                if (postData.isNullOrBlank()) {
                    return emptyList()
                }

                return postData.split("&")
                    .mapNotNull { part ->
                        if (part.isBlank()) {
                            return@mapNotNull null
                        }

                        val delimiterIndex = part.indexOf('=')
                        val rawKey = if (delimiterIndex >= 0) part.substring(0, delimiterIndex) else part
                        val rawValue = if (delimiterIndex >= 0) part.substring(delimiterIndex + 1) else ""
                        URLDecoder.decode(rawKey, StandardCharsets.UTF_8) to
                            URLDecoder.decode(rawValue, StandardCharsets.UTF_8)
                    }
            }
        }
    }

    override fun close() {
        runCatching { page.close() }
        runCatching { context.close() }
        runCatching { browser.close() }
        runCatching { playwright.close() }
    }

    companion object {
        private const val PAGE_TRANSITION_TIMEOUT_MILLIS = 20_000L
        private const val MAX_NAVIGATION_STEPS = 100
        private const val MAX_STORED_LIST_REQUESTS = 32
    }
}
