package com.zakpruitt.scalpatron9000.service;

import com.zakpruitt.scalpatron9000.model.ProductTarget;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class ScalperService {

    private final List<ProductTarget> targets = List.of(
            new ProductTarget("Target Prismatic ETB",
                    "https://www.target.com/p/2024-pok-scarlet-violet-s8-5-elite-trainer-box/-/A-93954435",
                    65.00, 3),
            new ProductTarget("Target Prismatic Bundles",
                    "https://www.target.com/p/-/A-93954446?clkid=37d4c6cbN146111f0934c0f6e18e47423",
                    40.00, 3),
            new ProductTarget("Target Prismatic Surprise Box",
                    "https://www.target.com/p/pokenotifyy/-/A-94336414",
                    30.00, 3),

            new ProductTarget("Target 151 Bundles",
                    "https://www.target.com/p/pokemon-scarlet-violet-s3-5-booster-bundle-box/-/A-88897904",
                    40.00, 3),

            new ProductTarget("Target Destined Rivals ETB",
                    "https://www.target.com/p/pok-233-mon-trading-card-game-scarlet-38-violet-8212-destined-rivals-elite-trainer-box/-/A-94300069",
                    65.00, 3),
            new ProductTarget("Target Destined Rivals Bundles",
                    "https://www.target.com/p/pok-233-mon-trading-card-game-scarlet-38-violet-8212-destined-rivals-booster-bundle/-/A-94300067",
                    40.00, 3),
            new ProductTarget("Target Destined Rivals Zebstrike Three-Pack Blister",
                    "https://www.target.com/p/-/A-94300073?clkid=5257c197N1e5d11f09f8fa94ab4ff40a0&cpng=&lnm=81938&afid=Mavely&ref=tgt_adv_xasd0002",
                    30.00, 3),
            new ProductTarget("Target Destined Rivals Kangaskhan Three-Pack Blister",
                    "https://www.target.com/p/-/A-94300082?clkid=a06321b5N1db611f09fcf85a34189b0ca",
                    30.00, 3)
    );

    private final AtomicBoolean busy = new AtomicBoolean(false);
    @Autowired
    private TargetBot targetBot;

    @Scheduled(fixedDelayString = "${scalper.delay}")
    public void scanAndBuy() {
        if (!busy.compareAndSet(false, true)) {
            log.debug("⏳ purchase in progress – skipping this tick");
            return;
        }

        try {
            for (ProductTarget t : targets) {
                if (targetBot.detectAndBuy(t)) {
                    break;
                }
            }
        } finally {
            busy.set(false);
        }
    }
}
