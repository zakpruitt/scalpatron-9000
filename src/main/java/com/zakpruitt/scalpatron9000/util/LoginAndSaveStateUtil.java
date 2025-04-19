package com.zakpruitt.scalpatron9000.util;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.nio.file.Paths;

public class LoginAndSaveStateUtil {
    public static void run(String profileDir, String stateJsonPath) {
        try (Playwright pw = Playwright.create()) {
            BrowserType chromium = pw.chromium();
            BrowserContext ctx = chromium.launchPersistentContext(
                    Paths.get(profileDir),
                    new BrowserType.LaunchPersistentContextOptions()
                            .setHeadless(false)
                            .setDevtools(true)
                            .setViewportSize(1280, 800)
            );

            Page page = ctx.newPage();
            page.navigate("https://www.google.com");

            System.out.println("""
                    🔐  Please log in manually in the opened browser.
                       – Enter your credentials
                       – Complete any CAPTCHA / “prove you’re human”
                       – Once you see your avatar in the header, come back here and press ENTER:
                    """);
            System.in.read();

            ctx.storageState(
                    new BrowserContext.StorageStateOptions()
                            .setPath(Paths.get(stateJsonPath))
            );
            System.out.println("✅  storageState saved to “" + stateJsonPath + "”");

            ctx.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
