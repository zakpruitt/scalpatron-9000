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
            new ProductTarget("Target 151 Bundles",
                    "https://www.target.com/p/pokemon-scarlet-violet-s3-5-booster-bundle-box/-/A-88897904",
                    40.00, 3),
            new ProductTarget("Target Prismatic Surprise Box",
                    "https://www.target.com/p/pokemon-scarlet-violet-s3-5-booster-bundle-box/-/A-88897904",
                    40.00, 3),
            new ProductTarget("test box",
                    "https://www.target.com/p/pokemon-tcg-pokemon-go-trading-card-booster-pack/-/A-1001539813",
                    20.99, 1)
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
