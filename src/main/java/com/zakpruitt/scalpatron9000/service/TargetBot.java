package com.zakpruitt.scalpatron9000.service;

import com.microsoft.playwright.*;
import com.zakpruitt.scalpatron9000.model.ProductTarget;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
public class TargetBot {

    private static final Path STORAGE_PATH = Paths.get("src/main/resources/target-storage.json");

    private static final List<String> DESKTOP_UAS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.91 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.91 Safari/537.36"
    );

    private static final int DEFAULT_TIMEOUT_MS = 25_000;
    private static final String ORDER_CONF_URL_PART = "/order-confirmation";

    @Value("${scalper.place-orders}")
    private boolean placeOrders;

    private Playwright playwright;
    private BrowserContext ctx;

    @PostConstruct
    public void init() {
        playwright = Playwright.create();

        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setArgs(List.of(
                        "--disable-blink-features=AutomationControlled",
                        "--lang=en-US,en;q=0.9",
                        "--window-size=1920,1080"
                )));

        Browser.NewContextOptions opt = new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setLocale("en-US")
                .setUserAgent(randomDesktopUA());

        if (Files.exists(STORAGE_PATH)) {
            opt.setStorageStatePath(STORAGE_PATH);
        }

        ctx = browser.newContext(opt);
        injectHeadersAndStealthPatches(ctx);

        ctx.setDefaultTimeout(DEFAULT_TIMEOUT_MS);
        ctx.setDefaultNavigationTimeout(DEFAULT_TIMEOUT_MS);

        log.info("✅ TargetBot ready in stealth mode");
    }

    public boolean detectAndBuy(ProductTarget t) {
        Page page = ctx.newPage();
        try {
            /* 1) PDP – check live price + stock -------------------------------- */
            page.navigate(t.url());

            String priceTxt = page.textContent("span[data-test='product-price']");
            double price = Double.parseDouble(priceTxt.replaceAll("[^0-9.]", ""));

            boolean inStock = page.isVisible("button[aria-label^='Add to cart']") &&
                    page.isEnabled("button[aria-label^='Add to cart']");

            log.info("[{}] price=${}, inStock={} (max ${})", t.alias(), price, inStock, t.maxUnitPrice());
            if (!inStock || price > t.maxUnitPrice()) return false;

            /* 2) Quantity tweaks ------------------------------------------------ */
            int wanted = Math.min(t.maxQuantity(), 3);
            if (wanted > 1) {
                page.click("button:has-text('Qty')");
                page.click("ul.Options_styles_options__TK_Sd a[aria-label='" + wanted + "']");
                page.waitForTimeout(500);
            }

            /* 3) Cart → Checkout ------------------------------------------------ */
            page.click("button[aria-label^='Add to cart']");
            page.waitForTimeout(800);
            page.click("a[href='/cart']");
            page.click("button[data-test='checkout-button']");

            /* 4) Final place‑order click (optional) ---------------------------- */
            page.waitForSelector("button[data-test='placeOrderButton']");
            if (!placeOrders) {
                log.warn("🧪 TEST‑MODE: reached Place‑Order screen – skipping final click");
                return true;
            }

            for (int attempt = 1; attempt <= 3; attempt++) {
                page.click("button[data-test='placeOrderButton']");

                // Wait until we land on the order‑confirmation page or timeout.
                boolean confirmed = waitForOrderConfirmation(page);
                if (confirmed) {
                    log.warn("✅ ORDER PLACED for {} (URL={})", t.alias(), page.url());
                    return true;
                }

                log.warn("⚠️  place‑order attempt {} failed – retrying", attempt);
                closeAnyModal(page);
            }

            log.error("❌ Could not place order after 3 attempts for {}", t.alias());
            return false;
        } catch (PlaywrightException ex) {
            log.error("❌ Checkout flow crashed: {}", ex.getMessage(), ex);
            return false;
        } finally {
            page.close();
        }
    }

    private boolean waitForOrderConfirmation(Page page) {
        try {
            page.waitForURL("**" + ORDER_CONF_URL_PART + "*", new Page.WaitForURLOptions().setTimeout(15_000));
            return true;
        } catch (PlaywrightException ignored) {
            return false;
        }
    }

    private void closeAnyModal(Page page) {
        Locator closeBtn = page.locator("button[aria-label='Close'],button[data-icon='close']");
        if (closeBtn.isVisible()) closeBtn.first().click();
        page.waitForTimeout(1_000);
    }

    private static String randomDesktopUA() {
        return DESKTOP_UAS.get(ThreadLocalRandom.current().nextInt(DESKTOP_UAS.size()));
    }

    private void injectHeadersAndStealthPatches(BrowserContext ctx) {
        ctx.setExtraHTTPHeaders(Map.of(
                "Accept-Language", "en-US,en;q=0.9",
                "User-Agent", randomDesktopUA(),
                "sec-ch-ua-platform", "\"Windows\""
        ));

        ctx.addInitScript("""
                Object.defineProperty(navigator,'webdriver',{get:()=>undefined});
                window.chrome = { runtime: {} };
                Object.defineProperty(navigator,'plugins',{get: () => [1,2,3,4,5]});
                Object.defineProperty(navigator,'mimeTypes',{get: () => [1,2,3]});
                Object.defineProperty(navigator,'languages',{get: () => ['en-US','en']});
                Object.defineProperty(navigator,'hardwareConcurrency',{get: () => 8});
                Intl.DateTimeFormat.prototype.resolvedOptions = () => ({ timeZone:'America/Chicago' });
                const getParameter = WebGLRenderingContext.prototype.getParameter;
                WebGLRenderingContext.prototype.getParameter = function(param){
                   if(param === 37445) return 'Intel Inc.';
                   if(param === 37446) return 'Intel Iris OpenGL';
                   return getParameter.call(this, param);
                };
                const permQuery = navigator.permissions.query;
                navigator.permissions.query = args =>
                     args.name === 'notifications' ?
                         Promise.resolve({ state: Notification.permission }) :
                         permQuery(args);
            """);
    }
}
